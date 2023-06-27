package com.example.dtprojectnew

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.preference.PreferenceActivity.Header
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException
import java.sql.Connection
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import kotlin.reflect.typeOf


interface ConnectionStatusObserver {
    fun onConnectionLost()
    fun crash()
    fun distance(dist:Int)
}

class BluetoothInterface(private val cntxt: MainActivity):Thread() {
    var k:Float = -10f
    private lateinit var mmSckt: BluetoothSocket
    var TAG = "BluetoothInterface"
    private var stop_sending = false
    private lateinit var Inpt: InputStream
    private lateinit var Outpt: OutputStream
    //private lateinit var hndler: AppInterface
    private var stop_listening = false

    private var aliveTimer: Timer? = null

    fun crash(){
        cntxt.crash()
    }
    fun startConnectionTimer(time:Long) {
        Log.i(TAG, "Wir starten Timer...")
        aliveTimer?.cancel()
        aliveTimer = Timer()
        aliveTimer?.schedule(object : TimerTask() {
            override fun run() {
                cntxt.onConnectionLost()
            }
        }, time)
    }

    private var Observers:MutableList<Observer> = mutableListOf()
    public fun addObserver(Obs:Observer){
        Observers.add(Obs)
    }
    public fun removeObserver(Obs:Observer){
        Observers.remove(Obs)
    }

    public fun isConnected():Boolean{
        if(!::mmSckt.isInitialized)
            return false
        if (mmSckt != null && mmSckt.isConnected)
            return true
        else
            return false
    }
    @SuppressLint("MissingPermission")
    //Startet derzeit einen Timer mit 3Minuten, so viel zeit hat man um ein Profil auszuwählen oder zu erstellen, ohne nochma connecten zu müssen
    public fun setSocket(fnd: BluetoothDevice, KNOWN_SPP_UUID: UUID):Boolean{
        stop_listening = false
        Log.i(TAG,"Trying to open Socket for ${fnd.name} : ${fnd.address} ")
        try {
            mmSckt = fnd.createInsecureRfcommSocketToServiceRecord(KNOWN_SPP_UUID)
            mmSckt?.let { BluetoothSocket -> BluetoothSocket.connect() }
            Inpt = mmSckt.inputStream
            Outpt = mmSckt.outputStream
            startConnectionTimer(200000)
        }
        catch (e: IOException){
            //Wird z.B. ausgelöst wenn das Gerät dann entfernt wird, ausgeht oder es bereits eine Verbindung und eine Kommunikation gibt.
            Log.e(TAG,"Couldn't create bluetooth Socket")
            Log.i(TAG, "Trying to close old one and make a new one")
            mmSckt.close()
            try {
                mmSckt = fnd.createInsecureRfcommSocketToServiceRecord(KNOWN_SPP_UUID)
                mmSckt?.let { BluetoothSocket -> BluetoothSocket.connect() }
                Inpt = mmSckt.inputStream
                Outpt = mmSckt.outputStream
                startConnectionTimer(200000)
            }
            catch (e: IOException){
                Log.e(TAG,"Still couldnt create Socket",e)
                return false
            }
        }
        return true
    }

    //TODO überleg mal warum das in zwei Funktionen nich läuft aber in einer aufeinmal schon...
    public fun connectSocket():Boolean{
        try {
            mmSckt?.let { BluetoothSocket -> BluetoothSocket.connect() }
            Inpt = mmSckt.inputStream
            Outpt = mmSckt.outputStream
        }
        catch (e: IOException){
            Log.e(TAG,"Couldn't connect to Device", e)
            return false
        }
        return true
    }

    public fun send(pckg:Package){
        Log.i(TAG, "Sending this Package")
        pckg.Logpckg()
        try{
            Outpt.write(pckg.Header)
            Outpt.write(pckg.Msg)
        }
        catch(e:java.io.IOException){}
    }

