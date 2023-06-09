package com.example.dtprojectnew

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.sql.Connection
import java.util.UUID
import kotlin.reflect.typeOf

class BluetoothInterface:Thread() {

    private lateinit var mmSckt: BluetoothSocket
    var TAG = "BluetoothInterface"
    private var stop_sending = false
    private lateinit var Inpt: InputStream
    private lateinit var Outpt: OutputStream
    //private lateinit var hndler: AppInterface
    private var stop_listening = false

    private var Observers:MutableList<Observer> = mutableListOf()
    public fun addObserver(Obs:Observer){
        Observers.add(Obs)
    }
    public fun removeObserver(Obs:Observer){
        Observers.remove(Obs)
    }
    @SuppressLint("MissingPermission")
    public fun set_Socket(fnd: BluetoothDevice, KNOWN_SPP_UUID: UUID):Boolean{

        Log.i(TAG,"Trying to open Socket for ${fnd.name} : ${fnd.address} ")
        try {
            mmSckt = fnd.createInsecureRfcommSocketToServiceRecord(KNOWN_SPP_UUID)
            mmSckt?.let { BluetoothSocket -> BluetoothSocket.connect() }
            Inpt = mmSckt.inputStream
            Outpt = mmSckt.outputStream
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
            }
            catch (e: IOException){
                Log.e(TAG,"Still couldnt create Socket",e)
                return false
            }
        }
        return true
    }

    //TODO überleg mal warum das in zwei Funktionen nich läuft aber in einer aufeinmal schon...
    public fun connect_to_Socket():Boolean{
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
        Outpt.write(pckg.Header)
        Outpt.write(pckg.Msg)
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

        var type:Byte//TODO vielleicht hier schon den Typ auslesen?
        try{
            while(!stop_listening || !currentThread().isInterrupted){
                //Log.i(TAG, "Current available Inpt ${Inpt.available()}")

//					  additional information
//Package:|   0xEF   | 0 0 0   0       0           0      |  0x00  |  0x00 |         0x00         |  0x00     |     ...
//		  |Startbyte |       Text   Negativ     Kommazahl | Msg-ID |   Typ |  Anzahl der Ganzzahl |  Länge    |     Msg
//		  |			 |								      |	       |       |        Packages      | der Msg   |
                if(Inpt.available()>=6){
                    val StartCommByte = Inpt.read().toByte()
                    if(StartCommByte != 0xEF.toByte()){
                        Log.e(TAG, "Unallowed Start comm-byte $StartCommByte, ${"0x"+(StartCommByte.toInt() and 0xFF).toUInt().toString(16)}")
                        continue
                    }
                    Log.i(TAG, "Got Message")
                    val header = ByteArray(6)
                    header[0] = 0xEF.toByte()
                    for(i in 1 until 6){
                        header[i] = Inpt.read().toByte()
                    }
                    val pckg:Package = Package()
                    pckg.BytetoHeadercpy(header)

                    var buffer = ByteArray(pckg.getTotalsize().toInt())
                    var curr_bytes:Int = 0

                    Log.i(TAG, "Msg Length is ${pckg.getTotalsize()}")
                    //Lies immer nur ein Byte mach mal das es mehere gleichzeitig liest.
                    while(curr_bytes < pckg.getTotalsize()){
                        buffer[curr_bytes] = Inpt.read().toByte()
                        curr_bytes++
                    }
                    pckg.BytetoMsgcpy(buffer)
                    pckg.Logpckg()
                    this.alertObservers(pckg)
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

    public override fun interrupt() {
        super.interrupt()
        Log.i(TAG, "Thread was properly terminated")
    }
}