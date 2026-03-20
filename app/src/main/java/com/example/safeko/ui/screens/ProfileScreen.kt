package com.example.safeko.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.safeko.utils.QRCodeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.example.safeko.utils.PhoneAuthManager
import android.app.Activity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onScanClick: () -> Unit,
    onPremium: () -> Unit,
    onVerifyFace: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("safeko_prefs", Context.MODE_PRIVATE) }
    
    // State
    var currentPhotoUrl by remember { mutableStateOf(auth.currentUser?.photoUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showShareLocation by remember { mutableStateOf(false) }
    
    // User Data from SharedPreferences or Auth
    val currentUserId = auth.currentUser?.uid ?: ""
    var userPhoneNumber by remember { 
        mutableStateOf(sharedPreferences.getString("user_phone_number_$currentUserId", "") ?: "") 
    }
    val isFaceVerified = sharedPreferences.getBoolean("is_face_verified_$currentUserId", false)
    var isPhoneVerified by remember { mutableStateOf(false) }

    var showOtpDialog by remember { mutableStateOf(false) }
    var otpCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val activity = context as? Activity
    val phoneAuthManager = remember(auth, activity) { 
        activity?.let { PhoneAuthManager(auth, it) } 
    }
    
    // Share Location Preference
    var sharingOption by remember { 
        mutableStateOf(sharedPreferences.getString("sharing_option", "Visible to others") ?: "Visible to others") 
    }

    // Plan State
    var userPlan by remember { mutableStateOf("Loading...") }
    var userRole by remember { mutableStateOf("user") }
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
             try {
                val snapshot = Firebase.firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    userPlan = snapshot.getString("plan") ?: "Free"
                    userRole = snapshot.getString("role") ?: "user"
                    val fetchedPhone = snapshot.getString("phoneNumber")
                    if (!fetchedPhone.isNullOrBlank()) {
                        userPhoneNumber = fetchedPhone
                        sharedPreferences.edit().putString("user_phone_number_$currentUserId", userPhoneNumber).apply()
                    }
                    isPhoneVerified = snapshot.getBoolean("phoneVerified") ?: false
                } else {
                     userPlan = "Free"
                }
            } catch (e: Exception) {
                userPlan = "Error"
            }
        }
    }

    // QR Code Dialog State
    var showQrCodeDialog by remember { mutableStateOf(false) }
    val profileLink = "https://safeko-3ca46.web.app/user/${auth.currentUser?.uid}"
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Debounce back click to prevent double navigation/app exit
    var lastBackClickTime by remember { mutableStateOf(0L) }
    val onBackDebounced = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackClickTime > 500) { // 500ms debounce
            lastBackClickTime = currentTime
            onBack()
        }
    }

    // Image Picker Launcher - MOVED HERE to be accessible by EditProfileDialog
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isUploading = true
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
                scope.launch {
                    try {
                        storageRef.putFile(uri).await()
                        val downloadUrl = storageRef.downloadUrl.await()
                        
                        // Update Firebase Auth Profile
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUrl)
                            .build()
                        
                        auth.currentUser?.updateProfile(profileUpdates)?.await()
                        
                        // Update Firestore users collection
                        Firebase.firestore.collection("users").document(userId)
                            .update("profilePhoto", downloadUrl.toString())
                            .await()
                        
                        // Update State
                        currentPhotoUrl = downloadUrl
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(profileLink) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            qrBitmap = QRCodeUtils.generateQRCode(profileLink)
        }
    }

    // Handle Back Press for overlays
    BackHandler(enabled = showQrCodeDialog || showPrivacyPolicy || showEditProfile || showShareLocation) {
        when {
            showQrCodeDialog -> showQrCodeDialog = false
            showPrivacyPolicy -> showPrivacyPolicy = false
            showEditProfile -> showEditProfile = false
            showShareLocation -> showShareLocation = false
        }
    }
    
    if (showQrCodeDialog) {
        Dialog(onDismissRequest = { showQrCodeDialog = false }) {
             Surface(
                 shape = RoundedCornerShape(16.dp),
                 color = Color.White,
                 modifier = Modifier.padding(16.dp)
             ) {
                 Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.padding(24.dp)
                 ) {
                     Text("Your Profile QR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     if (qrBitmap != null) {
                         Image(
                             bitmap = qrBitmap!!.asImageBitmap(),
                             contentDescription = "Profile QR Code",
                             modifier = Modifier
                                 .size(200.dp)
                                 .clip(RoundedCornerShape(8.dp))
                         )
                     }
                     
                     Spacer(modifier = Modifier.height(24.dp))
                     
                     Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Profile Link", profileLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Link")
                        }
                        TextButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out my profile on SafeKo: $profileLink")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Profile Link"))
                        }) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                     
                     Spacer(modifier = Modifier.height(16.dp))
                     TextButton(onClick = { showQrCodeDialog = false }) {
                         Text("Close")
                     }
                 }
             }
        }
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }

    if (showEditProfile) {
        EditProfileDialog(
            showDialog = showEditProfile,
            onDismiss = { showEditProfile = false },
            currentName = auth.currentUser?.displayName ?: "",
            currentEmail = auth.currentUser?.email ?: "",
            currentPhotoUrl = currentPhotoUrl?.toString(),
            initialPhoneNumber = userPhoneNumber,
            currentLocation = "Location not available", // Simplified for now
            isPhoneVerified = isPhoneVerified,
            onChangePhoto = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSave = { newName, rawPhone ->
                val userId = auth.currentUser?.uid
                if (userId != null && newName.isNotBlank() && newName != auth.currentUser?.displayName) {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                    auth.currentUser?.updateProfile(profileUpdates)
                    com.google.firebase.ktx.Firebase.firestore.collection("users").document(userId)
                        .update("name", newName)
                }

                // Format phone number to E.164 format (+63...)
                var newPhone = rawPhone.replace(Regex("[^\\d+]"), "")
                if (newPhone.startsWith("0")) {
                    newPhone = "+63" + newPhone.substring(1)
                } else if (!newPhone.startsWith("+")) {
                    newPhone = "+63$newPhone"
                }

                if (newPhone != userPhoneNumber && newPhone.isNotBlank()) {
                    if (phoneAuthManager != null) {
                        Toast.makeText(context, "Sending OTP...", Toast.LENGTH_SHORT).show()
                        phoneAuthManager.initiateOTP(newPhone, object : PhoneAuthManager.PhoneAuthCallback {
                            override fun onCodeSent() {
                                showOtpDialog = true
                                otpCallback = { code ->
                                    phoneAuthManager.verifyOTP(code, this)
                                    showOtpDialog = false
                                }
                            }
                            override fun onVerificationComplete() {
                                // Often handled automatically by Play Services, 
                                // avoiding manual entry.
                            }
                            override fun onVerificationFailed(exception: Exception) {
                                Toast.makeText(context, "Verification failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                showOtpDialog = false
                            }
                            override fun onOtpInvalid() {
                                Toast.makeText(context, "Invalid OTP code", Toast.LENGTH_SHORT).show()
                            }
                            override fun onSuccess() {
                                userPhoneNumber = newPhone
                                isPhoneVerified = true
                                sharedPreferences.edit().putString("user_phone_number_$currentUserId", newPhone).apply()
                                showEditProfile = false
                                Toast.makeText(context, "Phone number verified!", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(context, "Error initializing phone auth", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    userPhoneNumber = newPhone
                    sharedPreferences.edit().putString("user_phone_number_$currentUserId", newPhone).apply()
                    showEditProfile = false
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    if (showOtpDialog && otpCallback != null) {
        OtpInputDialog(
            showDialog = showOtpDialog,
            onDismiss = { showOtpDialog = false },
            onVerify = { code -> otpCallback?.invoke(code) }
        )
    }
    
    if (showShareLocation) {
        AlertDialog(
            onDismissRequest = { showShareLocation = false },
            title = { Text("Share Location") },
            text = {
                Column {
                    listOf("Visible to others", "Family", "No one").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sharingOption = option
                                    sharedPreferences.edit().putString("sharing_option", option).apply()
                                    showShareLocation = false
                                    Toast.makeText(context, "Location sharing: $option", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (sharingOption == option),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareLocation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackDebounced) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showQrCodeDialog = true }) {
                        Icon(Icons.Filled.QrCode, contentDescription = "Show Profile QR")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.displayCutout).padding(top = 12.dp)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape)
                ) {
                    if (currentPhotoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentPhotoUrl)
                                .crossfade(true)
                                .transformations(CircleCropTransformation())
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (auth.currentUser?.displayName?.take(1) ?: "U").uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = auth.currentUser?.displayName ?: "User Name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Verified Badge
                if (isFaceVerified) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (isPhoneVerified && !isFaceVerified) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFF3E0), // Light Orange
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFFFF9800), // Orange
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Semi Verified",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFE65100), // Dark Orange
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Plan Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (userPlan == "OWL+") Color(0xFFFFD700) else Color(0xFFEEEEEE),
                    modifier = Modifier
                        .clickable { onPremium() }
                        .padding(top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (userPlan == "OWL+") {
                            Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (userPlan == "Loading...") "Checking Plan..." else "Plan: $userPlan",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (userPlan == "OWL+") Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // 2. Banner (Get Verified)
            if (!isFaceVerified) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { 
                            if (!isPhoneVerified) {
                                Toast.makeText(context, "Please verify your phone number in Edit Profile first", Toast.LENGTH_LONG).show()
                                showEditProfile = true
                            } else {
                                onVerifyFace() 
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFEBF0) // Light Pink
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Get Verified",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock full features and safety tools.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Get started",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF00C853),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }



            // 4. Menu Items
            Text(
                text = "PROFILE MENU",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            ProfileMenuItem("Edit Profile", Icons.Outlined.Person) {
                showEditProfile = true
            }
            
            if (userRole == "user") {
                ProfileMenuItem("Premium", Icons.Rounded.Star) { 
                    onPremium() 
                }
            }
            ProfileMenuItem("What's new", Icons.Rounded.NewReleases) { 
                Toast.makeText(context, "What's new feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            ProfileMenuItem("Alert stats", Icons.Rounded.Insights) { 
                Toast.makeText(context, "Alert stats feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            ProfileMenuItem("History", Icons.Rounded.History) { 
                Toast.makeText(context, "History feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            ProfileMenuItem("Incoming Updates", Icons.Rounded.Update) { 
                Toast.makeText(context, "Incoming Updates feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            
            ProfileMenuItem("Emergency Contacts", Icons.Outlined.Phone) { 
                Toast.makeText(context, "Emergency Contacts feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            ProfileMenuItem("Share Location", Icons.Outlined.LocationOn) { showShareLocation = true }
            
            ProfileMenuItem("Settings", Icons.Rounded.Settings) { onSettings() }
            ProfileMenuItem("Customer service", Icons.Rounded.SupportAgent) { 
                Toast.makeText(context, "Customer Service feature coming soon", Toast.LENGTH_SHORT).show() 
            }
            ProfileMenuItem("Privacy Policy", Icons.Outlined.PrivacyTip) { showPrivacyPolicy = true }
            ProfileMenuItem("Logout", Icons.AutoMirrored.Outlined.Logout) { onLogout() }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Note: Logout button removed from bottom as it's now in the menu list for "drawer text" consistency,
            // or we can keep it as a prominent action if preferred. 
            // The user asked to "replace the text... with the same as the drawer text".
            // The drawer had Logout. I added it to the list above.
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileMenuItem(
    label: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color(0xFF424242))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = Color(0xFF212121)
        )
        if (badge != null) {
            Surface(
                color = Color(0xFFFF4081),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Last Updated: March 5, 2026",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                PrivacySection("Introduction", "Welcome to SafeKo. We respect your privacy and are committed to protecting your personal data. This privacy policy will inform you as to how we look after your personal data when you visit our application and tell you about your privacy rights and how the law protects you.")
                
                PrivacySection("Information We Collect", "SafeKo collects the following information to provide and improve our services:")
                BulletPoint("Google Account Email", "We collect your email address when you sign in using Google Sign-In for authentication purposes.")
                BulletPoint("Phone Number", "We collect your phone number to verify your identity via SMS OTP (One-Time Password).")
                BulletPoint("Camera Access (Optional)", "We may request access to your device's camera for facial verification features. This is optional and requires your explicit permission.")
                
                PrivacySection("How We Use the Information", "We use the collected data strictly for the following purposes:")
                BulletPoint(null, "To authenticate your identity and secure your account.")
                BulletPoint(null, "To provide the core functionality of the SafeKo application.")
                BulletPoint(null, "To enhance the security of our users.")
                
                PrivacySection("Data Security", "We implement appropriate security measures to prevent your personal data from being accidentally lost, used, or accessed in an unauthorized way.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun BulletPoint(title: String?, content: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "• ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Column {
            if (title != null) {
                Text(text = "$title: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Text(text = content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
