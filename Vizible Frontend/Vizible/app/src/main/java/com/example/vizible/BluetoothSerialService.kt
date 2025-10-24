package com.example.vizible

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vizible.audio.TextToSpeechEngine
import com.example.vizible.config.AppConfig
import com.example.vizible.data.ObstacleAlert
import com.example.vizible.data.SensorReading
import com.example.vizible.network.ApiClient
import com.example.vizible.utils.DataParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothSerialService : Service() {
    
    companion object {
        private const val TAG = "BluetoothSerialService"
        
        // Intent actions
        const val ACTION_CONNECT = "com.example.vizible.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vizible.ACTION_DISCONNECT"
        const val ACTION_DATA_RECEIVED = "com.example.vizible.ACTION_DATA_RECEIVED"
        
        // Intent extras
        const val EXTRA_DEVICE_ADDRESS = "com.example.vizible.EXTRA_DEVICE_ADDRESS"
        const val EXTRA_SENSOR_DATA = "com.example.vizible.EXTRA_SENSOR_DATA"
    }
    
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isConnected = false
    
    private lateinit var ttsEngine: TextToSpeechEngine
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothSerialService = this@BluetoothSerialService
    }
    
    override fun onCreate() {
        super.onCreate()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        ttsEngine = TextToSpeechEngine(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                deviceAddress?.let { connectToDevice(it) }
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    private fun connectToDevice(deviceAddress: String) {
        serviceScope.launch {
            try {
                bluetoothAdapter?.let { adapter ->
                    val device = adapter.getRemoteDevice(deviceAddress)
                    connectedDevice = device
                    
                    // Create RFCOMM socket
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(
                        UUID.fromString(AppConfig.BLUETOOTH_SERIAL_UUID) // Standard SPP UUID
                    )
                    
                    bluetoothSocket?.connect()
                    
                    inputStream = bluetoothSocket?.inputStream
                    outputStream = bluetoothSocket?.outputStream
                    
                    isConnected = true
                    
                    // Start foreground service
                    startForeground(AppConfig.NOTIFICATION_ID, createNotification("Connected to ${device.name}"))
                    
                    // Start reading data
                    startReadingData()
                    
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                isConnected = false
            }
        }
    }
    
    private fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnection error", e)
        }
    }
    
    private fun startReadingData() {
        serviceScope.launch {
            val buffer = ByteArray(1024)
            var bufferPosition = 0
            
            while (isConnected) {
                try {
                    val bytes = inputStream?.read(buffer, bufferPosition, buffer.size - bufferPosition)
                    if (bytes != null && bytes > 0) {
                        bufferPosition += bytes
                        val data = String(buffer, 0, bufferPosition)
                        
                        // Process complete lines
                        val lines = data.split('\n')
                        if (lines.size > 1) {
                            // Process all complete lines except the last one (which might be incomplete)
                            for (i in 0 until lines.size - 1) {
                                processSensorData(lines[i].trim())
                            }
                            
                            // Keep the last incomplete line in buffer
                            bufferPosition = lines.last().length
                            System.arraycopy(lines.last().toByteArray(), 0, buffer, 0, bufferPosition)
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading data", e)
                    break
                }
            }
        }
    }
    
    private fun processSensorData(data: String?) {
        if (data == null) return
        
        Log.d(TAG, "Received data: $data")
        
        // Parse sensor data
        val sensorReading = DataParser.parseSensorData(data)
        if (sensorReading != null) {
            mainHandler.post {
                handleSensorData(sensorReading)
            }
        }
    }
    
    private fun handleSensorData(sensorReading: SensorReading) {
        // Check for obstacles
        val obstacles = DataParser.getObstacles(sensorReading, AppConfig.OBSTACLE_THRESHOLD_CM)
        
        obstacles.forEach { direction ->
            val distance = when (direction) {
                "front" -> sensorReading.front
                "left" -> sensorReading.left
                "right" -> sensorReading.right
                else -> 0
            }
            
            // First, speak basic obstruction
            ttsEngine.speakBasicObstruction(direction, distance)
            
            // Then fetch and speak object detection data
            serviceScope.launch {
                fetchAndSpeakObjectData(direction, distance)
            }
        }
        
        // Broadcast sensor data
        val intent = Intent(ACTION_DATA_RECEIVED).apply {
            putExtra(EXTRA_SENSOR_DATA, sensorReading)
        }
        sendBroadcast(intent)
    }
    
    private suspend fun fetchAndSpeakObjectData(direction: String, distance: Int) {
        try {
            val response = ApiClient.apiService.getDetectedObjects()
            val parsedObjects = DataParser.parseObjectData(response.detections)
            
            parsedObjects?.let { (frontObjects, rightObjects, leftObjects) ->
                val objects = when (direction) {
                    "front" -> frontObjects
                    "right" -> rightObjects
                    "left" -> leftObjects
                    else -> emptyList()
                }
                
                if (objects.isNotEmpty()) {
                    mainHandler.post {
                        ttsEngine.speakObjectDetection(direction, distance, objects)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching object data", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConfig.NOTIFICATION_CHANNEL_ID,
                "Bluetooth Serial Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors Bluetooth serial data from Arduino"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, AppConfig.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bluetooth Serial Monitor")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    fun isConnected(): Boolean = isConnected
    
    fun getConnectedDevice(): BluetoothDevice? = connectedDevice
    
    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        ttsEngine.shutdown()
    }
}
