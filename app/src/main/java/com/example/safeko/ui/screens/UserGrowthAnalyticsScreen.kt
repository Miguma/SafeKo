package com.example.safeko.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGrowthAnalyticsScreen(
    onBack: () -> Unit
) {
    val firestore = Firebase.firestore
    var isLoading by remember { mutableStateOf(true) }
    var growthData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var recentSignups by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var totalUsersCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val result = firestore.collection("users").get().await()
            val signups = mutableListOf<UserDataExtended>()
            
            // For the chart, we'll calculate signups for the last 7 days
            val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val dailyMap = mutableMapOf<String, Int>()
            
            // Initialize last 7 days with 0
            for (i in 0..6) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -i)
                dailyMap[dayFormat.format(tempCal.time)] = 0
            }

            for (document in result) {
                val createdAt = document.getLong("createdAt") ?: 0L
                val name = document.getString("fullName") ?: document.getString("name") ?: "Unknown"
                val email = document.getString("email") ?: ""
                val plan = document.getString("plan") ?: "Free"
                
                val user = UserDataExtended(
                    id = document.id,
                    fullName = name,
                    email = email,
                    plan = plan,
                    createdAt = createdAt
                )
                signups.add(user)

                // Add to daily growth map if within last 7 days
                if (createdAt > 0) {
                    val dateStr = dayFormat.format(Date(createdAt))
                    if (dailyMap.containsKey(dateStr)) {
                        dailyMap[dateStr] = (dailyMap[dateStr] ?: 0) + 1
                    }
                }
            }

            totalUsersCount = signups.size
            // Sort signups by creation date descending
            recentSignups = signups.sortedByDescending { it.createdAt }.take(15)
            
            // Prepare chart data (chronological order)
            growthData = dailyMap.toList().reversed().map { it.first to it.second }
            
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Growth Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1565C0))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    GrowthHeader(totalUsersCount)
                }

                item {
                    Text(
                        "Signup Trend (Last 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    GrowthChart(growthData)
                }

                item {
                    Text(
                        "Newest Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(recentSignups) { user ->
                    SignupItem(user)
                }
            }
        }
    }
}

@Composable
fun GrowthHeader(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF43A047)), // Green for growth
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Community Members", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(
                "$total Users",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun GrowthChart(data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return
    
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1).toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                val width = size.width
                val height = size.height
                val spacing = width / (data.size - 1)
                
                val points = data.mapIndexed { index, pair ->
                    Offset(
                        x = index * spacing,
                        y = height - (pair.second.toFloat() / maxVal * height)
                    )
                }

                // Draw Area
                val fillPath = Path().apply {
                    moveTo(0f, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF43A047).copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw Line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF43A047),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw Dots
                points.forEach { point ->
                    drawCircle(
                        color = Color(0xFF43A047),
                        radius = 4.dp.toPx(),
                        center = point
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { pair ->
                    Text(
                        pair.first.split(" ")[1],
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SignupItem(user: UserDataExtended) {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = if (user.createdAt > 0) sdf.format(Date(user.createdAt)) else "Recently"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF43A047), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.fullName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(user.email, fontSize = 11.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(user.plan, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
                Text(dateStr, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
