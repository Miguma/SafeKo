package com.example.safeko.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.safeko.MainActivity
import com.example.safeko.R
import com.example.safeko.model.NavigationStep
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

import com.example.safeko.utils.RouteFetcher
import kotlin.math.*

class NavigationService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Navigation State
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _navigationSteps = MutableStateFlow<List<NavigationStep>>(emptyList())
    val navigationSteps: StateFlow<List<NavigationStep>> = _navigationSteps.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints.asStateFlow()

    private val _remainingRoutePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val remainingRoutePoints: StateFlow<List<LatLng>> = _remainingRoutePoints.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _distanceToNextTurn = MutableStateFlow(0.0)
    val distanceToNextTurn: StateFlow<Double> = _distanceToNextTurn.asStateFlow()
    
    private var destination: LatLng? = null
    private var isRerouting = false
    private var lastRerouteTime = 0L

    companion object {
        const val CHANNEL_ID = "navigation_channel"
        const val NOTIFICATION_ID = 123
        const val OFF_ROUTE_THRESHOLD_METERS = 50.0 // Distance to trigger reroute
        const val REROUTE_COOLDOWN_MS = 5000 // Minimum time between reroutes
    }

    inner class LocalBinder : Binder() {
        fun getService(): NavigationService = this@NavigationService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateNavigationProgress(location)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Navigation"
            val descriptionText = "Navigation updates"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startNavigation(steps: List<NavigationStep>, points: List<LatLng>, dest: LatLng) {
        if (steps.isEmpty()) return

        _navigationSteps.value = steps
        _routePoints.value = points
        _remainingRoutePoints.value = points
        _currentStepIndex.value = 0
        _isNavigating.value = true
        destination = dest
        isRerouting = false
        lastRerouteTime = 0L

        startForeground(NOTIFICATION_ID, buildNotification("Starting navigation..."))
        requestLocationUpdates()
    }

    fun stopNavigation() {
        _isNavigating.value = false
        _navigationSteps.value = emptyList()
        _routePoints.value = emptyList()
        _remainingRoutePoints.value = emptyList()
        _currentStepIndex.value = 0
        _distanceToNextTurn.value = 0.0
        removeLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(2000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateNavigationProgress(location: android.location.Location) {
        if (!_isNavigating.value || _navigationSteps.value.isEmpty()) return

        val userLatLng = LatLng(location.latitude, location.longitude)
        
        // 1. Check if off-route
        if (!isRerouting && System.currentTimeMillis() - lastRerouteTime > REROUTE_COOLDOWN_MS) {
             val minDistance = getMinDistanceToRoute(userLatLng, _routePoints.value)
             if (minDistance > OFF_ROUTE_THRESHOLD_METERS) {
                 triggerReroute(userLatLng)
                 return
             }
        }

        // Update remaining route for "dissolving" effect
        updateRemainingRoute(userLatLng)

        val steps = _navigationSteps.value
        val currentIndex = _currentStepIndex.value

        // Target the END of the current step (start of next step)
        val targetIndex = currentIndex + 1
        if (targetIndex < steps.size) {
            val targetStep = steps[targetIndex]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                location.latitude, location.longitude,
                targetStep.location.latitude, targetStep.location.longitude,
                results
            )
            val distance = results[0].toDouble()
            _distanceToNextTurn.value = distance

            // Update Notification
            updateNotification(steps[currentIndex].instruction, distance)

            // Threshold for next step (e.g., 30 meters)
            if (distance < 30) {
                _currentStepIndex.value = currentIndex + 1
                
                // If this was the last step, finish navigation
                if (_currentStepIndex.value >= steps.size - 1) {
                    stopNavigation() // Or handle arrival logic
                }
            }
        } else {
             // We are at the end
             _distanceToNextTurn.value = 0.0
             // Optional: Stop navigation if close to final destination
             val lastStep = steps.last()
             val results = FloatArray(1)
             android.location.Location.distanceBetween(
                 location.latitude, location.longitude,
                 lastStep.location.latitude, lastStep.location.longitude,
                 results
             )
             if (results[0] < 20) {
                 stopNavigation()
             }
        }
    }

    private fun triggerReroute(currentLocation: LatLng) {
        val dest = destination ?: return
        isRerouting = true
        lastRerouteTime = System.currentTimeMillis()
        
        serviceScope.launch {
            // Notify UI via notification (optional)
            updateNotification("Rerouting...", 0.0)

            val (points, steps) = RouteFetcher.fetchRoute(currentLocation, dest)
            if (points.isNotEmpty() && steps.isNotEmpty()) {
                _navigationSteps.value = steps
                _routePoints.value = points
                _remainingRoutePoints.value = points
                _currentStepIndex.value = 0
                // Stay navigating
            }
            isRerouting = false
        }
    }

    // Helper to find minimum distance from point to any segment in the route
    private fun getMinDistanceToRoute(point: LatLng, route: List<LatLng>): Double {
        if (route.size < 2) return 0.0
        var minDst = Double.MAX_VALUE
        
        // Check segments
        for (i in 0 until route.size - 1) {
            val dist = distanceToSegment(point, route[i], route[i+1])
            if (dist < minDst) minDst = dist
        }
        return minDst
    }

    // Calculate distance from point P to segment AB
    private fun distanceToSegment(p: LatLng, a: LatLng, b: LatLng): Double {
        val pLat = Math.toRadians(p.latitude)
        val pLon = Math.toRadians(p.longitude)
        val aLat = Math.toRadians(a.latitude)
        val aLon = Math.toRadians(a.longitude)
        val bLat = Math.toRadians(b.latitude)
        val bLon = Math.toRadians(b.longitude)

        // Project P onto line AB
        // Simplified flat-earth projection for small distances is sufficient and much faster
        // x = (lon - lon0) * cos(lat0)
        // y = lat - lat0
        val x = (pLon - aLon) * cos((aLat + pLat) / 2)
        val y = pLat - aLat
        val dx = (bLon - aLon) * cos((aLat + bLat) / 2)
        val dy = bLat - aLat

        val dot = x * dx + y * dy
        val lenSq = dx * dx + dy * dy
        val param = if (lenSq != 0.0) dot / lenSq else -1.0

        val (xx, yy) = if (param < 0) {
            0.0 to 0.0 // P is closest to A
        } else if (param > 1) {
            dx to dy // P is closest to B
        } else {
            dx * param to dy * param // P is closest to projection
        }

        val dLon = xx - x
        val dLat = yy - y
        
        // Convert back to meters (approx 6371000 meters radius)
        val distRad = sqrt(dLon * dLon + dLat * dLat)
        return distRad * 6371000
    }

    private fun updateRemainingRoute(userLocation: LatLng) {
        val currentRoute = _remainingRoutePoints.value
        if (currentRoute.isEmpty()) return

        // Search within the first few segments (e.g., 5) to find the closest segment
        // If the user has moved far along, we assume the previous points are already removed.
        val searchLimit = min(5, currentRoute.size - 1)
        var minDst = Double.MAX_VALUE
        var closestSegmentIndex = -1
        
        for (i in 0 until searchLimit) {
            val dist = distanceToSegment(userLocation, currentRoute[i], currentRoute[i+1])
            if (dist < minDst) {
                minDst = dist
                closestSegmentIndex = i
            }
        }

        // Only update if we are close to the route (e.g., within 50m) to avoid erratic jumps
        if (closestSegmentIndex != -1 && minDst < 50.0) {
             val newRoute = mutableListOf<LatLng>()
             // Start from user location to create a smooth connection
             newRoute.add(userLocation)
             // Add remaining points starting from the end of the current segment
             for (j in (closestSegmentIndex + 1) until currentRoute.size) {
                 newRoute.add(currentRoute[j])
             }
             _remainingRoutePoints.value = newRoute
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Navigation Active")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(instruction: String, distance: Double) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = "$instruction (${distance.toInt()}m)"
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
