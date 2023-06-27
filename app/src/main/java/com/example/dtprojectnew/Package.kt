package com.example.dtprojectnew

//bei Hallo sind GAnzzahl Packages 1?
//					  additional information
//Package:|   0xEF   | 0 0 0   0       0           0      |  0x00  |  0x00 |         0x00         |  0x00     |     ...
//		  |Startbyte |       Text   Negativ     Kommazahl | Msg-ID |   Typ |  Anzahl der Ganzzahl |  Länge    |     Msg
//		  |			 |								      |	       |       |        Packages      | der Msg   |

import android.preference.PreferenceActivity.Header
import android.util.Log
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.math.pow
//VERALTET!!!!!!
class Package {
    val TAG = "Package"
    var Header:ByteArray = ByteArray(HeaderIndizes.TOTALHEADERSIZE.value)
    lateinit var Msg:ByteArray
    var msglength:UInt = 0u
    val GEN_POLYNOM: UByte = 0x3Du.toUByte()
    var CRC:UByte = 0u

    constructor(Typ:Byte){
        this.Header[HeaderIndizes.STARTBYTE.value] = HeaderTypes.START.value
        this.Header[HeaderIndizes.TYP.value] = Typ
    }

    constructor():this(0.toByte()){}

    private fun setNgtvBit() {
        // Setze das Negativ-Bit im Header
        this.Header[HeaderIndizes.ADITTIONALINF.value] = (this.Header[HeaderIndizes.ADITTIONALINF.value].toInt() or 0x02).toByte()
    }

    private fun setSize(size: Byte) {
        // Setze die Länge der Nachricht im Header
        this.Header[HeaderIndizes.TOTALSIZE.value] = size
        this.msglength = size.toUInt()
    }

    private fun setIntSize(size: Byte) {
        // Setze die Länge der ganzzahligen Bytes im Header
        this.Header[HeaderIndizes.INTSIZE.value] = size
    }

    private fun setFltBit() {
        // Setze das Nachkomma-Bit im Header
        this.Header[HeaderIndizes.ADITTIONALINF.value] = (this.Header[HeaderIndizes.ADITTIONALINF.value].toInt() or 0x01).toByte()
    }
    private fun setFltSize(size: Byte){
        this.Header[HeaderIndizes.FLTSIZE.value] = size
    }
    private fun getFltSize():Byte{
        return this.Header[HeaderIndizes.FLTSIZE.value]
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
                setNgtvBit()
        } else {
            numintBytes = 1 // Der Wert ist 0, nur ein Byte wird benötigt
        }

        setSize(numintBytes)
        setIntSize(numintBytes)

        this.Msg = ByteArray(numintBytes.toInt())

