package com.app.vwflashtools

import android.app.*
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore


// Header we expect to receive on BLE packets
class BLEHeader {
    var hdID: Int = BLE_HEADER_ID
    var cmdFlags: Int = 0
    var rxID: Int = BLE_HEADER_RX
    var txID: Int = BLE_HEADER_TX
    var cmdSize: Int = 0
    var tickCount: Int = 0

    fun isValid(): Boolean {
        return hdID == BLE_HEADER_ID
    }

    fun toByteArray(): ByteArray {
        val bArray = ByteArray(8)
        bArray[0] = (hdID and 0xFF).toByte()
        bArray[1] = (cmdFlags and 0xFF).toByte()
        bArray[2] = (rxID and 0xFF).toByte()
        bArray[3] = ((rxID and 0xFF00) shr 8).toByte()
        bArray[4] = (txID and 0xFF).toByte()
        bArray[5] = ((txID and 0xFF00) shr 8).toByte()
        bArray[6] = (cmdSize and 0xFF).toByte()
        bArray[7] = ((cmdSize and 0xFF00) shr 8).toByte()

        return bArray
    }

    fun fromByteArray(bArray: ByteArray) {
        hdID = bArray[0] and 0xFF
        cmdFlags = bArray[1] and 0xFF
        rxID = ((bArray[3] and 0xFF) shl 8) + (bArray[2] and 0xFF)
        txID = ((bArray[5] and 0xFF) shl 8) + (bArray[4] and 0xFF)
        cmdSize = ((bArray[7] and 0xFF) shl 8) + (bArray[6] and 0xFF)
        tickCount = ((rxID  and 0xFFFF) shl 16) + (txID  and 0xFFFF)
    }
}

class BTService: Service() {
    //constants
    val TAG = "BTService"

    // Member fields
    private var mScanning: Boolean = false
    private var mState: Int = STATE_NONE
    private var mErrorStatus: String = ""
    private val mWriteSemaphore: Semaphore = Semaphore(1)
    private val mReadQueue = ConcurrentLinkedQueue<ByteArray>()
    private val mWriteQueue = ConcurrentLinkedQueue<ByteArray>()
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDevice: BluetoothDevice? = null
    private var mConnectionThread: ConnectionThread? = null
    private var mLogWriteState: Boolean = false

