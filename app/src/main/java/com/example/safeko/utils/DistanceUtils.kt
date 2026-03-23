package com.example.safeko.utils

import kotlin.math.*

/**
 * Calculates distance between two geographic coordinates using Haversine formula
 */
object DistanceUtils {
    
    private const val EARTH_RADIUS_M = 6371000.0  // Earth radius in meters
    
    /**
     * Calculate distance in meters between two coordinates
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in meters
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }
    
    /**
     * Format distance for display
     * @param distanceMeters Distance in meters
     * @return Formatted string (e.g., "500m", "2.5km")
     */
    fun formatDistance(distanceMeters: Double): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()}m"
            else -> "%.1fkm".format(distanceMeters / 1000)
        }
    }
    
    /**
     * Check if responder has reached destination
     * @param distanceMeters Distance in meters
     * @return True if within 15m
     */
    fun hasReachedDestination(distanceMeters: Double): Boolean {
        return distanceMeters <= 15.0
    }
}
