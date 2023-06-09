package com.example.dtprojectnew


import android.util.Log

//TODO mache ein KLassen UML für die Observer Klasse, und mache das mit dem LOCK und gucke das der Observer richtig läuft
enum class HeaderTypes(val value: Byte) {
    SPEED (0x01.toByte()),
    DEGREE (0x02.toByte()),
    PULS (0x03.toByte()),
    LOCATION (0x04.toByte()),
    LOCK (0x05.toByte()),
    RADIUS (0x06.toByte()),
    TEMPERATURE (0x07.toByte()),
    DISTANCE (0x08.toByte()),

    STOP (0xF0.toByte()),
    CONTINUE (0xF1.toByte()),
    CONNECTED (0xF4.toByte()),
    EXIT (0xF2.toByte()),
    MSG (0xF3.toByte()),
    ERROR (0xFF.toByte()),
    START (0xEF.toByte()),
    ALIVE (0xF5.toByte()),
    CRASH (0xF6.toByte())
}

enum class HeaderIndizes(val value: Int){
    STARTBYTE (0),
    TOTALHEADERSIZE (7),
    TYP (3),
    TOTALSIZE (5),
    ADITTIONALINF (1),
    MSGID (2),
    INTSIZE (4),
    FLTSIZE (6)
}


abstract class Observer(){
    abstract fun alert(pckg:Package):Boolean
}

class ConnectionObserver() :Observer() {
    //        alerta antifascista
    val TAG = "ConnectionObserver"
    //Intsize, size, msgId kann man zu ints machen eigentlich aber egal...
    var UI:MutableList<UIInterface> = mutableListOf()

    fun addInterface(UIintf:UIInterface){
        UI.add(UIintf)
    }
    fun removeInterface(UIintf:UIInterface){
        UI.remove(UIintf)
    }
    fun test(pckg: Package){
        if(pckg.isFloat())
            Log.i(TAG, "Ist ein Float ${pckg.combineFltBytesToNumber()}")
        if(pckg.isInt())
            Log.i(TAG,"Ist ein Int ${pckg.combineIntBytesToNumber()}")
        if(pckg.isTxt())
            Log.i(TAG, "Ist ein Txt ${pckg.combineTxtBytes()}")
    }

    override fun alert(pckg:Package):Boolean{
        //pckg.Logpckg()
        val fnct = pckg.getInterpretFunc()
        when(pckg.getTyp().toByte()){
            HeaderTypes.LOCATION.value ->{
                Log.i(TAG,"Location")
                for (k in UI){
                    k.setLocation(fnct())
                }
            }
            HeaderTypes.SPEED.value -> {
                Log.i(TAG, "Geschwindigkeit")
                for (k in UI){
                    k.setSpeed(fnct())
                }
            }
            HeaderTypes.DISTANCE.value->{
                Log.i(TAG, "Distance")
                for(k in UI){
                    k.setDistance(fnct())
                }
            }
            HeaderTypes.LOCK.value->{
                Log.i(TAG, "Lock")
                for(k in UI)
                    k.setLock(fnct())
            }
            HeaderTypes.DEGREE.value -> {
                Log.i(TAG, "Grad")
                for (k in UI)
                    k.setDegree(fnct())
            }
            HeaderTypes.PULS.value ->{
                Log.i(TAG,"Puls")
                for(k in UI)
                    k.setBpm(fnct())
            }
            HeaderTypes.MSG.value ->{
                Log.i(TAG, "Es ist ein Text")
                for (k in UI)
                    k.setmsg(fnct().toString())
            }
            HeaderTypes.STOP.value -> {
                Log.i (TAG, "Stop")
            }
            HeaderTypes.CONTINUE.value -> {
                Log.i(TAG, "Mache weiter")
            }
            HeaderTypes.EXIT.value -> {
                Log.i(TAG, "Hör auf komplett")
            }
            HeaderTypes.ERROR.value -> {
                Log.e(TAG, "Es gab einen Fehler")
            }
            HeaderTypes.RADIUS.value ->{
                Log.i(TAG, "Radius")
                for (k in UI)
                    k.setRadius(fnct())
            }
            HeaderTypes.TEMPERATURE.value ->{
                Log.i(TAG, "Temperatur")
                for (k in UI)
                    k.setTemp(fnct())
            }
            else -> {
                Log.e(TAG, "Unknown")
                pckg.Logpckg()
                return false
            }
        }
        //this.test(pckg)

        Log.i(TAG, "${fnct()}")
        return true
    }
}