package com.app.vwflashtools

import android.content.Context
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

//Equation list
//  0: x
//  1: x * 1000
//  2: (x - 2731.4) / 10.0
//  3: x / 1.28 - 100.0
//  4: x * 6.103515624994278
//  5: x / 128
//  6: x / 4.0
//  7: x * 0.75 - 48.0
//  8: x / 10.0
//  9: x / 100.0
// 10: x / 1000.0
// 11: x / 100000.0
// 12: x / 32
// 13: x / 51.2
// 14: x / 47.18142548596112
// 15: x / 10.24
// 16: x / 1024
// 17: x / 2.4
// 18: x / 200
// 19: x / 347.94711203897007654836464857342
// 20: 100.0 - x / 100.0
// 21: x / 2.66666666666667
// 22: (x - 95.0f) / 2.66666666666667
// 23: x / 655.3599999999997
// 24: x / 2.55
// 25: x / 16384
// 26: x / 250
// 27: x / 2.142128661087866
// 28: (x - 128) / 2.66666666666667
// 29: (x - 64) / -1.33333333333333
// 30: x - 40
// 31: x / 3.768805207949945
// 32: x / 376.8805207949945
// 33: x / 32767.99999999992
// 34: x / 188.4402603974972
// 35: x / 120.6017666543982
// 36: x / 12060.17666543982
// 37: x / 1.884402603974972
// 38: x * 10
// 39: (x-512)/32
// 40: (x-127)/10

data class DIDStruct(var address: Long,
                     var length: Int,
                     var equation: Int,
                     var signed: Boolean,
                     var progMin: Float,
                     var progMax: Float,
                     var warnMin: Float,
                     var warnMax: Float,
                     var smoothing: Float,
                     var value: Float,
                     var format: String,
                     var name: String,
                     var unit: String)

object DIDs {
    private val TAG = "DIDs"

    val list22: List<DIDStruct> = listOf(
        //P1
        DIDStruct(0x2033, 2, 19,false,0f,  220f,   -20f, 200f,   0.0f, 0f,"%06.2f","Speed",               "km/hr"),
        DIDStruct(0xf40C, 2, 6, false,0f,  7000f,  -1f,  6000f,  0.0f, 0f,"%06.1f","RPM",                 "rpm"),
        DIDStruct(0x39c0, 2, 8, false,0f,  300f,   0f,   300f,   0.0f, 0f,"%05.1f","MAP Actual",          "kpa"),
        DIDStruct(0x39c2, 2, 8, false,0f,  300f,   0f,   300f,   0.0f, 0f,"%05.1f","PUT Actual",          "kpa"),
        DIDStruct(0x10c0, 2, 16,false,0f,  2f,     0.7f, 4f,     0.0f, 0f,"%05.3f","Lambda Actual",       "l"),
        DIDStruct(0x2004, 2, 9, true, -10f,10f,    -15f, 60f,    0.0f, 0f,"%05.2f","Ignition angle",      "??"),
        DIDStruct(0x39a2, 2, 20,false,0f,  100f,   -1f,  100f,   0.0f, 0f,"%02.2f","Wastegate Position",  "%"),
        DIDStruct(0x2032, 2, 17,false,0f,  1500f,  -1f,  1500f,  0.0f, 0f,"%07.2f","Mass Airflow",        "g/s"),
        //P2
        DIDStruct(0x2950, 2, 16,false,0f,  2f,     0.7f, 4f,     0.0f, 0f,"%05.3f","Lambda Specified",    "l"),
        DIDStruct(0x13a0, 2, 18,false,0f,  190000f,0f,   185000f,0.0f, 0f,"%05.2f","Injector PW Cyl 1 DI","ms"),
        DIDStruct(0x437C, 2, 8, true, -50f,450f,   -100f,500f,   0.0f, 0f,"%03.2f","Engine torque",       "Nm"),
        DIDStruct(0x2027, 2, 38,false,0f,  28000f, -1f,  28000f, 0.0f, 0f,"%03.2f","HFP Actual",          "kpa"),
        DIDStruct(0xf406, 1, 3, false,-25f,25f,    -20f, 20f,    0.0f, 0f,"%02.2f","STFT",                "%"),
        DIDStruct(0x20ba, 2, 8, true, 0f,  100f,   -1f,  101f,   0.0f, 0f,"%05.1f","Throttle Sensor",     "%"),
        DIDStruct(0x1040, 2, 4, false,0f,  190000f,0f,   185000f,0.0f, 0f,"%08.1f","Turbo Speed",         "rpm"),
        DIDStruct(0x209a, 2, 9, false,0f,  100f,   -1f,  100f,   0.0f, 0f,"%03.2f","HPFP Volume",         "%"),
        //P3
        DIDStruct(0x200a, 2, 9, true, -10f,0f,     -4f,  1f,     0.0f, 0f,"%05.3f","Retard cylinder 1",   "??"),
        DIDStruct(0x200b, 2, 9, true, -10f,0f,     -4f,  1f,     0.0f, 0f,"%05.3f","Retard cylinder 2",   "??"),
        DIDStruct(0x200c, 2, 9, true, -10f,0f,     -4f,  1f,     0.0f, 0f,"%05.3f","Retard cylinder 3",   "??"),
        DIDStruct(0x200d, 2, 9, true, -10f,0f,     -4f,  1f,     0.0f, 0f,"%05.3f","Retard cylinder 4",   "??"),
        DIDStruct(0x2904, 2, 0, false,0f,  20f,    -1f,  10f,    0.0f, 0f,"%03.0f","Misfire Sum Global",  ""),
        DIDStruct(0x1001, 1, 7, false,-40f,55f,    -35f, 50f,    0.0f, 0f,"%03.2f","IAT",                 "??C"),
        DIDStruct(0x2025, 2, 8, false,0f,  1500f,  600f, 1500f,  0.0f, 0f,"%03.0f","LFP Actual",          "kpa"),
        DIDStruct(0x293b, 2, 38,false,0f,  25000f, 600f, 25000f, 0.0f, 0f,"%05.0f","HFP Command",         "kpa"),

        DIDStruct(0x2028, 2, 9, false,0f,  100f,   -1.0f,100f,   0.0f, 0f,"%04.1f","LPFP Duty",           "%"),
        DIDStruct(0x295c, 1, 0, false,0f,  1f,     -1f,  2f,     0.0f, 0f,"%01.0f","Flaps Actual",        ""),
        DIDStruct(0xf456, 1, 3, false,-25f,25f,    -20f, 20f,    0.0f, 0f,"%02.2f","LTFT",                "%"),
        DIDStruct(0x202f, 2, 2, false,-50f,130f,   0f,   120f,   0.0f, 0f,"%03.2f","Oil temp",            "??C"),
        DIDStruct(0x13ca, 2, 35,false,50f, 120f,   70f,  120.0f, 0.0f, 0f,"%07.2f","Ambient pressure",    "kpa"),
        DIDStruct(0x1004, 2, 5, true, -40f,50f,    -30f, 45f,    0.0f, 0f,"%07.2f","Ambient air temp",    "??C"),
        DIDStruct(0x4380, 2, 8, true, 0f,  500f,   -100f,500f,   0.0f, 0f,"%04.1f","Torque requested",    "Nm"),
        DIDStruct(0x203c, 2, 0, false,0f,  2f,     -1f,  100f,   0.0f, 0f,"%01.0f","Cruise control",      ""),
    )

