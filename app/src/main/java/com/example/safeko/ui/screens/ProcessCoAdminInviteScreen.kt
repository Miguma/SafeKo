package com.example.safeko.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.Date

@Composable
fun ProcessCoAdminInviteScreen(
    token: String,
    lgcId: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val currentUser = auth.currentUser
    
    var processingState by remember { mutableStateOf<ProcessingState>(ProcessingState.Processing) }
    var pendingRequestId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(token, lgcId) {
        Log.d("ProcessCoAdminInvite", "Screen launched with token=$token, lgcId=$lgcId")
        
        // Validate parameters
        if (token.isBlank() || lgcId.isBlank()) {
            Log.w("ProcessCoAdminInvite", "Invalid parameters - token: $token, lgcId: $lgcId")
            processingState = ProcessingState.Error("Invalid invite link - missing required parameters")
            return@LaunchedEffect
        }
        
        if (currentUser == null) {
            // Not logged in, show error
            Log.w("ProcessCoAdminInvite", "User not authenticated")
            processingState = ProcessingState.Error("Please log in to accept co-admin invitation")
            return@LaunchedEffect
        }
        
        Log.d("ProcessCoAdminInvite", "Current user: ${currentUser.uid}")
        
        try {
            // Create pending co-admin request in Firestore
            val pendingRequest = mapOf(
                "token" to token,
                "userId" to currentUser.uid,
                "userName" to (currentUser.displayName ?: "User"),
                "userEmail" to (currentUser.email ?: ""),
                "lgcId" to lgcId,
                "status" to "pending",
                "createdAt" to Date(),
                "expiresAt" to Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000) // 7 days
            )
            
            Log.d("ProcessCoAdminInvite", "Checking for existing pending requests...")
            // Check if this user already has a pending request
            val existingRequest = firestore.collection("pending_admins")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("lgcId", lgcId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            Log.d("ProcessCoAdminInvite", "Query result: ${existingRequest.documents.size} existing requests")
            
            if (existingRequest.documents.isNotEmpty()) {
                // Already has a pending request
                Log.w("ProcessCoAdminInvite", "User already has a pending request for this LGU")
                processingState = ProcessingState.Error("You already have a pending co-admin request for this LGU")
                return@LaunchedEffect
            }
            
            Log.d("ProcessCoAdminInvite", "Creating new pending admin request...")
            // Add new pending request
            val docRef = firestore.collection("pending_admins")
                .add(pendingRequest)
                .await()
            
            pendingRequestId = docRef.id
            Log.d("ProcessCoAdminInvite", "Pending request created with ID: ${docRef.id}")
            processingState = ProcessingState.Waiting("Waiting for admin approval...")
            
            // Set up real-time listener for approval
            firestore.collection("pending_admins")
                .document(docRef.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ProcessCoAdminInvite", "Error listening for updates", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status")
                        Log.d("ProcessCoAdminInvite", "Request status updated to: $status")
                        
                        when (status) {
                            "approved" -> {
                                Log.d("ProcessCoAdminInvite", "Request approved!")
                                processingState = ProcessingState.Success("You have been approved as a co-admin!")
                            }
                            "declined" -> {
                                Log.w("ProcessCoAdminInvite", "Request declined")
                                processingState = ProcessingState.Error("Your co-admin request was declined by the admin.")
                            }
                        }
                    }
                }
            
        } catch (e: Exception) {
            Log.e("ProcessCoAdminInvite", "Error during invite processing", e)
            processingState = ProcessingState.Error("An unexpected error occurred: ${e.message}")
        }
    }
    
    // Separate effect to handle navigation after success/error
    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Success) {
            delay(3000)
            onSuccess()
        }
    }
    
    // Show with a proper background and content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        when (processingState) {
            is ProcessingState.Processing -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing your co-admin invitation...",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            is ProcessingState.Waiting -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Waiting",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Waiting for Approval",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (processingState as ProcessingState.Waiting).message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The LGU admin will review and approve your co-admin request shortly.",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { onError() }) {
                        Text("Cancel")
                    }
                }
            }
            
            is ProcessingState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Success!",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = (processingState as ProcessingState.Success).message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Redirecting you to home...",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            is ProcessingState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (processingState as ProcessingState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onError() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}

sealed class ProcessingState {
    data object Processing : ProcessingState()
    data class Waiting(val message: String) : ProcessingState()
    data class Success(val message: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
