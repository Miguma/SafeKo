package com.example.safeko.utils

import android.location.Location
import com.example.safeko.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PlaceSearcher {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/search"
    
    suspend fun searchPlaces(query: String, userLat: Double, userLon: Double): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Construct URL
                // We prioritize results near the user by using the 'viewbox' or simply by checking distance later.
                // However, Nominatim has a 'viewbox' parameter but it requires a box.
                // A simpler way is to just search and let Nominatim handle relevancy, or append "near me" or "in [City]".
                // But appending "near me" doesn't work well with Nominatim.
                // We can use 'viewbox' if we construct a box around the user.
                // Let's create a rough box of ~30km around the user (0.3 degrees).
                // 1 degree lat ~= 111km.
                val boxSize = 0.3
                val viewbox = "${userLon - boxSize},${userLat + boxSize},${userLon + boxSize},${userLat - boxSize}"
                
                // q: Query
                // format: json
                // limit: 15
                // viewbox: x1,y1,x2,y2 (left, top, right, bottom)
                // bounded: 1 (restrict to viewbox) - REQUIRED for "only cebu" behavior
                // countrycodes: ph (Philippines) - Extra safety
                // addressdetails: 1
                
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlString = "$BASE_URL?q=$encodedQuery&format=json&limit=15&viewbox=$viewbox&bounded=1&countrycodes=ph&addressdetails=1"
                
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "SafeKoApp/1.0") // Required by Nominatim
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)
                    val results = mutableListOf<SearchResult>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val lat = item.getDouble("lat")
                        val lon = item.getDouble("lon")
                        val name = item.optString("display_name", "Unknown Place").split(",").firstOrNull() ?: "Unknown"
                        val fullAddress = item.optString("display_name", "")
                        val type = item.optString("type", "")
                        
                        // Calculate distance
                        val distMeters = calculateDistance(userLat, userLon, lat, lon)
                        val distString = if (distMeters < 1000) {
                            "${distMeters.toInt()} m"
                        } else {
                            String.format(Locale.US, "%.1f km", distMeters / 1000)
                        }
                        
                        results.add(
                            SearchResult(
                                id = item.optLong("place_id", i.toLong()),
                                name = name,
                                address = fullAddress,
                                status = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, // e.g. "hospital" -> "Hospital"
                                distance = distString,
                                imageRes = null, // Will be set in UI based on type
                                lat = lat,
                                lon = lon
                            )
                        )
                    }
                    
                    // Sort by distance (since Nominatim might return relevance over distance)
                    results.sortedBy { 
                        val distStr = it.distance
                        if (distStr.contains("km")) {
                            distStr.replace(" km", "").toDouble() * 1000
                        } else {
                            distStr.replace(" m", "").toDouble()
                        }
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
