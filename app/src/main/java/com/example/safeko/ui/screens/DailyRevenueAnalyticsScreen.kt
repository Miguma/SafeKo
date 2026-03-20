package com.example.safeko.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
fun DailyRevenueAnalyticsScreen(
    onBack: () -> Unit
) {
    val firestore = Firebase.firestore
    var isLoading by remember { mutableStateOf(true) }
    var revenueData by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var latestBuyers by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var totalPeriodRevenue by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        try {
            val result = firestore.collection("users").get().await()
            val buyers = mutableListOf<UserDataExtended>()
            
            // For the chart, we'll calculate revenue for the last 7 days
            val calendar = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val dailyMap = mutableMapOf<String, Double>()
            
            // Initialize last 7 days with 0
            for (i in 0..6) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -i)
                dailyMap[dayFormat.format(tempCal.time)] = 0.0
            }

            for (document in result) {
                val plan = document.getString("plan") ?: "Free"
                val purchaseDate = document.getLong("purchaseDate") ?: 0L
                val planExpiryDate = document.getLong("planExpiryDate") ?: 0L
                
                // Check if plan is active
                if (plan != "Free" && purchaseDate > 0 && (planExpiryDate == 0L || System.currentTimeMillis() < planExpiryDate)) {
                    val amount = if (plan == "Premium") 99.0 else 299.0
                    
                    // Add to latest buyers
                    buyers.add(UserDataExtended(
                        id = document.id,
                        fullName = document.getString("fullName") ?: document.getString("name") ?: "Unknown",
                        email = document.getString("email") ?: "",
                        plan = plan,
                        purchaseDate = purchaseDate
                    ))

                    // Add to daily map if within last 7 days
                    val dateStr = dayFormat.format(Date(purchaseDate))
                    if (dailyMap.containsKey(dateStr)) {
                        dailyMap[dateStr] = (dailyMap[dateStr] ?: 0.0) + amount
                    }
                    
                    totalPeriodRevenue += amount
                }
            }

            // Sort buyers by date descending
            latestBuyers = buyers.sortedByDescending { it.purchaseDate }.take(10)
            
            // Prepare chart data (chronological order)
            revenueData = dailyMap.toList().reversed()
            
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Revenue Analytics", fontWeight = FontWeight.Bold) },
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
                    AnalyticsHeader(totalPeriodRevenue)
                }

                item {
                    Text(
                        "Revenue Trend (Last 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    RevenueChart(revenueData)
                }

                item {
                    Text(
                        "Latest Buyers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(latestBuyers) { buyer ->
                    BuyerItem(buyer)
                }
            }
        }
    }
}

@Composable
fun AnalyticsHeader(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Subscription Value", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(
                "₱${String.format("%,.2f", total)}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun RevenueChart(data: List<Pair<String, Double>>) {
    if (data.isEmpty()) return
    
    val maxVal = data.maxOf { it.second }.coerceAtLeast(1.0).toFloat()
    
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
                        colors = listOf(Color(0xFF1565C0).copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw Line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF1565C0),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw Dots
                points.forEach { point ->
                    drawCircle(
                        color = Color(0xFF1565C0),
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
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
                        pair.first.split(" ")[1], // Just show day number
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
fun BuyerItem(user: UserDataExtended) {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(user.purchaseDate))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (user.plan == "OWL+") Color(0xFFFFD700) else Color(0xFF1565C0),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.fullName.take(1).uppercase(),
                    color = if (user.plan == "OWL+") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(user.email, fontSize = 12.sp, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (user.plan == "OWL+") "₱299" else "₱99",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32),
                    fontSize = 14.sp
                )
                Text(dateStr, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
