package com.example.vizible.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Sensor readings from different directions
@Parcelize
data class SensorReading(
    val front: Int,  // distance in cm
    val left: Int,   // distance in cm
    val right: Int   // distance in cm
) : Parcelable

// Detected objects in different directions
data class DetectedObjects(
    val front: List<String>,
    val left: List<String>,
    val right: List<String>
)

// Obstacle alert information
data class ObstacleAlert(
    val direction: String,
    val distance: Int,
    val objects: List<String>? = null
)
