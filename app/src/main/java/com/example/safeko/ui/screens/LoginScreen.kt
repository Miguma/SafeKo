package com.example.safeko.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.safeko.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onFacebookClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    
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
                            onLoginSuccess()
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
                .weight(0.55f) // Increase weight to push everything down
                .padding(16.dp)
        ) {
            // This is a placeholder for the image frames collage
            // You can replace these Boxes with your actual Image composables later
            
            // Example Layout: A large central frame and surrounding smaller frames
            // Adjust this layout to match your specific design requirements
            
            // Top Left Frame
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = floatingOffset }
                    .size(140.dp)
                    .offset(x = (-20).dp, y = 20.dp)
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
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 60.dp)
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
                    .size(140.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-10).dp, y = (-50).dp)
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
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = (-30).dp)
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
                    .fillMaxWidth(0.4f)
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
                    .size(120.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 60.dp) // Push logo down further to overlap properly
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
                .weight(0.45f) // Decrease weight to balance
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(40.dp)) // Add positive spacer to push text down
            
            Text(
                text = "You’re never off the map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = "Safety, right where you are",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Google Button
            OutlinedButton(
                onClick = { launcher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Google Icon
                    Image(
                        painter = painterResource(id = R.drawable.googleicontrans),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Continue with Google", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Facebook Button
            OutlinedButton(
                onClick = onFacebookClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Facebook Icon
                    Image(
                        painter = painterResource(id = R.drawable.fbicon),
                        contentDescription = "Facebook Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Continue with Facebook", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Support Text
            TextButton(onClick = onSupportClick) {
                Text(
                    text = "Can’t sign in? Please Contact ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Customer Support",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4FC3F7),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
