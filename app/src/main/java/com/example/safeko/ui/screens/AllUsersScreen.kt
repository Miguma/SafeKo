package com.example.safeko.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AllUsersScreen(
    onBack: () -> Unit
) {
    val firestore = Firebase.firestore
    val scope = rememberCoroutineScope()
    
    var users by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch all users on screen load
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                Log.d("AllUsersScreen", "Fetching all users...")
                val result = firestore.collection("users").get().await()
                
                val userList = mutableListOf<UserDataExtended>()
                
                for (document in result) {
                    val name = document.getString("fullName") ?: document.getString("name") ?: "Unknown"
                    val plan = document.getString("plan") ?: "Free"
                    val purchaseDate = document.getLong("purchaseDate") ?: 0L
                    val planExpiryDate = document.getLong("planExpiryDate") ?: 0L
                    
                    // Check if plan is expired
                    val actualPlan = if (planExpiryDate > 0 && System.currentTimeMillis() > planExpiryDate) {
                        "Free"
                    } else {
                        plan
                    }
                    
                    val user = UserDataExtended(
                        id = document.id,
                        fullName = name,
                        email = document.getString("email") ?: "",
                        role = document.getString("role") ?: "user",
                        plan = actualPlan,
                        purchaseDate = purchaseDate,
                        planExpiryDate = planExpiryDate,
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                    userList.add(user)
                }
                
                users = userList
                filteredUsers = userList
                isLoading = false
                Log.d("AllUsersScreen", "Loaded ${userList.size} users")
            } catch (e: Exception) {
                Log.e("AllUsersScreen", "Error: ${e.message}", e)
                isLoading = false
            }
        }
    }
    
    // Search filter
    LaunchedEffect(searchQuery) {
        filteredUsers = if (searchQuery.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.fullName.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.plan.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", 
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "All Users",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${filteredUsers.size} users total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar
                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search users...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Users List
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No users found", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        UserCardNew(user = user)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
