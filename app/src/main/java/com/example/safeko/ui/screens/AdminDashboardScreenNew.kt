package com.example.safeko.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class UserDataExtended(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "user",
    val department: String = "",
    val plan: String = "Free",
    val purchaseDate: Long = 0L,
    val planExpiryDate: Long = 0L,
    val createdAt: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreenNew(
    onLogout: () -> Unit,
    onSeeMoreUsers: () -> Unit = {},
    onViewDailyAnalytics: () -> Unit = {},
    onViewUserGrowth: () -> Unit = {},
    onManageAdmins: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    
    // Revenue data
    var dailyRevenue by remember { mutableStateOf(0.0) }
    var totalPremiumRevenue by remember { mutableStateOf(0.0) }
    var premiumSignupsToday by remember { mutableStateOf(0) }
    var owlSignupsToday by remember { mutableStateOf(0) }
    var totalUsers by remember { mutableStateOf(0) }
    var totalAdmins by remember { mutableStateOf(0) }
    
    // Users data
    var users by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Admin Overview data
    var totalAlerts by remember { mutableStateOf(0) }
    var activeAlerts by remember { mutableStateOf(0) }
    var showGlobalOverview by remember { mutableStateOf(false) }
    
    // Loading state
    var isLoading by remember { mutableStateOf(true) }

    // Fetch data when screen loads
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }
                
                Log.d("AdminDashboard", "Fetching all users...")
                val result = firestore.collection("users").get().await()
                
                val userList = mutableListOf<UserDataExtended>()
                var dailyRev = 0.0
                var totalRev = 0.0
                var premiumToday = 0
                var owlToday = 0
                var adminCount = 0
                
                val todayStart = getTodayStartTime()
                val todayEnd = getTodayEndTime()
                
                for (document in result) {
                    val name = document.getString("fullName") ?: document.getString("name") ?: "Unknown"
                    val plan = document.getString("plan") ?: "Free"
                    val role = document.getString("role") ?: "user"
                    val purchaseDate = document.getLong("purchaseDate") ?: 0L
                    val planExpiryDate = document.getLong("planExpiryDate") ?: 0L
                    
                    // Count admins
                    if (role == "admin" || role == "superadmin") {
                        adminCount++
                    }
                    
                    // Check if plan is expired
                    val actualPlan = if (planExpiryDate > 0 && System.currentTimeMillis() > planExpiryDate) {
                        "Free" // Auto-downgrade expired plans
                    } else {
                        plan
                    }
                    
                    val user = UserDataExtended(
                        id = document.id,
                        fullName = name,
                        email = document.getString("email") ?: "",
                        role = role,
                        department = document.getString("department") ?: "",
                        plan = actualPlan,
                        purchaseDate = purchaseDate,
                        planExpiryDate = planExpiryDate,
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                    userList.add(user)
                    
                    // Calculate revenue only if plan is active (not Free)
                    if (actualPlan != "Free" && purchaseDate > 0) {
                        val amount = if (actualPlan == "Premium") 99.0 else 299.0
                        totalRev += amount
                        
                        // Check if purchased today
                        if (purchaseDate >= todayStart && purchaseDate <= todayEnd) {
                            dailyRev += amount
                            if (actualPlan == "Premium") premiumToday++ 
                            else if (actualPlan == "OWL+") owlToday++
                        }
                    }
                }
                
                // Fetch alerts data
                try {
                    val alertsResult = firestore.collection("alerts").get().await()
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
                    Log.d("AdminDashboard", "✓ Total alerts: $alertCount | Active: $activeCount")
                } catch (e: Exception) {
                    Log.e("AdminDashboard", "Error fetching alerts: ${e.message}")
                    totalAlerts = 0
                    activeAlerts = 0
                }
                
                users = userList
                filteredUsers = userList
                totalUsers = userList.size
                totalAdmins = adminCount
                dailyRevenue = dailyRev
                totalPremiumRevenue = totalRev
                premiumSignupsToday = premiumToday
                owlSignupsToday = owlToday
                isLoading = false
                
                Log.d("AdminDashboard", "Loaded $totalUsers users | Admins: $adminCount | Daily: ₱$dailyRev | Total: ₱$totalRev")
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error: ${e.message}", e)
                Toast.makeText(context, "Error loading dashboard", Toast.LENGTH_SHORT).show()
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
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Monitor activity & revenue",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }

                // Admin Overview Button
                IconButton(
                    onClick = { showGlobalOverview = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Rounded.Dashboard, contentDescription = "Admin Overview", 
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }

                IconButton(
                    onClick = {
                        auth.signOut()
                        onLogout()
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .size(40.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", 
                        tint = Color.White, modifier = Modifier.size(20.dp))
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Revenue Stats - 2x2 Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RevenueCard(
                                title = "Daily Revenue",
                                value = "₱${String.format("%.0f", dailyRevenue)}",
                                icon = "💵",
                                bgColor = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f),
                                onClick = onViewDailyAnalytics
                            )
                            RevenueCard(
                                title = "Total Premium Revenue",
                                value = "₱${String.format("%.0f", totalPremiumRevenue)}",
                                icon = "⭐",
                                bgColor = Color(0xFFFFF3E0),
                                modifier = Modifier.weight(1f),
                                onClick = onViewDailyAnalytics
                            )
                        }
                        
                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RevenueCard(
                                title = "Total Users",
                                value = totalUsers.toString(),
                                icon = "👥",
                                bgColor = Color(0xFFE3F2FD),
                                modifier = Modifier.weight(1f),
                                onClick = onViewUserGrowth
                            )
                            RevenueCard(
                                title = "Premium Signups (Today)",
                                value = premiumSignupsToday.toString(),
                                icon = "👤",
                                bgColor = Color(0xFFF3E5F5),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3 - OWL signups and Admins
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RevenueCard(
                                title = "OWL+ Signups (Today)",
                                value = owlSignupsToday.toString(),
                                icon = "🦉",
                                bgColor = Color(0xFFFFF9C4),
                                modifier = Modifier.weight(1f)
                            )
                            RevenueCard(
                                title = "Admins",
                                value = totalAdmins.toString(),
                                icon = "👑",
                                bgColor = Color(0xFFFFCDD2),
                                modifier = Modifier.weight(1f),
                                onClick = onManageAdmins
                            )
                        }
                    }
                }

                // Recent Users Section
                item {
                    Text(
                        text = "Recent Users",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

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
                }

                // Users List
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No users found", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredUsers.take(5)) { user ->
                        UserCardNew(user = user)
                    }
                    
                    // See More Users Button
                    item {
                        Button(
                            onClick = onSeeMoreUsers,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text(
                                text = "See All Users (${filteredUsers.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Admin Overview Bottom Sheet
    if (showGlobalOverview) {
        val overviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        
        ModalBottomSheet(
            onDismissRequest = { showGlobalOverview = false },
            sheetState = overviewSheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
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
                        imageVector = Icons.Rounded.Dashboard,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Admin Overview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueCard(
    title: String,
    value: String,
    icon: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(
                text = icon,
                fontSize = 24.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun UserCardNew(
    user: UserDataExtended,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar and info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1565C0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.firstOrNull()?.uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            // Plan badge
            Box(
                modifier = Modifier
                    .background(
                        color = when (user.plan) {
                            "Premium" -> Color(0xFFBA68C8).copy(alpha = 0.2f)
                            "OWL+" -> Color(0xFFFFD54F).copy(alpha = 0.3f)
                            else -> Color(0xFFEEEEEE)
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = user.plan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = when (user.plan) {
                        "Premium" -> Color(0xFF7B1FA2)
                        "OWL+" -> Color(0xFFFBC02D)
                        else -> Color(0xFF616161)
                    }
                )
            }

            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, 
                    tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Helper functions
fun getTodayStartTime(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun getTodayEndTime(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}
