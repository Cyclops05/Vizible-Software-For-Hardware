# Vizible - Bluetooth Serial Monitor App

A Bluetooth serial monitor Android app that processes Arduino sensor data and generates audio alerts for obstacle detection.

## Features

- **Bluetooth Serial Communication**: Connects to Arduino devices via Bluetooth
- **Real-time Sensor Data Processing**: Parses ultrasonic sensor data in format "Front: 12cm | Left: 14cm | Right: 15cm"
- **Audio Alerts**: Generates text-to-speech alerts when obstacles are detected within 125cm
- **API Integration**: Fetches object detection data from Python server
- **Background Operation**: Runs as a foreground service for continuous monitoring

## Setup Instructions

### 1. Arduino Setup

Ensure your Arduino is configured to send data in the following format:
```
Front: 12cm | Left: 14cm | Right: 15cm
```

### 2. FastAPI Server Setup

Your FastAPI server should be running with the following endpoint:
- **URL**: `http://your-server-ip:5000/detections`
- **Response Format**: `{"detections": "Front:{object1,object2} | Right:{object3,object4} | Left:{object5,object6}"}`

To run your FastAPI server:
```bash
python your_fastapi_server.py
```

Make sure to:
1. Update the camera URLs in your FastAPI code to point to your actual camera streams
2. Ensure the YOLOv8 model file (`yolov8s.pt`) is in the same directory
3. The server will run on port 5000 by default

### 3. Android App Configuration

1. Update the API server URL in `app/src/main/java/com/example/vizible/config/AppConfig.kt`:
   ```kotlin
   const val API_BASE_URL = "http://YOUR_SERVER_IP:5000/"
   ```

2. Build and install the app on your Android device

## Usage

1. **Launch the App**: Open the Vizible app on your Android device
2. **Grant Permissions**: Allow Bluetooth and location permissions when prompted
3. **Pair Arduino Device**: Ensure your Arduino is paired with your Android device
4. **Connect**: Tap "Show Paired Devices" and select your Arduino device
5. **Monitor**: The app will run in the background and provide audio alerts when obstacles are detected

## Audio Alerts

The app generates audio alerts in the following scenarios:

### Basic Obstruction Alert
When distance is less than 125cm:
- "Obstruction in front at 112 centimeters"
- "Obstruction in left at 98 centimeters"
- "Obstruction in right at 85 centimeters"

### Object Detection Alert
When objects are detected via API:
- "car obstacle in front at 112 centimeters"
- "person, tree obstacle in left at 98 centimeters"

## Technical Details

### Architecture
- **MainActivity**: UI for device selection and connection
- **BluetoothSerialService**: Background service handling Bluetooth communication
- **DataParser**: Parses sensor data and object detection data
- **TextToSpeechEngine**: Generates audio alerts
- **ApiService**: Handles HTTP communication with Python server

### Permissions Required
- `BLUETOOTH`
- `BLUETOOTH_ADMIN`
- `BLUETOOTH_CONNECT`
- `BLUETOOTH_SCAN`
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `FOREGROUND_SERVICE`
- `WAKE_LOCK`

### Dependencies
- Bluetooth Serial Library
- Retrofit for HTTP communication
- Kotlin Coroutines for async operations
- Android TTS for audio alerts

## Troubleshooting

1. **Connection Issues**: Ensure Bluetooth is enabled and Arduino is paired
2. **No Audio**: Check device volume and TTS settings
3. **API Errors**: Verify server URL and network connectivity
4. **Permission Denied**: Grant all required permissions in device settings

## Configuration

Key configuration constants in `AppConfig.kt`:
- `OBSTACLE_THRESHOLD_CM`: Distance threshold for alerts (default: 125cm)
- `API_BASE_URL`: Python server endpoint URL
- `TTS_SPEECH_RATE`: Text-to-speech rate (default: 0.8)
- `TTS_PITCH`: Text-to-speech pitch (default: 1.0)

## License

This project is licensed under the MIT License.
