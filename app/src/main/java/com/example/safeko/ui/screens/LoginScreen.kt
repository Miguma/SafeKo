package com.example.safeko.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.safeko.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import android.util.Log

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onSupportClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPasswordField by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Check if this is a superadmin login attempt
    val isSuperAdminEmail = email == "superadmin@test.com"
    
    // Configure Google Sign In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val userData = hashMapOf(
                                            "uid" to user.uid,
                                            "fullName" to (user.displayName ?: ""),
                                            "email" to (user.email ?: ""),
                                            "role" to "user",
                                            "profilePhoto" to (user.photoUrl?.toString())
                                        )
                                        Firebase.firestore.collection("users").document(user.uid)
                                            .set(userData, SetOptions.merge())
                                            .addOnSuccessListener {
                                                onLoginSuccess(user.email ?: "")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Auth", "Error saving user data", e)
                                                onLoginSuccess(user.email ?: "")
                                            }
                                    } else {
                                        onLoginSuccess(auth.currentUser?.email ?: "")
                                    }
                                } else {
                                    Log.e("Auth", "Authentication Failed", task.exception)
                                    Toast.makeText(context, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
            }
        } catch (e: ApiException) {
            Log.e("Auth", "Google Sign In Failed: ${e.statusCode}", e)
            Toast.makeText(context, "Google Sign In Failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "floating_transition")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Section: Image Frames Collage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f) // Increased weight to push things down a bit
                .padding(top = 24.dp, start = 8.dp, end = 8.dp, bottom = 8.dp) // Added top padding
        ) {
            // This is a placeholder for the image frames collage
            // You can replace these Boxes with your actual Image composables later
            
            // Example Layout: A large central frame and surrounding smaller frames
            // Adjust this layout to match your specific design requirements
            
            // Top Left Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .size(100.dp)
                    .offset(x = (-15).dp, y = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                 Image(
                     painter = painterResource(id = R.drawable.frame1),
                     contentDescription = "Frame 1 Image",
                     modifier = Modifier
                         .fillMaxSize()
                         .scale(1.1f),
                     contentScale = ContentScale.Crop
                 )
            }

            // Top Right Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 15.dp, y = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                 Image(
                     painter = painterResource(id = R.drawable.frame2),
                     contentDescription = "Frame 2 Image",
                     modifier = Modifier
                         .fillMaxSize()
                         .scale(1.1f), // Slight scale to ensure full coverage
                     contentScale = ContentScale.Crop
                 )
            }
            
            // Bottom Left Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .size(100.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-10).dp, y = (-30).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                 Image(
                     painter = painterResource(id = R.drawable.frame3),
                     contentDescription = "Frame 3 Image",
                     modifier = Modifier
                         .fillMaxSize()
                         .scale(1.1f),
                     contentScale = ContentScale.Crop
                 )
            }

            // Bottom Right Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = (-15).dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            ) {
                 Image(
                     painter = painterResource(id = R.drawable.frame4),
                     contentDescription = "Frame 4 Image",
                     modifier = Modifier
                         .fillMaxSize()
                         .scale(1.1f),
                     contentScale = ContentScale.Crop
                 )
            }
            
            // Center Main Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .fillMaxWidth(0.30f)
                    .aspectRatio(0.7f)
                    .align(Alignment.Center)
            ) {
                 // Image
                 Image(
                     painter = painterResource(id = R.drawable.mainframe),
                     contentDescription = "Main Frame Image",
                     modifier = Modifier
                         .fillMaxSize()
                         .clip(RoundedCornerShape(24.dp))
                         .background(Color.Gray),
                     contentScale = ContentScale.Crop
                 )
            }
            
            // Center Bottom Icon Frame (Owl/Logo placeholder)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 40.dp) // Push logo down further to overlap properly
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                 // Logo
                 Image(
                     painter = painterResource(id = R.drawable.safekolog),
                     contentDescription = "App Logo",
                     modifier = Modifier
                         .fillMaxSize()
                         .padding(4.dp)
                         .clip(CircleShape)
                 )
            }
        }

        // Bottom Section: Login Options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.60f) 
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "You're never off the map",
                style = MaterialTheme.typography.headlineMedium, // Slightly larger
                fontWeight = FontWeight.ExtraBold, // Extra bold for impact
                color = Color.Black
            )
            Text(
                text = "Safety, right where you are",
                style = MaterialTheme.typography.bodyMedium, // Slightly larger
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email", color = Color(0xFFBDBDBD)) },
                shape = RoundedCornerShape(12.dp), // More rounded
                singleLine = true,
                enabled = !showPasswordField,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2962FF),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show password field for superadmin or registered users
            if (isSuperAdminEmail) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter password", color = Color(0xFFBDBDBD)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            } else if (showPasswordField) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter password", color = Color(0xFFBDBDBD)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (isSuperAdminEmail) {
                        // Superadmin password login
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // Check hardcoded superadmin credentials
                        if (password == "admin123") {
                            isLoading = true
                            Log.d("LoginScreen", "Attempting superadmin login: $email")
                            // Authenticate with Firebase Auth
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("LoginScreen", "Superadmin auth successful, UID: ${auth.currentUser?.uid}")
                                        val uid = auth.currentUser?.uid ?: "superadmin"
                                        
                                        // Create/update Firestore record for superadmin
                                        val superadminData = hashMapOf(
                                            "uid" to uid,
                                            "fullName" to "Super Administrator",
                                            "email" to email,
                                            "role" to "superadmin",
                                            "name" to "Super Administrator",
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        Log.d("LoginScreen", "Saving superadmin to Firestore: $superadminData")
                                        
                                        firestore.collection("users")
                                            .document(uid)
                                            .set(superadminData, SetOptions.merge())
                                            .addOnSuccessListener {
                                                Log.d("LoginScreen", "✓ Superadmin data saved successfully")
                                                isLoading = false
                                                Toast.makeText(context, "Superadmin logged in successfully", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess(email)
                                            }
                                            .addOnFailureListener { error ->
                                                Log.e("LoginScreen", "✗ Failed to save superadmin data: ${error.message}", error)
                                                isLoading = false
                                                Toast.makeText(context, "Error saving superadmin data: ${error.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Log.e("LoginScreen", "✗ Auth failed: ${task.exception?.message}")
                                        isLoading = false
                                        Toast.makeText(context, "Invalid superadmin credentials: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Invalid password", Toast.LENGTH_SHORT).show()
                        }
                    } else if (!showPasswordField) {
                        // Regular user: Check if email exists in database
                        if (email.isBlank()) {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        // Check if email exists in users collection
                        firestore.collection("users")
                            .limit(1) // Just need to check existence
                            .get()
                            .addOnSuccessListener { _ ->
                                // We have read access to the collection, now check the email
                                firestore.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener { documents ->
                                        isLoading = false
                                        if (documents.isEmpty) {
                                            // Email not found
                                            Toast.makeText(context, "Email not found", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Email found, show password field
                                            showPasswordField = true
                                            password = ""
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                // If we get permission denied here, it's likely a rules issue with collection-level queries
                                // Fallback: Allow login attempt anyway, or handle specifically
                                Log.w("LoginScreen", "Initial collection check failed: ${e.message}")
                                
                                // Alternative: Since we can't search THE WHOLE collection without a filter sometimes,
                                // we just let the user try to log in with password if the first email check fails 
                                // because of a simple 'PERMISSION_DENIED' on a filtered query.
                                showPasswordField = true 
                                isLoading = false
                            }
                    } else {
                        // Regular user: Verify password
                        if (password.isBlank()) {
                            Toast.makeText(context, "Please enter password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess(email)
                                } else {
                                    Toast.makeText(context, "Invalid password", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp), // Slightly taller
                shape = RoundedCornerShape(27.dp), // Pill shaped
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
            ) {
                Text(
                    text = if (isSuperAdminEmail) {
                        if (isLoading) "Signing in..." else "Sign In"
                    } else if (!showPasswordField) {
                        if (isLoading) "Checking email..." else "Continue"
                    } else {
                        if (isLoading) "Signing in..." else "Sign In"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back button when showing Password or for superadmin
            if (showPasswordField || isSuperAdminEmail) {
                TextButton(onClick = {
                    showPasswordField = false
                    password = ""
                    email = ""
                }, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(text = "← Back to Email", color = Color(0xFF2962FF), fontWeight = FontWeight.Bold)
                }
            }

            // Show divider and Google only for regular users (not when showing password)
            if (!isSuperAdminEmail && !showPasswordField) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Text(text = "OR", color = Color(0xFFBDBDBD), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE), thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Button
                OutlinedButton(
                    onClick = { 
                        googleSignInClient.signOut().addOnCompleteListener {
                            launcher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.googleicontrans),
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google", 
                            color = Color.Black,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Footer Support Text (only for regular users)
            if (!isSuperAdminEmail) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Can't sign in? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "Customer Support",
                        modifier = Modifier.clickable { onSupportClick() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF03A9F4),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
