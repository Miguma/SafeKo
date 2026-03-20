package com.example.safeko.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserData(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "user",
    val createdAt: Long = 0L
)

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    
    var totalUsers by remember { mutableStateOf(0) }
    var totalAdmins by remember { mutableStateOf(0) }
    var totalAlerts by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var users by remember { mutableStateOf<List<UserData>>(emptyList()) }

    // Fetch stats and users when screen loads
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = auth.currentUser
                Log.d("AdminDashboard", "=== DASHBOARD LOAD ===")
                Log.d("AdminDashboard", "Current User: ${currentUser?.email} (UID: ${currentUser?.uid})")
                
                if (currentUser == null) {
                    Log.e("AdminDashboard", "ERROR: Auth.currentUser is NULL!")
                    Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }
                
                Log.d("AdminDashboard", "Starting data fetch - Auth UID: ${auth.currentUser?.uid}, Email: ${auth.currentUser?.email}")
                
                // Fetch all users with details
                try {
                    Log.d("AdminDashboard", "Executing Firestore query for users collection...")
                    val result = firestore.collection("users").get().await()
                    Log.d("AdminDashboard", "✓ Firestore query successful, got ${result.size()} documents")
                    val userList = mutableListOf<UserData>()
                    for (document in result) {
                        Log.d("AdminDashboard", "Document ID: ${document.id}, Data: ${document.data}")
                        // Handle both "fullName" and "name" field names for compatibility
                        val name = document.getString("fullName") ?: document.getString("name") ?: "Unknown"
                        val user = UserData(
                            id = document.id,
                            fullName = name,
                            email = document.getString("email") ?: "",
                            role = document.getString("role") ?: "user",
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                        userList.add(user)
                        Log.d("AdminDashboard", "User loaded: ${user.fullName}, ${user.email}, ${user.role}")
                    }
                    users = userList
                    totalUsers = userList.size
                    Log.d("AdminDashboard", "✓ SUCCESS: Total users loaded: ${userList.size}")
                    Toast.makeText(context, "Loaded ${userList.size} users", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("AdminDashboard", "✗ FAILED: Error fetching users: ${e.message}", e)
                    Log.e("AdminDashboard", "Error type: ${e.javaClass.simpleName}")
                    Toast.makeText(context, "Error fetching users: ${e.message}", Toast.LENGTH_LONG).show()
                    totalUsers = 0
                    users = emptyList()
                }

                // Fetch total admins
                try {
                    Log.d("AdminDashboard", "Fetching admins count...")
                    val adminResult = firestore.collection("users")
                        .whereEqualTo("role", "admin")
                        .get().await()
                    totalAdmins = adminResult.size()
                    Log.d("AdminDashboard", "✓ Total admins: ${adminResult.size()}")
                } catch (e: Exception) {
                    Log.e("AdminDashboard", "✗ Error fetching admins: ${e.message}")
                    totalAdmins = 0
                }

                // Fetch total alerts
                try {
                    Log.d("AdminDashboard", "Fetching alerts count...")
                    val alertResult = firestore.collection("alerts").get().await()
                    totalAlerts = alertResult.size()
                    Log.d("AdminDashboard", "✓ Total alerts: ${alertResult.size()}")
                } catch (e: Exception) {
                    Log.e("AdminDashboard", "✗ Error fetching alerts: ${e.message}")
                    totalAlerts = 0
                }
                
                isLoading = false
                Log.d("AdminDashboard", "=== DASHBOARD LOAD COMPLETE ===")
            } catch (e: Exception) {
                Log.e("AdminDashboard", "✗ Exception: ${e.message}", e)
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2962FF))
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "Manage users and monitor system",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        auth.signOut()
                        onLogout()
                    },
                    modifier = Modifier
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2962FF))
            }
        } else {
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats Cards Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Users",
                            value = totalUsers.toString(),
                            icon = Icons.Filled.People,
                            backgroundColor = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Admins",
                            value = totalAdmins.toString(),
                            icon = Icons.Filled.PersonAdd,
                            backgroundColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats Cards Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Active Alerts",
                            value = totalAlerts.toString(),
                            icon = Icons.Filled.Warning,
                            backgroundColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "System",
                            value = "Healthy",
                            icon = Icons.Filled.People,
                            backgroundColor = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Users Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "User Accounts (${users.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                // Users List
                if (users.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                } else {
                    items(users) { user ->
                        UserListItem(user = user)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(backgroundColor, shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                fontSize = 32.sp
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF989898),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UserListItem(
    user: UserData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar and user info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(0xFF2962FF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // User details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),
                        maxLines = 1
                    )
                }
            }

            // Role badge
            Box(
                modifier = Modifier
                    .background(
                        color = when (user.role) {
                            "admin" -> Color(0xFFE3F2FD)
                            "superadmin" -> Color(0xFFFFF9C4)
                            else -> Color(0xFFF5F5F5)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = user.role.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (user.role) {
                        "admin" -> Color(0xFF1976D2)
                        "superadmin" -> Color(0xFFFBC02D)
                        else -> Color(0xFF424242)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // More options
            IconButton(
                onClick = { /* TODO: Show options */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
