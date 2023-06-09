package com.example.dtprojectnew

//bei Hallo sind GAnzzahl Packages 1?
//					  additional information
//Package:|   0xEF   | 0 0 0   0       0           0      |  0x00  |  0x00 |         0x00         |  0x00     |     ...
//		  |Startbyte |       Text   Negativ     Kommazahl | Msg-ID |   Typ |  Anzahl der Ganzzahl |  Länge    |     Msg
//		  |			 |								      |	       |       |        Packages      | der Msg   |

import android.util.Log
import kotlin.math.pow
//VERALTET!!!!!!
class Package {
    val TAG = "Package"
    var Header:ByteArray = ByteArray(6)
    lateinit var Msg:ByteArray
    var msglength:UInt = 0u

    constructor(Typ:Byte){
        this.Header[0] = 0xEF.toByte()
        this.Header[3] = Typ
    }

    constructor():this(0.toByte()){}

    private fun set_NgtvBit() {
        // Setze das Negativ-Bit im Header
        this.Header[1] = (this.Header[1].toInt() or 0x02).toByte()
    }

    private fun set_size(size: Byte) {
        // Setze die Länge der Nachricht im Header
        this.Header[5] = size
        this.msglength = size.toUInt()
    }

    private fun set_IntSize(size: Byte) {
        // Setze die Länge der ganzzahligen Bytes im Header
        this.Header[4] = size
    }

    private fun set_FltBit() {
        // Setze das Nachkomma-Bit im Header
        this.Header[1] = (this.Header[1].toInt() or 0x01).toByte()
    }

    fun intToBytes(value: Int) {
        var numintBytes:Byte

        // Ermittle die Anzahl der signifikanten Bytes des ganzzahligen Teils
        if (value != 0) {
            val log2 = { value: Double -> Math.log(value) / Math.log(2.0)}
            val tmpnumintBits = log2(Math.abs(value).toDouble()).toInt() + 1
            numintBytes = if (tmpnumintBits % 8 == 0)
                (tmpnumintBits / 8).toByte()
            else
                ((tmpnumintBits / 8) + 1).toByte()
            if (value < 0)
                set_NgtvBit()
        } else {
            numintBytes = 1 // Der Wert ist 0, nur ein Byte wird benötigt
        }

        set_size(numintBytes)
        set_IntSize(numintBytes)

        this.Msg = ByteArray(numintBytes.toInt())

        for (i in 0 until numintBytes) {
            this.Msg[i] = ((Math.abs(value) shr ((numintBytes - i - 1) * 8)) and 0xFF).toByte()
        }
    }

    fun floatToBytes(value: Float, precision: Int) {
        val intValue = value.toInt()
        val aftPoint = (value * 10.0.pow(precision.toDouble())).toInt() - (intValue * 10.0.pow(precision.toDouble())).toInt()

        // Ist Kommazahl, setze passendes Bit
        if (precision > 0) {
            set_FltBit()
        }

        intToBytes(intValue)

        //Wenn es garkeine Nachkommastelle hat lass das unten einfach sein
        if(aftPoint == 0)
            return

        // Anzahl der Byte-Pakete für die Nachkommastellen
        var numaftBytes:UInt = 0u
        if (precision > 0) {
            val log2 = { value: Double -> Math.log(value) / Math.log(2.0)}
            val tmpnumaftBits = log2(Math.abs(aftPoint).toDouble()).toInt() + 1
            numaftBytes = if (tmpnumaftBits % 8 == 0)
                (tmpnumaftBits / 8).toUInt()
            else
                ((tmpnumaftBits / 8) + 1).toUInt()
        }

        // Neues Array erstellen der passenden Größe
        val newArr = ByteArray((msglength + numaftBytes).toInt())

        this.Msg.copyInto(newArr)

        for (i in msglength until numaftBytes + msglength) {
            newArr[i.toInt()] = (Math.abs(aftPoint) shr (((numaftBytes - (i - msglength) - 1u) * 8u)).toInt() and 0xFF).toByte()
        }

        set_size((msglength + numaftBytes).toByte())

        this.Msg = newArr
    }



