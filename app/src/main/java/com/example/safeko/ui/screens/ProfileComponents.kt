package com.example.safeko.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.BackHandler
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.safeko.ui.components.FaceVerificationModal
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentName: String,
    currentEmail: String,
    currentPhotoUrl: String?,
    initialPhoneNumber: String,
    currentLocation: String,
    isPhoneVerified: Boolean,
    userRole: String = "user", // NEW: Add userRole parameter
    isFaceVerified: Boolean = false, // NEW: Face verification status
    onChangePhoto: () -> Unit,
    onFaceVerificationStart: () -> Unit = {}, // Optional external callback
    onFaceVerified: (faceEmbedding: String, bitmap: Bitmap) -> Unit = { _, _ -> },
    onFaceVerificationError: (String) -> Unit = {},
    onSave: (String, String) -> Unit // (newName, newPhone)
) {
    if (showDialog) {
        var phoneNumber by remember { mutableStateOf(initialPhoneNumber) }
        var showFaceVerificationModal by remember { mutableStateOf(false) }
        
        // NEW: Password change states for LGU admins
        var showPasswordSection by remember { mutableStateOf(false) }
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isChangingPassword by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf("") }
        
        // Use a full-screen Box with high Z-index to overlay everything
        // This avoids Window inset issues common with Dialog/Popup
        BackHandler { onDismiss() }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(100f) // Ensure it's on top of everything
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks so they don't pass through */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)
                    ) {
                        // Back Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF5F5F5),
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.CenterStart)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF1A1C1E)
                                )
                            }
                        }

                        // Title
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1C1E),
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Close Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF5F5F5),
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.CenterEnd)
                                .clickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF1A1C1E)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.size(90.dp).clickable { onChangePhoto() }
                        ) {
                            if (!currentPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentPhotoUrl)
                                        .crossfade(true)
                                        .transformations(CircleCropTransformation())
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFE0E0E0), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0E0E0),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(45.dp)
                                )
                            }
                        }
                    }

                    // Edit Icon Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(28.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.LightGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap to change photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Full Name Field (Editable)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Full Name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = currentName,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF5F6368),
                                    disabledBorderColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFF5F5F5)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Name is linked to your Google account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Email Field (Read Only)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = currentEmail,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF5F6368),
                                    disabledBorderColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFF5F5F5)
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Email is linked to your Google account",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Phone Number Field (Editable)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Phone Number",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                readOnly = isPhoneVerified,
                                enabled = !isPhoneVerified,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("+63 912 345 6789", color = Color.LightGray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = Color(0xFF4285F4),
                                    disabledTextColor = Color.Black,
                                    disabledBorderColor = Color(0xFFE0E0E0),
                                    disabledContainerColor = Color(0xFFF5F5F5)
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (isPhoneVerified) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800), // Orange
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Semi Verified. Number cannot be changed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Philippine format: +63 XXX XXX XXXX",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Current Location (Read Only)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Current Location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = currentLocation,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF5F6368),
                                    disabledBorderColor = Color.Transparent,
                                    disabledContainerColor = Color(0xFFF5F5F5)
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-updated via real-time tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        // NEW: Face Verification Section
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Identity Verification",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Face Verification Button
                        Button(
                            onClick = {
                                if (!isFaceVerified) {
                                    onFaceVerificationStart()
                                    showFaceVerificationModal = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isFaceVerified,
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isFaceVerified) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32),
                                    disabledContainerColor = Color(0xFFE8F5E9),
                                    disabledContentColor = Color(0xFF2E7D32)
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF5F5F5),
                                    contentColor = Color(0xFF1565C0),
                                    disabledContainerColor = Color(0xFFF5F5F5),
                                    disabledContentColor = Color(0xFF1565C0)
                                )
                            },
                            border = if (isFaceVerified) {
                                BorderStroke(1.dp, Color(0xFF4CAF50))
                            } else {
                                BorderStroke(1.dp, Color(0xFFE0E0E0))
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isFaceVerified) "✅ Face Verified" else "Add Face Verification",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isFaceVerified) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFaceVerified) "Fully verified - Phone + Face" else "Phone + Face verification = Fully Verified",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFaceVerified) Color(0xFF2E7D32) else Color.Gray
                            )
                        }

                        if (isFaceVerified) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Face verification is permanent and cannot be changed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        // NEW: Change Password Section (LGU Admin Only)
                        if (userRole == "lgu_admin") {
                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { showPasswordSection = !showPasswordSection },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF5F5F5),
                                    contentColor = Color(0xFF1565C0)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Icon(
                                    imageVector = if (showPasswordSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 8.dp)
                                )
                                Text("Change Password", fontWeight = FontWeight.Bold)
                            }
                            
                            if (showPasswordSection) {
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Current Password
                                OutlinedTextField(
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    label = { Text("Current Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    ),
                                    enabled = !isChangingPassword
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // New Password
                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    ),
                                    enabled = !isChangingPassword
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Confirm Password
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    ),
                                    enabled = !isChangingPassword
                                )
                                
                                if (passwordError.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = passwordError,
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Update Password Button
                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        // Validate
                                        passwordError = when {
                                            currentPassword.isBlank() -> "Current password required"
                                            newPassword.isBlank() -> "New password required"
                                            confirmPassword.isBlank() -> "Confirm password required"
                                            newPassword != confirmPassword -> "Passwords don't match"
                                            newPassword.length < 6 -> "Password must be at least 6 characters"
                                            else -> ""
                                        }
                                        
                                        if (passwordError.isBlank()) {
                                            isChangingPassword = true
                                            val auth = Firebase.auth
                                            val user = auth.currentUser
                                            
                                            if (user != null && user.email != null) {
                                                // Reauthenticate first
                                                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                                                    user.email!!,
                                                    currentPassword
                                                )
                                                
                                                user.reauthenticate(credential)
                                                    .addOnSuccessListener {
                                                        user.updatePassword(newPassword)
                                                            .addOnSuccessListener {
                                                                passwordError = ""
                                                                currentPassword = ""
                                                                newPassword = ""
                                                                confirmPassword = ""
                                                                showPasswordSection = false
                                                                isChangingPassword = false
                                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                    Toast.makeText(context, "✅ Password updated successfully!", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                passwordError = "Error: ${e.message}"
                                                                isChangingPassword = false
                                                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                    Toast.makeText(context, "❌ ${e.message}", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        passwordError = "❌ Current password is incorrect"
                                                        isChangingPassword = false
                                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                            Toast.makeText(context, "❌ Current password is incorrect", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    enabled = !isChangingPassword
                                ) {
                                    if (isChangingPassword) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Update Password", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Save Button
                        Button(
                            onClick = { onSave(currentName, phoneNumber) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp), // Fully rounded
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (showFaceVerificationModal) {
                FaceVerificationModal(
                    onDismiss = { showFaceVerificationModal = false },
                    onFaceVerified = { embedding, bitmap ->
                        onFaceVerified(embedding, bitmap)
                        showFaceVerificationModal = false
                    },
                    onError = { error ->
                        onFaceVerificationError(error)
                        showFaceVerificationModal = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpInputDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    if (showDialog) {
        var otpCode by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Verify Phone Number",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the 6-digit OTP sent to your phone number.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { 
                            if (it.length <= 6) { otpCode = it.filter { char -> char.isDigit() } }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("000000", color = Color.LightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF4285F4)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onVerify(otpCode) },
                    enabled = otpCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}