    private fun alertObservers(pckg:Package){
        for(k in this.Observers){

            when(k){
                is ConnectionObserver->{
                    k.alert(pckg)
                }
                else ->{
                    Log.e(TAG, "No alert Function for ${k::class}.")
                }
            }
        }
    }

    public override fun run(){

        try{
            while(!stop_listening && !currentThread().isInterrupted){
                //Log.i(TAG, "Current available Inpt ${Inpt.available()}")


//					        additional information
//Package:|   0xEF   | 0 0 0   0       0           0      |  0x00  |  0x00 |         0x00         |  0x00     |   0x00   |     ...		|	0x00
//		  |Startbyte |       Text   Negativ     Kommazahl | Msg-ID |   Typ |  Anzahl der Ganzzahl |  Laenge   | FltLänge |     Msg		|	 CRC
//		  |			 |								      |	       |       |        Packages      | der Msg   |          |				|
                //Header ist angekommen komplett
                if(Inpt.available()>=HeaderIndizes.TOTALHEADERSIZE.value){
                    val startCommByte = Inpt.read().toByte()
                    if(startCommByte != HeaderTypes.START.value){
                        Log.e(TAG, "Unallowed Start comm-byte $startCommByte, ${"0x"+(startCommByte.toInt() and 0xFF).toUInt().toString(16)}")
                        continue
                    }
                    Log.i(TAG, "Got Message")
                    val header = ByteArray(HeaderIndizes.TOTALHEADERSIZE.value)
                    header[0] = 0xEF.toByte()
                    for(i in 1 until HeaderIndizes.TOTALHEADERSIZE.value){
                        header[i] = Inpt.read().toByte()
                    }
                    val pckg:Package = Package()
                    pckg.BytetoHeadercpy(header)
                    var buffer:ByteArray
                    try{buffer = ByteArray(pckg.getTotalsize().toInt())}
                    catch(e:java.lang.NegativeArraySizeException){
                        Log.e(TAG, "Wir haben eine negative Array Size?? Wie ist das möglich ${pckg.getTotalsize().toInt()}")
                        continue
                    }
                    var currBytes:Int = 0

                    Log.i(TAG, "Msg Length is ${pckg.getTotalsize()}")
                    //Lies immer nur ein Byte mach mal das es mehere gleichzeitig liest.
                    while(currBytes < pckg.getTotalsize()){
                        buffer[currBytes] = Inpt.read().toByte()
                        currBytes++
                    }
                    pckg.BytetoMsgcpy(buffer)
                    pckg.Logpckg()

                    //Spezial Fall ist ne Alive Message, nicht für UI gedacht...
                    if (pckg.getTyp().toByte() == HeaderTypes.ALIVE.value){
                        try{
                            this.send(pckg)
                            Log.i(TAG, "Ladies n Gentelman we've gottem")
                            this.startConnectionTimer(6000)
                        }
                        catch(e:java.io.IOException){}
                    }
                    else if(pckg.getTyp().toByte() == HeaderTypes.CRASH.value)
                        this.crash()
                    else{
                        if(pckg.getTyp().toByte() == HeaderTypes.DISTANCE.value){
                            val dist = pckg.combineIntBytesToNumber()
                            if(dist <= 100)
                                this.distance(dist)
                        }

                        Log.i(TAG, "Wir haben ein normales Package bekommen")
                        this.alertObservers(pckg)
                    }
                }
            }
        }
        catch (e: IOException){
            Log.e(TAG, "Error while Reading Buffer", e)
        }
        catch(e: InterruptedException){
            Log.i(TAG, "Thread was Interrupted", e)
        }

        //Wenn Interrupted wurde Socket schließen
        mmSckt.close()

        Log.i(TAG,"Thread ended Successfully")
    }

    public fun disconnect(){
        stop_listening = true
    }
    public fun distance(dist:Int){
        cntxt.distance(dist)
    }
    public override fun interrupt() {
        Log.i(TAG, "Thread was properly terminated")
        super.interrupt()
    }
}