package com.example.safeko.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalDensity
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
    var isAdminLoginMode by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
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
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Section: Image Frames Collage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.47f)
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
                .weight(0.53f)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            AnimatedVisibility(
                visible = !isKeyboardVisible,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(140))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isAdminLoginMode) {
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
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Admin email", color = Color(0xFFBDBDBD)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Password", color = Color(0xFFBDBDBD)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please enter admin email and password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid == null) {
                                        isLoading = false
                                        Toast.makeText(context, "Admin login failed", Toast.LENGTH_SHORT).show()
                                        return@addOnCompleteListener
                                    }

                                    firestore.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            val role = doc.getString("role") ?: "user"
                                            isLoading = false
                                            if (role == "lgu_admin" || role == "superadmin") {
                                                onLoginSuccess(email)
                                            } else {
                                                auth.signOut()
                                                Toast.makeText(context, "This account is not an admin", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            auth.signOut()
                                            Toast.makeText(context, "Failed to verify admin role: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Invalid admin credentials", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Text(
                        text = if (isLoading) "Signing in..." else "Login as Admin",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAdminLoginMode) "Back to user login? " else "Need admin access? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575)
                )
                Text(
                    text = if (isAdminLoginMode) "Continue with Google" else "Login as Admin",
                    modifier = Modifier.clickable {
                        val nextAdminMode = !isAdminLoginMode
                        isAdminLoginMode = nextAdminMode
                        email = ""
                        password = ""
                        // In admin mode, show typed password by default for easier entry.
                        isPasswordVisible = nextAdminMode
                        isLoading = false
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF03A9F4),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
