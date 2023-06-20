package com.example.dtprojectnew


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import android.location.LocationManager
import android.nfc.Tag
import androidx.core.app.ActivityCompat
import com.example.bluetoothtest.MassPermission
import com.google.android.gms.maps.model.PolylineOptions
import java.lang.Math.cos
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

data class MonitoredData(
    val speed: Float,
    val height: Float,
    val pulse: Int,
    val location: String,
    val pathM: MutableList<LatLng> = mutableListOf()
)
lateinit var BtInterface:BluetoothInterface

val UI:UIInterface = UIInterface()
val Obs:ConnectionObserver = ConnectionObserver()



//TODO Samir:
//  Das ding schmiert ab und an mal ab weil es irgendwie den Socket nich richtig createn kann.
//  Der Connect Button sollte auch andersherum funktionieren vielleicht, also für nen Disconnect Sorgen bei erneutem drücken.
//  Guck dir nochma den ganzen Ablauf an der müsste besser gehen im Sinne von was passiert wenn, wann Buttons gedrückt werden und was dann passieren soll, vielleicht ein paar Alerts mit Infos gut...

class MainActivity : AppCompatActivity(), OnMapReadyCallback, MapUpdateProvider, ConnectionStatusObserver {
    lateinit var fnd: BluetoothDevice
    private val mapUpdateRunnable = MapUpdateRunnable(this)
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityMainBinding
    private val recordedData = mutableListOf<MonitoredData>()
    private var isRecording = false
    private lateinit var mMap: GoogleMap

    private var samplelocation: String = "48.76508,11.42372"
    private val path: MutableList<LatLng> = mutableListOf()
    private var inmap = false
    private fun moveNorthAndEast() {
        val coordinates = samplelocation.split(",")
        val currentLat = coordinates[0].toDouble()
        val currentLng = coordinates[1].toDouble()

        // Radius of the Earth in meters
        val R = 6371000.0

        // Distance to move in meters
        val dn = 10.0
        val de = 10.0

        // Coordinate offsets in radians
        val dLat = dn / R
        val dLng = de / (R * cos(Math.PI * currentLat / 180))

        // New coordinates in degrees
        val newLat = currentLat + dLat * 180 / Math.PI
        val newLng = currentLng + dLng * 180 / Math.PI

        samplelocation = "$newLat,$newLng"
        // Update the path
        path.add(LatLng(newLat, newLng))

        // Draw the updated path
        drawPath()
    }
    private fun clearMap() {
        if(::mMap.isInitialized) {
            mMap.clear() // Clear the entire map
            path.clear() // Clear the path array
        }
    }

    private fun drawPath() {
        if(::mMap.isInitialized && path.isNotEmpty()) {
            // Clear the old path
            mMap.clear()

            // Add a marker at the current location
            mMap.addMarker(MarkerOptions().position(path.last()).title("Current Location"))

            // Draw the new path
            mMap.addPolyline(PolylineOptions().addAll(path))

            // Move the camera to the last point in the path
            if (!inmap){
                mMap.moveCamera(CameraUpdateFactory.newLatLng(path.last()))
            }
        }
    }

    // Declare the variable
    private lateinit var mapUpdateProvider: MapUpdateProvider
    override fun getDataFromSensors(): MonitoredData {
        //return MonitoredData(UI.Speed, UI.Degree, UI.Bpm, UI.Location)

        return MonitoredData(UI.Speed, UI.Degree, UI.Bpm, UI.Location)
    }


    private var aliveTimer: Timer? = null
    private fun startAliveTimer() {
        val TAG = "Timer"
        aliveTimer = Timer()
        aliveTimer?.schedule(object : TimerTask() {
            override fun run() {
                Log.i(TAG, "Alive message timeout reached. Disconnecting Device")
                disconnectDevice()
            }
        }, 6000) // 6 Sekunden Timeout
    }
    private fun con_disconDevice(){
        val TAG = "con_discnwithDevice"
        if(::BtInterface.isInitialized && BtInterface.isConnected()){
            Log.i(TAG, "Disconnecting Device")
            disconnectDevice()
        }
        else{
            BtInterface = BluetoothInterface(this)
            Log.i(TAG, "Connecting Device")
            if(connectDevice())
                binding.btnConnect.setText("Disconnect")
        }
    }

