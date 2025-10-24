package com.example.vizible

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.vizible.data.SensorReading
import com.example.vizible.ui.theme.VizibleTheme

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothService: BluetoothSerialService? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                setupBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
            }
        }

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothSerialService.ACTION_DATA_RECEIVED -> {
                    val sensorData = intent.getParcelableExtra<SensorReading>(
                        BluetoothSerialService.EXTRA_SENSOR_DATA
                    )
                    // Update UI with sensorData if needed
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge rendering
        window.decorView.systemUiVisibility = 0

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Register broadcast receiver
        val filter = IntentFilter(BluetoothSerialService.ACTION_DATA_RECEIVED)
        registerReceiver(dataReceiver, filter)

        setContent {
            VizibleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BluetoothDeviceScreen(
                        modifier = Modifier.padding(innerPadding),
                        onConnectToDevice = { device -> connectToDevice(device) }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            setupBluetooth()
        }
    }

    private fun setupBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val serviceIntent = Intent(this, BluetoothSerialService::class.java).apply {
            action = BluetoothSerialService.ACTION_CONNECT
            putExtra(BluetoothSerialService.EXTRA_DEVICE_ADDRESS, device.address)
        }
        startForegroundService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceScreen(
    modifier: Modifier = Modifier,
    onConnectToDevice: (BluetoothDevice) -> Unit
) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (bluetoothAdapter == null) {
        Text(
            text = "Bluetooth is not supported on this device",
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    var isScanning by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bluetooth Serial Monitor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { pairedDevices = bluetoothAdapter.bondedDevices?.toList() ?: emptyList() },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Show Paired Devices")
        }

        Button(
            onClick = {
                isScanning = !isScanning
                if (isScanning) bluetoothAdapter.startDiscovery() else bluetoothAdapter.cancelDiscovery()
            },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(if (isScanning) "Stop Scanning" else "Scan for Devices")
        }

        if (pairedDevices.isNotEmpty()) {
            Text(
                text = "Paired Devices",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(pairedDevices) { device ->
                    BluetoothDeviceItem(
                        device = device,
                        onConnect = { onConnectToDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceItem(
    device: BluetoothDevice,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(onClick = onConnect) {
                Text("Connect")
            }
        }
    }
}