    //Gatt additional properties
    private fun BluetoothGattCharacteristic.isReadable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)
    private fun BluetoothGattCharacteristic.isWritable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)
    private fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
    private fun BluetoothGattCharacteristic.isIndicatable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)
    private fun BluetoothGattCharacteristic.isNotifiable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean = properties and property != 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent.action) {
            BT_START_SERVICE.toString() -> {
                doStartService()
            }
            BT_STOP_SERVICE.toString() -> {
                doStopService()
            }
            BT_DO_CONNECT.toString() -> {
                doConnect()
            }
            BT_DO_DISCONNECT.toString() -> {
                doDisconnect()
            }
            BT_DO_CHECK_VIN.toString() -> {
                mConnectionThread?.setTaskState(TASK_RD_VIN)
            }
            BT_DO_CHECK_PID.toString() -> {
                mConnectionThread?.setTaskState(TASK_LOGGING)
            }
            BT_DO_STOP_PID.toString() -> {
                mConnectionThread?.setTaskState(TASK_NONE)
            }
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        doDisconnect()
    }

    private val mScanCallback = object : ScanCallback() {
        val TAG = "mScanCallback"

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            result.device?.let { device ->
                Log.i(TAG, "Found BLE device! ${device.name}")

                if (mBluetoothDevice == null && device.name.contains(BLE_DEVICE_NAME, true)) {
                    mBluetoothDevice = device

                    if (mScanning)
                        stopScanning()

                    Log.i(TAG, "Initiating connection to ${device.name}")
                    device.connectGatt(applicationContext, false, mGattCallback, 2)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed: code $errorCode")
        }
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        val TAG = "BTGATTCallback"

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG, "Successfully connected to $deviceAddress")

                    //made connection, store our gatt
                    mBluetoothGatt = gatt

                    try {
                        //discover gatt table
                        mBluetoothGatt?.let { newGatt ->
                            Handler(Looper.getMainLooper()).post {
                                newGatt.discoverServices()
                            }
                        } ?: error("Gatt is invalid")
                    } catch (e: Exception) {
                        Log.e(TAG,"Exception requesting to discover services", e)
                        doDisconnect()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "Successfully disconnected from $deviceAddress")

                    //disable the read notification
                    disableNotifications(gatt.getService(BLE_SERVICE_UUID).getCharacteristic(BLE_DATA_RX_UUID))

                    //If gatt doesn't match ours make sure we close it
                    if(gatt != mBluetoothGatt) {
                        gatt.close()
                    }

                    //Do a full disconnect
                    doDisconnect()
                }
            } else {
                Log.w(TAG, "Error $status encountered for $deviceAddress! Disconnecting...")

                //If gatt doesn't match ours make sure we close it
                if(gatt != mBluetoothGatt) {
                    gatt.close()
                }

                //Set new connection error state
                mErrorStatus = status.toString()

                //Do a full disconnect
                doDisconnect(STATE_ERROR, true)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if(status == BluetoothGatt.GATT_SUCCESS) {
                with(gatt) {
                    Log.w(TAG, "Discovered ${services.size} services for ${device.address}")
                    printGattTable()
                    try {
                        mBluetoothGatt?.requestMtu(BLE_GATT_MTU_SIZE) ?: error("Gatt is invalid")
                    } catch (e: Exception) {
                        Log.e(TAG,"Exception while discovering services", e)
                        doDisconnect()
                    }
                }
            } else {
                //If gatt doesn't match ours make sure we close it
                if(gatt != mBluetoothGatt) {
                    gatt.close()
                }

                //Set new connection error state
                mErrorStatus = status.toString()

                //Do a full disconnect
                doDisconnect(STATE_ERROR, true)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.w(TAG,"ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")

            if(status == BluetoothGatt.GATT_SUCCESS) {
                //Set new connection state
                setConnectionState(STATE_CONNECTED)
                try {
                    mBluetoothGatt?.let { ourGatt ->
                        ourGatt.requestConnectionPriority(BLE_CONNECTION_PRIORITY)
                        enableNotifications(ourGatt.getService(BLE_SERVICE_UUID)!!.getCharacteristic(BLE_DATA_RX_UUID))
                    } ?: error("Gatt is invalid")
                } catch (e: Exception) {
                    Log.e(TAG,"Exception setting mtu", e)
                    doDisconnect()
                }
            } else {
                //If gatt doesn't match ours make sure we close it
                if(gatt != mBluetoothGatt) {
                    gatt.close()
                }

                //Set new connection error state
                mErrorStatus = status.toString()

                //Do a full disconnect
                doDisconnect(STATE_ERROR, true)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.d("onDescWrite", "success ${descriptor.toString()}")
                }
                else -> {
                    Log.d("onDescWrite", "failed ${descriptor.toString()}")
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(TAG, "Read characteristic $uuid:\n${value}")
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e(TAG, "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(TAG, "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid | value: $value")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }
            mWriteSemaphore.release()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: $value")

                //parse packet and check for multiple responses
                val bleHeader = BLEHeader()
                while(value.count() > 0) {
                    bleHeader.fromByteArray(value)
                    value = if(bleHeader.cmdSize+8 <= value.count()) {
                        mReadQueue.add(value.copyOfRange(0, bleHeader.cmdSize + 8))
                        value.copyOfRange(bleHeader.cmdSize + 8, value.count())
                    } else {
                        byteArrayOf()
                    }
                }
            }
        }
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(separator = "\n|--", prefix = "|--") {
                it.uuid.toString()
            }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable")
        }
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        mBluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(BLE_CCCD_UUID)?.let { cccDescriptor ->
            if (mBluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    private fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        characteristic.getDescriptor(BLE_CCCD_UUID)?.let { cccDescriptor ->
            if (mBluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    @Synchronized
    private fun stopScanning() {
        Log.i(TAG, "Stop Scanning")
        if (mScanning) {
            (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner.stopScan(mScanCallback)
            mScanning = false
        }
    }

    @Synchronized
    private fun doStopService() {
        LogFile.close()
        doDisconnect()
        stopForeground(true)
        stopSelf()
    }

    @Synchronized
    private fun doStartService() {
        val serviceChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        // Notification ID cannot be 0.
        startForeground(1, notification)
    }

    @Synchronized
    private fun doConnect() {
        doDisconnect()

        Log.w(TAG, "Searching for BLE device.")

        val filter = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BLE_SERVICE_UUID.toString()))
                .build()
        )
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        //set delay to stop scanning
        Handler(Looper.getMainLooper()).postDelayed({
            doTimeout()
        }, BLE_SCAN_PERIOD)

        //Set new connection status
        setConnectionState(STATE_CONNECTING)

        //Start scanning for BLE devices
        (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.bluetoothLeScanner.startScan(filter, settings, mScanCallback)
        mScanning = true
    }

    @Synchronized
    private fun doDisconnect(newState: Int = STATE_NONE, errorMessage: Boolean = false) {
        Log.w(TAG, "Disconnecting from BLE device.")
        if (mScanning)
            stopScanning()

        closeConnectionThread()

        if (mBluetoothGatt != null) {
            mBluetoothGatt!!.close()
            mBluetoothGatt = null
        }

        mBluetoothDevice = null

        //Set new connection status
        setConnectionState(newState, errorMessage)
    }

    @Synchronized
    private fun doTimeout() {
        if(mScanning)
            stopScanning()

        if(mState != STATE_CONNECTED) {
            //Set new connection status
            setConnectionState(STATE_NONE)
        }
    }

    @Synchronized
    private fun closeConnectionThread() {
        if(mConnectionThread != null) {
            mConnectionThread!!.cancel()
            mConnectionThread = null
        }
    }

    @Synchronized
    private fun createConnectionThread() {
        mConnectionThread = ConnectionThread()
        mConnectionThread?.priority = THREAD_PRIORITY_CONNECTION
        mConnectionThread?.start()
    }

    @Synchronized
    private fun setConnectionState(newState: Int, errorMessage: Boolean = false)
    {
        when(newState) {
            STATE_CONNECTED -> {
                closeConnectionThread()
                createConnectionThread()
            }
            STATE_NONE -> {
                closeConnectionThread()
            }
            STATE_ERROR -> {
                closeConnectionThread()
            }
        }
        //Broadcast a new message
        mState = newState
        val intentMessage = Intent(MESSAGE_STATE_CHANGE.toString())
        intentMessage.putExtra("newState", mState)
        intentMessage.putExtra("cDevice", mBluetoothGatt?.device?.name)
        if(errorMessage)
            intentMessage.putExtra("newError", mErrorStatus)
        sendBroadcast(intentMessage)
    }

    private inner class ConnectionThread: Thread() {
        //variables
        private var mTask: Int = TASK_NONE
        private var mTaskCount: Int = 0
        private var mTaskTime: Long = 0
        private var mLogFile: File? = null
        private var mBufferedWriter: BufferedWriter? = null

        init {
            setTaskState(TASK_NONE)
            Log.d(TAG, "create ConnectionThread")
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectionThread")
            logCreate()

            while (mState == STATE_CONNECTED && !currentThread().isInterrupted) {
                //See if there are any packets waiting to be sent
                if (!mWriteQueue.isEmpty() && mWriteSemaphore.tryAcquire()) {
                    try {
                        val txChar = mBluetoothGatt!!.getService(BLE_SERVICE_UUID)!!
                            .getCharacteristic(BLE_DATA_TX_UUID)
                        val writeType = when {
                            txChar.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            txChar.isWritableWithoutResponse() -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            else -> error("Characteristic ${txChar.uuid} cannot be written to")
                        }

                        val buff = mWriteQueue.poll()
                        if(buff != null)
                            logAdd(true, buff)

                        mBluetoothGatt?.let { gatt ->
                            txChar.writeType = writeType
                            txChar.value = buff
                            gatt.writeCharacteristic(txChar)
                        } ?: error("Not connected to a BLE device!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during write", e)
                        mWriteSemaphore.release()
                        cancel()
                        break
                    }
                }

                //See if there are any packets waiting to be sent
                if (!mReadQueue.isEmpty()) {
                    try {
                        val buff = mReadQueue.poll()
                        if(buff != null)
                            logAdd(false, buff)
                        
                        when (mTask) {
                            TASK_NONE -> {
                                //Broadcast a new message
                                val intentMessage = Intent(MESSAGE_READ.toString())
                                intentMessage.putExtra("readBuffer", buff)
                                sendBroadcast(intentMessage)
                            }
                            TASK_RD_VIN -> {
                                //Broadcast a new message
                                val intentMessage = Intent(MESSAGE_READ_VIN.toString())
                                intentMessage.putExtra("readBuffer", buff)
                                sendBroadcast(intentMessage)

                                setTaskState(TASK_NONE)
                            }
                            TASK_LOGGING -> {
                                mTaskCount++

                                //Are we still sending initial frames?
                                if(mTaskCount < UDSLogger.frameCount())
                                    mWriteQueue.add(UDSLogger.buildFrame(mTaskCount))

                                //Process frame
                                val result = UDSLogger.processFrame(mTaskCount, buff, applicationContext)

                                //Broadcast a new message
                                if((mTaskCount % Settings.updateRate == 0) or (result != UDS_OK)) {
                                    val intentMessage = Intent(MESSAGE_READ_LOG.toString())
                                    intentMessage.putExtra("readCount", mTaskCount)
                                    intentMessage.putExtra("readTime", System.currentTimeMillis() - mTaskTime)
                                    intentMessage.putExtra("readResult", result)
                                    sendBroadcast(intentMessage)
                                }

                                //If we changed logging write states broadcast a new message
                                if(UDSLogger.isEnabled() != mLogWriteState) {
                                    val intentMessage = Intent(MESSAGE_WRITE_LOG.toString())
                                    intentMessage.putExtra("enabled", UDSLogger.isEnabled())
                                    sendBroadcast(intentMessage)
                                    mLogWriteState = UDSLogger.isEnabled()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during read", e)
                        cancel()
                        break
                    }
                }
            }
            logClose()
        }

        fun cancel() {
            interrupt()
        }

        fun setTaskState(newTask: Int)
        {
            if (mState != STATE_CONNECTED) {
                mTask = TASK_NONE
                return
            }

            //Broadcast a new message
            mTaskCount = 0
            mTaskTime = System.currentTimeMillis()
            mTask = newTask
            val intentMessage = Intent(MESSAGE_TASK_CHANGE.toString())
            intentMessage.putExtra("newTask", mTask)
            sendBroadcast(intentMessage)

            when (mTask) {
                TASK_LOGGING -> {
                    //Set persist delay
                    val bleHeader = BLEHeader()
                    bleHeader.cmdSize = 2
                    bleHeader.cmdFlags = BLE_COMMAND_FLAG_SETTINGS or BRG_SETTING_PERSIST_DELAY
                    var dataBytes = byteArrayOf((Settings.persistDelay and 0xFF).toByte(), ((Settings.persistDelay and 0xFF00) shr 8).toByte())
                    var buf = bleHeader.toByteArray() + dataBytes
                    mWriteQueue.add(buf)

                    //Set persist Q delay
                    bleHeader.cmdFlags = BLE_COMMAND_FLAG_SETTINGS or BRG_SETTING_PERSIST_Q_DELAY
                    dataBytes = byteArrayOf((Settings.persistQDelay and 0xFF).toByte(), ((Settings.persistQDelay and 0xFF00) shr 8).toByte())
                    buf = bleHeader.toByteArray() + dataBytes
                    mWriteQueue.add(buf)

                    //Write first frame
                    mWriteQueue.add(UDSLogger.buildFrame(0))
                }
                TASK_RD_VIN -> {
                    val bleHeader = BLEHeader()
                    bleHeader.cmdSize = 3
                    bleHeader.cmdFlags = BLE_COMMAND_FLAG_PER_CLEAR

                    val dataBytes = byteArrayOf(0x22.toByte(), 0xF1.toByte(), 0x90.toByte())

                    val buf = bleHeader.toByteArray() + dataBytes
                    mWriteQueue.add(buf)
                }
                TASK_NONE -> {
                    val bleHeader = BLEHeader()
                    bleHeader.cmdFlags = BLE_COMMAND_FLAG_PER_CLEAR

                    val buf = bleHeader.toByteArray()
                    mWriteQueue.add(buf)
                }
            }
        }

        private fun logCreate() {
            if(!LOG_COMMUNICATIONS)
                return

            logClose()

            val path = applicationContext.getExternalFilesDir("")
            Log.i(TAG, "$path/data.log")
            mLogFile = File(path, "/data.log")
            if(mLogFile == null)
                return

            try {
                mLogFile!!.createNewFile()
                mBufferedWriter = BufferedWriter(FileWriter(mLogFile, true))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun logClose() {
            if(!LOG_COMMUNICATIONS)
                return

            if(mBufferedWriter != null) {
                mBufferedWriter!!.close()
                mBufferedWriter = null
            }

            mLogFile = null
        }

        private fun logAdd(from: Boolean, buff: ByteArray?) {
            if(!LOG_COMMUNICATIONS)
                return

            if(mLogFile == null || mBufferedWriter == null || buff == null)
                return

            try {
                if(from) mBufferedWriter!!.append("->[${buff.count()}]:${buff.toHex()}")
                else mBufferedWriter!!.append("<-[${buff.count()}]:${buff.toHex()}")
                mBufferedWriter!!.newLine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

