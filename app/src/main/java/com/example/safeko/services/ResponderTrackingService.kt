package com.example.safeko.services

import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tracks responder location in real-time while navigating to alert location.
 * Uploads location updates to Firebase activeResponses node.
 */
class ResponderTrackingService {
    
    companion object {
        const val TAG = "ResponderTracking"
        const val UPDATE_INTERVAL_MS = 5000L  // Update every 5 seconds
        const val RTDB_URL = "https://safeko-3ca46-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var isTracking = false
    private var currentAlertId: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startTracking(
        context: Context,
        alertId: String,
        alertLat: Double,
        alertLon: Double,
        onError: (String) -> Unit = {}
    ) {
        if (isTracking) {
            Log.w(TAG, "Tracking already active")
            return
        }

        currentAlertId = alertId
        fusedLocationClient = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(UPDATE_INTERVAL_MS).apply {
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    scope.launch {
                        uploadResponderLocation(
                            alertId = alertId,
                            responderLat = location.latitude,
                            responderLon = location.longitude,
                            alertLat = alertLat,
                            alertLon = alertLon,
                            onError = onError
                        )
                    }
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                null
            )
            isTracking = true
            Log.d(TAG, "✅ Started tracking for alert: $alertId")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Location permission error", e)
            onError("Location permission denied")
        }
    }

    fun stopTracking(alertId: String) {
        if (!isTracking || currentAlertId != alertId) return

        try {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            isTracking = false
            currentAlertId = null
            
            // Clean up active response from Firebase
            scope.launch {
                removeActiveResponse(alertId)
            }
            
            Log.d(TAG, "✅ Stopped tracking for alert: $alertId")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tracking", e)
        }
    }

    private suspend fun uploadResponderLocation(
        alertId: String,
        responderLat: Double,
        responderLon: Double,
        alertLat: Double,
        alertLon: Double,
        onError: (String) -> Unit
    ) {
        try {
            val userId = Firebase.auth.currentUser?.uid ?: return
            val token = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token ?: return
            
            val activeResponsePath = "$RTDB_URL/activeResponses/$alertId.json?auth=$token"

            val json = JSONObject().apply {
                put("responderId", userId)
                put("responderLat", responderLat)
                put("responderLon", responderLon)
                put("alertLat", alertLat)
                put("alertLon", alertLon)
                put("lastUpdate", System.currentTimeMillis())
            }.toString()

            val url = URL(activeResponsePath)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 3000
                readTimeout = 3000
            }

            connection.outputStream.use { it.write(json.toByteArray()) }
            
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e(TAG, "❌ Upload failed: HTTP $responseCode")
                onError("Failed to update location")
            } else {
                Log.d(TAG, "📍 Location uploaded: $responderLat, $responderLon")
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading location", e)
            onError(e.message ?: "Unknown error")
        }
    }

    private suspend fun removeActiveResponse(alertId: String) {
        try {
            val token = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token ?: return
            val url = URL("$RTDB_URL/activeResponses/$alertId.json?auth=$token")
            
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 3000
                readTimeout = 3000
            }
            
            connection.responseCode
            connection.disconnect()
            Log.d(TAG, "✅ Removed active response for alert: $alertId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing active response", e)
        }
    }

    fun isCurrentlyTracking(): Boolean = isTracking
    fun getCurrentAlertId(): String? = currentAlertId
}
