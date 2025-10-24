package com.example.vizible.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.vizible.config.AppConfig
import com.example.vizible.data.ObstacleAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TextToSpeechEngine(private val context: Context) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)
    
    init {
        tts = TextToSpeech(context, this)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle language not supported
                return
            }
            isInitialized = true
            
            // Set speech rate and pitch
            tts?.setSpeechRate(AppConfig.TTS_SPEECH_RATE)
            tts?.setPitch(AppConfig.TTS_PITCH)
        }
    }
    
    /**
     * Generate audio alert for obstacle detection
     */
    fun speakObstacleAlert(alert: ObstacleAlert) {
        if (!isInitialized) return
        
        val message = buildObstacleMessage(alert)
        speak(message)
    }
    
    /**
     * Generate audio alert for basic obstruction
     */
    fun speakBasicObstruction(direction: String?, distance: Int) {
        if (!isInitialized || direction == null) return
        
        val message = "Obstruction in $direction at $distance centimeters"
        speak(message)
    }
    
    /**
     * Generate audio alert for detected objects
     */
    fun speakObjectDetection(direction: String?, distance: Int, objects: List<String>?) {
        if (!isInitialized || direction == null || objects == null) return
        
        val objectText = if (objects.size == 1) {
            objects.first()
        } else {
            objects.joinToString(", ")
        }
        
        val message = "$objectText obstacle in $direction at $distance centimeters"
        speak(message)
    }
    
    private fun buildObstacleMessage(alert: ObstacleAlert): String {
        return if (alert.objects != null && alert.objects.isNotEmpty()) {
            val objectText = if (alert.objects.size == 1) {
                alert.objects.first()
            } else {
                alert.objects.joinToString(", ")
            }
            "$objectText obstacle in ${alert.direction} at ${alert.distance} centimeters"
        } else {
            "Obstruction in ${alert.direction} at ${alert.distance} centimeters"
        }
    }
    
    private fun speak(text: String) {
        scope.launch {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "obstacle_alert")
        }
    }
    
    /**
     * Stop current speech
     */
    fun stop() {
        tts?.stop()
    }
    
    /**
     * Shutdown the TTS engine
     */
    fun shutdown() {
        tts?.shutdown()
    }
}
