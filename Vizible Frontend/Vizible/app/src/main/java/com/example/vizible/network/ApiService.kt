package com.example.vizible.network

import com.example.vizible.data.SensorReading
import com.example.vizible.config.AppConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Response from your API
data class DetectionResponse(
    val detections: String
)

// Retrofit service interface
interface ApiService {
    @GET("detections")
    suspend fun getDetectedObjects(): DetectionResponse
}

// Singleton Retrofit client
object ApiClient {
    private const val BASE_URL = AppConfig.API_BASE_URL

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
