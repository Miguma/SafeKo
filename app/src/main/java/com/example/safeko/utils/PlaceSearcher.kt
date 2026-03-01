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
                // Added bounded=1 to strictly restrict search to the viewbox (near user)
                val urlString = "$BASE_URL?q=$encodedQuery&format=json&limit=15&viewbox=$viewbox&bounded=1&countrycodes=ph&addressdetails=1"
                
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "SafeKoApp/1.0 (contact@safeko.app)") // Required by Nominatim
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
                        
                        // Create result
                        val result = SearchResult(
                            id = item.optLong("place_id", i.toLong()),
                            name = name,
                            address = fullAddress,
                            status = type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, // e.g. "hospital" -> "Hospital"
                            distance = distString,
                            imageRes = null, // Will be set in UI based on type
                            lat = lat,
                            lon = lon
                        )
                        
                        results.add(result)
                    }
                    
                    // Sort by distance (since Nominatim might return relevance over distance)
                    results.sortBy { 
                        val distStr = it.distance
                        if (distStr.contains("km")) {
                            distStr.replace(" km", "").toDouble() * 1000
                        } else {
                            distStr.replace(" m", "").toDouble()
                        }
                    }
                    
                    results
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "SafeKoApp/1.0 (contact@safeko.app)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val address = json.optJSONObject("address")
                    
                    if (address != null) {
                        val houseNumber = address.optString("house_number", "")
                        val road = address.optString("road", "")
                        val pedestrian = address.optString("pedestrian", "")
                        val residential = address.optString("residential", "")
                        val highway = address.optString("highway", "")
                        val footway = address.optString("footway", "")
                        val path = address.optString("path", "")
                        
                        val neighbourhood = address.optString("neighbourhood", "")
                        val hamlet = address.optString("hamlet", "")
                        val village = address.optString("village", "")
                        val quarter = address.optString("quarter", "") // Often the Barangay
                        val suburb = address.optString("suburb", "") // Sometimes District or Barangay
                        val city = address.optString("city", "")
                        val town = address.optString("town", "")
                        val municipality = address.optString("municipality", "")
                        val county = address.optString("county", "")
                        val state = address.optString("state", "")
                        val region = address.optString("region", "")
                        val postcode = address.optString("postcode", "")
                        val country = address.optString("country", "")
                        
                        // Try to find a specific place name (amenity, building, etc.)
                        var specificPlace = ""
                        val potentialPlaceKeys = listOf(
                            "amenity", "building", "shop", "tourism", "leisure", 
                            "office", "emergency", "historic", "man_made"
                        )
                        for (key in potentialPlaceKeys) {
                            if (address.has(key)) {
                                specificPlace = address.getString(key)
                                break
                            }
                        }

                        // Construct address: Street, Sitio/Zone, Barangay, City
                        var streetPart = when {
                            road.isNotBlank() -> road
                            pedestrian.isNotBlank() -> pedestrian
                            residential.isNotBlank() -> residential
                            highway.isNotBlank() -> highway
                            footway.isNotBlank() -> footway
                            path.isNotBlank() -> path
                            else -> ""
                        }
                        
                        // Sitio / Specific Area
                        val sitioPart = when {
                            neighbourhood.isNotBlank() -> neighbourhood
                            hamlet.isNotBlank() -> hamlet
                            else -> ""
                        }

                        // Barangay / District
                        val barangayPart = when {
                            quarter.isNotBlank() -> quarter
                            suburb.isNotBlank() -> suburb
                            village.isNotBlank() -> village
                            else -> ""
                        }
                        
                        val cityPart = when {
                            city.isNotBlank() -> city
                            town.isNotBlank() -> town
                            municipality.isNotBlank() -> municipality
                            county.isNotBlank() -> county
                            else -> ""
                        }
                        
                        // Fallback: If streetPart is empty, check display_name components
                        if (streetPart.isBlank() && specificPlace.isBlank()) {
                            val displayName = json.optString("display_name", "")
                            val displayParts = displayName.split(",").map { it.trim() }
                            
                            // Collect known administrative parts to stop at (Barangay, City, etc.)
                            val stopTriggers = mutableSetOf<String>()
                            if (sitioPart.isNotBlank()) stopTriggers.add(sitioPart)
                            if (barangayPart.isNotBlank()) stopTriggers.add(barangayPart)
                            if (cityPart.isNotBlank()) stopTriggers.add(cityPart)
                            if (postcode.isNotBlank()) stopTriggers.add(postcode)
                            if (country.isNotBlank()) stopTriggers.add(country)
                            if (state.isNotBlank()) stopTriggers.add(state)
                            if (region.isNotBlank()) stopTriggers.add(region)
                            
                            val unknownParts = mutableListOf<String>()
                            
                            // Find all parts before the first known administrative part
                            for (part in displayParts) {
                                // Check if this part matches any stop trigger
                                val isStopTrigger = stopTriggers.any { known -> 
                                    part.equals(known, ignoreCase = true) || 
                                    (known.length > 4 && part.contains(known, ignoreCase = true)) 
                                }
                                
                                if (isStopTrigger) {
                                    // We hit a known administrative part. Stop collecting.
                                    break
                                }
                                
                                // Ignore house number if we already have it (don't add to street part)
                                val isHouseNumber = houseNumber.isNotBlank() && part.equals(houseNumber, ignoreCase = true)
                                
                                // Also ignore common administrative codes
                                val isIgnored = part.equals("Philippines", ignoreCase = true) || 
                                                part.matches(Regex("\\d{4}")) // Simple postcode check
                                
                                if (!isHouseNumber && !isIgnored) {
                                    unknownParts.add(part)
                                }
                            }
                            
                            if (unknownParts.isNotEmpty()) {
                                streetPart = unknownParts.joinToString(", ")
                            }
                        }

                        val parts = mutableListOf<String>()
                        if (specificPlace.isNotBlank()) parts.add(specificPlace)
                        if (houseNumber.isNotBlank()) parts.add(houseNumber)
                        
                        // Only add streetPart if it's unique
                        if (streetPart.isNotBlank() && 
                            !streetPart.equals(specificPlace, ignoreCase = true) &&
                            !streetPart.equals(sitioPart, ignoreCase = true) &&
                            !streetPart.equals(barangayPart, ignoreCase = true)) {
                            parts.add(streetPart)
                        }
                        
                        if (sitioPart.isNotBlank()) parts.add(sitioPart)
                        if (barangayPart.isNotBlank() && !barangayPart.equals(sitioPart, ignoreCase = true)) parts.add(barangayPart)
                        if (cityPart.isNotBlank()) parts.add(cityPart)
                        
                        if (parts.isNotEmpty()) {
                            parts.joinToString(", ")
                        } else {
                            json.optString("display_name", "")
                        }
                    } else {
                        json.optString("display_name", "")
                    }
                } else {
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
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
