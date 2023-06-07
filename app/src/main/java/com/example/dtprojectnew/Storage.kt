package com.example.dtprojectnew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dtprojectnew.databinding.ActivityStorageBinding
import com.example.dtprojectnew.databinding.ActivityStorageSelectionBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import java.io.File
import com.google.gson.reflect.TypeToken

private lateinit var binding: ActivityStorageBinding


data class Data(val speed: Double, val height: Double, val pulse: Int, val location: String)
data class Stats(val min: Double, val max: Double, val avg: Double)

class Storage : AppCompatActivity() {

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private fun displayStats(buttonName: String) {
        val fileName = "$buttonName.json"
        val fileContents = readJsonFile(fileName)

        val gson = Gson()
        val jsonData = readJsonFile("$buttonName.json")
        val dataList = gson.fromJson<List<Data>>(jsonData, object : TypeToken<List<Data>>() {}.type)


        val speedValues = dataList.map { it.speed }
        val pulseValues = dataList.map { it.pulse.toDouble() }
        val locationValues = dataList.map { it.location }
        val parsedLocations = locationValues.map {
            val split = it.split(",")
            LatLng(split[0].toDouble(), split[1].toDouble())
        }

        val speedStats = calculateStats(speedValues)
        val pulseStats = calculateStats(pulseValues)

        binding.tvSpeedStats.text = "Speed: Min ${speedStats.min} / Max ${speedStats.max} / Avg ${speedStats.avg}"
        binding.tvPulseStats.text = "Pulse: Min ${pulseStats.min} / Max ${pulseStats.max} / Avg ${pulseStats.avg}"



        mapFragment.getMapAsync { googleMap ->
            // Save a reference to the map for later use
            mMap = googleMap

            // Create a Polyline and add it to the map
            mMap?.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .addAll(parsedLocations)
            )

            // Optionally move the camera to the last location
            if (parsedLocations.isNotEmpty()) {
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(parsedLocations.last(), 15f))
            }
        }
    }



    private fun calculateStats(values: List<Double>): Stats {
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val avg = values.average()
        return Stats(min, max, avg)
    }
    private fun readJsonFile(fileName: String): String {
        val file = File(filesDir, fileName)
        return file.readText()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        // Retrieve the button name passed from the StorageSelection activity
        val buttonName = intent.getStringExtra("buttonName") ?: ""
        displayStats(buttonName)

        binding.backButton.setOnClickListener {
            val intent = Intent(this, StorageSelection::class.java)
            startActivity(intent)
        }


    }
}