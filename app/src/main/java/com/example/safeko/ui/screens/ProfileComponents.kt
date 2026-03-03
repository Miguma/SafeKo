package com.example.safeko.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentName: String,
    currentEmail: String,
    currentPhotoUrl: String?,
    initialPhoneNumber: String,
    currentLocation: String,
    onSave: (String) -> Unit
) {
    if (showDialog) {
        var phoneNumber by remember { mutableStateOf(initialPhoneNumber) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding() // Avoid overlapping system navigation
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (!currentPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentPhotoUrl)
                                        .crossfade(true)
                                        .transformations(CircleCropTransformation())
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color(0xFFE0E0E0), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0E0E0),
                                    modifier = Modifier.size(100.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = "Photo from Google Account",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 24.dp),
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )

                        // Full Name (Read-only)
                        Text(
                            text = "Full Name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = currentName,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.LightGray,
                                disabledContainerColor = Color.White
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Email (Read-only)
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = currentEmail,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.LightGray,
                                disabledContainerColor = Color(0xFFF5F5F5) // Slightly grey background to indicate read-only/linked
                            ),
                            singleLine = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email, // Or lock icon
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Email is linked to your Google account",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Number (Editable)
                        Text(
                            text = "Phone Number",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("+63 912 345 6789") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Philippine format: +63 XXX XXX XXXX",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Current Location (Read-only)
                        Text(
                            text = "Current Location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = currentLocation,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.Black,
                                disabledBorderColor = Color.LightGray,
                                disabledContainerColor = Color(0xFFF5F5F5)
                            ),
                            singleLine = true
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Auto-updated via real-time tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    // Footer with Save Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Button(
                            onClick = { onSave(phoneNumber) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4) // Google Blue / Brand Blue
                            )
                        ) {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
