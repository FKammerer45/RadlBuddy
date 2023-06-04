package com.example.dtprojectnew

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.dtprojectnew.databinding.ActivityStorageBinding
import com.example.dtprojectnew.databinding.ActivityStorageSelectionBinding
import com.google.gson.Gson
import java.io.File
import com.google.gson.reflect.TypeToken

private lateinit var binding: ActivityStorageBinding


data class Data(val speed: Double, val height: Double, val pulse: Int, val location: String)
data class Stats(val min: Double, val max: Double, val avg: Double)

class Storage : AppCompatActivity() {

    private fun displayStats(buttonName: String) {
        val fileName = "$buttonName.json"
        val fileContents = readJsonFile(fileName)

        val gson = Gson()
        val jsonData = readJsonFile("$buttonName.json")
        val dataList = gson.fromJson<List<Data>>(jsonData, object : TypeToken<List<Data>>() {}.type)


        val speedValues = dataList.map { it.speed }
        val pulseValues = dataList.map { it.pulse.toDouble() }

        val speedStats = calculateStats(speedValues)
        val pulseStats = calculateStats(pulseValues)

        binding.tvSpeedStats.text = "Speed: Min ${speedStats.min} / Max ${speedStats.max} / Avg ${speedStats.avg}"
        binding.tvPulseStats.text = "Pulse: Min ${pulseStats.min} / Max ${pulseStats.max} / Avg ${pulseStats.avg}"
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

        // Retrieve the button name passed from the StorageSelection activity
        val buttonName = intent.getStringExtra("buttonName") ?: ""
        displayStats(buttonName)

        binding.backButton.setOnClickListener {
            val intent = Intent(this, StorageSelection::class.java)
            startActivity(intent)
        }


    }
}