    val list3E: List<DIDStruct> = listOf(
        DIDStruct(0xd0012400, 2, 0, false,0f,   7000f,  -1.0f, 6000f,  0.0f, 0f,"%03.0f","Engine Speed",        "rpm"),
        DIDStruct(0xd00136ac, 2, 37,false,0f,   28000f, 0f,    28000f, 0.0f, 0f,"%05.0f","Fuel Pressure DI",    "kpa"),
        DIDStruct(0xd000f00c, 1, 3, false,-25f, 25f,    -20f,  20f,    0.5f, 0f,"%04.1f","Fuel Trim Short Term","%"),
        DIDStruct(0xd000c179, 1, 29,false,-50f, 70f,    -20f,  50f,    0.5f, 0f,"%03.2f","Intake Air Temp",     "??C"),
        DIDStruct(0xd001988e, 1, 28,false,-5f,  5f,     -3.0f, 3f,     0.5f, 0f,"%05.3f","Knock Retard",        "??"),
        DIDStruct(0xd00120e2, 2, 33,false,0.5f, 1.5f,   -0.1f, 5f,     0.0f, 0f,"%04.2f","Lambda SAE",          "l"),
        DIDStruct(0xd00098fc, 4, 10,false,0f,   300f,   -1f,   300f,   0.0f, 0f,"%05.1f","PUT Actual",          "kpa"),
        DIDStruct(0xd0011e76, 2, 4, false,0f,   195000f,-100f, 190000f,0.0f, 0f,"%05.0f","Turbo Speed",         "rpm"),
        DIDStruct(0xd00097b4, 4, 1, false,0f,   2f,     -100f, 1000f,  0.0f, 0f,"%05.3f","Airmass",             "g/stk"),
        DIDStruct(0xd00097fc, 4, 1, false,0f,   2f,     -100f, 1000f,  0.0f, 0f,"%05.3f","Airmass Setpoint",    "g/stk"),
        DIDStruct(0xd000c177, 1, 29,false,-25f, 45f,    -100f, 100f,   0.5f, 0f,"%03.2f","Ambient Air Temp",    "??C"),
        DIDStruct(0xd0013c76, 2, 35,false,0f,   110f,   0f,    120f,   0.5f, 0f,"%03.2f","Ambient Pressure",    "kpa"),
        DIDStruct(0xd0015172, 2, 13,true, 10f,  15f,    7f,    16f,    0.5f, 0f,"%01.0f","Battery Volts",       "V"),
        DIDStruct(0xd000c36e, 1, 0, false,-10f, 10f,    -100f, 100f,   0.0f, 0f,"%05.2f","Combustion Mode",     ""),
        DIDStruct(0xd000c6f5, 1, 29,false,-50f, 130f,   -100f, 150f,   0.5f, 0f,"%03.0f","Coolant Temp",        "??C"),
        DIDStruct(0xd001397a, 2, 21,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","EOI Limit",           "??"),
        DIDStruct(0xd0013982, 2, 21,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","EOI Actual",          "??"),
        DIDStruct(0xd000c1d4, 1, 24,false,-100f,100f,   -100f, 100f,   0.0f, 0f,"%01.0f","Ethanol Content",     "%"),
        DIDStruct(0xd0011e04, 2, 25,false,0f,   1.5f,   -1f,   10f,    0.0f, 0f,"%03.2f","Exhaust Flow Factor", ""),
        DIDStruct(0xd001566e, 2, 5, true, -45f, 45f,    -100f, 100f,   0.0f, 0f,"%04.1f","Exhaust Cam Position","??"),
        DIDStruct(0xd0011eba, 2, 35,false,0f,   500f,   -100f, 1000f,  0.0f, 0f,"%03.0f","Exhaust Pres Desired","kpa"),
        DIDStruct(0xd00135e0, 2, 14,false,-100f,100f,   -100f, 1000f,  0.0f, 0f,"%01.0f","Fuel Flow Desired",   "mg/stk"),
        DIDStruct(0xd0013636, 2, 14,false,-100f,100f,   -100f, 1000f,  0.0f, 0f,"%01.0f","Fuel Flow",           "mg/stk"),
        DIDStruct(0xd00192b1, 1, 5, false,-100f,100f,   -100f, 100f,   0.0f, 0f,"%01.0f","Fuel Flow Split MPI", ""),
        DIDStruct(0xd0013600, 2, 23,false,0f,   100f,   -1000f,100f,   0.0f, 0f,"%01.0f","Fuel LPFP Duty",      "%"),
        DIDStruct(0xd0011b26, 2, 31,false,0f,   1500f,  -1000f,2000f,  0.0f, 0f,"%04.0f","Fuel LPFR",           "kpa"),
        DIDStruct(0xd001360c, 2, 31,false,0f,   1500f,  -1000f,2000f,  0.0f, 0f,"%04.0f","Fuel LPFR Setpoint",  "kpa"),
        DIDStruct(0xd0013640, 2, 37,false,0f,   28000f, -1000f,30000f, 0.0f, 0f,"%05.0f","Fuel HPFR Setpoint",  "kpa"),
        DIDStruct(0xd001363c, 2, 23,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","Fuel HPFP Volume",    "%"),
        DIDStruct(0xd000f00b, 1, 3, false,-25f, 25f,    -20f,  20f,    0.5f, 0f,"%01.0f","Fuel Trim Long Term", "%"),
        DIDStruct(0xd000f39a, 1, 0, false,0f,   6f,     -1f,   7f,     0.0f, 0f,"%02.2f","Gear",                "gear"),
        DIDStruct(0xd000e57e, 1, 22,false,-5f,  15f,    -100f, 100f,   0.0f, 0f,"%01.0f","Ignition Table Value","??"),
        DIDStruct(0xd000e59c, 1, 22,false,-5f,  15f,    -100f, 100f,   0.0f, 0f,"%01.0f","Ignition Timing",     "??"),
        DIDStruct(0xd001566c, 2, 5, true, -100f,100f,   -100f, 100f,   0.0f, 0f,"%01.0f","Intake Cam Position", "??"),
        DIDStruct(0xd0013b16, 2, 26,false,-25f, 25f,    -1000f,1000f,  0.0f, 0f,"%02.2f","Injector PW DI",      "ms"),
        DIDStruct(0xd0013824, 2, 26,false,0f,   100f,   -1000f,1000f,  0.0f, 0f,"%05.1f","Injector PW MPI",     "ms"),
        DIDStruct(0xd0011e08, 2, 25,false,0f,   20f,    -1000f,1000f,  0.0f, 0f,"%03.0f","Intake Flow Factor",  "-"),
        DIDStruct(0xd000efb1, 1, 28,false,0f,   250f,   -5f,   1000f,  0.5f, 0f,"%03.2f","Knock Retard Cyl 1",  "??"),
        DIDStruct(0xd000efb2, 1, 28,false,0f,   2f,     -5f,   1000f,  0.5f, 0f,"%05.3f","Knock Retard Cyl 2",  "??"),
        DIDStruct(0xd000efb3, 1, 28,false,0f,   2f,     -5f,   1000f,  0.5f, 0f,"%05.3f","Knock Retard Cyl 3",  "??"),
        DIDStruct(0xd000efb4, 1, 28,false,0f,   2f,     -5f,   1000f,  0.5f, 0f,"%05.3f","Knock Retard Cyl 4",  "??"),
        DIDStruct(0xd00143f6, 2, 16,false,0f,   2f,     -100f, 500f,   0.0f, 0f,"%03.2f","Lambda Setpoint",     "l"),
        DIDStruct(0xd00098cc, 4, 10,false,0f,   300f,   -300f, 300f,   0.0f, 0f,"%05.1f","MAP",                 "kpa"),
        DIDStruct(0xd00098f4, 4, 10,false,0f,   300f,   -300f, 300f,   0.0f, 0f,"%05.1f","MAP Setpoint",        "kpa"),
        DIDStruct(0xd000c5ae, 1, 30,false,-25f, 120f,   -1000f,1000f,  0.5f, 0f,"%01.0f","Oil Temp",            "??C"),
        DIDStruct(0xd000e578, 1, 28,true, 0f,   220f,   -1000f,1000f,  0.0f, 0f,"%03.2f","openflex_cor",        "??CRK"),
        DIDStruct(0xd001de8d, 1, 28,true, 0f,   7000f,  -1000f,1000f,  0.0f, 0f,"%06.1f","openflex_max_cor",    "??CRK"),
        DIDStruct(0xd001de8e, 1, 5, false,0f,   3f,     -1000f,1000f,  0.0f, 0f,"%05.3f","openflex_fac_cor",    ""),
        DIDStruct(0xd0012028, 2, 15,false,0f,   100f,   -1000f,1000f,  0.0f, 0f,"%04.1f","Pedal Position",      "%"),
        DIDStruct(0xd0000aa1, 1, 0, false,0f,   1f,     -1000f,1000f,  0.0f, 0f,"%01.0f","Port Flap Position",  ""),
        DIDStruct(0xd0011eee, 2, 35,false,0f,   300f,   -1000f,1000f,  0.0f, 0f,"%05.1f","PUT Setpoint",        "kpa"),
        DIDStruct(0xd0013a42, 2, 21,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","SOI Limit",           "??"),
        DIDStruct(0xd0013a44, 2, 21,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","SOI Actual",          "??"),
        DIDStruct(0xd000f377, 1, 27,false,0.5f, 1.3f,   -1000f,1000f,  0.0f, 0f,"%07.2f","TPS",                 "%"),
        DIDStruct(0xd0015344, 2, 12,true, -40f, 500f,   -1000f,1000f,  0.0f, 0f,"%07.2f","Torque Actual",       "Nm"),
        DIDStruct(0xd0011f0c, 2, 0, false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","Torque Limitation",   ""),
        DIDStruct(0xd0012048, 2, 12,true, -40f, 500f,   -1000f,500f,   0.0f, 0f,"%03.2f","Torque Requested",    "Nm"),
        DIDStruct(0xd000f3c1, 1, 30,false,-100f,100f,   -1000f,120f,   0.5f, 0f,"%01.0f","Transmission Temp",   "??C"),
        DIDStruct(0xd0019b75, 1, 0, false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","Valve Lift Position", ""),
        DIDStruct(0xd00155b6, 2, 9, false,0f,   220f,   -1000f,220f,   0.0f, 0f,"%03.0f","Vehicle Speed",       "km/h"),
        DIDStruct(0xd0015c2c, 2, 23,false,-10f, 0f,     -1000f,1000f,  0.0f, 0f,"%05.3f","Wastegate Setpoint",  "%"),
        DIDStruct(0xd0011e10, 2, 23,false,-10f, 0f,     -1000f,1000f,  0.0f, 0f,"%05.3f","Wastegate Actual",    "%"),
        DIDStruct(0xd0015c5e, 2, 12,false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","Wastegate Flow Req",  "kg/h"),
        DIDStruct(0xd00141ba, 2, 39,false,-10f, 10f,    -1000f,1000f,  0.3f, 0f,"%05.3f","Accel. Long",         "m/s2"),
        DIDStruct(0xd000ee2a, 1, 40,false,-10f, 10f,    -1000f,1000f,  0.3f, 0f,"%05.3f","Accel. Lat",          "m/s2"),
        DIDStruct(0xd001b6cd, 1, 0, false,-100f,100f,   -1000f,1000f,  0.0f, 0f,"%01.0f","Cruise Control",      ""),
        )

    fun list(): List<DIDStruct> {
        when(UDSLogger.getMode()) {
            UDS_LOGGING_22 -> return list22
            UDS_LOGGING_3E -> return list3E
        }
        return list22
    }

    fun getDID(address: Long): DIDStruct? {
        when(UDSLogger.getMode()) {
            UDS_LOGGING_22 -> {
                for (i in 0 until list22.count()) {
                    if(list22[i].address == address) {
                        return list22[i]
                    }
                }
            }
            UDS_LOGGING_3E -> {
                for (i in 0 until list3E.count()) {
                    if(list3E[i].address == address) {
                        return list3E[i]
                    }
                }
            }
        }

        return null
    }

    fun setValue(did: DIDStruct?, x: Float): Float {
        if(did == null)
            return 0f

        //Used in smoothing calculation
        val previousValue = did.value

        when(did.equation) {
            0 -> did.value = x
            1 -> did.value = x * 1000f
            2 -> did.value = (x - 2731.4f) / 10.0f
            3 -> did.value = x / 1.28f - 100.0f
            4 -> did.value = x * 6.103515624994278f
            5 -> did.value = x / 128f
            6 -> did.value = x / 4.0f
            7 -> did.value = x * 0.75f - 48.0f
            8 -> did.value = x / 10.0f
            9 -> did.value = x / 100.0f
            10 -> did.value = x / 1000.0f
            11 -> did.value = x / 100000.0f
            12 -> did.value = x / 32f
            13 -> did.value = x / 51.2f
            14 -> did.value = x / 47.18142548596112f
            15 -> did.value = x / 10.24f
            16 -> did.value = x / 1024f
            17 -> did.value = x / 2.4f
            18 -> did.value = x / 200f
            19 -> did.value = x / 347.94711203897007654836464857342f
            20 -> did.value = 100.0f - x / 100.0f
            21 -> did.value = x / 2.66666666666667f
            22 -> did.value = (x - 95.0f) / 2.66666666666667f
            23 -> did.value = x / 655.3599999999997f
            24 -> did.value = x / 2.55f
            25 -> did.value = x / 16384f
            26 -> did.value = x / 250f
            27 -> did.value = x / 2.142128661087866f
            28 -> did.value = (x - 128f) / 2.66666666666667f
            29 -> did.value = (x - 64f) / 1.33333333333333f
            30 -> did.value = x - 40f
            31 -> did.value = x / 3.768805207949945f
            32 -> did.value = x / 376.8805207949945f
            33 -> did.value = x / 32767.99999999992f
            34 -> did.value = x / 188.4402603974972f
            35 -> did.value = x / 120.6017666543982f
            36 -> did.value = x / 12060.17666543982f
            37 -> did.value = x / 1.884402603974972f
            38 -> did.value = x * 10
            39 -> did.value = (x - 512f) / 32f
            40 -> did.value = (x - 127f) / 10f
        }

        //Add smoothing
        if(did.smoothing > 0f && did.smoothing < 1f)
            did.value = ((1f-did.smoothing) * did.value) + (did.smoothing * previousValue)

        return did.value
    }

    fun getValue(did: DIDStruct?): Float {
        if (did == null)
            return 0f

        return did.value
    }
}

object UDSLogger {
    private val TAG = "UDSLogger"
    private var mLastEnabled = false
    private var mMode = UDS_LOGGING_22
    private var mTorquePID = -1
    private var mEngineRPMPID = -1
    private var mMS2PID = -1
    private var mGearPID = -1
    private var mVelocityPID = -1
    private var mTireCircumference = -1f
    private var mFoundMS2PIDS = false
    private var mFoundTQPIDS = false

    fun isCalcHP(): Boolean {
        if(Settings.calculateHP && (mFoundTQPIDS || mFoundMS2PIDS)) {
            return true
        }

        return false
    }

    fun getTorque(): Float {
        if(Settings.calculateHP) {
            if(mFoundMS2PIDS && Settings.useMS2Torque) {
                val gearValue = DIDs.list()[mGearPID].value.toInt()

                if(gearValue in 1..7) {
                    val ms2Value = sqrt(DIDs.list()[mMS2PID].value)
                    val velValue = DIDs.list()[mVelocityPID].value
                    val weightValue = Settings.curbWeight * KG_TO_N
                    val ratioValue = sqrt(Settings.gearRatios[gearValue - 1] * Settings.gearRatios[7])
                    val dragValue = 1.0f + (velValue * velValue * Settings.dragCoefficient)

                    return weightValue * ms2Value * dragValue / ratioValue / mTireCircumference / TQ_CONSTANT
                }

                return 0f
            } else if(mFoundTQPIDS) {
                return DIDs.list()[mTorquePID].value
            }
        }

        return 0f
    }

    fun getHP(tq: Float): Float {
        if(Settings.calculateHP && mEngineRPMPID != -1) {
            val rpmValue = DIDs.list()[mEngineRPMPID].value
            return tq * rpmValue / 7127f
        }

        return 0f
    }

    fun isEnabled(): Boolean {
        return mLastEnabled
    }

    fun setMode(mode: Int) {
        when(mode) {
            UDS_LOGGING_22 -> {
                mMode = mode
            }
            UDS_LOGGING_3E -> {
                mMode = mode
            }
        }
    }

    fun getMode(): Int {
        return mMode
    }

    fun frameCount(): Int {
        when(mMode) {
            UDS_LOGGING_22 -> {
                return frameCount22()
            }
            UDS_LOGGING_3E -> {
                return frameCount3E()
            }
        }
        return 0
    }

    fun buildFrame(index: Int): ByteArray {
        when(mMode) {
            UDS_LOGGING_22 -> {
                return buildFrame22(index)
            }
            UDS_LOGGING_3E -> {
                return buildFrame3E(index)
            }
        }
        return byteArrayOf()
    }

    fun processFrame(tick: Int, buff: ByteArray?, context: Context): Int {
        when(mMode) {
            UDS_LOGGING_22 -> {
                return processFrame22(tick, buff, context)
            }
            UDS_LOGGING_3E -> {
                return processFrame3E(tick, buff, context)
            }
        }
        return UDS_ERROR_NULL
    }

    private fun resetHPPIDS() {
        mFoundTQPIDS = false
        mFoundMS2PIDS = false
        mTorquePID = -1
        mEngineRPMPID = -1
        mMS2PID = -1
        mGearPID = -1
        mVelocityPID = -1
        mTireCircumference = Settings.tireDiameter * 3.14f
    }

    private fun findHPPIDS() {
        if(mMode == UDS_LOGGING_22) {
            for (x in 0 until DIDs.list22.count()) {
                //Look for torque PID
                if(DIDs.list22[x].address == 0x437C.toLong()) {
                    mTorquePID = x
                }

                //Look for rpm PID
                if(DIDs.list22[x].address == 0xf40C.toLong()) {
                    mEngineRPMPID = x
                }
            }
            //Did we find the PIDs required?
            if(mEngineRPMPID != -1 && mTorquePID != -1)
                mFoundTQPIDS = true

        } else {
            for (x in 0 until DIDs.list3E.count()) {
                //Look for torque PID
                if(DIDs.list3E[x].address == 0xd0015344) {
                    mTorquePID = x
                }
                //Look for rpm PID
                if(DIDs.list3E[x].address == 0xd0012400) {
                    mEngineRPMPID = x
                }

                //Look for MS2 PID
                if(DIDs.list3E[x].address == 0xd00141ba) {
                    mMS2PID = x
                }

                //Look for Gear PID
                if(DIDs.list3E[x].address == 0xd000f39a) {
                    mGearPID = x
                }

                //Look for Velocity PID
                if(DIDs.list3E[x].address == 0xd00155b6) {
                    mVelocityPID = x
                }
            }
            //Did we find the PIDs required?
            if(mEngineRPMPID != -1 && mMS2PID != -1 && mGearPID != -1 && mVelocityPID != -1)
                mFoundMS2PIDS = true

            if(mEngineRPMPID != -1 && mTorquePID != -1)
                mFoundTQPIDS = true
        }
    }

    private fun frameCount22(): Int {
        return 8
    }

    private fun frameCount3E(): Int {
        return (DIDs.list3E.count() * 5 / 0x8F) + 2
    }

    private fun buildFrame22(index: Int): ByteArray {
        val bleHeader = BLEHeader()
        bleHeader.cmdSize = 1
        bleHeader.cmdFlags = BLE_COMMAND_FLAG_PER_ADD
        if(index == 0) {
            bleHeader.cmdFlags += BLE_COMMAND_FLAG_PER_CLEAR
        }
        if(index == frameCount22()-1) {
            bleHeader.cmdFlags += BLE_COMMAND_FLAG_PER_ENABLE
        }

        var buff: ByteArray = byteArrayOf(0x22.toByte())
        if(index % 2 == 0) {
            //Write P1 PIDS
            for (i in 0 until 8) {
                val did: DIDStruct = DIDs.list22[i]
                bleHeader.cmdSize += 2
                buff += ((did.address and 0xFF00) shr 8).toByte()
                buff += (did.address and 0xFF).toByte()
            }
        } else {
            //Write P2 PIDS
            var startIndex = 8 + ((index % 4) / 2 * 4)
            var endIndex = startIndex + 4
            for (i in startIndex until endIndex) {
                val did: DIDStruct = DIDs.list22[i]
                bleHeader.cmdSize += 2
                buff += ((did.address and 0xFF00) shr 8).toByte()
                buff += (did.address and 0xFF).toByte()
            }
            //Write P3 PIDS
            startIndex = 16 + ((index % 8) / 2 * 4)
            endIndex = startIndex + 4
            for (i in startIndex until endIndex) {
                val did: DIDStruct = DIDs.list22[i]
                bleHeader.cmdSize += 2
                buff += ((did.address and 0xFF00) shr 8).toByte()
                buff += (did.address and 0xFF).toByte()
            }
        }

        return bleHeader.toByteArray() + buff
    }

    private fun buildFrame3E(index: Int): ByteArray {
        var addressArray: ByteArray = byteArrayOf()
        for(i in 0 until DIDs.list3E.count()) {
            addressArray += (DIDs.list3E[i].length and 0xFF).toByte()
            addressArray += DIDs.list3E[i].address.toArray4()
        }
        addressArray += 0

        //Do we even have any PIDs in the range?  If not send persist message
        if((index*0x8F >= addressArray.count()) and (index == frameCount3E()-1)) {
            val bleHeader = BLEHeader()
            bleHeader.cmdSize = 6
            bleHeader.cmdFlags = BLE_COMMAND_FLAG_PER_CLEAR or BLE_COMMAND_FLAG_PER_ADD or BLE_COMMAND_FLAG_PER_ENABLE

            val writeBuffer: ByteArray = bleHeader.toByteArray() + byteArrayOf(0x3e.toByte(), 0x33.toByte(), 0xb0.toByte(), 0x01.toByte(), 0xe7.toByte(), 0x00.toByte())

            Log.d(TAG, writeBuffer.toHex())
            Log.d(TAG, writeBuffer.count().toString())

            return writeBuffer
        }

        //constrain copy range or we will receive an exception
        val endOfArray = if((1+index)*0x8F > addressArray.count()) {
            addressArray.count()
        } else {
            (1+index)*0x8F
        }
        val selectArray: ByteArray = addressArray.copyOfRange(index*0x8F, endOfArray)
        val bleHeader = BLEHeader()
        bleHeader.cmdSize = 8 + selectArray.count()
        bleHeader.cmdFlags = BLE_COMMAND_FLAG_PER_CLEAR

        val memoryOffset = 0xB001E700 + (index * 0x8F)
        val writeBuffer: ByteArray = bleHeader.toByteArray() + byteArrayOf(0x3e.toByte(), 0x32.toByte()) + memoryOffset.toArray4() + selectArray.count().toArray2() + selectArray

        Log.d(TAG, writeBuffer.toHex())
        Log.d(TAG, writeBuffer.count().toString())

        return writeBuffer
    }

    fun processFrame22(tick: Int, buff: ByteArray?, context: Context): Int {
        // if the buffer is null abort
        if(buff == null) {
            return UDS_ERROR_NULL
        }

        // check to make sure ble header byte matches
        val bleHeader = BLEHeader()
        bleHeader.fromByteArray(buff)
        val bData = buff.copyOfRange(8, buff.size)
        if(!bleHeader.isValid()) {
            return UDS_ERROR_HEADER
        }

        // does the size of the data match the header?
        if(bData.count() != bleHeader.cmdSize) {
            return UDS_ERROR_CMDSIZE
        }

        // make sure we received an 'OK' from the ECU
        if(bData[0] != 0x62.toByte()) {
            return UDS_ERROR_RESPONSE
        }

        // process the data in the buffer
        var i = 1
        while(i < bleHeader.cmdSize-3) {
            val did: DIDStruct = DIDs.getDID(((bData[i++] and 0xFF) shl 8) + (bData[i++] and 0xFF).toLong()) ?: return UDS_ERROR_UNKNOWN
            if(did.length == 1) {
                if(did.signed) {
                    DIDs.setValue(did, (bData[i++] and 0xFF).toByte().toFloat())
                } else {
                    DIDs.setValue(did, (bData[i++] and 0xFF).toFloat())
                }
            } else {
                if(did.signed) {
                    DIDs.setValue(did, (((bData[i++] and 0xFF) shl 8) + (bData[i++] and 0xFF)).toShort().toFloat())
                } else {
                    DIDs.setValue(did, (((bData[i++] and 0xFF) shl 8) + (bData[i++] and 0xFF)).toFloat())
                }
            }
        }

        //Update Log every 2nd tick
        if(tick % 2 == 0) {
            val dEnable = DIDs.list22[DIDs.list22.count()-1]
            if((!Settings.invertCruise && dEnable.value != 0.0f) ||
                    (Settings.invertCruise && dEnable.value == 0.0f)) {
                //If we were not enabled before we must open a log to start writing
                if (!mLastEnabled) {
                    val currentDateTime = LocalDateTime.now()
                    LogFile.create("vwflashtools-${currentDateTime.format(DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss"))}.csv", context)
                    //Start with time, its required
                    var strItems: String? = "Time"

                    //Add all PIDS
                    for (x in 0 until DIDs.list22.count()) {
                        strItems += ",${DIDs.list22[x].name}"
                    }

                    //reset torque / rpm pids
                    resetHPPIDS()

                    //Are we supposed to calculate HP? find PIDS
                    if(Settings.calculateHP) {
                        //Look for PIDS related to calculating HP
                        findHPPIDS()

                        //If we found PIDs add column
                        if(isCalcHP())
                            strItems += ",HP"
                    }

                    LogFile.addLine(strItems)
                }
                mLastEnabled = true

                //Write new values to log
                var strItems: String? = (bleHeader.tickCount.toFloat() / 1000.0f).toString()
                for (x in 0 until DIDs.list22.count()) {
                    strItems += ",${DIDs.list22[x].value}"
                }

                //Calculate HP and found PIDS?
                if(isCalcHP()) {
                    val calcHP = getHP(getTorque())
                    strItems += ",${calcHP}"
                }

                LogFile.addLine(strItems)
            } else {
                if (mLastEnabled) {
                    LogFile.close()
                }
                mLastEnabled = false
            }
        }

        return UDS_OK
    }

    private fun processFrame3E(tick: Int, buff: ByteArray?, context: Context): Int {
        // if the buffer is null abort
        if(buff == null) {
            return UDS_ERROR_NULL
        }

        // check to make sure ble header byte matches
        val bleHeader = BLEHeader()
        bleHeader.fromByteArray(buff)
        val bData = buff.copyOfRange(8, buff.size)
        if(!bleHeader.isValid()) {
            return UDS_ERROR_HEADER
        }

        // does the size of the data match the header?
        if(bData.count() != bleHeader.cmdSize) {
            return UDS_ERROR_CMDSIZE
        }

        // make sure we received an 'OK' from the ECU
        if(bData[0] != 0x7e.toByte()) {
            return UDS_ERROR_RESPONSE
        }

        //still in the initial setup?
        if(tick < frameCount3E()) {
            return UDS_OK
        }

        //Update PID Values
        var dPos = 1
        for(i in 0 until DIDs.list3E.count()) {
            val did: DIDStruct = DIDs.list3E[i]

            //make sure we are in range
            if(dPos + did.length > bData.count())
                break

            //Build the value in little endian
            var newValue: Int = bData[dPos+did.length-1] and 0xFF
            for(d in 1 until did.length) {
                newValue = newValue shl 8
                newValue += bData[dPos+did.length-d-1] and 0xFF
            }
            dPos += did.length

            //set pid values
            if(did.signed) {
                when(did.length) {
                    1 -> DIDs.setValue(did, newValue.toByte().toFloat())
                    2 -> DIDs.setValue(did, newValue.toShort().toFloat())
                    4 -> DIDs.setValue(did, newValue.toFloat())
                }
            } else {
                when(did.length) {
                    1 -> DIDs.setValue(did, newValue.toFloat())
                    2 -> DIDs.setValue(did, newValue.toFloat())
                    4 -> DIDs.setValue(did, Float.fromBits(newValue))
                }
            }
        }

        val dEnable = DIDs.list3E[DIDs.list3E.count()-1]
        if((!Settings.invertCruise && dEnable.value != 0.0f) ||
            (Settings.invertCruise && dEnable.value == 0.0f)) {
            //If we were not enabled before we must open a log to start writing
            if (!mLastEnabled) {
                val currentDateTime = LocalDateTime.now()
                LogFile.create("vwflashtools-${currentDateTime.format(DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss"))}.csv", context)

                //Add time its required
                var strItems: String? = "Time"

                //Add PIDs
                for (x in 0 until DIDs.list3E.count()) {
                    strItems += ",${DIDs.list3E[x].name}"
                }

                //reset torque / rpm pids
                resetHPPIDS()

                //Are we supposed to calculate HP? find PIDS
                if(Settings.calculateHP) {
                    //Look for PIDS related to calculating HP
                    findHPPIDS()

                    //If we found PIDs add column
                    if(isCalcHP())
                        strItems += ",TQ,HP"
                }

                LogFile.addLine(strItems)
            }
            mLastEnabled = true

            //Write new values to log
            var strItems: String? = (bleHeader.tickCount.toFloat() / 1000.0f).toString()
            for (x in 0 until DIDs.list3E.count()) {
                strItems += ",${DIDs.list3E[x].value}"
            }

            //Calculate HP and found PIDS?
            if(isCalcHP()) {
                val calcTQ = getTorque()
                val calcHP = getHP(calcTQ)
                strItems += ",${calcTQ},${calcHP}"
            }

            LogFile.addLine(strItems)
        } else {
            if (mLastEnabled) {
                LogFile.close()
            }
            mLastEnabled = false
        }

        return UDS_OK
    }
}