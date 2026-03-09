package com.example.safeko.utils

import com.example.safeko.model.NavigationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL

object RouteFetcher {
    suspend fun fetchRoute(start: LatLng, end: LatLng): Pair<List<LatLng>, List<NavigationStep>> {
        return withContext(Dispatchers.IO) {
            try {
                // Request steps=true
                val urlString =
                    "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson&steps=true"
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val routes = json.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val route = routes.getJSONObject(0)

                        // Parse Geometry
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val points = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
                        }

                        // Parse Steps
                        val steps = mutableListOf<NavigationStep>()
                        val legs = route.optJSONArray("legs")
                        if (legs != null && legs.length() > 0) {
                            val leg = legs.getJSONObject(0)
                            val stepsArray = leg.optJSONArray("steps")
                            if (stepsArray != null) {
                                for (i in 0 until stepsArray.length()) {
                                    val stepJson = stepsArray.getJSONObject(i)
                                    val maneuver = stepJson.getJSONObject("maneuver")
                                    val type = maneuver.optString("type")
                                    val modifier = maneuver.optString("modifier")
                                    val name = stepJson.optString("name")
                                    val distance = stepJson.optDouble("distance")

                                    // Parse location
                                    val locationArr = maneuver.optJSONArray("location")
                                    val lat =
                                        if (locationArr != null) locationArr.getDouble(1) else 0.0
                                    val lon =
                                        if (locationArr != null) locationArr.getDouble(0) else 0.0

                                    // Construct better instruction
                                    val ref = stepJson.optString("ref")
                                    val destination = when {
                                        name.isNotBlank() -> name
                                        ref.isNotBlank() -> ref
                                        else -> ""
                                    }

                                    var displayInstruction = when (type) {
                                        "turn" -> "Turn $modifier" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "new name" -> "Continue" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "depart" -> "Head $modifier"
                                        "arrive" -> "Arrive at destination"
                                        "merge" -> "Merge $modifier" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "on ramp" -> "Take ramp" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "off ramp" -> "Take exit" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "fork" -> "Keep $modifier" + if (destination.isNotBlank()) " onto $destination" else ""
                                        "end of road" -> "Turn $modifier" + if (destination.isNotBlank()) " onto $destination" else ""
                                        else -> if (destination.isNotBlank()) destination else "Continue"
                                    }
                                    
                                    if (displayInstruction.isNotEmpty()) {
                                        displayInstruction = displayInstruction.replaceFirstChar { it.uppercase() }
                                    }

                                    steps.add(
                                        NavigationStep(
                                            instruction = displayInstruction,
                                            maneuverType = type,
                                            maneuverModifier = modifier,
                                            name = name,
                                            distance = distance,
                                            location = LatLng(lat, lon)
                                        )
                                    )
                                }
                            }
                        }

                        return@withContext Pair(points, steps)
                    }
                }
                Pair(emptyList(), emptyList())
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(emptyList(), emptyList())
            }
        }
    }
}
