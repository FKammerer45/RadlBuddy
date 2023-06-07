package com.example.dtprojectnew

import android.os.Handler
import android.os.Looper

interface MapUpdateProvider {
    fun updateLocationOnMap(location: String)
    fun getDataFromSensors(): MonitoredData
}

class MapUpdateRunnable(private val provider: MainActivity) : Runnable {

    private val handler = Handler(Looper.getMainLooper())

    override fun run() {
        val data = provider.getDataFromSensors()
        provider.updateLocationOnMap(data.location)
        handler.postDelayed(this, 5000) // Update map every 5 seconds
    }

    fun start() {
        handler.post(this)
    }

    fun stop() {
        handler.removeCallbacks(this)
    }
}
