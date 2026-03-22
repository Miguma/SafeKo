package com.example.safeko.utils

import com.example.safeko.model.RemoteAlert
import kotlin.math.*

// Clustering radius in meters
const val CLUSTER_RADIUS_METERS = 150.0

// Data class for clustered alerts
data class ClusteredAlert(
    val clusterId: String,              // Unique cluster ID
    val centerLat: Double,              // Center of cluster
    val centerLon: Double,              // Center of cluster
    val alerts: List<RemoteAlert>,      // All alerts in cluster
    val totalPings: Int,                // Sum of all pingCounts
    val highestSeverityAlert: RemoteAlert,  // Alert with highest priority
    val isCritical: Boolean             // true if any alert has pingCount >= 3
)

/**
 * Severity priority: Medical (1) > Fire (2) > Car Accident (3)
 */
fun getAlertSeverity(type: String): Int = when (type) {
    "Emergency Rescue" -> 1    // Medical - highest priority
    "Fire Emergency" -> 2       // Fire - middle priority
    "Road Accident" -> 3        // Car Accident - lowest priority
    else -> 4
}

/**
 * Calculate distance between two coordinates in meters
 * Uses Haversine formula
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusM = 6371e3 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}

/**
 * Cluster alerts by proximity and return clustered alert data
 * If any alert in a cluster has pingCount >= 3, the entire cluster becomes "critical" (red pulse)
 */
fun clusterAlerts(alerts: List<RemoteAlert>): List<ClusteredAlert> {
    if (alerts.isEmpty()) return emptyList()

    val visited = mutableSetOf<String>()
    val clusters = mutableListOf<ClusteredAlert>()

    for (alert in alerts) {
        if (alert.id in visited) continue

        // Start a new cluster with this alert
        val cluster = mutableListOf(alert)
        visited.add(alert.id)

        // Find all alerts within CLUSTER_RADIUS_METERS
        for (other in alerts) {
            if (other.id !in visited) {
                val distance = calculateDistance(alert.lat, alert.lon, other.lat, other.lon)
                if (distance <= CLUSTER_RADIUS_METERS) {
                    cluster.add(other)
                    visited.add(other.id)
                }
            }
        }

        // Calculate cluster properties
        val totalPings = cluster.sumOf { it.pingCount }
        val highestSeverity = cluster.minByOrNull { getAlertSeverity(it.type) } ?: cluster[0]
        val centerLat = cluster.map { it.lat }.average()
        val centerLon = cluster.map { it.lon }.average()
        val isCritical = cluster.any { it.pingCount >= 3 }

        clusters.add(
            ClusteredAlert(
                clusterId = alert.id,  // Use first alert's ID as cluster ID
                centerLat = centerLat,
                centerLon = centerLon,
                alerts = cluster,
                totalPings = totalPings,
                highestSeverityAlert = highestSeverity,
                isCritical = isCritical
            )
        )
    }

    return clusters
}

/**
 * Get the display color for a cluster
 * RED if critical (pingCount >= 3), otherwise use highest severity alert's color
 */
fun getClusterColor(cluster: ClusteredAlert): String = if (cluster.isCritical) {
    "#E53935"  // Red for critical
} else {
    when (cluster.highestSeverityAlert.type) {
        "Emergency Rescue" -> "#E53935"      // Red for Medical
        "Fire Emergency" -> "#FF6F00"        // Orange for Fire
        "Road Accident" -> "#F9A825"         // Dark Amber for Accident
        else -> "#9C27B0"                    // Purple default
    }
}