package com.example.safeko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: String,
    val subtitle: String,
    val features: List<String>,
    val buttonText: String,
    val color: Color,
    val isBestValue: Boolean = false,
    val isCurrent: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onVerifyPhone: () -> Unit,
    onVerifyFace: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    var currentPlanId by remember { mutableStateOf("Free") }
    var isVerified by remember { mutableStateOf(false) }
    var isPhoneVerified by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showVerificationModal by remember { mutableStateOf(false) }
    var showFaceConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    currentPlanId = snapshot.getString("plan") ?: "Free"
                    userRole = snapshot.getString("role")
                    isVerified = snapshot.getBoolean("faceVerified") ?: false
                    isPhoneVerified = snapshot.getBoolean("phoneVerified") ?: false
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
        isLoading = false
    }

    // Auto-dismiss for LGU/Admin
    LaunchedEffect(userRole) {
        if (userRole == "lgu_admin" || userRole == "admin" || userRole == "superadmin") {
            onBack()
            Toast.makeText(context, "Subscription management is for users only.", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePlan(newPlanId: String, newPlanName: String) {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val updateData = mutableMapOf<String, Any>("plan" to newPlanId)
                
                // Add purchase date and expiry if upgrading to Premium or OWL+
                if (newPlanId == "Premium" || newPlanId == "OWL+") {
                    val now = System.currentTimeMillis()
                    val thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000) // 30 days in milliseconds
                    updateData["purchaseDate"] = now
                    updateData["planExpiryDate"] = thirtyDaysLater
                } else if (newPlanId == "Free") {
                    // Remove plan expiry for Free tier using FieldValue.delete()
                    updateData["purchaseDate"] = com.google.firebase.firestore.FieldValue.delete()
                    updateData["planExpiryDate"] = com.google.firebase.firestore.FieldValue.delete()
                }
                
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                currentPlanId = newPlanId
                Toast.makeText(context, "Plan updated to $newPlanName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to update plan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val plans = listOf(
        SubscriptionPlan(
            id = "Free",
            name = "Free Plan",
            price = "Free",
            subtitle = "Basic community safety features.",
            features = listOf(
                "View the live safety map",
                "See active emergency alerts",
                "Report SOS incidents",
                "Respond to nearby emergencies",
                "No tracking features"
            ),
            buttonText = if (currentPlanId == "Free") "Current Plan" else "Downgrade to Free",
            color = Color(0xFF757575), // Grey
            isCurrent = currentPlanId == "Free"
        ),
        SubscriptionPlan(
            id = "Premium",
            name = "Premium Plan",
            price = "₱99",
            subtitle = "Family Tracking",
            features = listOf(
                "Track up to 5 members",
                "Real-time location monitoring",
                "Option to add +5 members for ₱99",
                "No SMS or email emergency alerts"
            ),
            buttonText = if (currentPlanId == "Premium") "Current Plan" else "Get Premium",
            color = Color(0xFFBA68C8), // Purple
            isBestValue = true,
            isCurrent = currentPlanId == "Premium"
        ),
        SubscriptionPlan(
            id = "OWL+",
            name = "OWL+ Plan",
            price = "₱299",
            subtitle = "Advanced Emergency Monitoring",
            features = listOf(
                "Includes everything in Premium",
                "Real-time family tracking",
                "Email notifications when a member sends SOS",
                "SMS alerts for emergencies",
                "Direct call option to contact the member in danger",
                "Faster emergency awareness"
            ),
            buttonText = if (currentPlanId == "OWL+") "Current Plan" else "Get OWL+",
            color = Color(0xFFFFD54F), // Gold/Amber
            isCurrent = currentPlanId == "OWL+"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 32.dp), // Lower the top bar to avoid camera notch
                title = { Text("Subscription Plans", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        
        if (showVerificationModal) {
            AlertDialog(
                onDismissRequest = { showVerificationModal = false },
                title = { Text("Verification Required", fontWeight = FontWeight.Bold) },
                text = { Text("You must be fully verified (Face Verification) to purchase a premium plan. Please complete verification in your Profile.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showVerificationModal = false
                            if (!isPhoneVerified) {
                                onVerifyPhone()
                            } else {
                                showFaceConfirmationDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Proceed verified", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVerificationModal = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        if (showFaceConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showFaceConfirmationDialog = false },
                title = { Text("Face Verification", fontWeight = FontWeight.Bold) },
                text = { Text("Do you want to proceed and verify your face now to unlock premium features?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showFaceConfirmationDialog = false
                            onVerifyFace()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6))
                    ) {
                        Text("Proceed", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFaceConfirmationDialog = false }) {
                        Text("Later", color = Color.Gray)
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Choose the plan that fits your safety needs.",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            items(plans) { plan ->
                SubscriptionCard(plan, onSelect = {
                    if (!plan.isCurrent) {
                        if (!isVerified && plan.id != "Free") {
                            showVerificationModal = true
                        } else {
                            updatePlan(plan.id, plan.name)
                        }
                    }
                })
            }
        }
    }
}

@Composable
fun SubscriptionCard(plan: SubscriptionPlan, onSelect: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), // Light gray for white background
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (plan.isBestValue) {
                Text(
                    text = "BEST VALUE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(plan.color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = plan.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black // Dark text for white background
            )
            
            Text(
                text = plan.price,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black, // Dark text for white background
                fontWeight = FontWeight.SemiBold
            )
            
            if (plan.price.contains("₱")) {
                Text(
                   text = "/ month",
                   style = MaterialTheme.typography.bodySmall,
                   color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plan.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray // Darker gray for better contrast
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE0E0E0)) // Lighter divider
            Spacer(modifier = Modifier.height(16.dp))

            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = plan.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black // Dark text for white background
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSelect,
                enabled = !plan.isCurrent,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = plan.color,
                    contentColor = if (plan.color == Color(0xFF757575)) Color.White else Color.Black,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = plan.buttonText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}
