package com.example.vizible.config

object AppConfig {
    // Configuration constants
    const val OBSTACLE_THRESHOLD_CM = 125
    const val BLUETOOTH_SERIAL_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    
    // API Configuration - Update this with your FastAPI server URL
    const val API_BASE_URL = "http://192.168.1.100:5000/" // Replace with your server IP and port
    
    // Notification Configuration
    const val NOTIFICATION_ID = 1
    const val NOTIFICATION_CHANNEL_ID = "bluetooth_serial_channel"
    
    // Text-to-Speech Configuration
    const val TTS_SPEECH_RATE = 0.8f
    const val TTS_PITCH = 1.0f
}
