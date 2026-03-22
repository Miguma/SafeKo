package com.example.safeko.model

import org.maplibre.android.geometry.LatLng

data class NavigationStep(
    val instruction: String,
    val maneuverType: String,
    val maneuverModifier: String?,
    val name: String,
    val distance: Double,
    val location: LatLng
)

data class RemoteAlert(
    val id: String = "",
    val type: String,
    val lat: Double,
    val lon: Double,
    val notes: String = "",
    val address: String = "",
    val details: String = "",
    val photoBase64: String = "",
    val userId: String? = null,
    val userName: String? = null,
    val userPhotoUrl: String? = null,
    val timestamp: Long = 0L,
    val status: String = "active",
    val pingCount: Int = 1  // Track duplicates at same location
)