        for (i in 0 until numintBytes) {
            this.Msg[i] = ((Math.abs(value) shr ((numintBytes - i - 1) * 8)) and 0xFF).toByte()
        }
    }

    fun floatToBytes(value: Float, precision: Byte) {
        val intValue = value.toInt()
        val aftPoint = (value * 10.0.pow(precision.toDouble())).toInt() - (intValue * 10.0.pow(precision.toDouble())).toInt()

        // Ist Kommazahl, setze passendes Bit
        if (precision > 0) {
            setFltBit()
            setFltSize(precision)
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

        setSize((msglength + numaftBytes).toByte())

        this.Msg = newArr
    }



    fun combineIntBytesToNumber():Int {
        var result: Int = 0
        for (i in 0 until this.getIntsize()) {
            result = result shl 8 or (this.Msg[i].toInt() and 0xFF)
        }
        if(this.isNgtv())
            result *=-1
        return result
    }
    //TODO hier muss das mit dem Float noch gemacht werden...
    fun combineFltBytesToNumber():Float{
        val decpart:Int = combineIntBytesToNumber()
        val Fltstart:Int = this.getIntsize().toInt()
        val decSize:Int = this.getTotalsize().toInt()-Fltstart
        var Fltpart:Float = 0f

        if(decSize > 0) {
            var tmpInt:Int = 0
            for (i in Fltstart until this.getTotalsize().toInt())
                tmpInt = tmpInt shl 8 or (this.Msg[i].toInt() and 0xFF)

            Fltpart = tmpInt/10f.pow(this.getFltSize().toInt())
            if(this.isNgtv())
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
            msglength = Header[HeaderIndizes.TOTALSIZE.value].toUInt()
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
    fun isNgtv():Boolean{
        return (this.Header[HeaderIndizes.ADITTIONALINF.value].toInt() and 0x02) != 0
    }
    fun getTotalsize():Short{
        return Header[HeaderIndizes.TOTALSIZE.value].toShort()
    }
    fun getIntsize():Short{
        return Header[HeaderIndizes.INTSIZE.value].toShort()
    }
    fun getTyp():Short{
        return Header[HeaderIndizes.TYP.value].toShort()
    }
    fun getmsgID():Short{
        return Header[HeaderIndizes.MSGID.value].toShort()
    }
    fun getadditByte():Short{
        return Header[HeaderIndizes.ADITTIONALINF.value].toShort()
    }

    fun NoFunctionFound(){
        Log.e(TAG, "There was no fitting function found... that can't be actually")
        this.Logpckg()
    }
    //TODO hier ist irgendwas mit Reflexion falsch des Weiteren gibt es Reflexion erst seit Android 26 also sowieso blöd
    fun getInterpretFunc():()->Any{
        if(this.isTxt()){
            Log.i(TAG, "It is Text")
            return this::combineTxtBytes
        }
        if(this.isInt()){
            Log.i(TAG, "It is Int")
            return this::combineIntBytesToNumber
        }
        if(this.isFloat()){
            Log.i(TAG, "It is Float")
            return this::combineFltBytesToNumber
        }

        return this::NoFunctionFound
    }
    fun isFloat():Boolean{
        return (this.Header[HeaderIndizes.ADITTIONALINF.value].toInt() and 0x01) == 0x01
    }
    //Es ist ein Int wenn es kein float und kein Text ist
    fun isInt():Boolean{
        return (!this.isFloat() and !this.isTxt())
    }
    fun isTxt():Boolean{
        return ((this.Header[HeaderIndizes.ADITTIONALINF.value].toInt() and 0x04) == 0x04)
    }
    fun isMsg():Boolean{
        return ((this.getTyp().toByte() == HeaderTypes.MSG.value) and this.isTxt())
    }
    fun Logpckg(){
        Log.d(TAG, "Header:")
        for(byte in Header)
            Log.d(TAG,byte.toUByte().toString(2).padStart(8, '0'))
        Log.d(TAG, "Msg:")
        for(byte in Msg)
            Log.d(TAG,byte.toUByte().toString(2).padStart(8, '0'))

    }

    fun calculateCRC(): UByte {
        var crc: UByte = 0u

        // Überspringe das SFD
        for (i in 1 until HeaderIndizes.TOTALHEADERSIZE.value) {
            crc = crc xor this.Header[i].toUByte()

            for (j in 0 until 8) {
                if ((crc.toInt() and 0x80) != 0) {
                    crc = ((crc.toInt() shl 1) xor GEN_POLYNOM.toInt()).toUByte()
                } else {
                    crc = (crc.toInt() shl 1).toUByte()
                }
            }
        }

        crc = crc xor this.msglength.toUByte()

        for (i in 0u until this.msglength) {
            crc = crc xor this.Msg[i.toInt()].toUByte()

            for (j in 0 until 8) {
                if ((crc.toInt() and 0x80) != 0) {
                    crc = ((crc.toInt() shl 1) xor GEN_POLYNOM.toInt()).toUByte()
                } else {
                    crc = (crc.toInt() shl 1).toUByte()
                }
            }
        }

        println("\n\nBerechnete CRC: $crc\n\n")
        return crc
    }
    fun makeCRC(){
        this.CRC = this.calculateCRC()
    }
    fun checkCRC():Boolean{
        return this.CRC == this.calculateCRC()
    }
}