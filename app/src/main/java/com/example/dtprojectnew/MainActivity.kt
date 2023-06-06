package com.example.dtprojectnew


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.example.dtprojectnew.databinding.ActivityMainBinding
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import kotlin.random.Random
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.bluetoothtest.MassPermission
import java.util.UUID

data class MonitoredData(
    val speed: Float,
    val height: Float,
    val pulse: Int,
    val location: String
)
val BtInterface:BluetoothInterface = BluetoothInterface()

val UI:UIInterface = UIInterface()
val Obs:ConnectionObserver = ConnectionObserver()

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var fnd: BluetoothDevice

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityMainBinding
    private val recordedData = mutableListOf<MonitoredData>()
    private var isRecording = false
    private lateinit var mMap: GoogleMap
    private fun getDataFromSensors(): MonitoredData {
        return MonitoredData(UI.Speed, UI.Degree, UI.Bpm, UI.Location)
    }

    @SuppressLint("MissingPermission")
    fun ConnectwithDevice(view: MainActivity) {
        Log.v("Main", "You clicked the right Button. There's just one but still good job.")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)

        if (bluetoothManager.adapter == null)
            Log.v(
                "Main",
                "How do you not have Bluetooth? Bro add me on Discord I wanna know: FUKA Strong Woman#2494"
            )
        if (bluetoothManager.adapter?.isEnabled == false){
            Log.v("Error", "Bluetooth is off. Someone needs to be punished")
            Log.i("Main", "I would like to turn Bluetooth on but someone has Android API 33.")
            //btn.apply{text = "Activate Bluetooth and Click again"}
        }

        else if (bluetoothManager.adapter?.isEnabled == true) {
            Log.v("Main", "Bluetooth on. Such a good little Boy")

            //Bluetooth Connect ist api level 31/Bluetooth Scan ebenfalls/BackgroundLocation api level 29
            val Bluetooth_Prms = MassPermission(this@MainActivity, this as Activity, listOf(Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
                ,1)

            //TODO funktioniert noch nicht richtig
            Bluetooth_Prms.grant_prms()
            //val LH = Locationhandler(this@MainActivity)


            //Log.i("Main","Can get Location ${LH.canGetLocation()}")
            //guckt den Kontext an in dem wir uns befinden und checkt die derzeitigen Permissions
            if (Bluetooth_Prms.check_for_perm(Manifest.permission.BLUETOOTH_ADMIN)
                && Bluetooth_Prms.check_for_perm(Manifest.permission.BLUETOOTH)
            ) {
                //
                val pairedDevices: Set<BluetoothDevice>? = bluetoothManager.adapter?.bondedDevices
                Log.d("Main", "All paired Devices")
                pairedDevices?.forEach { device ->
                    Log.d("Main", "Name " + device.name)
                    Log.d("Main", "MAC  " + device.address)
                }
                val searched_adress = "00:22:09:01:02:9E"
                val wanted = pairedDevices?.find { it.address == searched_adress }
                if (wanted != null) {
                    Log.v("Main", "Found Adress " + wanted.address + " with name " + wanted.name)
                    //btn.apply{text = "Connecting..."}
                    val Adpt:BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device = Adpt.getRemoteDevice(wanted.address)
                    if(BtInterface.set_Socket(device, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))){
                        //TODO trenn das obere möglichst bald wieder in zwei BtInterface.connect_to_Socket()
                        Obs.addInterface(UI)
                        BtInterface.addObserver(Obs)
                        BtInterface.start()
                    }
                } else {
                    Log.v("Main", "Adress Not Found I Scan for a device")
                    //muss eigetentlich wahrscheinlich nach coarse location und fine location überprüfen

                    //btn.apply{text = "Searching for nearby Devices..."}
                    val filter = IntentFilter()
                    filter.addAction(BluetoothDevice.ACTION_FOUND)
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    registerReceiver(disc,filter)
                    bluetoothManager.adapter.startDiscovery()
                    //if(!bluetoothManager.adapter.startDiscovery()) btn.apply{text = "Activate Location on your Device and try again"}
                }
            } else
                Log.v("Error", "Not the Permission to watch bluetooth")
            //TODO get the Permission to use Bluetooth
        } else
            Log.v("Error", "Bluetooth is somehow not possible. Women am I right.")
    }
    @SuppressLint("MissingPermission")
    var disc = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val TAG = "BroadcastReceiver"
            val msg = intent?.action.toString()
            Log.d(TAG,"I try so hard $msg")
            when(msg){
                BluetoothAdapter.ACTION_DISCOVERY_STARTED->
                    Log.i(TAG, "Discovery started")
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{
                    Log.i(TAG, "Finished Discovery")
                    //Überprüfe ein Gerät gefunden wurde mit passender IP
                    if(::fnd.isInitialized && fnd != null){
                        //Wenn Gerät gefunden öffne einen Socket zu dem Gerät und starte dann das Interface
                        if(BtInterface.set_Socket(fnd, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))){
                            //TODO trenn das obere möglichst bald wieder in zwei BtInterface.connect_to_Socket()
                            Obs.addInterface(UI)
                            BtInterface.addObserver(Obs)
                            BtInterface.start()
                        }
                    }
                    else if(!::fnd.isInitialized){
                        Log.i(TAG, "HC-05 probably not in range or turned off")
                    }
                }
                BluetoothDevice.ACTION_FOUND->{
                    Log.i(TAG, "Found Device")
                    //TODO finde nicht deprecated Way
                    val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if(device!=null){
                        //ls.add(device)
                        if(context!=null){
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_ADMIN
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return
                            }
                            Log.d(TAG,"${device.name} ${device.address}")
                            if(device.address == "00:22:09:01:02:9E"){
                                fnd = device
                                Log.d(TAG, "Found the Device")
                            }
                        }
                        else
                            Log.d(TAG,"Es gibt keinen Context?")
                    }
                    else
                        Log.d(TAG,"Es gibt scheinbar kein Device")
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val updateTextViewsRunnable = object : Runnable {
        override fun run() {
            updateTextViews()
            handler.postDelayed(this, 100) // Update text views every 100 ms (1 second)
        }
    }

    private fun updateTextViews() {
        val data = getDataFromSensors()

        binding.tvSpeed.text = "${data.speed}"
        binding.tvHeight.text = "${data.height}"
        binding.tvPulse.text = "${data.pulse}"
        binding.tvLocation.text = "${data.location}"
        updateLocationOnMap(data.location)
    }

    private fun startMonitoringData() {
        if (isRecording) {
            recordedData.add(getDataFromSensors())
            Handler(Looper.getMainLooper()).postDelayed(::startMonitoringData, 5000)
        }
    }

    private fun promptForDataName(): String? {
        var dataName: String? = null

        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Enter a name for the data")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                dataName = editText.text.toString()
                saveRecordedData(dataName!!)
            }
            .setNegativeButton("Cancel", null)
            .show()

        return dataName
    }

    private fun sanitizeFileName(fileName: String, replacementChar: Char = '_'): String {
        // Define a regular expression pattern to match special characters
        val pattern = Regex("[^a-zA-Z0-9.-]")

        // Replace special characters with the specified replacement character
        return pattern.replace(fileName, replacementChar.toString())
    }

    private fun saveRecordedData(dataName: String) {
        // Convert the recorded data to JSON using Gson
        val sanitizedDataName = sanitizeFileName(dataName)
        val fileName = "$sanitizedDataName.json"
        val gson = Gson()
        val jsonData = gson.toJson(recordedData)

        // Save the JSON data to a file

        openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(jsonData.toByteArray())
        }
    }

    private fun onSwitchToggled(isChecked: Boolean) {
        isRecording = isChecked
        if (isChecked) {
            // Start monitoring data
            startMonitoringData()
        } else {
            // Stop monitoring data, prompt for a name, and save the data locally
            val dataName = promptForDataName()
            if (dataName != null) {
                saveRecordedData(dataName)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun updateLocationOnMap(location: String) {
        if(::mMap.isInitialized) {
            mMap.clear()
            val coordinates = location.split(",")
            val latLng = LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())
            mMap.addMarker(MarkerOptions().position(latLng).title("Marker"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("appData", MODE_PRIVATE)
        handler.post(updateTextViewsRunnable)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->
            googleMap.setOnMapClickListener {
                // Do nothing here, we just want to intercept the event
            }

            googleMap.setOnMapLongClickListener {
                // Do nothing here, we just want to intercept the event
            }

            googleMap.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    // The user made a gesture on the map, so disable the ScrollView
                    binding.customScrollView.setScrollingEnabled(false)
                } else if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
                    // The map is being moved due to a programmatic change, so enable the ScrollView
                    binding.customScrollView.setScrollingEnabled(true)
                }
            }
        }
        binding.customScrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Der Benutzer hat die Scrollansicht berÃ¼hrt, also aktivieren wir das Scrollen.
                binding.customScrollView.setScrollingEnabled(true)
            }
            false
        }

        // Creating an instance of the PulseAnimation class
        val pulseAnimation = PulseAnimation()

        // Getting the pulse value from the TextView. If the text cannot be parsed to an Int, it defaults to 0
        val pulseValue = binding.tvPulse.text.toString().toIntOrNull() ?: 0

        // Starting the pulse animation on the ImageView using the pulse value
        pulseAnimation.startAnimation(binding.ivHeart, pulseValue)





        binding.btnConnect.setOnClickListener{
            ConnectwithDevice(this)
        }

        binding.btnLock.setOnClickListener {
            val pckg:Package = Package(HeaderTypes.LOCK.value)
            pckg.intToBytes(1)
            pckg.Logpckg()
            BtInterface.send(pckg)
        }

        binding.btnStorageSelection.setOnClickListener {
            val intent = Intent(this, StorageSelection::class.java)
            startActivity(intent)

        }
        binding.switchStartStop.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggled(isChecked)
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTextViewsRunnable)

        unregisterReceiver(disc)
        //Thread interrupten, dort wird das interrupt flag gesetzt, darauf wird auch überprüft
        BtInterface.interrupt()
    }

}