    fun combineIntBytesToNumber():Int {
        var result: Int = 0
        for (i in 0 until this.getIntsize()) {
            result = result shl 8 or (this.Msg[i].toInt() and 0xFF)
        }
        if(this.is_Ngtv())
            result *=-1
        return result
    }
    fun combineFltBytesToNumber():Float{
        val decpart:Int = combineIntBytesToNumber()
        val Fltstart:Int = this.getIntsize().toInt()
        val decSize:Int = this.getTotalsize().toInt()-Fltstart
        var Fltpart:Float = 0f

        if(decSize > 0) {
            var tmpInt:Int = 0
            for (i in Fltstart until this.getTotalsize().toInt()) {
                tmpInt = tmpInt shl 8 or (this.Msg[i].toInt() and 0xFF)
            }
            Fltpart = tmpInt/10f.pow(tmpInt.toString().count())
            if(this.is_Ngtv())
                Fltpart *=-1
        }
        return decpart.toFloat()+Fltpart
    }
    fun combineTxtBytes():String{
        return String(this.Msg)
    }
    fun BytetoHeadercpy(header:ByteArray){
        try {
            header.copyInto(Header)
            msglength = Header[5].toUInt()
        }
        catch (e:IndexOutOfBoundsException){
            Log.e(TAG, "The transmittet Header has not the right size of 6")
        }
    }
    fun BytetoMsgcpy(msg:ByteArray){
        try {
            Msg = msg
        }
        catch(e:UninitializedPropertyAccessException){
            Log.e(TAG, "Okay das sollte jetzt nicht mehr passieren. Hoffe ich doch...")
        }
    }
    fun is_Ngtv():Boolean{
        return (this.Header[1].toInt() and 0x02) != 0
    }
    fun getTotalsize():Short{
        return Header[5].toShort()
    }
    fun getIntsize():Short{
        return Header[4].toShort()
    }
    fun getTyp():Short{
        return Header[3].toShort()
    }
    fun getmsgID():Short{
        return Header[2].toShort()
    }
    fun getadditByte():Short{
        return Header[1].toShort()
    }

    fun NoFunctionFound(){
        Log.e(TAG, "There was no fitting function found... that can't be actually")
        this.Logpckg()
    }
    //TODO hier ist irgendwas mit Reflexion falsch des Weiteren gibt es Reflexion erst seit Android 26 also sowieso blöd
    fun getInterpreteFunc():()->Any{
        if(this.is_txt()){
            Log.i(TAG, "It is Text")
            return this::combineTxtBytes
        }
        if(this.is_int()){
            Log.i(TAG, "It is Int")
            return this::combineIntBytesToNumber
        }
        if(this.is_float()){
            Log.i(TAG, "It is Float")
            return this::combineFltBytesToNumber
        }

        return this::NoFunctionFound
    }
    fun is_float():Boolean{
        return (this.Header[1].toInt() and 0x01) == 0x01
    }
    //Es ist ein Int wenn es kein float und kein Text ist
    fun is_int():Boolean{
        return (!this.is_float() and !this.is_txt())
    }
    fun is_txt():Boolean{
        return ((this.Header[1].toInt() and 0x04) == 0x04)
    }
    fun is_msg():Boolean{
        return ((this.getTyp().toByte() == HeaderTypes.MSG.value) and this.is_txt())
    }
    fun Logpckg(){
        Log.d(TAG, "Header:")
        for(byte in Header)
            Log.d(TAG,byte.toUByte().toString(2).padStart(8, '0'))
        Log.d(TAG, "Msg:")
        for(byte in Msg)
            Log.d(TAG,byte.toUByte().toString(2).padStart(8, '0'))

    }
}