package com.example.vizible.utils

import com.example.vizible.data.SensorReading
import java.util.regex.Pattern

object DataParser {
    
    private val sensorPattern = Pattern.compile(
        "Front:\\s*(\\d+)cm\\s*\\|\\s*Left:\\s*(\\d+)cm\\s*\\|\\s*Right:\\s*(\\d+)cm",
        Pattern.CASE_INSENSITIVE
    )
    
    private val objectPattern = Pattern.compile(
        "Front:\\s*\\{([^}]*)\\}\\s*\\|\\s*Right:\\s*\\{([^}]*)\\}\\s*\\|\\s*Left:\\s*\\{([^}]*)\\}",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * Parse sensor data from Arduino format: "Front: 12cm | Left: 14cm | Right: 15cm"
     */
    fun parseSensorData(data: String?): SensorReading? {
        if (data == null) return null
        
        try {
            val matcher = sensorPattern.matcher(data.trim())
            if (matcher.matches()) {
                val front = matcher.group(1).toInt()
                val left = matcher.group(2).toInt()
                val right = matcher.group(3).toInt()
                return SensorReading(front, left, right)
            }
        } catch (e: Exception) {
            // Log error if needed
        }
        return null
    }
    
    /**
     * Parse object detection data from API format: "Front:{x,x,x,x,x} | Right:{x,x,x,x,x} | Left:{x,x,x,x,x}"
     */
    fun parseObjectData(data: String?): Triple<List<String>, List<String>, List<String>>? {
        if (data == null) return null
        
        try {
            val matcher = objectPattern.matcher(data.trim())
            if (matcher.matches()) {
                val frontObjects = parseObjectList(matcher.group(1))
                val rightObjects = parseObjectList(matcher.group(2))
                val leftObjects = parseObjectList(matcher.group(3))
                return Triple(frontObjects, rightObjects, leftObjects)
            }
        } catch (e: Exception) {
            // Log error if needed
        }
        return null
    }
    
    private fun parseObjectList(objectString: String?): List<String> {
        if (objectString == null || objectString.isBlank()) {
            return emptyList()
        }
        
        return objectString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    /**
     * Check if any distance is less than threshold (125cm)
     */
    fun hasObstacle(sensorReading: SensorReading, threshold: Int = 125): Boolean {
        return sensorReading.front < threshold || 
               sensorReading.left < threshold || 
               sensorReading.right < threshold
    }
    
    /**
     * Get obstacles below threshold
     */
    fun getObstacles(sensorReading: SensorReading, threshold: Int = 125): List<String> {
        val obstacles = mutableListOf<String>()
        
        if (sensorReading.front < threshold) {
            obstacles.add("front")
        }
        if (sensorReading.left < threshold) {
            obstacles.add("left")
        }
        if (sensorReading.right < threshold) {
            obstacles.add("right")
        }
        
        return obstacles
    }
}
