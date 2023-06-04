    package com.example.dtprojectnew

    import android.util.Log

    class UIInterface() {
        val TAG = "UIInterface"
        var Speed:Float = 0f
        var Degree:Float = 0f
        var Bpm:Int = 0
        var Lock:Boolean = false
        var Location:String = ""
        var Txt:String = ""
        var Msg:String = ""

        //TODO allgemein sollte man nen Observer benutzen der das weiterträgt?? damit könnte man z.B. den Text einzeln behandeln
        //TODO füge else Fälle ein...
        fun set_Speed(spd:Any) {
            when (spd) {
                is Float -> {
                    Speed = spd
                }

                is Int -> {
                    Speed = spd.toFloat()
                }

                is String -> {
                    Txt = "Speed $spd"
                    Log.i(TAG, Txt)
                }
            }
        }
        fun set_Location(location:Any){
            when(location){
                is String->{
                    Location = location
                }
                else->{
                    Log.e(TAG, "Wrong Datatype for Location")
                }
            }
        }
        fun set_Lock(lck:Any){
            when(lck){
                is Int->{
                if(lck == 0x00)
                    Lock = false
                else
                    Lock = true
                }
                else ->
                    Log.e(TAG, "Non allowed Type for Lock, $lck")
            }
        }
        fun set_Degree(dgr:Any){
            when(dgr){
                is Float->{
                    Degree = dgr
                }
                is Int->{
                    Degree = dgr.toFloat()
                }
                is String->{
                    Txt = "Degree $dgr"
                    Log.i(TAG,Txt)
                }
            }
        }
        fun set_Bpm(bpm:Any){
            when(bpm){
                is Float->{
                    Bpm = bpm.toInt()
                }
                is Int->{
                    Bpm = bpm
                }
                is String->{
                    Txt = "BPM $bpm"
                    Log.i(TAG,Txt)
                }
            }
        }
        fun set_msg(msg:String){
            Msg = msg
            Log.i(TAG,msg)
        }

        fun logcurrentInfo(){
            Log.i(TAG, "Current Info:")
            Log.i(TAG,"Speed: $Speed")
            Log.i(TAG,"Degree: $Degree")
            Log.i(TAG,"Bpm: $Bpm")
            Log.i(TAG,"Txt: $Txt")
            Log.i(TAG,"Msg: $Msg")
            Log.i(TAG, "Lock: $Lock")
            Log.i(TAG, "GPS Location. $Location" )
        }
    }