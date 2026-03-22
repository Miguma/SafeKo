package com.example.safeko.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    onBack: () -> Unit
) {
    val firestore = Firebase.firestore
    val auth = Firebase.auth
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var admins by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var filteredAdmins by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var currentUserRole by remember { mutableStateOf("user") }
    
    // Add Admin Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var newAdminEmail by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("") }
    var newAdminName by remember { mutableStateOf("") }
    var selectedLguType by remember { mutableStateOf("Fire") }
    val lguTypes = listOf("Fire", "Rescue", "Medical")
    var isLguAccount by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var isAdding by remember { mutableStateOf(false) }

    // Get current user's role
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                currentUserRole = userDoc.getString("role") ?: "user"
            } catch (e: Exception) {
                Log.e("AdminManagement", "Error fetching user role: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val result = firestore.collection("users")
                .whereIn("role", listOf("admin", "superadmin", "lgu_admin"))
                .get()
                .await()
            
            val adminList = result.map { document ->
                UserDataExtended(
                    id = document.id,
                    fullName = document.getString("fullName") ?: document.getString("name") ?: "Unknown",
                    email = document.getString("email") ?: "",
                    role = document.getString("role") ?: "admin",
                    department = document.getString("department") ?: "",
                    plan = document.getString("plan") ?: "Free"
                )
            }
            admins = adminList
            filteredAdmins = adminList
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            Toast.makeText(context, "Error loading admins", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(searchQuery) {
        filteredAdmins = if (searchQuery.isBlank()) {
            admins
        } else {
            admins.filter { 
                it.fullName.contains(searchQuery, ignoreCase = true) || 
                it.email.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAdding) showAddDialog = false },
            title = { Text(if (isLguAccount) "Create LGU Admin Account" else "Grant Admin Access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isLguAccount, onCheckedChange = { isLguAccount = it })
                        Text("Create new LGU account", fontSize = 14.sp)
                    }

                    if (isLguAccount) {
                        OutlinedTextField(
                            value = newAdminName,
                            onValueChange = { newAdminName = it },
                            label = { Text("Full Name (e.g., City Fire Dept)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isLguAccount) {
                        OutlinedTextField(
                            value = newAdminPassword,
                            onValueChange = { newAdminPassword = it },
                            label = { Text("Set Password") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedLguType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("LGU Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                lguTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedLguType = type
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Promote an existing user to admin by email.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isLguAccount) {
                            if (newAdminEmail.isNotBlank() && newAdminPassword.isNotBlank() && newAdminName.isNotBlank()) {
                                isAdding = true
                                val auth = Firebase.auth
                                auth.createUserWithEmailAndPassword(newAdminEmail, newAdminPassword)
                                    .addOnSuccessListener { authResult ->
                                        val uid = authResult.user?.uid ?: ""
                                        val user = authResult.user
                                        
                                        // Set Firebase Auth displayName
                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName(newAdminName)
                                            .build()
                                        
                                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                            if (!profileTask.isSuccessful) {
                                                Log.w("AdminManagement", "Failed to update profile: ${profileTask.exception}")
                                            }
                                        }
                                        
                                        val lguData = hashMapOf(
                                            "uid" to uid,
                                            "fullName" to newAdminName,
                                            "email" to newAdminEmail,
                                            "role" to "lgu_admin",
                                            "lgc_id" to uid,
                                            "department" to selectedLguType,
                                            "plan" to "LGU",
                                            "emailVerified" to true,
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        
                                        firestore.collection("users").document(uid)
                                            .set(lguData)
                                            .addOnSuccessListener {
                                                // 🎯 Auto-create group chat for LGU
                                                val circleId = UUID.randomUUID().toString()
                                                val groupChat = hashMapOf(
                                                    "id" to circleId,
                                                    "name" to "$selectedLguType Group",
                                                    "ownerId" to uid,
                                                    "members" to listOf(uid),
                                                    "memberLimit" to 20,
                                                    "type" to "Group",
                                                    "createdAt" to System.currentTimeMillis(),
                                                    "imageUrl" to ""
                                                )
                                                
                                                firestore.collection("circles").document(circleId)
                                                    .set(groupChat)
                                                    .addOnSuccessListener {
                                                        Log.d("LguAdminCreation", "✅ Group chat created for $selectedLguType: $circleId")
                                                        Toast.makeText(context, "$selectedLguType Admin Created + Group Chat! 🎉", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("LguAdminCreation", "❌ Failed to create group chat: ${e.message}")
                                                        Toast.makeText(context, "$selectedLguType Admin Created (Chat creation failed)", Toast.LENGTH_SHORT).show()
                                                    }
                                                
                                                showAddDialog = false
                                                isAdding = false
                                                newAdminEmail = ""
                                                newAdminPassword = ""
                                                newAdminName = ""
                                                onBack() 
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        isAdding = false
                                    }
                            } else {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (newAdminEmail.isNotBlank()) {
                                isAdding = true
                                firestore.collection("users")
                                    .whereEqualTo("email", newAdminEmail)
                                    .get()
                                    .addOnSuccessListener { docs ->
                                        if (docs.isEmpty) {
                                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                                            isAdding = false
                                        } else {
                                            val uid = docs.documents[0].id
                                            firestore.collection("users").document(uid)
                                                .update("role", "admin")
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Admin added!", Toast.LENGTH_SHORT).show()
                                                    showAddDialog = false
                                                    isAdding = false
                                                    newAdminEmail = ""
                                                    onBack() 
                                                }
                                        }
                                    }
                            }
                        }
                    },
                    enabled = !isAdding
                ) {
                    Text(if (isAdding) "Processing..." else if (isLguAccount) "Create Account" else "Promote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isAdding) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admins Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search and Add Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search admins...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1565C0))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAdmins) { admin ->
                        AdminItem(
                            admin = admin,
                            currentUserRole = currentUserRole,
                            firestore = firestore,
                            context = context,
                            onAdminDeleted = {
                                // Refresh the list after deletion
                                filteredAdmins = filteredAdmins.filter { it.id != admin.id }
                                admins = admins.filter { it.id != admin.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminItem(
    admin: UserDataExtended,
    currentUserRole: String = "user",
    firestore: com.google.firebase.firestore.FirebaseFirestore? = null,
    context: android.content.Context? = null,
    onAdminDeleted: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Admin?") },
            text = { Text("Are you sure you want to delete ${admin.fullName}? They will be converted back to a regular user.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        // Delete/demote the admin
                        firestore?.collection("users")?.document(admin.id)
                            ?.update(mapOf(
                                "role" to "user",
                                "lgc_id" to "",
                                "department" to ""
                            ))
                            ?.addOnSuccessListener {
                                isDeleting = false
                                showDeleteDialog = false
                                Toast.makeText(context, "${admin.fullName} has been removed as admin", Toast.LENGTH_SHORT).show()
                                onAdminDeleted()
                            }
                            ?.addOnFailureListener { e ->
                                isDeleting = false
                                showDeleteDialog = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("AdminDelete", "Failed to delete admin: ${e.message}")
                            }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (currentUserRole == "superadmin") {
                        showDeleteDialog = true
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when (admin.role) {
                            "superadmin" -> Color(0xFFD32F2F)
                            "lgu_admin" -> Color(0xFF43A047)
                            else -> Color(0xFF1976D2)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    admin.fullName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(admin.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(admin.email, fontSize = 12.sp, color = Color.Gray)
            }
            
            Surface(
                color = when (admin.role) {
                    "superadmin" -> Color(0xFFFFEBEE)
                    "lgu_admin" -> Color(0xFFF1F8E9)
                    else -> Color(0xFFE3F2FD)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (admin.role == "lgu_admin") admin.department.uppercase() else admin.role.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when (admin.role) {
                        "superadmin" -> Color(0xFFC62828)
                        "lgu_admin" -> Color(0xFF388E3C)
                        else -> Color(0xFF1565C0)
                    }
                )
            }
        }
    }
}
