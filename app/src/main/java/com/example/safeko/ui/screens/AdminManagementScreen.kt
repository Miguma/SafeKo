package com.example.safeko.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    onBack: () -> Unit
) {
    val firestore = Firebase.firestore
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var admins by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var filteredAdmins by remember { mutableStateOf<List<UserDataExtended>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
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
                                        val lguData = hashMapOf(
                                            "uid" to uid,
                                            "fullName" to newAdminName,
                                            "email" to newAdminEmail,
                                            "role" to "lgu_admin",
                                            "department" to selectedLguType,
                                            "plan" to "LGU",
                                            "createdAt" to System.currentTimeMillis()
                                        )
                                        
                                        firestore.collection("users").document(uid)
                                            .set(lguData)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "$selectedLguType Admin Created!", Toast.LENGTH_SHORT).show()
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
                        AdminItem(admin)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminItem(admin: UserDataExtended) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
