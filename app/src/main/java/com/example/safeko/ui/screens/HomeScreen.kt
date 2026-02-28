package com.example.safeko.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.view.Window
import com.example.safeko.model.SearchResult
import com.example.safeko.utils.PlaceSearcher
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import java.net.HttpURLConnection
import java.net.URL

// Helper to find Activity
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ForceImmersiveMode() {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (!view.isInEditMode) {
        DisposableEffect(lifecycleOwner, view) {
            // Try to find the window from the view hierarchy (for Dialogs)
            var parent = view.parent
            var dialogWindow: Window? = null
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    dialogWindow = parent.window
                    break
                }
                parent = parent.parent
            }
            
            val window = dialogWindow ?: context.findActivity()?.window

            fun hideSystemUI() {
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }

            // Hide immediately
            hideSystemUI()

            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hideSystemUI()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val auth = remember { Firebase.auth }

    // State for selected location and marker
    var selectedLocation by remember { mutableStateOf<SearchResult?>(null) }
    var showLocationModal by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<Marker?>(null) }
    var currentRouteOverlay by remember { mutableStateOf<Polyline?>(null) }

    // Force Hide System Navigation Bar (Immersive Sticky)
    ForceImmersiveMode()

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Search Results State
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    
    // Alert Sheet State
    var showAlertSheet by remember { mutableStateOf(false) }
    val alertSheetState = rememberModalBottomSheetState()

    // Map State
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val alertMarkers = remember { mutableStateMapOf<String, Marker>() }

    // Initialize MapLibre immediately
    MapLibre.getInstance(context)

    // MapView
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    // Updated to Asia Southeast 1 (Singapore)
    val rtdbBaseUrl = "https://safeko-3ca46-default-rtdb.asia-southeast1.firebasedatabase.app"

    data class RemoteAlert(
        val type: String,
        val lat: Double,
        val lon: Double
    )

    suspend fun fetchAlerts(): Map<String, RemoteAlert> {
        return withContext(Dispatchers.IO) {
            val url = URL("$rtdbBaseUrl/alerts.json")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            try {
                if (connection.responseCode != 200) {
                    return@withContext emptyMap()
                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                if (response == "null") return@withContext emptyMap()

                val root = JSONObject(response)
                val result = mutableMapOf<String, RemoteAlert>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val obj = root.optJSONObject(id) ?: continue
                    val type = obj.optString("type", "")
                    val lat = obj.optDouble("lat", Double.NaN)
                    val lon = obj.optDouble("lon", Double.NaN)
                    if (type.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                        result[id] = RemoteAlert(type = type, lat = lat, lon = lon)
                    }
                }
                result
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // Helper to get Icon from Drawable resource
    fun getIcon(resId: Int): org.maplibre.android.annotations.Icon {
        val iconFactory = IconFactory.getInstance(context)
        val drawable = ContextCompat.getDrawable(context, resId)
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val w = drawable!!.intrinsicWidth.takeIf { it > 0 } ?: 1
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(b)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            b
        }
        return iconFactory.fromBitmap(bitmap)
    }

    // Polling Alerts
    LaunchedEffect(Unit) {
        while (true) {
            val alerts = fetchAlerts()
            
            // Add new markers
            alerts.forEach { (id, alert) ->
                if (!alertMarkers.containsKey(id)) {
                    val latLng = LatLng(alert.lat, alert.lon)
                    
                    // Select icon based on type
                    val iconRes = when (alert.type) {
                        "Fire Emergency" -> com.example.safeko.R.drawable.ic_fire_emergency
                        "Road Accident" -> com.example.safeko.R.drawable.ic_road_accident
                        "Emergency Rescue" -> com.example.safeko.R.drawable.ic_emergency_rescue
                        else -> com.example.safeko.R.drawable.ic_emergency_rescue // Default
                    }
                    
                    mapLibreMap?.let { map ->
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(alert.type)
                                .icon(getIcon(iconRes))
                        )
                        alertMarkers[id] = marker
                    }
                }
            }
            
            // Remove old markers
            val currentIds = alerts.keys
            val toRemove = alertMarkers.keys.filter { !currentIds.contains(it) }
            toRemove.forEach { id ->
                alertMarkers[id]?.let { marker ->
                    mapLibreMap?.removeMarker(marker)
                }
                alertMarkers.remove(id)
            }
            
            delay(5000)
        }
    }

    // Location Permission State
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Request Permission on Start
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Route Fetching Logic (OSRM)
    suspend fun fetchRoute(start: LatLng, end: LatLng): List<LatLng> {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
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
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val points = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            points.add(LatLng(coord.getDouble(1), coord.getDouble(0)))
                        }
                        return@withContext points
                    }
                }
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Map Lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    fun sendAlert(type: String) {
        val location = mapLibreMap?.locationComponent?.lastKnownLocation
        if (location == null) {
            Toast.makeText(context, "Waiting for location...", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid
        val body = JSONObject().apply {
            put("type", type)
            put("lat", location.latitude)
            put("lon", location.longitude)
            put("userId", userId ?: JSONObject.NULL)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        scope.launch(Dispatchers.IO) {
            val url = URL("$rtdbBaseUrl/alerts.json")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                if (code in 200..299) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Alert Sent: $type", Toast.LENGTH_SHORT).show()
                        showAlertSheet = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to send alert", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error sending alert", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Perform Search
    fun performSearch() {
        keyboardController?.hide()
        if (searchQuery.isBlank()) return
        
        isSearching = true
        scope.launch {
            val currentLocation = mapLibreMap?.locationComponent?.lastKnownLocation
            val lat = currentLocation?.latitude ?: 10.3157
            val lon = currentLocation?.longitude ?: 123.8854
            
            searchResults = PlaceSearcher.searchPlaces(searchQuery, lat, lon)
            isSearching = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Map View (Background)
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.getMapAsync { map ->
                        if (mapLibreMap == null) {
                            mapLibreMap = map
                            
                            // Load style from assets manually to ensure it's read correctly
                            // Using CartoDB Light style for a clean map (roads only, no clutter)
                            try {
                                val styleJson = context.assets.open("style_3d.json").bufferedReader().use { it.readText() }
                                map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                                    map.uiSettings.isRotateGesturesEnabled = true
                                map.uiSettings.isTiltGesturesEnabled = true
                                map.uiSettings.isCompassEnabled = false // Disable default compass
                                
                                if (hasLocationPermission) {
                                    try {
                                        val locationComponent = map.locationComponent
                                        locationComponent.activateLocationComponent(
                                            LocationComponentActivationOptions.builder(context, style).build()
                                        )
                                        locationComponent.isLocationComponentEnabled = true
                                        locationComponent.cameraMode = CameraMode.TRACKING
                                        locationComponent.renderMode = RenderMode.COMPASS
                                    } catch (e: Exception) {
                                        Log.e("MapLibre", "Location component error", e)
                                    }
                                }
                                
                                // Marker Click Listener
                                map.setOnMarkerClickListener { marker ->
                                    val alertId = alertMarkers.entries.find { it.value == marker }?.key
                                    if (alertId != null) {
                                        // It's an alert marker
                                        // Handle alert click if needed
                                        true
                                    } else {
                                        // Search result marker
                                    selectedMarker = marker
                                    if (selectedLocation != null && 
                                        marker.position.latitude == selectedLocation!!.lat &&
                                        marker.position.longitude == selectedLocation!!.lon) {
                                        showLocationModal = true
                                    }
                                    true
                                }
                            }
                        } // Close setStyle
                        } catch (e: Exception) {
                            Log.e("MapLibre", "Error loading style.json", e)
                            Toast.makeText(context, "Error loading map style", Toast.LENGTH_LONG).show()
                            
                            // Fallback to OSM Raster if asset fails
                            val osmStyle = Style.Builder()
                                .withSource(RasterSource("osm-source", TileSet("tiles", "https://tile.openstreetmap.org/{z}/{x}/{y}.png"), 256))
                                .withLayer(RasterLayer("osm-layer", "osm-source"))
                            map.setStyle(osmStyle) { style -> 
                                map.uiSettings.isRotateGesturesEnabled = true
                                map.uiSettings.isTiltGesturesEnabled = true
                                map.uiSettings.isCompassEnabled = false // Disable default compass
                            }
                        }
                        
                        // Set initial camera position only once
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(10.3157, 123.8854)) // Cebu
                                .zoom(12.0)
                                .tilt(45.0)
                                .build()
                        }
                    }
                }
            )

            // Top Overlay Section (Sharing Location & Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Sharing Location Pill (Center)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color(0xFF2B2B2B).copy(alpha = 0.9f),
                        contentColor = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sharing Location",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Pulsing Dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                        }
                    }

                    // Top Right Icons (Settings & Compass)
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { /* TODO: Settings */ },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }

                        SmallFloatingActionButton(
                            onClick = { 
                                mapLibreMap?.let { map ->
                                    map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(map.cameraPosition.target)
                                            .zoom(map.cameraPosition.zoom)
                                            .bearing(0.0) // Reset bearing to North
                                            .tilt(0.0)
                                            .build()
                                    ))
                                }
                            },
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.Explore, contentDescription = "Compass")
                        }
                    }
                }
            }

            // Bottom Navigation Bar (Custom)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // White Navigation Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Home
                        IconButton(onClick = { 
                            mapLibreMap?.let { map ->
                                map.animateCamera(CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(10.3157, 123.8854)) // Reset to Cebu
                                        .zoom(12.0)
                                        .tilt(0.0)
                                        .bearing(0.0)
                                        .build()
                                ))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Home",
                                tint = Color(0xFF29B6F6), // Light Blue
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 2. Search
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // Spacer for Floating Alert Button
                        Spacer(modifier = Modifier.width(56.dp))
                        
                        // 3. Notifications
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        // 4. Family
                        IconButton(onClick = { /* TODO: Family */ }) {
                            Icon(
                                imageVector = Icons.Rounded.Group, // Family/Group Icon
                                contentDescription = "Family",
                                tint = Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Center Floating Alert Button (Overlapping)
                FloatingActionButton(
                    onClick = { showAlertSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-36).dp) // Float above the bar
                        .size(72.dp),
                    shape = CircleShape,
                    containerColor = Color(0xFFFF9800), // Orange/Amber
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Alert",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }

    // Search Bottom Sheet (Drawer Style)
    if (showBottomSheet) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            // Remove delay for faster keyboard appearance
            focusRequester.requestFocus()
            // Ensure window is ready before showing keyboard
            snapshotFlow { sheetState.isVisible }
                .collect { isVisible ->
                    if (isVisible) {
                         keyboardController?.show()
                    }
                }
        }

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            ForceImmersiveMode()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Take up half the screen height (middle)
                    .padding(horizontal = 24.dp)
                    .imePadding() // Ensure it moves with keyboard
            ) {
                Text(
                    text = "Search Destination",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Where do you want to go?") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(searchResults) { result ->
                        SearchResultItem(result) {
                            selectedLocation = result
                            showBottomSheet = false
                            showLocationModal = true
                            
                            // Move map to location
                            mapLibreMap?.let { map ->
                                val position = LatLng(result.lat, result.lon)
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15.0))
                                
                                // Add marker
                                val marker = map.addMarker(MarkerOptions().position(position).title(result.name))
                                selectedMarker = marker
                            }
                        }
                    }
                }
                
                if (searchResults.isEmpty() && !isSearching && searchQuery.isNotEmpty()) {
                    Text(
                        text = "No results found",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Alert Bottom Sheet
    if (showAlertSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAlertSheet = false },
            sheetState = alertSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            ForceImmersiveMode()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Alert Type",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                AlertOptionItem(
                    icon = Icons.Rounded.Warning,
                    label = "Emergency Rescue",
                    color = Color.Red,
                    onClick = { sendAlert("Emergency Rescue") }
                )

                AlertOptionItem(
                    icon = Icons.Rounded.Whatshot,
                    label = "Fire Emergency",
                    color = Color(0xFFFF5722), // Deep Orange
                    onClick = { sendAlert("Fire Emergency") }
                )

                AlertOptionItem(
                    icon = Icons.Rounded.DirectionsCar,
                    label = "Road Accident",
                    color = Color(0xFFFFC107), // Amber
                    onClick = { sendAlert("Road Accident") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Location Details Modal
    if (showLocationModal && selectedLocation != null) {
        LocationDetailsModal(
            location = selectedLocation!!,
            isNavigating = currentRouteOverlay != null,
            onDismiss = { showLocationModal = false },
            onCancelNavigation = {
                showLocationModal = false
                currentRouteOverlay?.let { mapLibreMap?.removePolyline(it) }
                currentRouteOverlay = null
                Toast.makeText(context, "Navigation Canceled", Toast.LENGTH_SHORT).show()
            },
            onGetDirections = {
                showLocationModal = false
                
                if (hasLocationPermission) {
                    val currentLocation = mapLibreMap?.locationComponent?.lastKnownLocation
                    if (currentLocation != null) {
                        Toast.makeText(context, "Calculating route...", Toast.LENGTH_SHORT).show()
                        
                        scope.launch {
                            val start = LatLng(currentLocation.latitude, currentLocation.longitude)
                            val end = LatLng(selectedLocation!!.lat, selectedLocation!!.lon)
                            
                            val points = fetchRoute(start, end)
                            if (points.isNotEmpty()) {
                                mapLibreMap?.let { map ->
                                    val polyline = map.addPolyline(
                                        PolylineOptions()
                                            .addAll(points)
                                            .color(android.graphics.Color.BLUE)
                                            .width(5f)
                                    )
                                    currentRouteOverlay = polyline
                                    
                                    // Zoom to fit route
                                    // Note: MapLibre doesn't have a simple "zoom to bounds" for a list of points in one line
                                    // We would need to calculate bounds. For now, just center on start.
                                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 14.0))
                                }
                            } else {
                                Toast.makeText(context, "Could not find route", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Waiting for location...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF5F5F5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
            Text(
                text = result.distance,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AlertOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LocationDetailsModal(
    location: SearchResult,
    isNavigating: Boolean,
    onDismiss: () -> Unit,
    onGetDirections: () -> Unit,
    onCancelNavigation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ForceImmersiveMode()
            Text(
                text = location.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isNavigating) {
                        onCancelNavigation()
                    } else {
                        onGetDirections()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNavigating) Color.Red else Color(0xFF2196F3),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNavigating) "Cancel Navigation" else "Get Directions")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
