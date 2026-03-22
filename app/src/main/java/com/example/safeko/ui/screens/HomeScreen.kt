package com.example.safeko.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import com.example.safeko.data.model.Circle
import androidx.compose.ui.window.Dialog
import java.util.UUID
import androidx.compose.animation.core.*

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI
import androidx.compose.runtime.*
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import android.view.Window
import com.example.safeko.model.SearchResult
import com.example.safeko.ui.components.QRCodeScanner
import com.example.safeko.utils.PlaceSearcher
import com.example.safeko.utils.QRCodeUtils
import com.example.safeko.utils.clusterAlerts
import com.example.safeko.utils.getClusterColor
import com.example.safeko.utils.calculateDistance
import com.example.safeko.services.NavigationService
import com.example.safeko.model.NavigationStep
import com.example.safeko.model.RemoteAlert
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.net.HttpURLConnection
import java.net.URL

import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.saveable.rememberSaveable

import com.example.safeko.utils.RouteFetcher

data class CircleMember(
    val uid: String,
    val name: String,
    val profilePhoto: String?,
    val sharedCircles: List<String>,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val lastActive: Long? = null
)

fun formatLastActive(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "Never seen"
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMin = diffMs / 60_000
    return when {
        diffMin < 2   -> "Online now"
        diffMin < 60  -> "${diffMin}m ago"
        diffMin < 1440 -> "${diffMin / 60}h ago"
        else          -> "${diffMin / 1440}d ago"
    }
}

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

// LGU Approval State - must be before HomeScreen composable
sealed class LguApprovalState {
    data object Idle : LguApprovalState()
    data object Processing : LguApprovalState()
    data object Waiting : LguApprovalState()
    data class Success(val message: String = "") : LguApprovalState()
    data class Error(val message: String) : LguApprovalState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Updated to Asia Southeast 1 (Singapore)
    val rtdbBaseUrl = "https://safeko-3ca46-default-rtdb.asia-southeast1.firebasedatabase.app"

    val auth = remember { Firebase.auth }