    override fun onConnectionLost() {
        runOnUiThread {
            binding.btnLock.text = "Lock" // Setze den Text des Buttons auf "Lock"
            disconnectDevice()
        }
    }

    fun disconnectDevice(){
        //Thread interrupten, dort wird das interrupt flag gesetzt, darauf wird auch überprüft
        binding.btnConnect.setText("Connect")
        var pckg:Package = Package(HeaderTypes.CONNECTED.value)
        pckg.intToBytes(0)
        try {BtInterface.send(pckg)}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized couldnt send not connected")
        }
        pckg = Package(HeaderTypes.LOCK.value)
        pckg.intToBytes(1)
        try {BtInterface.send(pckg)}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized couldnt send Close Lock")
        }
        try{BtInterface.interrupt()}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized before interrupt")
        }
    }
    @SuppressLint("MissingPermission")
    private fun connectDevice():Boolean {
        Log.v("Main", "You clicked the right Button. There's just one but still good job.")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)

        if (bluetoothManager.adapter == null){
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Searching went wrong")
            alertDialogBuilder.setMessage("Your Device HAS no bluetooth??")

            val dialog = alertDialogBuilder.create()
            dialog.show()

            return false
        }
        if (bluetoothManager.adapter?.isEnabled == false){
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Searching went wrong")
            alertDialogBuilder.setMessage("Enable Bluetooth and try again")

            val dialog = alertDialogBuilder.create()
            dialog.show()
            return false
        }

        else if (bluetoothManager.adapter?.isEnabled == true) {
            Log.v("Main", "Bluetooth on. Such a good little Boy")

            //Bluetooth Connect ist api level 31/Bluetooth Scan ebenfalls/BackgroundLocation api level 29
            val Bluetooth_Prms = MassPermission(this@MainActivity, this as Activity, listOf(Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
                ,1)

            Bluetooth_Prms.grant_prms()

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
                val searchedadress = "00:22:09:01:02:9E"
                val wanted = pairedDevices?.find { it.address == searchedadress }
                if (wanted != null) {
                    Log.v("Main", "Found Adress " + wanted.address + " with name " + wanted.name)
                    //btn.apply{text = "Connecting..."}
                    val Adpt:BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device = Adpt.getRemoteDevice(wanted.address)

                    if(BtInterface.setSocket(device, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))){
                        //TODO trenn das obere möglichst bald wieder in zwei BtInterface.connect_to_Socket()
                        Obs.addInterface(UI)
                        BtInterface.addObserver(Obs)
                        BtInterface.start()
                        showSavedProfiles()
                        return true
                    }
                    else {
                        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
                        alertDialogBuilder.setTitle("Connecting went wrong")
                        alertDialogBuilder.setMessage("Maybe your Microcontroller is turned off?")

                        val dialog = alertDialogBuilder.create()
                        dialog.show()
                        return false
                    }
                } else {
                    Log.v("Main", "Adress Not Found I Scan for a device")
                    //muss eigetentlich wahrscheinlich nach coarse location und fine location überprüfen
                    if(!isLocationEnabled(this@MainActivity)){
                        val alertDialogBuilder = AlertDialog.Builder(this)
                        alertDialogBuilder.setTitle("Searching went wrong")
                        alertDialogBuilder.setMessage("Enable Location and try again")

                        val dialog = alertDialogBuilder.create()
                        dialog.show()
                        return false
                    }
                    //btn.apply{text = "Searching for nearby Devices..."}
                    val filter = IntentFilter()
                    filter.addAction(BluetoothDevice.ACTION_FOUND)
                    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    registerReceiver(disc,filter)
                    bluetoothManager.adapter.startDiscovery()
                    //if(!bluetoothManager.adapter.startDiscovery()) btn.apply{text = "Activate Location on your Device and try again"}
                }
            } else
                return false

            //TODO get the Permission to use Bluetooth
        } else
            return false
        return false
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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
                        //Wenn Gerät gefunden öffne einen Socket zu dem Gerät und starte dann das Interface sollte man abfangen, dass das Gerät
                        //in der Zwischenzeit ausgemacht wurde?? wäre schon irgendwie Schwachsinn das zu tun bzw vielleicht gehen Baterrien leer
                        if(BtInterface.setSocket(fnd, UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))){
                            //TODO trenn das obere möglichst bald wieder in zwei BtInterface.connect_to_Socket()
                            Obs.addInterface(UI)
                            BtInterface.addObserver(Obs)
                            BtInterface.start()
                            showSavedProfiles()
                            binding.btnConnect.setText("Disconnect")
                        }
                    }
                    else if(!::fnd.isInitialized){
                        Log.e(TAG, "HC-05 probably not in range or turned off")
                        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
                        alertDialogBuilder.setTitle("Searching went wrong")
                        alertDialogBuilder.setMessage("Maybe your Microcontroller is turned off?")

                        val dialog = alertDialogBuilder.create()
                        dialog.show()
                    }
                }
                BluetoothDevice.ACTION_FOUND->{
                    Log.i(TAG, "Found Device")
                    //TODO finde nicht deprecated Way
                    val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if(device!=null){
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
                                //val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                                //bluetoothManager.adapter?.cancelDiscovery()
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
            handler.postDelayed(this, 500) // Update text views every 100 ms (1 second)
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

    override fun updateLocationOnMap(location: String) {

        //Log.i("Mapdebug","outside")
        if(::mMap.isInitialized&&(location!="")) {
            //Log.i("Mapdebug"," 1inupdateLocationOnMap location: $location")
            mMap.clear()
            val coordinates = location.split(",")
            val latLng = LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())
            mMap.addMarker(MarkerOptions().position(latLng).title("Marker"))

            if (!inmap){
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }


            // Update the path
            path.add(LatLng(coordinates[0].toDouble(),coordinates[1].toDouble()))

            // Draw the new path
            mMap.addPolyline(PolylineOptions().addAll(path))

            //sample values
            //moveNorthAndEast()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sharedPreferences = getSharedPreferences("appData", MODE_PRIVATE)
        handler.post(updateTextViewsRunnable)
        mapUpdateRunnable.start()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync { googleMap ->

            mMap = googleMap

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
                    inmap = true
                } else if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION) {
                    // The map is being moved due to a programmatic change, so enable the ScrollView
                    binding.customScrollView.setScrollingEnabled(true)
                    inmap = false
                }
            }
        }
        binding.customScrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Der Benutzer hat die Scrollansicht berÃ¼hrt, also aktivieren wir das Scrollen.
                binding.customScrollView.setScrollingEnabled(true)
                inmap = false
            }
            false
        }

        // Creating an instance of the PulseAnimation class
        val pulseAnimation = PulseAnimation()

        // Getting the pulse value from the TextView. If the text cannot be parsed to an Int, it defaults to 0
        val pulseValue = binding.tvPulse.text.toString().toIntOrNull() ?: 0

        // Starting the pulse animation on the ImageView using the pulse value
        pulseAnimation.startAnimation(binding.ivHeart, pulseValue)



        binding.btnClearMap.setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle("Clear Map")
                .setMessage("Are you sure you want to clear the map?")
                .setPositiveButton("Yes") { dialog, _ ->
                    clearMap()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Do nothing, user cancelled
                    dialog.dismiss()
                }
                .show()
        }

        binding.btnConnect.setOnClickListener{
            con_disconDevice()
        }
        // ich glaube 1 ist also lock und 0 müsste open sein glaub ich...
        binding.btnLock.setOnClickListener {
            val pckg:Package = Package(HeaderTypes.LOCK.value)

            if (!UI.Locked)
                binding.btnLock.setText("Lock")
            else
                binding.btnLock.setText("Unlock")

            pckg.intToBytes(if (!UI.Locked) 1 else 0)
            pckg.Logpckg()
            try{BtInterface.send(pckg)}
            catch(e:UninitializedPropertyAccessException){}
        }

        binding.btnStorageSelection.setOnClickListener {
            val intent = Intent(this, StorageSelection::class.java)
            startActivity(intent)
        }
        binding.switchStartStop.setOnCheckedChangeListener { _, isChecked ->
            onSwitchToggled(isChecked)
        }


    }

    //falls später mal nötig, weil lieber zoll als cm
    //Zoll = Zentimeter / 2,54
    //Zentimeter = Zoll * 2,54
    private fun showSavedProfiles() {
        val TAG:String = "ShowSavedProfiles"
        val profileStorage = ProfileStorage(this)
        val profiles = profileStorage.loadProfiles()

        //Mal gucken wie man das macht, mit der Anzeige, man kann das bestimmt schöner lösen als in einer Zeile
        val profileNames = profiles.map { profile -> "${profile.name}, ${profile.value}cm" }.toTypedArray()

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Saved")
        alertDialogBuilder.setItems(profileNames) { _, index ->
            val selectedProfile = profiles[index]
            //Sending that we are connected
            var pckg:Package = Package(HeaderTypes.CONNECTED.value)
            pckg.intToBytes(1)
            try { BtInterface.send(pckg) }
            catch(e:UninitializedPropertyAccessException){}

            pckg = Package(HeaderTypes.RADIUS.value)
            pckg.floatToBytes(selectedProfile.value, 3)
            pckg.Logpckg()
            try{BtInterface.send(pckg)}
            catch(e:UninitializedPropertyAccessException){}
        }

        alertDialogBuilder.setPositiveButton("Create Profile") { _, _ ->
            showCreateProfileDialog()
        }

        val dialog = alertDialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun showCreateProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.create_profile, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Create Profile")
            .setPositiveButton("Save") { _, _ ->
                val profileName = dialogView.findViewById<EditText>(R.id.etProfileName).text.toString()
                var profileValue = dialogView.findViewById<EditText>(R.id.etProfileValue).text.toString()

                if (profileName.isNotEmpty() && profileValue.isNotEmpty()) {
                    if(profileValue.indexOf(',') != -1)
                        profileValue = profileValue.replaceRange(profileValue.indexOf(','), profileValue.indexOf(',') + 1, '.'.toString())

                    val cleanprofileValue = profileValue.toFloat()

                    val profile = Profile(profileName, cleanprofileValue)
                    val profileStorage = ProfileStorage(this)
                    profileStorage.saveProfile(profile)

                    //Sending that we are connected
                    var pckg:Package = Package(HeaderTypes.CONNECTED.value)
                    pckg.intToBytes(1)
                    try { BtInterface.send(pckg) }
                    catch(e:UninitializedPropertyAccessException){}
                    //Sending the value

                    pckg = Package(HeaderTypes.RADIUS.value)
                    pckg.floatToBytes(profile.value, 3)
                    pckg.Logpckg()
                    try{BtInterface.send(pckg)}
                    catch(e:UninitializedPropertyAccessException){}

                }
            }
            .setNegativeButton("Abort", null)

        val dialog = dialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateTextViewsRunnable)
        mapUpdateRunnable.stop()
        try {
            unregisterReceiver(disc)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered, ignore
        }

        //Thread interrupten, dort wird das interrupt flag gesetzt, darauf wird auch überprüft
        var pckg:Package = Package(HeaderTypes.CONNECTED.value)
        pckg.intToBytes(0)
        try {BtInterface.send(pckg)}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized couldnt send not connected")
        }
        pckg = Package(HeaderTypes.LOCK.value)
        pckg.intToBytes(1)
        try {BtInterface.send(pckg)}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized couldnt send Close Lock")
        }
        try{BtInterface.interrupt()}
        catch(e:UninitializedPropertyAccessException){
            Log.e("OnDestroy", "BtInterface was not inititialized before interrupt")
        }
        super.onDestroy()
    }
}