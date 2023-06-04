package com.example.bluetoothtest

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MassPermission(val Cntxt: Context, val Act: Activity, val Permissions: List<String> = listOf<String>(),val nmb: Int) {
    val TAG = "MassPermission"
    fun grant_prms(){
        ActivityCompat.requestPermissions(Act,Permissions.toTypedArray(),nmb)
        for(i in Permissions){
            check_for_perm(i)
        }
    }
    fun check_for_perm(Prm:String):Boolean{
        if(ContextCompat.checkSelfPermission(Cntxt,Prm) == PackageManager.PERMISSION_GRANTED){
            Log.i(TAG,"$Prm is granted")
            return true
        }
        else if(ContextCompat.checkSelfPermission(Cntxt,Prm) == PackageManager.PERMISSION_DENIED)
            Log.e(TAG, "$Prm is not granted")

        else
            Log.wtf(TAG, "$Prm is neither granted nor not granted")
        return false
    }
}