    // User Plan State — real-time listener so downgrades take effect immediately
    var userPlan by remember { mutableStateOf("Loading...") }
    var userRole by remember { mutableStateOf("user") }
    var userDepartment by remember { mutableStateOf("") }
    var userLgcId by remember { mutableStateOf("") }
    var showGlobalOverview by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }

    // Co-Admin Invite State
    var showCoAdminInvite by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var coAdminInviteQR by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var coAdminInviteToken by remember { mutableStateOf<String?>(null) }
    var coAdminDeepLink by remember { mutableStateOf<String?>(null) }
    
    // LGU Membership Confirmation Modal
    var showLguConfirmationModal by remember { mutableStateOf(false) }
    var pendingInviteToken by remember { mutableStateOf<String?>(null) }
    var pendingInviteLgcId by remember { mutableStateOf<String?>(null) }
    
    // LGU Approval Waiting Modal State
    var showLguApprovalWaitingModal by remember { mutableStateOf(false) }
    var lguApprovalProcessingState by remember { mutableStateOf<LguApprovalState>(LguApprovalState.Idle) }
    var lguApprovalMessage by remember { mutableStateOf("") }
    var pendingLguRequestId by remember { mutableStateOf<String?>(null) }

    // Pending Co-Admin Requests State
    var pendingCoAdmins by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loadingPendingRequests by remember { mutableStateOf(false) }

    // Co-Admin List State
    var coAdminsList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loadingCoAdmins by remember { mutableStateOf(false) }

    // Group Owner State
    var groupOwnerData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var loadingGroupOwner by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid
        val registration = if (uid != null) {
            try {
                Firebase.firestore.collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("HomeScreen", "Firestore error: ${error.message}")
                            return@addSnapshotListener
                        }
                        
                        try {
                            if (snapshot != null && snapshot.exists()) {
                                val newRole = snapshot.getString("role") ?: "user"
                                val oldRole = userRole
                                
                                userPlan = snapshot.getString("plan") ?: "Free"
                                userRole = newRole
                                userDepartment = snapshot.getString("department") ?: ""
                                val lgcId = snapshot.getString("lgc_id")
                                userLgcId = if (lgcId?.isNotEmpty() == true) lgcId else uid
                                
                                if (newRole != oldRole || newRole == "lgu_admin") {
                                    Log.d("UserDataLoading", "🔄 ROLE UPDATED: $oldRole → $newRole (admin: ${newRole == "lgu_admin"})")
                                }
                                Log.d("UserDataLoading", "Loaded from Firestore: uid=$uid, role=$userRole, lgc_id=$userLgcId (raw from db: $lgcId), plan=$userPlan")
                            } else {
                                userPlan = "Free"
                                Log.w("UserDataLoading", "User document does not exist in Firestore for uid=$uid")
                            }
                        } catch (e: Exception) {
                            Log.e("UserDataLoading", "Error processing snapshot: ${e.message}", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error setting up listener: ${e.message}")
                null
            }
        } else null
        onDispose { 
            try {
                registration?.remove()
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error removing listener: ${e.message}")
            }
        }
    }

    // Auto-show admin overview when user becomes lgu_admin (after QR approval)
    LaunchedEffect(userRole) {
        Log.d("AdminAutoShow", "🔍 LaunchedEffect triggered. userRole=$userRole, showGlobalOverview=$showGlobalOverview")
        
        val isAdminRole = (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin")
        Log.d("AdminAutoShow", "   isAdminRole=$isAdminRole, showGlobalOverview=$showGlobalOverview, condition=!showGlobalOverview is ${!showGlobalOverview}")
        
        if (isAdminRole && !showGlobalOverview) {
            Log.d("AdminAutoShow", "✅ SETTING showGlobalOverview = true (was: $showGlobalOverview)")
            showGlobalOverview = true
        } else if (isAdminRole && showGlobalOverview) {
            Log.d("AdminAutoShow", "⏭️ Already showing admin overview, skipping")
        } else {
            Log.d("AdminAutoShow", "❌ NOT admin role or condition failed: isAdminRole=$isAdminRole")
        }
    }

    // Service Binding
    var navigationService by remember { mutableStateOf<NavigationService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                val binder = service as NavigationService.LocalBinder
                navigationService = binder.getService()
                isBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                isBound = false
                navigationService = null
            }
        }
    }

    DisposableEffect(Unit) {
        Intent(context, NavigationService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        onDispose {
            if (isBound) {
                context.unbindService(connection)
                isBound = false
            }
        }
    }

    // State for selected location and marker
    var selectedLocation by remember { mutableStateOf<SearchResult?>(null) }
    var showLocationModal by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }
    var navigationSteps by remember { mutableStateOf<List<NavigationStep>>(emptyList()) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var distanceToNextTurn by remember { mutableStateOf(0.0) }

    // Circle State
    var showLinkJoinDialog by remember { mutableStateOf(false) }
    var isCreatingCircle by remember { mutableStateOf(false) }
    var userCircles by remember { mutableStateOf<List<Circle>>(emptyList()) }
    var ownedGroupCount by remember { mutableIntStateOf(0) }
    var selectedCircle by remember { mutableStateOf<Circle?>(null) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                // Listen for circles where user is a member or owner
                // Simplified for now: just getting circles where user is owner or member
                // Since we don't have composite indexes yet, we might need to query carefully or just owner for MVP
                // Or we can query by "members" array-contains
                Firebase.firestore.collection("circles")
                    .whereArrayContains("members", uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val allCircles = snapshot.toObjects(Circle::class.java)
                            var gCount = 0
                            for (c in allCircles) {
                                if (c.ownerId == uid) {
                                    if (c.type != "Duo") gCount++
                                }
                            }
                            ownedGroupCount = gCount
                            userCircles = allCircles.filter { circle -> circle.type != "Duo" }
                        }
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Map State
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var alerts by remember { mutableStateOf<Map<String, RemoteAlert>>(emptyMap()) }
    var selectedAlert by remember { mutableStateOf<RemoteAlert?>(null) }
    var clusteredAlerts by remember { mutableStateOf<List<com.example.safeko.utils.ClusteredAlert>>(emptyList()) }
    var selectedCluster by remember { mutableStateOf<com.example.safeko.utils.ClusteredAlert?>(null) }
    var showClusterModal by remember { mutableStateOf(false) }
    var showAlertDetails by remember { mutableStateOf(false) }

    // Unified Alert listener (Realtime)
    LaunchedEffect(userRole, userDepartment) {
        val database = FirebaseDatabase.getInstance(rtdbBaseUrl)
        val alertsRef = database.getReference("alerts")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newAlerts = mutableMapOf<String, RemoteAlert>()
                for (alertSnapshot in snapshot.children) {
                    val id = alertSnapshot.key ?: continue
                    val type = alertSnapshot.child("type").getValue(String::class.java) ?: ""

                    // Filter alerts for LGU Admins
                    if (userRole == "lgu_admin") {
                        val isRelevant = when (userDepartment) {
                            "Firestation", "Fire" -> type == "Fire Emergency"
                            "Rescue" -> type == "Emergency Rescue" || type == "Road Accident"
                            "Medical" -> type == "Emergency Rescue"
                            else -> true
                        }
                        if (!isRelevant) continue
                    }

                    val lat = alertSnapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lon = alertSnapshot.child("lon").getValue(Double::class.java) ?: 0.0
                    val status =
                        alertSnapshot.child("status").getValue(String::class.java) ?: "pending"
                    val userName = alertSnapshot.child("userName").getValue(String::class.java)
                    val userPhotoUrl =
                        alertSnapshot.child("userPhotoUrl").getValue(String::class.java)
                    val userId = alertSnapshot.child("userId").getValue(String::class.java)
                    val timestamp =
                        alertSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val address = alertSnapshot.child("address").getValue(String::class.java) ?: ""
                    val photoBase64 =
                        alertSnapshot.child("photoBase64").getValue(String::class.java) ?: ""
                    val details = alertSnapshot.child("details").getValue(String::class.java) ?: ""
                    val phoneNumber =
                        alertSnapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    val notes = alertSnapshot.child("notes").getValue(String::class.java) ?: ""

                    newAlerts[id] = RemoteAlert(
                        id = id,
                        type = type,
                        lat = lat,
                        lon = lon,
                        status = status,
                        userName = userName,
                        userPhotoUrl = userPhotoUrl,
                        userId = userId,
                        timestamp = timestamp,
                        address = address,
                        photoBase64 = photoBase64,
                        details = details,
                        notes = notes
                    )
                }
                alerts = newAlerts
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeScreen", "Alerts listener cancelled", error.toException())
            }
        }
        alertsRef.addValueEventListener(listener)
    }

    // Sync with Service
    val serviceIsNavigating by navigationService?.isNavigating?.collectAsState(initial = false)
        ?: mutableStateOf(false)
    val serviceSteps by navigationService?.navigationSteps?.collectAsState(initial = emptyList())
        ?: mutableStateOf(emptyList())
    val serviceCurrentStepIndex by navigationService?.currentStepIndex?.collectAsState(initial = 0)
        ?: mutableStateOf(0)
    val serviceDistance by navigationService?.distanceToNextTurn?.collectAsState(initial = 0.0)
        ?: mutableStateOf(0.0)
    val serviceRoutePoints by navigationService?.routePoints?.collectAsState(initial = emptyList())
        ?: mutableStateOf(emptyList())
    val serviceRemainingRoutePoints by navigationService?.remainingRoutePoints?.collectAsState(
        initial = emptyList()
    ) ?: mutableStateOf(emptyList())

    LaunchedEffect(
        serviceIsNavigating,
        serviceSteps,
        serviceCurrentStepIndex,
        serviceDistance,
        serviceRemainingRoutePoints,
        mapLibreMap
    ) {
        if (serviceIsNavigating) {
            isNavigating = true
            if (serviceSteps.isNotEmpty()) {
                navigationSteps = serviceSteps
            }
            currentStepIndex = serviceCurrentStepIndex
            distanceToNextTurn = serviceDistance

            if (serviceRemainingRoutePoints.isNotEmpty() && mapLibreMap != null) {
                mapLibreMap?.getStyle { style ->
                    val lineString = LineString.fromLngLats(serviceRemainingRoutePoints.map {
                        Point.fromLngLat(it.longitude, it.latitude)
                    })

                    val source = style.getSource("route-source") as? GeoJsonSource
                    if (source != null) {
                        source.setGeoJson(Feature.fromGeometry(lineString))
                    } else {
                        val newSource = GeoJsonSource(
                            "route-source",
                            Feature.fromGeometry(lineString),
                            GeoJsonOptions().withLineMetrics(true)
                        )
                        style.addSource(newSource)
                    }

                    if (style.getLayer("route-layer") == null) {
                        val layer = LineLayer("route-layer", "route-source")
                        layer.setProperties(
                            PropertyFactory.lineWidth(8f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineGradient(
                                Expression.interpolate(
                                    Expression.linear(),
                                    Expression.lineProgress(),
                                    Expression.stop(
                                        0f,
                                        Expression.color(android.graphics.Color.BLUE)
                                    ),
                                    Expression.stop(
                                        1f,
                                        Expression.color(android.graphics.Color.CYAN)
                                    )
                                )
                            )
                        )
                        style.addLayer(layer)
                    }
                }
            }
        } else {
            if (isNavigating) {
                isNavigating = false
                navigationSteps = emptyList()
                currentStepIndex = 0
                distanceToNextTurn = 0.0

                mapLibreMap?.getStyle { style ->
                    if (style.getLayer("route-layer") != null) style.removeLayer("route-layer")
                    if (style.getSource("route-source") != null) style.removeSource("route-source")
                }
                Toast.makeText(context, "Navigation Ended", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showScanner by remember { mutableStateOf(false) }

    // Sharing Location State
    var showSharingSheet by remember { mutableStateOf(false) }
    var showGroupSheet by remember { mutableStateOf(false) }
    // Initialize from SharedPreferences
    val sharedPreferences =
        remember { context.getSharedPreferences("safeko_prefs", Context.MODE_PRIVATE) }
    var sharingOption by remember {
        mutableStateOf(
            sharedPreferences.getString("sharing_option", "Visible to others")
                ?: "Visible to others"
        )
    }

    // Tutorial State
    val currentUserId = auth.currentUser?.uid ?: ""
    val tutorialKey = "tutorial_completed_$currentUserId"
    var showTutorial by remember {
        mutableStateOf(!sharedPreferences.getBoolean(tutorialKey, false))
    }

    // Edit Profile State
    var showEditProfile by remember { mutableStateOf(false) }
    var userPhoneNumber by remember {
        mutableStateOf(sharedPreferences.getString("user_phone_number_$currentUserId", "") ?: "")
    }
    var currentUserAddress by remember { mutableStateOf("Locating...") }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                isUploadingPhoto = true
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val storageRef =
                        FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
                    scope.launch {
                        try {
                            storageRef.putFile(uri).await()
                            val downloadUrl = storageRef.downloadUrl.await()

                            // Update Firebase Auth Profile
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUrl)
                                .build()

                            auth.currentUser?.updateProfile(profileUpdates)?.await()

                            Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT)
                                .show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Failed to update profile picture: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isUploadingPhoto = false
                        }
                    }
                }
            }
        }

    var currentTutorialStep by remember { mutableStateOf(0) }
    var searchButtonRect by remember { mutableStateOf<Rect?>(null) }
    var notificationsButtonRect by remember { mutableStateOf<Rect?>(null) }
    var alertButtonRect by remember { mutableStateOf<Rect?>(null) }
    var sharingLocationRect by remember { mutableStateOf<Rect?>(null) }
    var profileRect by remember { mutableStateOf<Rect?>(null) }
    var groupButtonRect by remember { mutableStateOf<Rect?>(null) }

    val tutorialSteps = remember {
        listOf(
            TutorialStep(
                id = "search",
                title = "Search",
                description = "Tap here to find places and get directions to your destination.",
                stepInfo = "1/6"
            ),
            TutorialStep(
                id = "group",
                title = "Family Group",
                description = "View your family members and their status.",
                stepInfo = "2/6"
            ),
            TutorialStep(
                id = "alert",
                title = "Emergency Alert",
                description = "Long press this button to send an emergency alert to your contacts.",
                stepInfo = "3/6"
            ),
            TutorialStep(
                id = "notifications",
                title = "Notifications",
                description = "Check here for alerts and updates from your family and community.",
                stepInfo = "4/6"
            ),
            TutorialStep(
                id = "sharing",
                title = "Location Sharing",
                description = "Tap to manage who can see your location.",
                stepInfo = "5/6"
            ),
            TutorialStep(
                id = "profile",
                title = "Profile",
                description = "Access your profile settings and logout options here.",
                stepInfo = "6/6"
            )
        )
    }

    // Save sharing option when it changes
    LaunchedEffect(sharingOption) {
        sharedPreferences.edit().putString("sharing_option", sharingOption).apply()
    }

    // Route Animation State
    // Optimized: Removed infiniteTransition to reduce main thread load.
    // Animation is now handled manually in LaunchedEffect loop below.


    // Force Hide System Navigation Bar (Immersive Sticky)
    ForceImmersiveMode()

    // Fetch address for profile when dialog opens
    LaunchedEffect(showEditProfile) {
        if (showEditProfile) {
            val loc = mapLibreMap?.locationComponent?.lastKnownLocation
            if (loc != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val address = PlaceSearcher.reverseGeocode(loc.latitude, loc.longitude)
                        withContext(Dispatchers.Main) {
                            currentUserAddress = address
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            currentUserAddress = "Location not available"
                        }
                    }
                }
            } else {
                currentUserAddress = "Location not available"
            }
        }
    }

    // Update Search Result Marker on Map
    LaunchedEffect(selectedLocation) {
        mapLibreMap?.getStyle { style ->
            val source = style.getSourceAs<GeoJsonSource>("search-result-source")
            if (source != null) {
                if (selectedLocation != null) {
                    val point = Point.fromLngLat(selectedLocation!!.lon, selectedLocation!!.lat)
                    val feature = Feature.fromGeometry(point)
                    source.setGeoJson(feature)
                } else {
                    source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                }
            }
        }
    }

    // Update Location Component Color/Visibility based on Sharing Option
    LaunchedEffect(sharingOption, mapLibreMap) {
        mapLibreMap?.let { map ->
            if (map.style != null && map.locationComponent.isLocationComponentActivated) {
                val locationComponent = map.locationComponent

                when (sharingOption) {
                    "Visible to others" -> {
                        locationComponent.isLocationComponentEnabled = true
                        val options = LocationComponentOptions.builder(context)
                            .foregroundTintColor(android.graphics.Color.BLUE)
                            .bearingTintColor(android.graphics.Color.BLUE)
                            .accuracyColor(android.graphics.Color.BLUE)
                            .accuracyAlpha(0.2f)
                            .build()
                        locationComponent.applyStyle(options)
                    }

                    "Family" -> {
                        locationComponent.isLocationComponentEnabled = true
                        val options = LocationComponentOptions.builder(context)
                            .foregroundTintColor(android.graphics.Color.GREEN)
                            .bearingTintColor(android.graphics.Color.GREEN)
                            .accuracyColor(android.graphics.Color.GREEN)
                            .accuracyAlpha(0.2f)
                            .build()
                        locationComponent.applyStyle(options)
                    }

                    "No one" -> {
                        locationComponent.isLocationComponentEnabled = false
                    }
                }
            }
        }
    }

    @Composable
    fun RadialMenuOverlay(
        selectedOption: String?,
        onDismiss: () -> Unit,
        onOptionSelected: (String) -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
                .zIndex(100f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .offset(y = (-36).dp)
                    .size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                var expanded by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { expanded = true }

                val transition = updateTransition(targetState = expanded, label = "RadialMenu")

                val options = listOf(
                    Triple(Icons.Rounded.LocalFireDepartment, "Fire", Color(0xFFFF5722)),
                    Triple(Icons.Rounded.MedicalServices, "Rescue", Color(0xFFF44336)),
                    Triple(Icons.Rounded.CarCrash, "Accident", Color(0xFFFFC107))
                )

                val angles = listOf(210f, 270f, 330f)

                options.zip(angles).forEach { (option, angle) ->
                    val (icon, label, color) = option

                    val distance by transition.animateDp(
                        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy) },
                        label = "Distance"
                    ) { if (it) 140.dp else 0.dp }

                    val isSelected = selectedOption == label
                    val targetScale = if (isSelected) 1.5f else 1f
                    val scale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "Scale"
                    )

                    val rad = Math.toRadians(angle.toDouble())
                    val xOffset = (distance.value * kotlin.math.cos(rad)).dp
                    val yOffset = (distance.value * kotlin.math.sin(rad)).dp

                    Column(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .scale(if (expanded) scale else 0f)
                            .clickable { onOptionSelected(label) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.None,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                val centerScale by animateFloatAsState(
                    targetValue = if (selectedOption == null) 1.0f else 0.8f,
                    label = "CenterScale"
                )

                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(centerScale)
                        .clickable { onDismiss() },
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }

    // Navigation Update Logic
    // Logic moved to NavigationService to support background navigation and rerouting.
    // Sync is handled by LaunchedEffect above.


    // Circle Members - global state fetched in background every 15 seconds
    var circleMembers by remember { mutableStateOf<List<CircleMember>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(false) }

    // Member Tap Popup State
    var selectedMemberOnMap by remember { mutableStateOf<CircleMember?>(null) }
    var selectedMemberAddress by remember { mutableStateOf("") }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Search Results State
    var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }

    // Alert Sheet State
    var showAlertSheet by remember { mutableStateOf(false) }
    var showRadialMenu by remember { mutableStateOf(false) }
    var radialSelection by remember { mutableStateOf<String?>(null) }
    val alertSheetState = rememberModalBottomSheetState()

    // Notifications State
    var showNotifications by remember { mutableStateOf(false) }

    // Check Auth State
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(
                "HomeScreen",
                "User Logged In: ${currentUser.uid}, Name: ${currentUser.displayName}"
            )
            if (currentUser.displayName.isNullOrBlank()) {
                Log.d("HomeScreen", "User Name is null, attempting reload...")
                try {
                    currentUser.reload().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(
                                "HomeScreen",
                                "User Reloaded. Name: ${auth.currentUser?.displayName}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error reloading user", e)
                }
            }
        } else {
            Log.d("HomeScreen", "User Not Logged In")
        }
    }

    // Add Details State
    var showAddDetails by remember { mutableStateOf(false) }
    var detailsText by remember { mutableStateOf("") }
    var capturedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Helper to create image file
    fun createImageFile(context: Context): File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // Restore bitmap if uri exists but bitmap is null (e.g. after rotation)
    LaunchedEffect(capturedImageUri) {
        if (capturedImageUri != null && capturedBitmap == null) {
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(capturedImageUri!!)
                    var bitmap = BitmapFactory.decodeStream(inputStream)
                    // Resize logic same as below
                    val maxDimension = 1024
                    if (bitmap != null && (bitmap.width > maxDimension || bitmap.height > maxDimension)) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val newWidth =
                            if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                        val newHeight =
                            if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    }
                    withContext(Dispatchers.Main) {
                        capturedBitmap = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera Launcher
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && capturedImageUri != null) {
                // Trigger reload via LaunchedEffect by toggling or just let it handle it?
                // Actually LaunchedEffect(capturedImageUri) might not trigger if uri didn't change reference but content did.
                // But here uri is constant for the capture session.
                // So we should load it here explicitly too.
                try {
                    val inputStream = context.contentResolver.openInputStream(capturedImageUri!!)
                    var bitmap = BitmapFactory.decodeStream(inputStream)

                    // Resize if too large (max 1024x1024)
                    val maxDimension = 1024
                    if (bitmap != null && (bitmap.width > maxDimension || bitmap.height > maxDimension)) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val newWidth =
                            if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                        val newHeight =
                            if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    }

                    capturedBitmap = bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Handle Back Press to prevent white screen and handle overlays
    var backPressState by remember { mutableStateOf(0L) }
    BackHandler(enabled = true) {
        when {
            showRadialMenu -> showRadialMenu = false
            showBottomSheet -> showBottomSheet = false
            showAlertDetails -> showAlertDetails = false
            showNotifications -> showNotifications = false
            showAddDetails -> showAddDetails = false
            showSharingSheet -> showSharingSheet = false
            showEditProfile -> showEditProfile = false
            showLocationModal -> showLocationModal = false
            showScanner -> showScanner = false
            showTutorial -> {
                if (currentTutorialStep > 0) {
                    currentTutorialStep--
                } else {
                    showTutorial = false
                    sharedPreferences.edit().putBoolean(tutorialKey, true).apply()
                }
            }

            selectedLocation != null -> selectedLocation = null
            isNavigating -> {
                isNavigating = false
                navigationSteps = emptyList()
                currentStepIndex = 0
                mapLibreMap?.getStyle { style ->
                    if (style.getLayer("route-layer") != null) style.removeLayer("route-layer")
                    if (style.getSource("route-source") != null) style.removeSource("route-source")
                }
                Toast.makeText(context, "Navigation Canceled", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressState < 2000) {
                    context.findActivity()?.finish()
                } else {
                    backPressState = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Initialize MapLibre immediately
    MapLibre.getInstance(context)

    // MapView
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    // Helper to get Bitmap from Drawable resource
    fun getBitmap(resId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, resId)
        return if (drawable is BitmapDrawable) {
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
    }

    // Helper to create smooth colored dot bitmaps for alert markers
    fun createColoredDot(color: Int, size: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = size / 2f

        val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        glowPaint.shader = android.graphics.RadialGradient(
            radius,
            radius,
            radius,
            intArrayOf(
                android.graphics.Color.argb(
                    80,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)
                ),
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0.0f, 1.0f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawCircle(radius, radius, radius, glowPaint)

        val ringPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        ringPaint.style = android.graphics.Paint.Style.STROKE
        ringPaint.strokeWidth = size * 0.08f
        ringPaint.color = android.graphics.Color.WHITE
        canvas.drawCircle(radius, radius, radius * 0.55f, ringPaint)

        val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        dotPaint.color = color
        canvas.drawCircle(radius, radius, radius * 0.42f, dotPaint)

        return bitmap
    }

    suspend fun markAlertResolved(alertId: String, token: String?) {
        withContext(Dispatchers.IO) {
            var currentToken = token
            var retries = 0
            
            while (retries < 2) {
                val authParam = if (currentToken != null) "?auth=$currentToken" else ""
                val url = URL("$rtdbBaseUrl/alerts/$alertId.json$authParam")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                try {
                    val payload = JSONObject()
                        .put("status", "resolved")
                        .put("resolvedAt", System.currentTimeMillis())
                    connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                    
                    val responseCode = connection.responseCode
                    when (responseCode) {
                        200 -> {
                            connection.inputStream.close()
                            Log.d("HomeScreen", "Alert $alertId resolved successfully")
                            return@withContext
                        }
                        404 -> {
                            Log.d("HomeScreen", "Alert $alertId not found (already deleted or never existed)")
                            return@withContext
                        }
                        401 -> {
                            // Token expired/invalid, retry with fresh token
                            if (retries == 0) {
                                Log.d("HomeScreen", "Token expired for alert $alertId, refreshing...")
                                currentToken = try {
                                    auth.currentUser?.getIdToken(true)?.await()?.token
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Failed to refresh token", e)
                                    null
                                }
                                retries++
                                connection.disconnect()
                                continue
                            } else {
                                Log.e("HomeScreen", "Failed to auto-resolve alert $alertId: HTTP $responseCode (retry failed)")
                                return@withContext
                            }
                        }
                        else -> {
                            Log.e("HomeScreen", "Failed to auto-resolve alert $alertId: HTTP $responseCode")
                            return@withContext
                        }
                    }
                } catch (e: Exception) {
                    if (e !is java.io.FileNotFoundException) {
                        Log.e("HomeScreen", "Failed to auto-resolve alert $alertId", e)
                    }
                    return@withContext
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    suspend fun fetchAlerts(): Map<String, RemoteAlert> {
        return withContext(Dispatchers.IO) {
            val token = try {
                auth.currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error getting auth token", e)
                null
            }

            val authParam = if (token != null) "?auth=$token" else ""
            val url = URL("$rtdbBaseUrl/alerts.json$authParam")

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            try {
                if (connection.responseCode != 200) {
                    Log.e(
                        "HomeScreen",
                        "Failed to fetch alerts: ${connection.responseCode} ${connection.responseMessage}"
                    )
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
                    val notes = obj.optString("notes", "")
                    val address = obj.optString("address", "")
                    val details = obj.optString("details", "")
                    val photoBase64 = obj.optString("photoBase64", "")
                    val userId = obj.optString("userId", null)
                    val userName = obj.optString("userName", null)
                    val userPhotoUrl = obj.optString("userPhotoUrl", null)
                    val timestamp = obj.optLong("timestamp", 0L)
                    var status = obj.optString("status", "active")
                    val maxAgeMs = 24L * 60L * 60L * 1000L
                    if (timestamp > 0L && status != "resolved") {
                        val ageMs = System.currentTimeMillis() - timestamp
                        if (ageMs >= maxAgeMs) {
                            markAlertResolved(id, token)
                            status = "resolved"
                        }
                    }

                    if (type.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                        result[id] = RemoteAlert(
                            id = id,
                            type = type,
                            lat = lat,
                            lon = lon,
                            notes = notes,
                            address = address,
                            details = details,
                            photoBase64 = photoBase64,
                            userId = userId,
                            userName = userName,
                            userPhotoUrl = userPhotoUrl,
                            timestamp = timestamp,
                            status = status
                        )
                    }
                }
                result
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // Global Timer for "Time Ago" updates
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTime = System.currentTimeMillis()
        }
    }

    // Background loop: fetch circle member profiles & locations every 15 seconds
    // Free-tier users cannot see other users on the map — members list stays empty.
    LaunchedEffect(userCircles, userPlan) {
        while (true) {
            if (userPlan == "Free" || userPlan == "Loading...") {
                // Free plan: clear any existing markers and skip fetching
                circleMembers = emptyList()
            } else if (userCircles.isEmpty()) {
                circleMembers = emptyList()
            } else {
                isLoadingMembers = circleMembers.isEmpty() // show spinner only on first load
                val currentUid = auth.currentUser?.uid
                if (currentUid != null) {
                    val membersMap = mutableMapOf<String, MutableList<String>>()
                    userCircles.forEach { circle ->
                        circle.members.forEach { uid ->
                            if (uid != currentUid) {
                                membersMap.getOrPut(uid) { mutableListOf() }.add(circle.name)
                            }
                        }
                    }
                    val fetchedMembers = mutableListOf<CircleMember>()
                    for ((uid, sharedNames) in membersMap) {
                        try {
                            val doc = withContext(Dispatchers.IO) {
                                Firebase.firestore.collection("users").document(uid).get().await()
                            }
                            val name = doc.getString("name") ?: "Unknown User"
                            val profilePhoto = doc.getString("profilePhoto")
                            val lastLat = doc.getDouble("lastLat")
                            val lastLon = doc.getDouble("lastLon")
                            val lastActive = doc.getLong("lastActive")
                            fetchedMembers.add(
                                CircleMember(
                                    uid,
                                    name,
                                    profilePhoto,
                                    sharedNames,
                                    lastLat,
                                    lastLon,
                                    lastActive
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "Error fetching member $uid", e)
                        }
                    }
                    circleMembers = fetchedMembers.sortedBy { it.name }
                }
                isLoadingMembers = false
            }
            delay(15_000)
        }
    }

    // Background loop: upload own location and lastActive to Firestore every 15 seconds
    LaunchedEffect(isMapReady) {
        if (!isMapReady) return@LaunchedEffect
        while (true) {
            delay(15_000)
            val currentUid = auth.currentUser?.uid ?: continue
            val loc = mapLibreMap?.locationComponent?.lastKnownLocation ?: continue
            try {
                withContext(Dispatchers.IO) {
                    Firebase.firestore.collection("users").document(currentUid)
                        .update(
                            mapOf(
                                "lastLat" to loc.latitude,
                                "lastLon" to loc.longitude,
                                "lastActive" to System.currentTimeMillis()
                            )
                        ).await()
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error uploading own location", e)
            }
        }
    }

    // Polling Alerts (Filter for LGU Admin)
    LaunchedEffect(userRole, userDepartment) {
        while (true) {
            val fetchedAlerts = fetchAlerts()

            // Filter alerts for LGU Admins
            val filteredAlerts = if (userRole == "lgu_admin") {
                fetchedAlerts.filter { (_, alert) ->
                    when (userDepartment) {
                        "Firestation", "Fire" -> alert.type == "Fire Emergency"
                        "Rescue" -> alert.type == "Emergency Rescue" || alert.type == "Road Accident"
                        "Medical" -> alert.type == "Emergency Rescue"
                        else -> true
                    }
                }
            } else {
                fetchedAlerts
            }

            // Perform comparison on background thread
            val hasChanged = withContext(Dispatchers.Default) {
                filteredAlerts != alerts
            }

            if (hasChanged) {
                alerts = filteredAlerts
            }

            delay(5000)
        }
    }

    // Update Map Markers when circle members change or map becomes ready
    LaunchedEffect(circleMembers, isMapReady) {
        if (!isMapReady) return@LaunchedEffect

        // Build features and load profile images on IO
        val features = withContext(Dispatchers.IO) {
            circleMembers
                .filter { it.lastLat != null && it.lastLon != null }
                .map { member ->
                    // Load and crop profile photo to a circle
                    val iconKey = "member-icon-${member.uid}"
                    val photoBitmap: Bitmap? = if (member.profilePhoto != null) {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(member.profilePhoto)
                                .allowHardware(false)
                                .size(56, 56)
                                .build()
                            val result = context.imageLoader.execute(request)
                            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        } catch (e: Exception) {
                            null
                        }
                    } else null

                    // Create circular bitmap with border
                    val size = 56
                    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(output)
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    borderPaint.color = android.graphics.Color.WHITE
                    val centerF = size / 2f
                    val radiusF = centerF
                    canvas.drawCircle(centerF, centerF, radiusF, borderPaint)

                    if (photoBitmap != null) {
                        val scaledBitmap =
                            Bitmap.createScaledBitmap(photoBitmap, size - 8, size - 8, true)
                        val innerPaint =
                            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        val shader = android.graphics.BitmapShader(
                            scaledBitmap,
                            android.graphics.Shader.TileMode.CLAMP,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        innerPaint.shader = shader
                        canvas.drawCircle(centerF, centerF, radiusF - 4, innerPaint)
                    } else {
                        // Fallback: colored circle with initial letter
                        paint.color = android.graphics.Color.parseColor("#FF9800")
                        canvas.drawCircle(centerF, centerF, radiusF - 4, paint)
                        val textPaint =
                            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                        textPaint.color = android.graphics.Color.WHITE
                        textPaint.textSize = 36f
                        textPaint.textAlign = android.graphics.Paint.Align.CENTER
                        textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                        val textY = centerF - (textPaint.descent() + textPaint.ascent()) / 2
                        canvas.drawText(member.name.take(1).uppercase(), centerF, textY, textPaint)
                    }

                    Triple(member, iconKey, output)
                }
        }

        mapLibreMap?.getStyle { style ->
            val featureList = mutableListOf<Feature>()
            for ((member, iconKey, bitmap) in features) {
                // Register icon image (replace if already exists)
                if (style.getImage(iconKey) == null) {
                    style.addImage(iconKey, bitmap)
                } else {
                    style.removeImage(iconKey)
                    style.addImage(iconKey, bitmap)
                }
                val point = Point.fromLngLat(member.lastLon!!, member.lastLat!!)
                val feature = Feature.fromGeometry(point)
                feature.addStringProperty("uid", member.uid)
                feature.addStringProperty("name", member.name)
                feature.addStringProperty("member-icon", iconKey)
                featureList.add(feature)
            }
            val source = style.getSourceAs<GeoJsonSource>("members-source")
            source?.setGeoJson(FeatureCollection.fromFeatures(featureList))
        }
    }

    // Update Map Markers when alerts change or map becomes ready (with clustering)
    LaunchedEffect(alerts, isMapReady) {
        if (!isMapReady) return@LaunchedEffect

        val features = if (alerts.isNotEmpty()) {
            // Prepare features on background thread
            withContext(Dispatchers.Default) {
                val activeAlerts = alerts.values.filter { it.status != "resolved" }
                
                // Cluster alerts by proximity
                val clusters = clusterAlerts(activeAlerts)
                clusteredAlerts = clusters
                
                // Create features for each cluster (only one marker per cluster)
                clusters.map { cluster ->
                    val point = Point.fromLngLat(cluster.centerLon, cluster.centerLat)
                    val feature = Feature.fromGeometry(point)
                    
                    // Use cluster ID and highest severity type
                    feature.addStringProperty("clusterId", cluster.clusterId)
                    feature.addStringProperty("type", cluster.highestSeverityAlert.type)
                    feature.addStringProperty("isCritical", cluster.isCritical.toString())
                    feature.addNumberProperty("clusterSize", cluster.alerts.size.toLong())
                    
                    // Determine icon based on critical status
                    val iconImage = if (cluster.isCritical) {
                        "icon-critical"  // Red pulse icon for critical
                    } else {
                        when (cluster.highestSeverityAlert.type) {
                            "Fire Emergency" -> "icon-fire"
                            "Road Accident" -> "icon-accident"
                            "Emergency Rescue" -> "icon-rescue"
                            else -> "icon-rescue"
                        }
                    }
                    feature.addStringProperty("icon-image", iconImage)
                    feature
                }
            }
        } else {
            clusteredAlerts = emptyList()
            emptyList()
        }

        mapLibreMap?.getStyle { style ->
            val source = style.getSourceAs<GeoJsonSource>("alerts-source")
            source?.setGeoJson(FeatureCollection.fromFeatures(features))
            
            // Force layer to refresh by updating paint properties
            val layer = style.getLayer("alerts-layer") as? SymbolLayer
            if (layer != null) {
                // This forces the layer to re-evaluate icon expressions
                layer.withProperties(
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
                Log.d("AlertSubmission", "🔄 Refreshed alerts layer with ${features.size} clusters")
            }
        }
    }

    // Route Animation Effect
    // Optimize: Throttle updates to ~30fps to reduce Main Thread load
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            while (isActive) {
                // Manually animate progress instead of using InfiniteTransition which runs every frame
                val startTime = System.currentTimeMillis()
                val duration = 1500L

                while (isActive) {
                    val currentTime = System.currentTimeMillis()
                    val elapsed = (currentTime - startTime) % duration
                    val progress = elapsed / duration.toFloat()

                    mapLibreMap?.getStyle { style ->
                        val layer = style.getLayer("route-layer") as? LineLayer
                        if (layer != null) {
                            val p = progress.coerceIn(0.05f, 0.95f)
                            val width = 0.15f

                            val stops = mutableListOf<Expression.Stop>()
                            stops.add(
                                Expression.stop(
                                    0f,
                                    Expression.color(android.graphics.Color.BLUE)
                                )
                            )

                            val start = p - width
                            if (start > 0.01f) {
                                stops.add(
                                    Expression.stop(
                                        start,
                                        Expression.color(android.graphics.Color.BLUE)
                                    )
                                )
                            }

                            stops.add(
                                Expression.stop(
                                    p,
                                    Expression.color(android.graphics.Color.WHITE)
                                )
                            )

                            val end = p + width
                            if (end < 0.99f) {
                                stops.add(
                                    Expression.stop(
                                        end,
                                        Expression.color(android.graphics.Color.BLUE)
                                    )
                                )
                            }

                            stops.add(
                                Expression.stop(
                                    1f,
                                    Expression.color(android.graphics.Color.BLUE)
                                )
                            )

                            layer.setProperties(
                                PropertyFactory.lineGradient(
                                    Expression.interpolate(
                                        Expression.linear(),
                                        Expression.lineProgress(),
                                        *stops.toTypedArray()
                                    )
                                )
                            )
                        }
                    }

                    // Delay to target ~30fps (33ms)
                    delay(33)
                }
            }
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
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    // Request Permission on Start
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasLocationPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasCameraPermission) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Route Fetching Logic moved to RouteFetcher.kt

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
        val user = auth.currentUser
        
        scope.launch(Dispatchers.IO) {
            try {
                Log.d("AlertSubmission", "🔍 Checking for existing alert at (${location.latitude}, ${location.longitude})")
                Log.d("AlertSubmission", "📊 Cached alerts: ${alerts.size}")
                
                // Small delay to let real-time listener catch up with previous update
                if (alerts.isNotEmpty()) {
                    delay(300)  // Wait 300ms for listener to receive update from Firebase
                    Log.d("AlertSubmission", "⏳ After delay - cached alerts: ${alerts.size}")
                }
                
                // Check for existing alert at same location from current (hopefully updated) state
                val existingAlert = alerts.values.find { alert ->
                    val distance = calculateDistance(alert.lat, alert.lon, location.latitude, location.longitude)
                    val isSameType = alert.type == type
                    val isActive = alert.status != "resolved"
                    val isWithinRadius = distance <= com.example.safeko.utils.CLUSTER_RADIUS_METERS
                    
                    Log.d("AlertSubmission", "🔎 Alert ${alert.id}: type=$isSameType, distance=${String.format("%.1f", distance)}m, pingCount=${alert.pingCount}")
                    
                    isSameType && isActive && isWithinRadius
                }
                
                if (existingAlert != null) {
                    Log.d("AlertSubmission", "✅ FOUND existing alert: ${existingAlert.id} (pingCount=${existingAlert.pingCount})")
                    
                    // Get fresh token for this request
                    val token = try {
                        auth.currentUser?.getIdToken(true)?.await()?.token  // Force refresh
                    } catch (e: Exception) {
                        Log.e("AlertSubmission", "Error getting auth token", e)
                        null
                    }
                    
                    if (token == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Authentication failed", Toast.LENGTH_SHORT).show()
                            Log.e("AlertSubmission", "❌ No auth token available")
                        }
                        return@launch
                    }
                    
                    val authParam = "?auth=$token"
                    
                    // Increment ping count for existing alert - Try writing entire object back
                    val newPingCount = existingAlert.pingCount + 1
                    val updatedAlert = existingAlert.copy(
                        pingCount = newPingCount,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Serialize the updated alert
                    val gson = com.google.gson.Gson()
                    val updateBody = gson.toJson(updatedAlert)
                    
                    val url = URL("$rtdbBaseUrl/alerts/${existingAlert.id}.json$authParam")
                    Log.d("AlertSubmission", "📍 PUT URL: $url")
                    Log.d("AlertSubmission", "📤 PUT body: $updateBody")
                    
                    try {
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "PUT"  // Use PUT instead of PATCH
                            doOutput = true
                            connectTimeout = 5000
                            readTimeout = 5000
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        }
                        
                        connection.outputStream.use { it.write(updateBody.toByteArray(Charsets.UTF_8)) }
                        val code = connection.responseCode
                        
                        // Read response from correct stream
                        val response = if (code >= 400) {
                            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        } else {
                            connection.inputStream.bufferedReader().use { it.readText() }
                        }
                        
                        Log.d("AlertSubmission", "📡 PUT response code: $code, response: $response")
                        
                        if (code in 200..299) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "🔔 Alert pinged! (Count: $newPingCount)", Toast.LENGTH_SHORT).show()
                                showAlertSheet = false
                                Log.d("AlertSubmission", "✅ Successfully pinged alert with new count: $newPingCount")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "❌ Failed to ping alert (code: $code)", Toast.LENGTH_SHORT).show()
                                Log.e("AlertSubmission", "❌ PUT failed with code $code: $response")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Error pinging alert: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("AlertSubmission", "❌ PUT exception: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d("AlertSubmission", "➕ No existing alert found, creating new one...")
                    
                    // Get fresh token for this request
                    val token = try {
                        auth.currentUser?.getIdToken(true)?.await()?.token  // Force refresh
                    } catch (e: Exception) {
                        Log.e("AlertSubmission", "Error getting auth token", e)
                        null
                    }
                    
                    if (token == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Authentication failed", Toast.LENGTH_SHORT).show()
                            Log.e("AlertSubmission", "❌ No auth token available")
                        }
                        return@launch
                    }
                    
                    val authParam = "?auth=$token"
                    
                    // Create new alert with pingCount = 1
                    val body = JSONObject().apply {
                        put("type", type)
                        put("lat", location.latitude)
                        put("lon", location.longitude)
                        put("userId", userId ?: JSONObject.NULL)
                        put("userName", user?.displayName ?: "Unknown User")
                        put("userPhotoUrl", user?.photoUrl?.toString() ?: "")
                        put("timestamp", System.currentTimeMillis())
                        put("pingCount", 1)  // Initialize new alerts with pingCount = 1
                    }.toString()
                    
                    val url = URL("$rtdbBaseUrl/alerts.json$authParam")
                    Log.d("AlertSubmission", "📍 POST URL: $url")
                    
                    try {
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = true
                            connectTimeout = 5000
                            readTimeout = 5000
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        }
                        
                        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                        val code = connection.responseCode
                        
                        val response = if (code >= 400) {
                            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        } else {
                            connection.inputStream.bufferedReader().use { it.readText() }
                        }
                        
                        Log.d("AlertSubmission", "📡 POST response code: $code, response: $response")
                        
                        if (code in 200..299) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "🚨 Alert Sent: $type", Toast.LENGTH_SHORT).show()
                                showAlertSheet = false
                                Log.d("AlertSubmission", "✅ New alert created with pingCount = 1")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "❌ Failed to send alert (code: $code)", Toast.LENGTH_SHORT).show()
                                Log.e("AlertSubmission", "❌ POST failed with code $code: $response")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "❌ Error sending alert: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("AlertSubmission", "❌ POST exception: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AlertSubmission", "Error: ${e.message}", e)
                }
            }
        }
    }

    fun updateAlert(id: String, details: String, photoBase64: String?, phoneNumber: String = "") {
        scope.launch(Dispatchers.IO) {
            val token = try {
                auth.currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error getting auth token", e)
                null
            }
            val authParam = if (token != null) "?auth=$token" else ""

            val body = JSONObject().apply {
                if (details.isNotBlank()) put("details", details)
                if (!photoBase64.isNullOrBlank()) put("photoBase64", photoBase64)
                if (phoneNumber.isNotBlank()) put("phoneNumber", phoneNumber)
            }.toString()

            val url = URL("$rtdbBaseUrl/alerts/$id.json$authParam")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
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
                        Toast.makeText(context, "Details Updated", Toast.LENGTH_SHORT).show()
                        showAddDetails = false
                        showAlertDetails = false
                        // Reset state
                        detailsText = ""
                        capturedBitmap = null
                        capturedImageUri = null
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to update alert", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error updating alert", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun resolveAlert(id: String) {
        scope.launch(Dispatchers.IO) {
            val token = try {
                auth.currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error getting auth token", e)
                null
            }
            val authParam = if (token != null) "?auth=$token" else ""

            val body = JSONObject().apply {
                put("status", "resolved")
            }.toString()

            val url = URL("$rtdbBaseUrl/alerts/$id.json$authParam")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
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
                        Toast.makeText(context, "Alert Resolved", Toast.LENGTH_SHORT).show()
                        showAlertDetails = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to resolve alert", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error resolving alert", Toast.LENGTH_SHORT).show()
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
                                val styleJson =
                                    context.assets.open("style_3d.json").bufferedReader()
                                        .use { it.readText() }
                                map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                                    // Add Images for SymbolLayer - Use colored dots for alerts
                                    style.addImage(
                                        "icon-fire",
                                        createColoredDot(
                                            android.graphics.Color.parseColor("#FF6F00"),
                                            64
                                        ) // Orange for Fire
                                    )
                                    style.addImage(
                                        "icon-accident",
                                        createColoredDot(
                                            android.graphics.Color.parseColor("#F9A825"),
                                            72
                                        ) // Darker amber for better contrast
                                    )
                                    style.addImage(
                                        "icon-rescue",
                                        createColoredDot(
                                            android.graphics.Color.parseColor("#E53935"),
                                            64
                                        ) // Red for Medical
                                    )
                                    // Critical/Red Pulse Icon (larger than normal)
                                    style.addImage(
                                        "icon-critical",
                                        createColoredDot(
                                            android.graphics.Color.parseColor("#D32F2F"),
                                            92
                                        ) // Dark red, larger for emphasis
                                    )
                                    // Add Source and Layer for Alerts
                                    style.addSource(GeoJsonSource("alerts-source"))
                                    style.addLayer(
                                        SymbolLayer("alerts-layer", "alerts-source")
                                            .withProperties(
                                                PropertyFactory.iconImage("{icon-image}"),
                                                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                                                PropertyFactory.iconAllowOverlap(true),
                                                PropertyFactory.iconIgnorePlacement(true),
                                                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                                                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                                                PropertyFactory.iconSize(1.0f)
                                            )
                                    )

                                    // Add Source and Layer for Search Results
                                    style.addSource(GeoJsonSource("search-result-source"))
                                    style.addLayer(
                                        CircleLayer("search-result-layer", "search-result-source")
                                            .withProperties(
                                                PropertyFactory.circleColor(android.graphics.Color.BLUE),
                                                PropertyFactory.circleRadius(8f),
                                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                                                PropertyFactory.circleStrokeWidth(2f)
                                            )
                                    )

                                    // Add Source and Layer for Circle Members (profile photos)
                                    style.addSource(GeoJsonSource("members-source"))
                                    style.addLayer(
                                        SymbolLayer("members-layer", "members-source")
                                            .withProperties(
                                                PropertyFactory.iconImage("{member-icon}"),
                                                PropertyFactory.iconSize(1.0f),
                                                PropertyFactory.iconAllowOverlap(true),
                                                PropertyFactory.iconIgnorePlacement(true),
                                                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                                                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                                                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER)
                                            )
                                    )

                                    map.uiSettings.isRotateGesturesEnabled = true
                                    map.uiSettings.isTiltGesturesEnabled = true
                                    map.uiSettings.isCompassEnabled =
                                        false // Disable default compass

                                    if (hasLocationPermission) {
                                        try {
                                            val locationComponent = map.locationComponent
                                            locationComponent.activateLocationComponent(
                                                LocationComponentActivationOptions.builder(
                                                    context,
                                                    style
                                                ).build()
                                            )
                                            locationComponent.isLocationComponentEnabled = true
                                            locationComponent.cameraMode = CameraMode.TRACKING
                                            locationComponent.renderMode = RenderMode.COMPASS
                                        } catch (e: Exception) {
                                            Log.e("MapLibre", "Location component error", e)
                                        }
                                    }

                                    // Map Click Listener (Alerts & Search Results)
                                    map.addOnMapClickListener { point ->
                                        try {
                                            val screenPoint =
                                                map.projection.toScreenLocation(point)

                                            // 1. Check Member Markers
                                            val memberFeatures = map.queryRenderedFeatures(
                                                screenPoint,
                                                "members-layer"
                                            )
                                            if (memberFeatures.isNotEmpty()) {
                                                val uid = memberFeatures[0].getStringProperty("uid")
                                                val tappedMember =
                                                    circleMembers.find { it.uid == uid }
                                                if (tappedMember != null) {
                                                    selectedMemberOnMap = tappedMember
                                                    selectedMemberAddress = "Fetching location..."
                                                    // Reverse-geocode on background
                                                    scope.launch {
                                                        val addr = try {
                                                            if (tappedMember.lastLat != null && tappedMember.lastLon != null) {
                                                                PlaceSearcher.reverseGeocode(
                                                                    tappedMember.lastLat,
                                                                    tappedMember.lastLon
                                                                )
                                                            } else ""
                                                        } catch (e: Exception) {
                                                            ""
                                                        }
                                                        selectedMemberAddress =
                                                            if (addr.isNotBlank()) addr
                                                            else if (tappedMember.lastLat != null) "${tappedMember.lastLat}, ${tappedMember.lastLon}"
                                                            else "Location unknown"
                                                    }
                                                }
                                                return@addOnMapClickListener true
                                            }

                                            // 2. Check Search Results
                                            val searchFeatures = map.queryRenderedFeatures(
                                                screenPoint,
                                                "search-result-layer"
                                            )
                                            if (searchFeatures.isNotEmpty()) {
                                                if (selectedLocation != null) {
                                                    showLocationModal = true
                                                }
                                                return@addOnMapClickListener true
                                            }

                                            // 3. Check Alerts (Clustered)
                                            val features = map.queryRenderedFeatures(
                                                screenPoint,
                                                "alerts-layer"
                                            )
                                            if (features.isNotEmpty()) {
                                                val feature = features[0]
                                                val clusterId = feature.getStringProperty("clusterId")
                                                
                                                // Find the cluster and show cluster modal
                                                val cluster = clusteredAlerts.find { it.clusterId == clusterId }
                                                if (cluster != null) {
                                                    selectedCluster = cluster
                                                    showClusterModal = true
                                                    Log.d("AlertSubmission", "📍 Cluster tapped: ${cluster.alerts.size} alerts (${cluster.highestSeverityAlert.type})")
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        } catch (e: Exception) {
                                            Log.e("HomeScreen", "Error handling map click", e)
                                            false
                                        }
                                    }
                                    isMapReady = true
                                } // Close setStyle
                            } catch (e: Exception) {
                                Log.e("MapLibre", "Error loading style.json", e)
                                Toast.makeText(
                                    context,
                                    "Error loading map style",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Fallback to OSM Raster if asset fails
                                val osmStyle = Style.Builder()
                                    .withSource(
                                        RasterSource(
                                            "osm-source",
                                            TileSet(
                                                "tiles",
                                                "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                                            ),
                                            256
                                        )
                                    )
                                    .withLayer(RasterLayer("osm-layer", "osm-source"))
                                map.setStyle(osmStyle) { style ->
                                    map.uiSettings.isRotateGesturesEnabled = true
                                    map.uiSettings.isTiltGesturesEnabled = true
                                    map.uiSettings.isCompassEnabled =
                                        false // Disable default compass
                                    isMapReady = true
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
                    // Sharing Location Pill (Center) or Navigation Card
                    if (isNavigating && navigationSteps.isNotEmpty()) {
                        // Navigation Instruction Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF29B6F6), // Light Blue
                            contentColor = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 60.dp) // Leave space for right icons
                                .align(Alignment.CenterStart)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                val step = navigationSteps.getOrNull(currentStepIndex)
                                if (step != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Direction Icon
                                        val icon = when {
                                            step.maneuverModifier?.contains("left") == true -> Icons.Rounded.ArrowBack
                                            step.maneuverModifier?.contains("right") == true -> Icons.Rounded.ArrowForward
                                            else -> Icons.Rounded.ArrowUpward
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = step.instruction,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (distanceToNextTurn > 0) {
                                                Text(
                                                    text = "${distanceToNextTurn.toInt()} m",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        "Arrived",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    } else {
                        // Sharing Location Indicator
                        val (sharingLabel, sharingIcon, sharingColor) = when (sharingOption) {
                            "Visible to others" -> Triple(
                                "Sharing Location",
                                Icons.Rounded.Public,
                                Color(0xFF2196F3)
                            ) // Blue
                            "Family" -> Triple(
                                "Shared to family",
                                Icons.Rounded.Home,
                                Color(0xFF4CAF50)
                            ) // Green
                            "No one" -> Triple(
                                "Not sharing location",
                                Icons.Rounded.LocationOff,
                                Color(0xFFFF5252)
                            )

                            else -> Triple(
                                "Sharing Location",
                                Icons.Rounded.Public,
                                Color(0xFF2196F3)
                            )
                        }

                        Surface(
                            onClick = { showSharingSheet = true },
                            shape = RoundedCornerShape(percent = 50),
                            color = Color(0xFF2B2B2B).copy(alpha = 0.9f),
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInRoot()
                                    val size = coordinates.size
                                    sharingLocationRect = Rect(
                                        offset = position,
                                        size = Size(size.width.toFloat(), size.height.toFloat())
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = sharingIcon,
                                    contentDescription = null,
                                    tint = sharingColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sharingLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(sharingColor, CircleShape)
                                )
                            }
                        }
                    }

                    // Top Right Icons (Profile & Compass)
                    Column(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { navController.navigate("profile") }
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInRoot()
                                    val size = coordinates.size
                                    profileRect = Rect(
                                        offset = position,
                                        size = Size(size.width.toFloat(), size.height.toFloat())
                                    )
                                }
                        ) {
                            val photoUrl = auth.currentUser?.photoUrl?.toString()
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photoUrl)
                                        .crossfade(true)
                                        .transformations(CircleCropTransformation())
                                        .build(),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = {
                                mapLibreMap?.let { map ->
                                    map.animateCamera(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.Builder()
                                                .target(map.cameraPosition.target)
                                                .zoom(map.cameraPosition.zoom)
                                                .bearing(0.0) // Reset bearing to North
                                                .tilt(0.0)
                                                .build()
                                        )
                                    )
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
                                map.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(LatLng(10.3157, 123.8854)) // Reset to Cebu
                                            .zoom(12.0)
                                            .tilt(0.0)
                                            .bearing(0.0)
                                            .build()
                                    )
                                )
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
                        IconButton(
                            onClick = { showBottomSheet = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val size = coordinates.size
                                searchButtonRect = Rect(
                                    offset = position,
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // 3. Admin Overview (Admin Only)
                        if (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin") {
                            IconButton(
                                onClick = { showGlobalOverview = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Dashboard,
                                    contentDescription = "Admin Overview",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Spacer for Floating Alert Button (only for non-admins)
                        if (userRole != "lgu_admin" && userRole != "superadmin" && userRole != "admin") {
                            Spacer(modifier = Modifier.width(56.dp))
                        }

                        // 4. Notifications
                        IconButton(
                            onClick = { showNotifications = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val size = coordinates.size
                                notificationsButtonRect = Rect(
                                    offset = position,
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            }
                        ) {
                            // Badge logic: Check if there are any alerts
                            Box {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = "Notifications",
                                    tint = if (showNotifications) Color(0xFF29B6F6) else Color.Gray,
                                    modifier = Modifier.size(28.dp)
                                )
                                if (alerts.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(10.dp)
                                            .background(Color.Red, CircleShape)
                                    )
                                }
                            }
                        }

                        // 5. Family
                        IconButton(
                            onClick = {
                                if (userPlan == "Free") {
                                    Toast.makeText(
                                        context,
                                        "Map tracking is a premium feature. Upgrade your plan to see members on the map.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    showGroupSheet = true
                                }
                            },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val size = coordinates.size
                                groupButtonRect = Rect(
                                    offset = position,
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Group, // Family/Group Icon
                                contentDescription = "Family",
                                tint = if (userPlan == "Free") Color(0xFFBDBDBD) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Center Floating Alert Button (Overlapping)
                val buttonScale = remember { Animatable(1f) }

                if (userRole != "lgu_admin" && userRole != "superadmin") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-36).dp)
                            .scale(buttonScale.value)
                            .size(72.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                            .onGloballyPositioned { coordinates ->
                                val position = coordinates.positionInRoot()
                                val size = coordinates.size
                                alertButtonRect = Rect(
                                    offset = position,
                                    size = Size(size.width.toFloat(), size.height.toFloat())
                                )
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)

                                        // Start bloating animation
                                        val animationJob = scope.launch {
                                            buttonScale.animateTo(
                                                targetValue = 1.5f, // Bloat to 150%
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = LinearEasing
                                                )
                                            )
                                        }

                                        // Wait for long press only
                                        try {
                                            withTimeout(500) {
                                                // If user lifts finger before timeout, it's a tap - IGNORE IT
                                                val up = waitForUpOrCancellation()
                                                if (up != null) {
                                                    // Tap detected - Cancel animation and revert
                                                    animationJob.cancel()
                                                    scope.launch {
                                                        buttonScale.animateTo(
                                                            1f,
                                                            spring(stiffness = Spring.StiffnessLow)
                                                        )
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Timeout reached -> Long Press detected
                                            showRadialMenu = true

                                            // Reset scale
                                            scope.launch {
                                                buttonScale.animateTo(
                                                    1f,
                                                    spring(stiffness = Spring.StiffnessLow)
                                                )
                                            }
                                            // Start tracking drag
                                            var dragging = true
                                            while (dragging) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull()
                                                if (change != null) {
                                                    if (change.pressed) {
                                                        // Dragging
                                                        val position = change.position
                                                        val centerX = size.width / 2
                                                        val centerY = size.height / 2
                                                        val dx = position.x - centerX
                                                        val dy = position.y - centerY
                                                        val dist = sqrt(dx * dx + dy * dy)

                                                        // Threshold to leave center (e.g. 20% of width)
                                                        if (dist > size.width * 0.4f) {
                                                            // Calculate angle
                                                            var angle = Math.toDegrees(
                                                                atan2(
                                                                    dy.toDouble(),
                                                                    dx.toDouble()
                                                                )
                                                            )
                                                            if (angle < 0) angle += 360.0

                                                            // Map angle to options
                                                            // Fire: 210 (Left-ish)
                                                            // Rescue: 270 (Top)
                                                            // Accident: 330 (Right-ish)

                                                            radialSelection = when {
                                                                angle >= 180 && angle < 240 -> "Fire"
                                                                angle >= 240 && angle < 300 -> "Rescue"
                                                                angle >= 300 || angle < 0 -> "Accident" // 330 area
                                                                else -> null
                                                            }
                                                        } else {
                                                            radialSelection = null
                                                        }
                                                    } else {
                                                        // Up
                                                        dragging = false
                                                        if (radialSelection != null) {
                                                            // Execute selection
                                                            val alertType = when (radialSelection) {
                                                                "Rescue" -> "Emergency Rescue"
                                                                "Fire" -> "Fire Emergency"
                                                                "Accident" -> "Road Accident"
                                                                else -> null
                                                            }
                                                            if (alertType != null) {
                                                                sendAlert(alertType)
                                                            }
                                                            showRadialMenu = false
                                                            radialSelection = null
                                                        } else {
                                                            // Released at center or outside selection zones
                                                            showRadialMenu = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
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

            // Tutorial Overlay
            if (showTutorial && currentTutorialStep < tutorialSteps.size) {
                val currentStep = tutorialSteps[currentTutorialStep]
                val targetRect = when (currentStep.id) {
                    "search" -> searchButtonRect
                    "group" -> groupButtonRect
                    "alert" -> alertButtonRect
                    "notifications" -> notificationsButtonRect
                    "sharing" -> sharingLocationRect
                    "profile" -> profileRect
                    else -> null
                }

                TutorialOverlay(
                    targetRect = targetRect,
                    step = currentStep,
                    onNext = {
                        if (currentTutorialStep < tutorialSteps.size - 1) {
                            currentTutorialStep++
                        } else {
                            showTutorial = false
                            sharedPreferences.edit().putBoolean(tutorialKey, true).apply()
                        }
                    },
                    onBack = if (currentTutorialStep > 0) {
                        { currentTutorialStep-- }
                    } else null,
                    onDismiss = { /* Optional: Allow dismiss on tap outside? For now, stick to buttons */ }
                )
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
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.padding(top = 48.dp) // Lower the drawer below camera
        ) {
            ForceImmersiveMode()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight() // Allow full height
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

                // NestedScrollConnection to prevent bottom sheet from consuming scroll events
                // when scrolling up (dragging down) on the list.
                val listState = rememberLazyListState()

                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // If dragging down (scrolling up) and list is at top, consume it so sheet doesn't close
                            if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                return available
                            }
                            return Offset.Zero
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // Consume all available vertical scroll so the BottomSheet doesn't capture it
                            return available
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            // Consume all available vertical velocity
                            return available
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection)
                ) {
                    items(searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLocation = result
                                    showBottomSheet = false
                                    showLocationModal = true

                                    mapLibreMap?.let { map ->
                                        val position = LatLng(result.lat, result.lon)
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                position,
                                                15.0
                                            )
                                        )
                                    }
                                }
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

    // Radial Menu Overlay
    if (showRadialMenu) {
        RadialMenuOverlay(
            selectedOption = radialSelection,
            onDismiss = { showRadialMenu = false },
            onOptionSelected = { type ->
                val alertType = when (type) {
                    "Rescue" -> "Emergency Rescue"
                    "Fire" -> "Fire Emergency"
                    "Accident" -> "Road Accident"
                    else -> type
                }
                sendAlert(alertType)
                showRadialMenu = false
            }
        )
    }

    // Member On Map Popup
    if (selectedMemberOnMap != null) {
        val member = selectedMemberOnMap!!
        Dialog(onDismissRequest = { selectedMemberOnMap = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFEEEEEE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (member.profilePhoto != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(member.profilePhoto)
                                    .crossfade(true)
                                    .transformations(CircleCropTransformation())
                                    .build(),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFFF9800), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }

                        // Online indicator dot
                        val isOnlineNow =
                            (member.lastActive ?: 0L) > System.currentTimeMillis() - 120_000L
                        if (isOnlineNow) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.BottomEnd)
                                    .background(Color(0xFF4CAF50), CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Last active
                    Text(
                        text = formatLastActive(member.lastActive),
                        style = MaterialTheme.typography.bodySmall,
                        color = if ((member.lastActive
                                ?: 0L) > System.currentTimeMillis() - 120_000L
                        )
                            Color(0xFF4CAF50) else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Last seen address
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedMemberAddress.isBlank()) "Location unknown" else selectedMemberAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cancel
                        OutlinedButton(
                            onClick = { selectedMemberOnMap = null },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                        ) {
                            Text("Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }

                        // Navigate
                        Button(
                            onClick = {
                                val destLat = member.lastLat
                                val destLon = member.lastLon
                                if (destLat != null && destLon != null) {
                                    selectedMemberOnMap = null
                                    // Start navigation to member's last location
                                    scope.launch {
                                        val currentLocation =
                                            mapLibreMap?.locationComponent?.lastKnownLocation
                                        if (currentLocation != null) {
                                            isNavigating = true
                                            navigationSteps = emptyList()
                                            currentStepIndex = 0
                                            val (points, steps) = com.example.safeko.utils.RouteFetcher.fetchRoute(
                                                LatLng(
                                                    currentLocation.latitude,
                                                    currentLocation.longitude
                                                ),
                                                LatLng(destLat, destLon)
                                            )
                                            navigationSteps = steps

                                            // Draw route on map
                                            if (points.isNotEmpty()) {
                                                val coords = points.map {
                                                    Point.fromLngLat(
                                                        it.longitude,
                                                        it.latitude
                                                    )
                                                }
                                                mapLibreMap?.getStyle { style ->
                                                    val geojson = FeatureCollection.fromFeature(
                                                        Feature.fromGeometry(
                                                            LineString.fromLngLats(
                                                                coords
                                                            )
                                                        )
                                                    ).toJson()
                                                    val src =
                                                        style.getSourceAs<GeoJsonSource>("route-source")
                                                    if (src != null) {
                                                        src.setGeoJson(geojson)
                                                    } else {
                                                        style.addSource(
                                                            GeoJsonSource(
                                                                "route-source",
                                                                geojson
                                                            )
                                                        )
                                                        style.addLayer(
                                                            LineLayer("route-layer", "route-source")
                                                                .withProperties(
                                                                    PropertyFactory.lineColor(
                                                                        android.graphics.Color.BLUE
                                                                    ),
                                                                    PropertyFactory.lineWidth(5f),
                                                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                                                    PropertyFactory.lineJoin(
                                                                        Property.LINE_JOIN_ROUND
                                                                    ),
                                                                    PropertyFactory.lineGradient(
                                                                        Expression.interpolate(
                                                                            Expression.linear(),
                                                                            Expression.lineProgress(),
                                                                            Expression.stop(
                                                                                0f,
                                                                                Expression.color(
                                                                                    android.graphics.Color.BLUE
                                                                                )
                                                                            ),
                                                                            Expression.stop(
                                                                                1f,
                                                                                Expression.color(
                                                                                    android.graphics.Color.BLUE
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Waiting for your location...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Member location not available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                        ) {
                            Icon(
                                Icons.Rounded.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Cluster Details Modal - Show all alerts in cluster
    if (showClusterModal && selectedCluster != null) {
        ModalBottomSheet(
            onDismissRequest = { showClusterModal = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val cluster = selectedCluster!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${cluster.alerts.size} Alerts in Area",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Badge(
                        modifier = Modifier
                            .background(
                                if (cluster.isCritical) Color.Red else Color.Gray,
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            "${cluster.totalPings}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of alerts in cluster
                cluster.alerts.forEach { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                // Open alert details
                                selectedAlert = alert
                                showAlertDetails = true
                                showClusterModal = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (alert.pingCount >= 3) Color(0xFFFFE0E0) else Color(0xFFF5F5F5)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    alert.type,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${alert.lat}, ${alert.lon}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                if (alert.details.isNotBlank()) {
                                    Text(
                                        alert.details.take(50) + if (alert.details.length > 50) "..." else "",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                if (alert.pingCount >= 3) {
                                    Badge(
                                        modifier = Modifier
                                            .background(Color.Red, shape = CircleShape)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            "🔴 ${alert.pingCount}",
                                            color= Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                } else {
                                    Text(
                                        "📍 ${alert.pingCount}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    alert.userName ?: "Unknown",
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Alert Details Modal
    if (showAlertDetails && selectedAlert != null) {
        ModalBottomSheet(
            onDismissRequest = { showAlertDetails = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val alert = selectedAlert!!
            val currentUserId = auth.currentUser?.uid
            val isOwner = alert.userId != null && alert.userId == currentUserId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Address (moved under alert type)
                var displayAddress by remember { mutableStateOf(alert.address) }

                LaunchedEffect(alert.id, alert.address) {
                    if (displayAddress.isBlank()) {
                        withContext(Dispatchers.IO) {
                            try {
                                val address =
                                    PlaceSearcher.reverseGeocode(alert.lat, alert.lon)
                                if (address.isNotBlank()) {
                                    withContext(Dispatchers.Main) {
                                        displayAddress = address
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF7F7F7),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!alert.userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(alert.userPhotoUrl)
                                    .crossfade(true)
                                    .transformations(CircleCropTransformation())
                                    .build(),
                                contentDescription = "User Profile",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Profile",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .padding(12.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.userName ?: "Unknown User",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (icon, color) = when (alert.type) {
                                    "Fire Emergency" -> Icons.Rounded.LocalFireDepartment to Color(
                                        0xFFFF5722
                                    )

                                    "Road Accident" -> Icons.Rounded.CarCrash to Color(0xFFFFC107)
                                    "Emergency Rescue" -> Icons.Rounded.MedicalServices to Color.Red
                                    else -> Icons.Rounded.Warning to Color.Red
                                }

                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = alert.type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = displayAddress.ifBlank { "Lat: ${alert.lat}, Lon: ${alert.lon}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Details Column (Left Aligned)
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isOwner) {
                        var phoneNumberText by remember(alert.id) {
                            mutableStateOf(userPhoneNumber)
                        }
                        var localDetailsText by remember(alert.id) {
                            mutableStateOf(alert.details)
                        }

                        Text(
                            text = "Contact",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF7F7F7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Phone Number *",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = phoneNumberText,
                                    onValueChange = { phoneNumberText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "+63 912 345 6789",
                                            color = Color.LightGray
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFE0E0E0),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Details (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = localDetailsText,
                            onValueChange = { localDetailsText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            placeholder = {
                                Text(
                                    "Describe the situation...",
                                    color = Color.LightGray
                                )
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color(0xFFF7F7F7),
                                unfocusedContainerColor = Color(0xFFF7F7F7)
                            )
                        )

                        if (alert.photoBase64.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val imageBitmap = remember(alert.photoBase64) {
                                try {
                                    val decodedBytes =
                                        Base64.decode(alert.photoBase64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(
                                        decodedBytes,
                                        0,
                                        decodedBytes.size
                                    )?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (imageBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEDEDED))
                                ) {
                                    androidx.compose.foundation.Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Attached Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Photos (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        if (capturedBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEDEDED))
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Captured Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        val photoFile = createImageFile(context)
                                        capturedImageUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        cameraLauncher.launch(capturedImageUri!!)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Rounded.PhotoCamera,
                                        contentDescription = "Retake",
                                        tint = Color.White
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val photoFile = createImageFile(context)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        capturedImageUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            "Cannot open camera: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFF7F7F7)
                                )
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                var photoBase64: String? = null
                                if (capturedBitmap != null) {
                                    val outputStream = ByteArrayOutputStream()
                                    capturedBitmap!!.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        70,
                                        outputStream
                                    )
                                    val byteArray = outputStream.toByteArray()
                                    photoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                                }
                                updateAlert(
                                    alert.id,
                                    localDetailsText,
                                    photoBase64,
                                    phoneNumberText
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                        ) {
                            Text("Submit Details")
                        }
                    }

                }

                Spacer(modifier = Modifier.height(32.dp))

                // Navigation Button (Only for non-owners)
                if (!isOwner) {
                    Button(
                        onClick = {
                            val userLocation = mapLibreMap?.locationComponent?.lastKnownLocation
                            if (userLocation != null) {
                                scope.launch {
                                    Toast.makeText(
                                        context,
                                        "Calculating route...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val start =
                                        LatLng(userLocation.latitude, userLocation.longitude)
                                    val end = LatLng(alert.lat, alert.lon)

                                    val (points, steps) = RouteFetcher.fetchRoute(start, end)

                                    if (points.isNotEmpty()) {
                                        navigationService?.startNavigation(steps, points, end)
                                        mapLibreMap?.getStyle { style ->
                                            // Remove existing route layer and source if any
                                            if (style.getLayer("route-layer") != null) style.removeLayer(
                                                "route-layer"
                                            )
                                            if (style.getSource("route-source") != null) style.removeSource(
                                                "route-source"
                                            )

                                            // Create GeoJsonSource with lineMetrics enabled
                                            val lineString = LineString.fromLngLats(points.map {
                                                Point.fromLngLat(
                                                    it.longitude,
                                                    it.latitude
                                                )
                                            })
                                            val source = GeoJsonSource(
                                                "route-source",
                                                Feature.fromGeometry(lineString),
                                                GeoJsonOptions().withLineMetrics(true)
                                            )
                                            style.addSource(source)

                                            // Create LineLayer with Gradient
                                            val layer = LineLayer("route-layer", "route-source")
                                            layer.setProperties(
                                                PropertyFactory.lineWidth(8f),
                                                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                                PropertyFactory.lineGradient(
                                                    Expression.interpolate(
                                                        Expression.linear(),
                                                        Expression.lineProgress(),
                                                        Expression.stop(
                                                            0f,
                                                            Expression.color(android.graphics.Color.BLUE)
                                                        ),
                                                        Expression.stop(
                                                            1f,
                                                            Expression.color(android.graphics.Color.CYAN)
                                                        )
                                                    )
                                                )
                                            )
                                            style.addLayer(layer)

                                            // Zoom to start
                                            mapLibreMap?.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    start,
                                                    16.0
                                                )
                                            )
                                            isNavigating = true
                                        }
                                        showAlertDetails = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Route not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Waiting for current location...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E))
                    ) {
                        Icon(
                            Icons.Rounded.NearMe,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Navigate to Location",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (isOwner && alert.status != "resolved") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { resolveAlert(alert.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Resolve Alert")
                    }
                }
            }
        }
    }

    // Add Details Modal
    if (showAddDetails && selectedAlert != null) {
        ModalBottomSheet(
            onDismissRequest = { showAddDetails = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val alert = selectedAlert!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .imePadding()
            ) {
                Text(
                    text = "Add Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = detailsText,
                    onValueChange = { detailsText = it },
                    label = { Text("Additional Information") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Camera Button and Preview
                if (capturedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Retake Button
                        IconButton(
                            onClick = {
                                val photoFile = createImageFile(context)
                                capturedImageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(capturedImageUri!!)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.PhotoCamera,
                                contentDescription = "Retake",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val photoFile = createImageFile(context)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                capturedImageUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "Cannot open camera: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Convert Bitmap to Base64
                        var photoBase64: String? = null
                        if (capturedBitmap != null) {
                            val outputStream = ByteArrayOutputStream()
                            capturedBitmap!!.compress(
                                Bitmap.CompressFormat.JPEG,
                                70,
                                outputStream
                            )
                            val byteArray = outputStream.toByteArray()
                            photoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        }

                        updateAlert(alert.id, detailsText, photoBase64)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Submit Details")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Notifications Modal
    if (showNotifications) {
        // Use same skipPartiallyExpanded logic as search sheet if desired,
        // though typically notifications might want to be half-expanded.
        // User asked to make it "same as the search", so we should use skipPartiallyExpanded = true
        // and apply the nested scroll fix.
        val notificationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showNotifications = false },
            sheetState = notificationSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            // Lower the drawer to be below the camera (add top padding)
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                // .fillMaxHeight(0.7f) // Remove fixed height to behave like search sheet (full/dynamic)
            ) {
                var showResolvedAlerts by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (showResolvedAlerts) "Resolved Alerts" else "Alert Notifications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Surface(
                        onClick = { showResolvedAlerts = !showResolvedAlerts },
                        shape = RoundedCornerShape(20.dp),
                        color = if (showResolvedAlerts) Color.Black else Color(0xFFF5F5F5),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showResolvedAlerts) Icons.Rounded.History else Icons.Rounded.Notifications,
                                contentDescription = null,
                                tint = if (showResolvedAlerts) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showResolvedAlerts) "Show Active" else "Show Resolved",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (showResolvedAlerts) Color.White else Color.Black
                            )
                        }
                    }
                }

                val displayedAlerts =
                    remember(alerts, showResolvedAlerts, userRole, userDepartment) {
                        alerts.values.filter { alert ->
                            val statusMatch =
                                if (showResolvedAlerts) alert.status == "resolved" else alert.status != "resolved"
                            if (!statusMatch) return@filter false

                            // Filter alerts for LGU Admins in the notifications list
                            if (userRole == "lgu_admin") {
                                when (userDepartment) {
                                    "Firestation", "Fire" -> alert.type == "Fire Emergency"
                                    "Rescue" -> alert.type == "Emergency Rescue" || alert.type == "Road Accident"
                                    "Medical" -> alert.type == "Emergency Rescue"
                                    else -> true
                                }
                            } else {
                                true
                            }
                        }.sortedByDescending { it.timestamp }
                    }

                if (displayedAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    imageVector = if (showResolvedAlerts)
                                        Icons.Rounded.CheckCircle
                                    else
                                        Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }

                            Text(
                                text = if (showResolvedAlerts) "No Resolved Alerts" else "No Active Alerts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = if (showResolvedAlerts)
                                    "All alerts have been successfully resolved"
                                else
                                    "Stay alert for emergency notifications in your area",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    // NestedScrollConnection Fix
                    val listState = rememberLazyListState()
                    val nestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                    return available
                                }
                                return Offset.Zero
                            }

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset = available

                            override suspend fun onPostFling(
                                consumed: Velocity,
                                available: Velocity
                            ): Velocity = available
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection)
                            .padding(bottom = 24.dp) // Add bottom padding
                    ) {
                        items(displayedAlerts, key = { it.id }) { alert ->
                            // Lightweight card entrance animation
                            val animatedVisibility = remember { Animatable(0.92f) }
                            LaunchedEffect(Unit) {
                                animatedVisibility.animateTo(
                                    1f,
                                    animationSpec = tween(
                                        220,
                                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                                    )
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = animatedVisibility.value
                                        // Only scale vertically for a subtle slide-in feel
                                        scaleY = animatedVisibility.value
                                    },
                                onClick = {
                                    try {
                                        if (alert.status == "resolved") {
                                            Toast.makeText(
                                                context,
                                                "Alert already resolved",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            showNotifications = false
                                            // Focus on this alert
                                            mapLibreMap?.let { map ->
                                                val position = LatLng(alert.lat, alert.lon)
                                                map.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        position,
                                                        16.0
                                                    )
                                                )
                                            }
                                            selectedAlert = alert
                                            showAlertDetails = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e(
                                            "HomeScreen",
                                            "Error handling notification click",
                                            e
                                        )
                                        Toast.makeText(
                                            context,
                                            "Error opening alert",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 2.dp
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    2.dp,
                                    when (alert.type) {
                                        "Fire Emergency" -> Color(0xFFFF6B00)
                                        "Emergency Rescue" -> Color(0xFFE53935)
                                        "Road Accident" -> Color(0xFFFFC107)
                                        else -> Color(0xFF9E9E9E)
                                    }
                                )
                            ) {
                                // Derive per-type values
                                val alertColor = when (alert.type) {
                                    "Fire Emergency" -> Color(0xFFFF6B00)
                                    "Emergency Rescue" -> Color(0xFFE53935)
                                    "Road Accident" -> Color(0xFFFFC107)
                                    else -> Color(0xFF9E9E9E)
                                }
                                val alertIcon = when (alert.type) {
                                    "Emergency Rescue" -> Icons.Rounded.MedicalServices
                                    "Fire Emergency" -> Icons.Rounded.LocalFireDepartment
                                    "Road Accident" -> Icons.Rounded.DirectionsCar
                                    else -> Icons.Rounded.Warning
                                }
                                val diff = currentTime - alert.timestamp
                                val timeAgo = when {
                                    diff < 60000 -> "Just now"
                                    diff < 3600000 -> "${diff / 60000}m ago"
                                    diff < 86400000 -> "${diff / 3600000}h ago"
                                    else -> java.text.SimpleDateFormat(
                                        "MMM dd",
                                        java.util.Locale.getDefault()
                                    ).format(java.util.Date(alert.timestamp))
                                }

                                var notificationAddress by remember { mutableStateOf(alert.address) }
                                LaunchedEffect(alert.id, alert.address) {
                                    if (notificationAddress.isBlank()) {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val address = PlaceSearcher.reverseGeocode(
                                                    alert.lat,
                                                    alert.lon
                                                )
                                                if (address.isNotBlank()) withContext(Dispatchers.Main) {
                                                    notificationAddress = address
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // ── Top row: avatar + name/type + time chip ──
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Circular avatar with tinted bg
                                        Surface(
                                            shape = CircleShape,
                                            color = alertColor.copy(alpha = 0.12f),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            if (!alert.userPhotoUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(alert.userPhotoUrl)
                                                        .crossfade(true)
                                                        .transformations(CircleCropTransformation())
                                                        .build(),
                                                    contentDescription = "User Profile",
                                                    modifier = Modifier.size(46.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        imageVector = alertIcon,
                                                        contentDescription = null,
                                                        tint = alertColor,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Name + colored type label
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = alert.userName ?: "Unknown User",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = alertIcon,
                                                    contentDescription = alert.type,
                                                    tint = alertColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = alert.type.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = alertColor,
                                                letterSpacing = 0.5.sp
                                            )
                                        }

                                        // Time chip
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFF5F5F5)
                                        ) {
                                            Text(
                                                text = timeAgo,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 5.dp
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFF0F0F0))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // ── LOCATION row ──
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "LOCATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9E9E9E),
                                            modifier = Modifier.width(72.dp)
                                        )
                                        Text(
                                            text = notificationAddress.ifEmpty {
                                                "${
                                                    String.format(
                                                        "%.4f",
                                                        alert.lat
                                                    )
                                                }, ${String.format("%.4f", alert.lon)}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (alert.status != "resolved") {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        // ── Details button ──
                                        Button(
                                            onClick = {
                                                try {
                                                    showNotifications = false
                                                    mapLibreMap?.let { map ->
                                                        map.animateCamera(
                                                            CameraUpdateFactory.newLatLngZoom(
                                                                LatLng(alert.lat, alert.lon),
                                                                16.0
                                                            )
                                                        )
                                                    }
                                                    selectedAlert = alert
                                                    showAlertDetails = true
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "HomeScreen",
                                                        "Error handling notification click",
                                                        e
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE3F2FD),
                                                contentColor = Color(0xFF1565C0)
                                            ),
                                            contentPadding = PaddingValues(vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "Details",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Resolved",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    // Location Details Modal
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

    if (showLocationModal && selectedLocation != null) {
        LocationDetailsModal(
            location = selectedLocation!!,
            isNavigating = isNavigating,
            onDismiss = { showLocationModal = false },
            onCancelNavigation = {
                showLocationModal = false
                navigationService?.stopNavigation()
            },
            onGetDirections = {
                showLocationModal = false

                if (hasLocationPermission) {
                    val currentLocation = mapLibreMap?.locationComponent?.lastKnownLocation
                    if (currentLocation != null) {
                        Toast.makeText(context, "Calculating route...", Toast.LENGTH_SHORT)
                            .show()

                        scope.launch {
                            val start =
                                LatLng(currentLocation.latitude, currentLocation.longitude)
                            val end = LatLng(selectedLocation!!.lat, selectedLocation!!.lon)

                            val (points, steps) = RouteFetcher.fetchRoute(start, end)
                            if (points.isNotEmpty()) {
                                navigationService?.startNavigation(steps, points, end)
                                mapLibreMap?.getStyle { style ->
                                    // Remove existing if any
                                    if (style.getLayer("route-layer") != null) style.removeLayer(
                                        "route-layer"
                                    )
                                    if (style.getSource("route-source") != null) style.removeSource(
                                        "route-source"
                                    )

                                    // Create GeoJsonSource with lineMetrics enabled (required for gradient)
                                    val lineString = LineString.fromLngLats(points.map {
                                        Point.fromLngLat(
                                            it.longitude,
                                            it.latitude
                                        )
                                    })
                                    val source = GeoJsonSource(
                                        "route-source",
                                        Feature.fromGeometry(lineString),
                                        GeoJsonOptions().withLineMetrics(true)
                                    )
                                    style.addSource(source)

                                    // Create LineLayer with Gradient
                                    val layer = LineLayer("route-layer", "route-source")
                                    layer.setProperties(
                                        PropertyFactory.lineWidth(8f),
                                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                        // Initial gradient (will be animated)
                                        PropertyFactory.lineGradient(
                                            Expression.interpolate(
                                                Expression.linear(),
                                                Expression.lineProgress(),
                                                Expression.stop(
                                                    0f,
                                                    Expression.color(android.graphics.Color.BLUE)
                                                ),
                                                Expression.stop(
                                                    1f,
                                                    Expression.color(android.graphics.Color.CYAN)
                                                )
                                            )
                                        )
                                    )
                                    style.addLayer(layer)

                                    // Zoom to start
                                    mapLibreMap?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            start,
                                            16.0
                                        )
                                    )
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Could not find route",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Waiting for location...", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }

    // Sharing Location Bottom Sheet
    if (showSharingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSharingSheet = false },
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Who can see your location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "If you share, your precise location updates every time you open SafeKo, but disappears if you don't open the app for 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                val options = listOf(
                    Triple("Visible to others", "Share with everyone", Icons.Rounded.Public),
                    Triple("Family", "Share with family only", Icons.Rounded.Home),
                    Triple("No one", "Don't share location", Icons.Rounded.LocationOff)
                )

                options.forEach { (option, description, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sharingOption = option }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (option) {
                                "Visible to others" -> Color(0xFF29B6F6)
                                "Family" -> Color(0xFF00E676)
                                else -> Color.LightGray
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        RadioButton(
                            selected = (sharingOption == option),
                            onClick = { sharingOption = option },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF29B6F6),
                                unselectedColor = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showSharingSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                ) {
                    Text(
                        text = "Share location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Group Bottom Sheet
    if (showGroupSheet) {
        val groupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

        ModalBottomSheet(
            onDismissRequest = {
                if (isCreatingCircle) {
                    isCreatingCircle = false
                } else {
                    showGroupSheet = false
                }
            },
            sheetState = groupSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            if (isCreatingCircle) {
                var circleName by remember { mutableStateOf("") }
                var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
                var isUploading by remember { mutableStateOf(false) }

                val pickMedia =
                    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                        if (uri != null) selectedImageUri = uri
                    }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { isCreatingCircle = false }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "New group",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                if (circleName.isNotBlank() && !isUploading) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        isUploading = true
                                        val circleId = UUID.randomUUID().toString()

                                        val createCircle = { imageUrl: String? ->
                                            val limit = if (userPlan == "OWL+") 10 else 5
                                            val newCircle = Circle(
                                                id = circleId,
                                                name = circleName,
                                                ownerId = uid,
                                                members = listOf(uid),
                                                memberLimit = limit,
                                                type = "Group",
                                                createdAt = System.currentTimeMillis(),
                                                imageUrl = imageUrl
                                            )
                                            Firebase.firestore.collection("circles")
                                                .document(newCircle.id)
                                                .set(newCircle)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        context,
                                                        "Circle created!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    isCreatingCircle = false
                                                    isUploading = false
                                                    navController.navigate("chat/${newCircle.id}")
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "Failed: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    Log.e("HomeScreen", "Error creating circle", e)
                                                    isUploading = false
                                                }
                                        }

                                        if (selectedImageUri != null) {
                                            val storageRef =
                                                com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(
                                                    "circle_images/$circleId.jpg"
                                                )
                                            storageRef.putFile(selectedImageUri!!)
                                                .addOnSuccessListener {
                                                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                                                        createCircle(uri.toString())
                                                    }
                                                        .addOnFailureListener { e ->
                                                            Toast.makeText(
                                                                context,
                                                                "Failed to get image URL: ${e.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            createCircle(null)
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        context,
                                                        "Image upload failed: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    createCircle(null)
                                                }
                                        } else {
                                            createCircle(null)
                                        }
                                    }
                                }
                            },
                            enabled = circleName.isNotBlank() && !isUploading
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF29B6F6),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Create",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (circleName.isNotBlank()) Color(0xFF29B6F6) else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Image Picker
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.AddAPhoto,
                                contentDescription = "Add Photo",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Group name (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            androidx.compose.foundation.text.BasicTextField(
                                value = circleName,
                                onValueChange = { circleName = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Members must be added directly or join through an invite link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                val isEligible = userPlan == "Premium" || userPlan == "OWL+"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.95f)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header row: title + icon actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Circles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isEligible || userRole == "lgu_admin" || userRole == "admin" || userRole == "superadmin") {
                            IconButton(onClick = {
                                showLinkJoinDialog = true; showGroupSheet = false
                            }) {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = "Join by Link",
                                    tint = Color.Gray
                                )
                            }
                            IconButton(onClick = { 
                                if (hasCameraPermission) {
                                    showScanner = true
                                    showGroupSheet = false
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                                    Toast.makeText(context, "Please grant camera permission", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.QrCodeScanner,
                                    contentDescription = "Scan QR",
                                    tint = Color.Gray
                                )
                            }
                            val canCreateCircle = when (userPlan) {
                                "Premium", "OWL+" -> ownedGroupCount == 0
                                else -> false
                            }
                            if (canCreateCircle) {
                                IconButton(onClick = { isCreatingCircle = true }) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Create circle",
                                        tint = Color(0xFF29B6F6)
                                    )
                                }
                            } else {
                                IconButton(onClick = {
                                    Toast.makeText(
                                        context,
                                        "You can only create 1 group max.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Create circle",
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content (List or Empty State)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (userPlan == "Loading...") {
                            CircularProgressIndicator()
                        } else if (isEligible || userRole == "lgu_admin" || userRole == "admin" || userRole == "superadmin") {
                            if (userCircles.isNotEmpty()) {
                                // List User Circles
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    item {
                                        Text(
                                            text = "Groups",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    items(userCircles) { circle ->
                                        Card(
                                            onClick = { navController.navigate("chat/${circle.id}") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(
                                                    0xFFF5F5F5
                                                )
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!circle.imageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = circle.imageUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .background(
                                                                Color(0xFF29B6F6),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = circle.name.take(1).uppercase(),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        text = circle.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${circle.members.size} members",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Spacer(modifier = Modifier.weight(1.0f))
                                                Icon(
                                                    Icons.Rounded.ArrowForwardIos,
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (circleMembers.isNotEmpty() || isLoadingMembers) {
                                        item {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 16.dp),
                                                color = Color(0xFFEEEEEE)
                                            )
                                            Text(
                                                text = "Members",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        if (isLoadingMembers) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator()
                                                }
                                            }
                                        } else {
                                            items(circleMembers) { member ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (member.profilePhoto != null) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(
                                                                    LocalContext.current
                                                                )
                                                                    .data(member.profilePhoto)
                                                                    .crossfade(true)
                                                                    .transformations(
                                                                        CircleCropTransformation()
                                                                    )
                                                                    .build(),
                                                                contentDescription = "Profile Picture",
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(CircleShape),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .background(
                                                                        Color(0xFFFF9800),
                                                                        CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = member.name.take(1)
                                                                        .uppercase(),
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.Bold,
                                                                    style = MaterialTheme.typography.titleMedium
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column {
                                                            Text(
                                                                text = member.name,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "In: ${
                                                                    member.sharedCircles.joinToString(
                                                                        ", "
                                                                    )
                                                                }",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.Gray
                                                            )
                                                            Text(
                                                                text = "Last active: ${
                                                                    formatLastActive(
                                                                        member.lastActive
                                                                    )
                                                                }",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if ((member.lastActive
                                                                        ?: 0L) > System.currentTimeMillis() - 120_000L
                                                                )
                                                                    Color(0xFF4CAF50) else Color.Gray
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // Create/Scan moved to header icons above
                            } else {
                                // Show Create Group button (Empty State)
                                Text(
                                    text = "No active circles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Create a circle to add a Family channel.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                val canCreateCircle = when (userPlan) {
                                    "Premium", "OWL+" -> ownedGroupCount == 0
                                    else -> false
                                }
                                Button(
                                    onClick = {
                                        if (canCreateCircle) {
                                            isCreatingCircle = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "You reached your group creation limits.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canCreateCircle) Color(
                                            0xFF29B6F6
                                        ) else Color.LightGray
                                    )
                                ) {
                                    Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Create New Circle",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Upgrade Prompt
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unlock Circles",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Upgrade to Premium or OWL+ to create and join circles.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    navController.navigate("premium")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFFFD54F
                                    )
                                ) // Gold
                            ) {
                                Text(
                                    text = "View Plans",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Scan and Create actions moved to header icons above
                    }
                }
            }
        }
    }

    // Force-show admin overview for admins (this ensures it shows on login)
    LaunchedEffect(userRole) {
        val isAdmin = userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin"
        if (isAdmin) {
            Log.d("AdminForceShow", "✅ User is admin ($userRole), forcing showGlobalOverview = true")
            showGlobalOverview = true
        }
    }

    // Admin Overview Bottom Sheet (Admin Only)
    if (showGlobalOverview && (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin")) {
        val overviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        var totalAlerts by remember { mutableStateOf(0) }
        var activeAlerts by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val alertsResult = Firebase.firestore.collection("alerts").get().await()
                    var alertCount = 0
                    var activeCount = 0

                    for (alertDoc in alertsResult) {
                        alertCount++
                        val status = alertDoc.getString("status") ?: "active"
                        if (status == "active" || status.isEmpty()) {
                            activeCount++
                        }
                    }

                    totalAlerts = alertCount
                    activeAlerts = activeCount
                } catch (e: Exception) {
                    Log.e("GlobalOverview", "Error fetching alerts: ${e.message}")
                    totalAlerts = 0
                    activeAlerts = 0
                }
            }
        }

        // Set up real-time listener for Pending Co-Admin Requests
        LaunchedEffect(showGlobalOverview) {
            if (showGlobalOverview) {
                loadingPendingRequests = true
                val currentLgcId = auth.currentUser?.uid ?: ""
                Log.d("PendingAdmins", "Loading pending requests for lgcId=$currentLgcId")
                
                val listener = Firebase.firestore
                    .collection("pending_admins")
                    .whereEqualTo("lgcId", currentLgcId)
                    .whereEqualTo("status", "pending")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("PendingAdmins", "❌ Error listening to pending requests: ${error.message}")
                            loadingPendingRequests = false
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            Log.d("PendingAdmins", "Found ${snapshot.documents.size} pending requests")
                            pendingCoAdmins = snapshot.documents.map { doc ->
                                val userid = doc.getString("userId") ?: ""
                                val username = doc.getString("userName") ?: ""
                                Log.d("PendingAdmins", "Request: userId=$userid, userName=$username")
                                mapOf(
                                    "id" to (doc.id as Any),
                                    "userName" to (username as Any),
                                    "userEmail" to (doc.getString("userEmail") ?: "" as Any),
                                    "createdAt" to (try {
                                        doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now()
                                    } catch (e: Exception) {
                                        val ms = doc.getLong("createdAt") ?: 0L
                                        if (ms > 0) com.google.firebase.Timestamp(ms / 1000, 0) else com.google.firebase.Timestamp.now()
                                    } as Any),
                                    "userId" to (userid as Any)
                                )
                            }
                            loadingPendingRequests = false
                            Log.d("PendingAdmins", "Loaded ${pendingCoAdmins.size} pending co-admins")
                        }
                    }
            }
        }

        // Set up real-time listener for Co-Admin List
        LaunchedEffect(showGlobalOverview, userRole, userLgcId) {
            if (showGlobalOverview) {
                loadingCoAdmins = true
                // For lgu_admin, use their lgc_id; for admin/superadmin, use their uid
                val searchLgcId = if (userRole == "lgu_admin" && userLgcId.isNotEmpty()) {
                    userLgcId
                } else {
                    auth.currentUser?.uid ?: ""
                }
                
                val listener = Firebase.firestore
                    .collection("users")
                    .whereEqualTo("lgc_id", searchLgcId)
                    .whereEqualTo("role", "lgu_admin")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("CoAdminsList", "Error listening to co-admins: ${error.message}")
                            loadingCoAdmins = false
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null) {
                            coAdminsList = snapshot.documents.map { doc ->
                                mapOf(
                                    "id" to (doc.id as Any),
                                    "displayName" to (doc.getString("displayName")
                                        ?: doc.getString("name") ?: "Unknown" as Any),
                                    "email" to (doc.getString("email") ?: "" as Any),
                                    "createdAt" to (try {
                                        doc.getTimestamp("createdAt") ?: doc.getTimestamp("joinedDate") ?: com.google.firebase.Timestamp.now()
                                    } catch (e: Exception) {
                                        val ms = doc.getLong("createdAt") ?: 0L
                                        if (ms > 0) com.google.firebase.Timestamp(ms / 1000, 0) else com.google.firebase.Timestamp.now()
                                    } as Any)
                                )
                            }
                            loadingCoAdmins = false
                        }
                    }
            }
        }

        // Set up real-time listener for Group Owner (only for lgu_admin users)
        LaunchedEffect(showGlobalOverview, userRole, userLgcId) {
            if (showGlobalOverview && userRole == "lgu_admin" && userLgcId.isNotEmpty()) {
                loadingGroupOwner = true
                // Fetch the group owner (the user where uid == lgc_id of current user)
                val listener = Firebase.firestore
                    .collection("users")
                    .document(userLgcId)  // userLgcId is the boss's uid
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("GroupOwner", "Error listening to group owner: ${error.message}")
                            loadingGroupOwner = false
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null && snapshot.exists()) {
                            groupOwnerData = mapOf(
                                "id" to (snapshot.id as Any),
                                "displayName" to (snapshot.getString("displayName")
                                    ?: snapshot.getString("name") ?: "Unknown" as Any),
                                "email" to (snapshot.getString("email") ?: "" as Any),
                                "role" to (snapshot.getString("role") ?: "lgu_admin" as Any)
                            )
                            loadingGroupOwner = false
                        }
                    }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showGlobalOverview = false },
            sheetState = overviewSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title with QR and Link Icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Dashboard,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Admin Overview",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            // DEBUG: Show current user role
                            Text(
                                text = "Your role: $userRole",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // QR and Link Icons (visible to all admins, but QR restricted to group owners)
                    val currentUid = auth.currentUser?.uid ?: "NULL"
                    val isGroupOwner = userRole == "admin" || userRole == "superadmin" || (userRole == "lgu_admin" && currentUid == userLgcId)
                    Log.d("AdminOverview", "QR Check - userRole=$userRole, currentUid=$currentUid, userLgcId=$userLgcId, Match=${currentUid == userLgcId}, isGroupOwner=$isGroupOwner")
                    if (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // QR Icon - using clickable instead of IconButton (only visible to group owners)
                            if (isGroupOwner) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCode2,
                                    contentDescription = "Show QR Code",
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(8.dp)
                                        .clickable {
                                            Log.d("QRIcon", "QR icon clicked, userRole: $userRole, isGroupOwner: $isGroupOwner")
                                            if (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin") {
                                            val lgcId = auth.currentUser?.uid ?: ""
                                            Log.d("QRIcon", "Generating QR for lgcId: $lgcId")
                                            val invite = QRCodeUtils.generateCoAdminInviteQR(lgcId)
                                            Log.d("QRIcon", "QR generated: ${invite != null}, showQrDialog before: $showQrDialog")
                                            if (invite != null) {
                                                coAdminInviteQR = invite.qrBitmap
                                                coAdminInviteToken = invite.token
                                                coAdminDeepLink = invite.deepLink
                                                showQrDialog = true
                                                Log.d("QRIcon", "showQrDialog after: $showQrDialog")
                                                Toast.makeText(context, "QR Code Generated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Log.e("QRIcon", "Failed to generate QR code")
                                                Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Log.d("QRIcon", "User role not admin: $userRole")
                                        }
                                    }
                                )
                            }

                            // Link Icon - using clickable instead of IconButton (only visible to group owners)
                            if (isGroupOwner) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = "Copy Invite Link",
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(8.dp)
                                        .clickable {
                                            Log.d("LinkIcon", "Link icon clicked, userRole: $userRole, isGroupOwner: $isGroupOwner")
                                            if (isGroupOwner) {
                                            val lgcId = auth.currentUser?.uid ?: ""
                                            Log.d("LinkIcon", "Generating QR for lgcId: $lgcId")
                                            val invite = QRCodeUtils.generateCoAdminInviteQR(lgcId)
                                            Log.d("LinkIcon", "QR generated: ${invite != null}")
                                            if (invite != null) {
                                                coAdminInviteQR = invite.qrBitmap
                                                coAdminInviteToken = invite.token
                                                coAdminDeepLink = invite.deepLink
                                                showQrDialog = true
                                                Toast.makeText(context, "QR Code Generated!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Log.e("LinkIcon", "Failed to generate QR code")
                                                Toast.makeText(context, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Log.d("LinkIcon", "User not group owner: userRole=$userRole, isGroupOwner=$isGroupOwner")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Statistics Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Alerts Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = totalAlerts.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Active Alerts Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeAlerts.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57C00)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Active Alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Recent Activity Section
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Activity Placeholder
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No recent activity",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pending Co-Admin Requests Section - NOW OUTSIDE CARD
                if (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin") {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0xFFEEEEEE)
                    )

                    Text(
                        text = "Pending Co-Admin Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (loadingPendingRequests) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        } else if (pendingCoAdmins.isEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No pending requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingCoAdmins.size) { index ->
                                    val request = pendingCoAdmins[index]
                                    val userName = request["userName"] as? String ?: "Unknown"
                                    val userEmail = request["userEmail"] as? String ?: ""
                                    val requestId = request["id"] as? String ?: ""
                                    val userId = request["userId"] as? String ?: ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(
                                                0xFFFAFAFA
                                            )
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            // User Info
                                            Text(
                                                text = userName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = userEmail,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action Buttons - Show for admin, superadmin, and lgu_admin
                                            if (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin") {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            Toast.makeText(context, "🔄 Starting approval process...", Toast.LENGTH_SHORT).show()
                                                            Log.d("ApprovalFlow", "=== APPROVAL BUTTON CLICKED ===")
                                                            scope.launch {
                                                                try {
                                                                    val currentLgcId = auth.currentUser?.uid ?: ""
                                                                    Toast.makeText(context, "Approving as admin: $currentLgcId", Toast.LENGTH_SHORT).show()
                                                                    Log.d("ApprovalFlow", "Starting approval for userId=$userId, lgcId=$currentLgcId")

                                                                    // Step 1: Update user role to lgu_admin and set lgc_id using SET with merge (more reliable)
                                                                    val updateData = mapOf(
                                                                        "role" to "lgu_admin",
                                                                        "lgc_id" to currentLgcId,
                                                                        "department" to userDepartment
                                                                    )
                                                                    Log.d("ApprovalFlow", "Writing data: $updateData")
                                                                    
                                                                    try {
                                                                        Firebase.firestore.collection("users")
                                                                            .document(userId)
                                                                            .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                                                                            .await()
                                                                        Log.d("ApprovalFlow", "✓ User document updated successfully with SET+merge")
                                                                    } catch (e: Exception) {
                                                                        Log.e("ApprovalFlow", "✗ FAILED to update user document!", e)
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Error updating user: ${e.message}",
                                                                            Toast.LENGTH_LONG
                                                                        ).show()
                                                                        throw e
                                                                    }

                                                                    // Step 2: Verify the update worked by reading back the document
                                                                    Log.d("ApprovalFlow", "Step 2: Verifying update...")
                                                                    val verifyDoc = Firebase.firestore.collection("users")
                                                                        .document(userId)
                                                                        .get()
                                                                        .await()
                                                                    
                                                                    val verifiedRole = verifyDoc.getString("role")
                                                                    val verifiedLgcId = verifyDoc.getString("lgc_id")
                                                                    Log.d("ApprovalFlow", "Database shows - role=$verifiedRole, lgc_id=$verifiedLgcId | Expected: role=lgu_admin, lgc_id=$currentLgcId")
                                                                    
                                                                    if (verifiedRole != "lgu_admin" || verifiedLgcId != currentLgcId) {
                                                                        Log.e("ApprovalFlow", "❌ CRITICAL: Update did not persist! DB values don't match expected. This points to Firestore security rules or a write conflict.")
                                                                        Toast.makeText(
                                                                            context,
                                                                            "⚠️ Update failed to persist. Check Firestore rules for users/$userId. Actual: role=$verifiedRole, lgc_id=$verifiedLgcId",
                                                                            Toast.LENGTH_LONG
                                                                        ).show()
                                                                        return@launch
                                                                    }
                                                                    Log.d("ApprovalFlow", "✓✓ Verification PASSED - data persisted correctly!")

                                                                    // Step 3: Update request status to approved
                                                                    Firebase.firestore.collection("pending_admins")
                                                                        .document(requestId)
                                                                        .update(
                                                                            mapOf(
                                                                                "status" to "approved",
                                                                                "approvedBy" to auth.currentUser?.uid
                                                                            )
                                                                        )
                                                                        .await()
                                                                    Log.d("ApprovalFlow", "Pending request marked as approved")

                                                                    // Refresh the pending requests list
                                                                    val updatedResult = Firebase.firestore
                                                                        .collection("pending_admins")
                                                                        .whereEqualTo("lgcId", currentLgcId)
                                                                        .whereEqualTo("status", "pending")
                                                                        .get()
                                                                        .await()

                                                                    pendingCoAdmins =
                                                                        updatedResult.documents.map { doc ->
                                                                            mapOf(
                                                                                "id" to (doc.id as Any),
                                                                                "userName" to (doc.getString(
                                                                                    "userName"
                                                                                ) ?: "" as Any),
                                                                                "userEmail" to (doc.getString(
                                                                                    "userEmail"
                                                                                ) ?: "" as Any),
                                                                                "createdAt" to (try {
                                                                                    doc.getTimestamp(
                                                                                        "createdAt"
                                                                                    ) ?: com.google.firebase.Timestamp.now()
                                                                                } catch (e: Exception) {
                                                                                    val ms = doc.getLong("createdAt") ?: 0L
                                                                                    if (ms > 0) com.google.firebase.Timestamp(ms / 1000, 0) else com.google.firebase.Timestamp.now()
                                                                                } as Any),
                                                                                "userId" to (doc.getString(
                                                                                    "userId"
                                                                                ) ?: "" as Any)
                                                                            )
                                                                        }

                                                                    Toast.makeText(
                                                                        context,
                                                                        "$userName approved as co-admin ✓",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    Log.d("ApprovalFlow", "Approval completed successfully")
                                                                } catch (e: Exception) {
                                                                    Log.e("ApprovalFlow", "Approval failed: ${e.message}", e)
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Error approving request: ${e.message}",
                                                                        Toast.LENGTH_LONG
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF4CAF50)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Approve",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }

                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                try {
                                                                    // Update request status to declined
                                                                    Firebase.firestore.collection("pending_admins")
                                                                        .document(requestId)
                                                                        .update(
                                                                            mapOf("status" to "declined")
                                                                        )
                                                                    // Refresh the pending requests list
                                                                    val currentLgcId =
                                                                        auth.currentUser?.uid ?: ""
                                                                    val updatedResult =
                                                                        Firebase.firestore
                                                                            .collection("pending_admins")
                                                                            .whereEqualTo(
                                                                                "lgcId",
                                                                                currentLgcId
                                                                            )
                                                                            .whereEqualTo(
                                                                                "status",
                                                                                "pending"
                                                                            )
                                                                            .get()
                                                                            .await()

                                                                    pendingCoAdmins =
                                                                        updatedResult.documents.map { doc ->
                                                                            mapOf(
                                                                                "id" to (doc.id as Any),
                                                                                "userName" to (doc.getString(
                                                                                    "userName"
                                                                                ) ?: "" as Any),
                                                                                "userEmail" to (doc.getString(
                                                                                    "userEmail"
                                                                                ) ?: "" as Any),
                                                                                "createdAt" to (try {
                                                                                    doc.getTimestamp(
                                                                                        "createdAt"
                                                                                    ) ?: com.google.firebase.Timestamp.now()
                                                                                } catch (e: Exception) {
                                                                                    val ms = doc.getLong("createdAt") ?: 0L
                                                                                    if (ms > 0) com.google.firebase.Timestamp(ms / 1000, 0) else com.google.firebase.Timestamp.now()
                                                                                } as Any),
                                                                                "userId" to (doc.getString(
                                                                                    "userId"
                                                                                ) ?: "" as Any)
                                                                            )
                                                                        }

                                                                    Toast.makeText(
                                                                        context,
                                                                        "Request declined",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Error declining request: ${e.message}",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(36.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFF44336)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Decline",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall
                                                        )
                                                    }
                                                }
                                            } else {
                                                // For co-admins, show read-only state
                                                Text(
                                                    text = "Pending review by main admin",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF1565C0),
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFFEEEEEE)
                )

                // GROUP OWNER Section (only for lgu_admin users)
                if (userRole == "lgu_admin") {
                    Text(
                        text = "Group Owner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (loadingGroupOwner) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (groupOwnerData != null) {
                        val ownerName = groupOwnerData!!["displayName"] as? String ?: "Unknown"
                        val ownerEmail = groupOwnerData!!["email"] as? String ?: ""
                        val ownerId = groupOwnerData!!["id"] as? String ?: ""

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            elevation = CardDefaults.cardElevation(0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            Color(0xFF43A047),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        ownerName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ownerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(ownerEmail, fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color(0xFF43A047).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "GROUP OWNER",
                                            color = Color(0xFF43A047),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // See Members Button
                    Button(
                        onClick = { showMembersSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            tint = Color.White
                        )
                        Text("See Members", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // QR Code Dialog (Root Level - shown when icons are clicked)
        Log.d("QRDialog", "Dialog condition check: showQrDialog=$showQrDialog, qrNotNull=${coAdminInviteQR != null}, userRole=$userRole")
        if (showQrDialog && coAdminInviteQR != null && (userRole == "admin" || userRole == "superadmin" || userRole == "lgu_admin")) {
            Log.d("QRDialog", "Dialog should be showing!")
            Dialog(
                onDismissRequest = { showQrDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = "Invite Co-Admin",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )

                        // QR Code
                        Card(
                            modifier = Modifier
                                .size(200.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color(0xFF1565C0))
                        ) {
                            Image(
                                bitmap = coAdminInviteQR!!.asImageBitmap(),
                                contentDescription = "Co-Admin Invite QR",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Text(
                            text = "Scan with phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFE0E0E0)
                        )

                        // Shareable Link
                        Text(
                            text = "Or share this link:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText(
                                        "Invite Link",
                                        coAdminDeepLink ?: ""
                                    )
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        "Link copied to clipboard",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            border = BorderStroke(1.dp, Color(0xFF1565C0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = coAdminDeepLink?.take(35) + "...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1565C0),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy Link",
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Scan and Close Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Scan Button
                            Button(
                                onClick = { 
                                    showQrDialog = false
                                    showQrScanner = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Scan",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            // Close Button
                            Button(
                                onClick = { showQrDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // QR Code Scanner
        if (showQrScanner) {
            QRCodeScanner(
                onQrCodeScanned = { scannedValue ->
                    Log.d("QRScanner", "QR code scanned: $scannedValue")
                    showQrScanner = false
                    // Parse the deep link and navigate to ProcessCoAdminInviteScreen
                    if (!scannedValue.isNullOrBlank()) {
                        Log.d("QRScanner", "Checking if starts with safeko://invite: ${scannedValue.startsWith("safeko://invite")}")
                        
                        when {
                            scannedValue.startsWith("safeko://invite") -> {
                                // Extract token and lgcId from the deep link
                                try {
                                    val uri = android.net.Uri.parse(scannedValue)
                                    val token = uri.getQueryParameter("token")
                                    val lgcId = uri.getQueryParameter("lgcId")
                                    Log.d("QRScanner", "Extracted - token: $token, lgcId: $lgcId")
                                    if (!token.isNullOrBlank() && !lgcId.isNullOrBlank()) {
                                        Log.d("QRScanner", "Navigating to process_invite with token=$token, lgcId=$lgcId")
                                        // Navigate using the route name defined in MainActivity
                                        navController.navigate("process_invite?token=${java.net.URLEncoder.encode(token, "UTF-8")}&lgcId=${java.net.URLEncoder.encode(lgcId, "UTF-8")}")
                                    } else {
                                        Toast.makeText(context, "Invalid QR code format - missing token or LGU ID", Toast.LENGTH_SHORT).show()
                                        Log.w("QRScanner", "Invalid QR format - token: $token, lgcId: $lgcId")
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error processing QR code: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("QRScanner", "Error parsing QR code", e)
                                }
                            }
                            scannedValue.contains("safeko") && scannedValue.contains("invite") -> {
                                Log.d("QRScanner", "Detected SafeKo invite but wrong format, trying to parse anyway")
                                try {
                                    val uri = android.net.Uri.parse(scannedValue)
                                    val token = uri.getQueryParameter("token")
                                    val lgcId = uri.getQueryParameter("lgcId")
                                    if (!token.isNullOrBlank() && !lgcId.isNullOrBlank()) {
                                        navController.navigate("process_invite?token=${java.net.URLEncoder.encode(token, "UTF-8")}&lgcId=${java.net.URLEncoder.encode(lgcId, "UTF-8")}")
                                    } else {
                                        Toast.makeText(context, "This is not a valid SafeKo invite QR code", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                Toast.makeText(context, "This is not a SafeKo invite QR code", Toast.LENGTH_SHORT).show()
                                Log.d("QRScanner", "Not a SafeKo QR: $scannedValue")
                            }
                        }
                    }
                },
                onClose = {
                    showQrScanner = false
                    showQrDialog = true  // Go back to the dialog
                }
            )
        }



        // Edit Profile Dialog
        if (showEditProfile) {
            EditProfileDialog(
                showDialog = showEditProfile,
                onDismiss = { showEditProfile = false },
                currentName = auth.currentUser?.displayName ?: "User",
                currentEmail = auth.currentUser?.email ?: "No Email",
                currentPhotoUrl = auth.currentUser?.photoUrl?.toString(),
                initialPhoneNumber = userPhoneNumber,
                currentLocation = currentUserAddress,
                isPhoneVerified = false, // Not using full auth flow here to save space, user should edit in Profile
                userRole = userRole, // NEW: Pass user role for password change feature
                onChangePhoto = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onSave = { newName, newPhone ->
                    userPhoneNumber = newPhone
                    sharedPreferences.edit()
                        .putString("user_phone_number_${auth.currentUser?.uid}", newPhone)
                        .apply()

                    val userId = auth.currentUser?.uid
                    if (userId != null && newName.isNotBlank() && newName != auth.currentUser?.displayName) {
                        val profileUpdates =
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(newName)
                                .build()
                        auth.currentUser?.updateProfile(profileUpdates)
                        com.google.firebase.ktx.Firebase.firestore.collection("users")
                            .document(userId)
                            .update("name", newName)
                    }

                    showEditProfile = false
                    Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showLinkJoinDialog) {
            var linkInput by remember { mutableStateOf("") }
            var isJoining by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showLinkJoinDialog = false },
                title = { Text("Join Circle", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter the group invite link to join.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = linkInput,
                            onValueChange = { linkInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://safeko.app/join/...") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val circleIdStr = if (linkInput.contains("/join/")) {
                                linkInput.substringAfterLast("/join/")
                            } else {
                                linkInput // User might just paste the ID
                            }

                            val parsedCircleId = circleIdStr.trim().removeSuffix("/")

                            if (parsedCircleId.isNotBlank()) {
                                isJoining = true
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    com.google.firebase.ktx.Firebase.firestore.collection("circles")
                                        .document(parsedCircleId)
                                        .get()
                                        .addOnSuccessListener { doc ->
                                            if (doc.exists()) {
                                                val members = doc.get("members") as? List<*>
                                                    ?: emptyList<Any>()
                                                val limit = doc.getLong("memberLimit")?.toInt()
                                                    ?: 5 // Legacy fallback
                                                val type = doc.getString("type") ?: "Group"

                                                if (type == "Duo") {
                                                    isJoining = false
                                                    Toast.makeText(
                                                        context,
                                                        "This circle type is no longer supported.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else if (userPlan != "Premium" && userPlan != "OWL+") {
                                                    isJoining = false
                                                    Toast.makeText(
                                                        context,
                                                        "Upgrade to Premium to join family circles.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else if (members.size >= limit) {
                                                    isJoining = false
                                                    Toast.makeText(
                                                        context,
                                                        "This circle has reached its maximum member limit!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    com.google.firebase.ktx.Firebase.firestore.collection(
                                                        "circles"
                                                    ).document(parsedCircleId)
                                                        .update(
                                                            "members",
                                                            com.google.firebase.firestore.FieldValue.arrayUnion(
                                                                uid
                                                            )
                                                        )
                                                        .addOnSuccessListener {
                                                            isJoining = false
                                                            showLinkJoinDialog = false
                                                            Toast.makeText(
                                                                context,
                                                                "Joined group!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            navController.navigate("chat/$parsedCircleId")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            isJoining = false
                                                            Toast.makeText(
                                                                context,
                                                                "Failed: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                }
                                            } else {
                                                isJoining = false
                                                Toast.makeText(
                                                    context,
                                                    "Circle not found",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isJoining = false
                                            Toast.makeText(
                                                context,
                                                "Failed to join: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Invalid join link", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        enabled = linkInput.isNotBlank() && !isJoining,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Join", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkJoinDialog = false }, enabled = !isJoining) {
                        Text("Cancel", color = Color.Gray)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
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

    // Members Sheet - displays all co-admins
    if (showMembersSheet && userRole == "lgu_admin") {
        val membersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showMembersSheet = false },
            sheetState = membersSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.People,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Team Members",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (loadingCoAdmins) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (coAdminsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No co-admins yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(coAdminsList.size) { index ->
                            val admin = coAdminsList[index]
                            val adminName = admin["displayName"] as? String ?: admin["fullName"] as? String ?: "Unknown"
                            val adminEmail = admin["email"] as? String ?: ""

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(0.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(0xFF1565C0),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            adminName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(adminName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(adminEmail, fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = Color(0xFF1565C0).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "CO-ADMIN",
                                                color = Color(0xFF1565C0),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Full-screen QR Scanner Overlay (appears on top of everything when active)
    if (showScanner) {
        Box(modifier = Modifier.fillMaxSize()) {
            QRCodeScanner(
                onQrCodeScanned = { result ->
                    Log.d("UserQRScanner", "========== QR CODE SCANNED ==========")
                    Log.d("UserQRScanner", "Raw result: $result")

                    // Decode URL-encoded values
                    val decodedResult = try {
                        java.net.URLDecoder.decode(result, "UTF-8")
                    } catch (e: Exception) {
                        Log.e("UserQRScanner", "Decode error", e)
                        result
                    }
                    Log.d("UserQRScanner", "Decoded result: $decodedResult")

                    // Handle co-admin invite deep link first
                    if (decodedResult.contains("safeko://invite")) {
                        Log.d("UserQRScanner", "✓ Detected as safeko://invite deep link")
                        try {
                            val uri = android.net.Uri.parse(decodedResult)
                            val token = uri.getQueryParameter("token")
                            val lgcId = uri.getQueryParameter("lgcId")
                            Log.d("UserQRScanner", "Parsed params - token: $token, lgcId: $lgcId")
                            
                            if (token != null && lgcId != null && token.isNotBlank() && lgcId.isNotBlank()) {
                                Log.d("UserQRScanner", "✓ Valid token and lgcId - showing confirmation modal")
                                // Store the invite details and show confirmation modal
                                pendingInviteToken = token
                                pendingInviteLgcId = lgcId
                                showLguConfirmationModal = true
                                Log.d("UserQRScanner", "✓ Modal state set to: $showLguConfirmationModal")
                            } else {
                                Log.e("UserQRScanner", "✗ Invalid or empty token/lgcId")
                                Toast.makeText(context, "Invalid invite QR code format", Toast.LENGTH_SHORT).show()
                                showScanner = false
                            }
                        } catch (e: Exception) {
                            Log.e("UserQRScanner", "✗ Error parsing co-admin invite QR", e)
                            Toast.makeText(context, "Error processing invite QR code: ${e.message}", Toast.LENGTH_SHORT).show()
                            showScanner = false
                        }
                        Log.d("UserQRScanner", "========== END QR SCAN ==========")
                        return@QRCodeScanner
                    }

                    // Try to handle circle join QR
                    if (decodedResult.contains("/join/")) {
                        var circleId = decodedResult.substringAfterLast("/join/")
                        // Remove any trailing slashes or URL parameters
                        circleId = circleId.split("[/?#]".toRegex()).firstOrNull() ?: circleId
                        circleId = circleId.trim()
                        
                        Log.d("UserQRScanner", "Circle join detected. Circle ID extracted: '$circleId' (length: ${circleId.length})")
                        
                        if (circleId.isNotBlank()) {
                            val currentUserId = auth.currentUser?.uid
                            if (currentUserId != null) {
                                com.google.firebase.ktx.Firebase.firestore.collection("circles")
                                    .document(circleId)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        if (doc.exists()) {
                                            val members =
                                                doc.get("members") as? List<*> ?: emptyList<Any>()
                                            val limit = doc.getLong("memberLimit")?.toInt()
                                                ?: 5 // Legacy fallback to 5
                                            val type = doc.getString("type") ?: "Group"

                                            if (type == "Duo") {
                                                Toast.makeText(
                                                    context,
                                                    "This circle type is no longer supported.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else if (userPlan != "Premium" && userPlan != "OWL+") {
                                                Toast.makeText(
                                                    context,
                                                    "Upgrade to Premium to join family circles.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else if (members.size >= limit) {
                                                Toast.makeText(
                                                    context,
                                                    "This circle has reached its maximum member limit!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                com.google.firebase.ktx.Firebase.firestore.collection(
                                                    "circles"
                                                ).document(circleId)
                                                    .update(
                                                        "members",
                                                        com.google.firebase.firestore.FieldValue.arrayUnion(
                                                            currentUserId
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            context,
                                                            "Joined group!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        navController.navigate("chat/$circleId")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            context,
                                                            "Failed: ${e.message}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                            }
                                        } else {
                                            Log.e("UserQRScanner", "Circle not found in Firestore: $circleId")
                                            Toast.makeText(
                                                context,
                                                "Circle not found",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to join: ${it.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        } else {
                            Log.e("UserQRScanner", "Failed to extract circle ID from: $decodedResult")
                            Toast.makeText(context, "Invalid join link", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("UserQRScanner", "Scanned value doesn't contain /join/, attempting to parse as user/profile")
                        val uid = if (decodedResult.contains("/user/")) {
                            decodedResult.substringAfterLast("/user/")
                        } else if (!decodedResult.startsWith("http")) {
                            decodedResult
                        } else {
                            ""
                        }
                        Log.d("UserQRScanner", "Extracted UID: $uid")
                        if (uid.isNotBlank() && !uid.contains("/")) {
                            Log.d("UserQRScanner", "Navigation to public profile: $uid")
                            navController.navigate("public_profile/$uid")
                        } else {
                            Log.e("UserQRScanner", "Invalid QR Code format: $decodedResult")
                            Toast.makeText(context, "Invalid QR Code", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onClose = { showScanner = false }
            )
        }
    }

    // LGU Membership Confirmation Modal - MUST be AFTER scanner so it renders on top
    if (showLguConfirmationModal && pendingInviteToken != null && pendingInviteLgcId != null) {
        AlertDialog(
            onDismissRequest = { showLguConfirmationModal = false },
            title = {
                Text("Join LGU Membership")
            },
            text = {
                Text("Are you sure you want to continue as an LGU member? You will gain additional administrative privileges and responsibilities.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLguConfirmationModal = false
                        lguApprovalProcessingState = LguApprovalState.Processing
                        lguApprovalMessage = "Processing your LGU membership request..."
                        showLguApprovalWaitingModal = true
                        
                        Log.d("LguConfirmation", "User confirmed, creating pending request with token=$pendingInviteToken, lgcId=$pendingInviteLgcId")
                        
                        // Create pending request in a coroutine
                        scope.launch {
                            try {
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    lguApprovalProcessingState = LguApprovalState.Error("User not authenticated")
                                    return@launch
                                }
                                
                                val pendingRequest = mapOf(
                                    "token" to (pendingInviteToken ?: ""),
                                    "userId" to currentUser.uid,
                                    "userName" to (currentUser.displayName ?: "User"),
                                    "userEmail" to (currentUser.email ?: ""),
                                    "lgcId" to (pendingInviteLgcId ?: ""),
                                    "status" to "pending",
                                    "createdAt" to com.google.firebase.Timestamp.now(),
                                    "expiresAt" to com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                                )
                                
                                // Check for existing pending requests
                                val existingRequest = Firebase.firestore.collection("pending_admins")
                                    .whereEqualTo("userId", currentUser.uid)
                                    .whereEqualTo("lgcId", pendingInviteLgcId)
                                    .whereEqualTo("status", "pending")
                                    .get()
                                    .await()
                                
                                if (existingRequest.documents.isNotEmpty()) {
                                    lguApprovalProcessingState = LguApprovalState.Error("You already have a pending LGU membership request")
                                    return@launch
                                }
                                
                                // Create the pending request
                                val docRef = Firebase.firestore.collection("pending_admins")
                                    .add(pendingRequest)
                                    .await()
                                
                                pendingLguRequestId = docRef.id
                                lguApprovalProcessingState = LguApprovalState.Waiting
                                lguApprovalMessage = "Waiting for admin approval..."
                                
                                Log.d("LguConfirmation", "Pending request created with ID: ${docRef.id}")
                                
                                // Set up real-time listener for approval
                                Firebase.firestore.collection("pending_admins")
                                    .document(docRef.id)
                                    .addSnapshotListener { snapshot, error ->
                                        if (error != null) {
                                            Log.e("LguConfirmation", "Listener error", error)
                                            return@addSnapshotListener
                                        }
                                        
                                        if (snapshot != null && snapshot.exists()) {
                                            val status = snapshot.getString("status")
                                            Log.d("LguConfirmation", "Request status updated to: $status")
                                            
                                            when (status) {
                                                "approved" -> {
                                                    lguApprovalProcessingState = LguApprovalState.Success("You have been approved!")
                                                    scope.launch {
                                                        delay(2000)
                                                        showLguApprovalWaitingModal = false
                                                        pendingInviteToken = null
                                                        pendingInviteLgcId = null
                                                        lguApprovalProcessingState = LguApprovalState.Idle
                                                    }
                                                }
                                                "declined" -> {
                                                    lguApprovalProcessingState = LguApprovalState.Error("Your LGU membership request was declined")
                                                }
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                Log.e("LguConfirmation", "Error creating pending request", e)
                                lguApprovalProcessingState = LguApprovalState.Error("Error: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showLguConfirmationModal = false
                        pendingInviteToken = null
                        pendingInviteLgcId = null
                        Log.d("LguConfirmation", "User cancelled")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // LGU Approval Waiting Modal
    if (showLguApprovalWaitingModal) {
        // Auto-close modal when approval succeeds
        LaunchedEffect(lguApprovalProcessingState) {
            if (lguApprovalProcessingState is LguApprovalState.Success) {
                Log.d("LguConfirmation", "Approval successful, closing modal in 3 seconds")
                delay(3000)
                showLguApprovalWaitingModal = false
                showScanner = false  // Close QR scanner overlay
                lguApprovalProcessingState = LguApprovalState.Idle
                pendingInviteToken = null
                pendingInviteLgcId = null
            }
        }
        
        AlertDialog(
            onDismissRequest = { },
            title = {
                when (lguApprovalProcessingState) {
                    LguApprovalState.Processing -> Text("Processing...")
                    LguApprovalState.Waiting -> Text("Waiting for Approval")
                    is LguApprovalState.Success -> Text("Success!")
                    is LguApprovalState.Error -> Text("Error")
                    LguApprovalState.Idle -> Text("Processing...")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (lguApprovalProcessingState) {
                        LguApprovalState.Processing -> {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = lguApprovalMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        LguApprovalState.Waiting -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Waiting",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = lguApprovalMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "The LGU admin will review and approve your membership request shortly.",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is LguApprovalState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "You have been approved as an LGU member!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Returning to home...",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is LguApprovalState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (lguApprovalProcessingState as LguApprovalState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        LguApprovalState.Idle -> {}
                    }
                }
            },
            confirmButton = {
                when (lguApprovalProcessingState) {
                    LguApprovalState.Waiting -> {
                        Button(
                            onClick = {
                                Log.d("LguConfirmation", "User closed waiting modal manually")
                                showLguApprovalWaitingModal = false
                                showScanner = false  // Close QR scanner overlay
                                lguApprovalProcessingState = LguApprovalState.Idle
                                pendingInviteToken = null
                                pendingInviteLgcId = null
                            }
                        ) {
                            Text("Close")
                        }
                    }
                    is LguApprovalState.Error -> {
                        Button(
                            onClick = {
                                showLguApprovalWaitingModal = false
                                showScanner = false  // Close QR scanner overlay
                                lguApprovalProcessingState = LguApprovalState.Idle
                                pendingInviteToken = null
                                pendingInviteLgcId = null
                            }
                        ) {
                            Text("OK")
                        }
                    }
                    else -> {}
                }
            }
        )
    }
}