package com.example.safeko.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.safeko.utils.FaceAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceVerificationScreen(
    auth: FirebaseAuth,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var isCapturing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var faceDetected by remember { mutableStateOf(false) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Face Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            imageCapture = ImageCapture.Builder().build()

                            // ImageAnalyzer for ML Kit
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, FaceAnalyzer {
                                        // Face detected callback (runs on background thread, move to main if updating UI)
                                        scope.launch { faceDetected = true }
                                    })
                                }

                            // We want front camera for face verification
                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                Log.e("FaceVerification", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Guidance Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Cutout area (simulate a circle)
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(
                                width = 4.dp,
                                color = if (faceDetected) Color(0xFF4CAF50) else Color.White,
                                shape = CircleShape
                            )
                    )
                }
                
                // Instructions
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (faceDetected) "Face detected! Capturing..." else "Position your face in the circle",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Capture Progress Indicator
                if (isCapturing || isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                // Automated Capture Trigger
                LaunchedEffect(faceDetected) {
                    if (faceDetected && !isCapturing && !isUploading) {
                        isCapturing = true
                        takePhoto(
                            imageCapture = imageCapture,
                            context = context,
                            executor = ContextCompat.getMainExecutor(context),
                            onImageCaptured = { uri ->
                                isCapturing = false
                                isUploading = true
                                uploadToFirebase(uri, auth, context) { success ->
                                    isUploading = false
                                    if (success) {
                                        onSuccess()
                                    } else {
                                        faceDetected = false // Reset to try again
                                    }
                                }
                            },
                            onError = {
                                isCapturing = false
                                faceDetected = false
                            }
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required.", color = Color.White)
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    executor: java.util.concurrent.Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val capture = imageCapture ?: return
    val photoFile = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
        "face_verification_${System.currentTimeMillis()}.jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
            override fun onError(exc: ImageCaptureException) {
                Log.e("FaceVerification", "Photo capture failed: ${exc.message}", exc)
                onError(exc)
            }
        }
    )
}

private fun uploadToFirebase(uri: Uri, auth: FirebaseAuth, context: Context, onComplete: (Boolean) -> Unit) {
    val uid = auth.currentUser?.uid
    if (uid == null) {
        onComplete(false)
        return
    }

    val storageRef = FirebaseStorage.getInstance().reference.child("face_verification/$uid.jpg")
    val db = Firebase.firestore

    storageRef.putFile(uri)
        .addOnSuccessListener {
            // Update Firestore
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "faceVerified" to true,
                        "verificationLevel" to 2
                    )
                )
                .addOnSuccessListener {
                    val prefs = context.getSharedPreferences("safeko_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_face_verified_$uid", true).apply()
                    Toast.makeText(context, "Face verification successful!", Toast.LENGTH_SHORT).show()
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("FaceVerification", "Firestore update failed", e)
                    Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
        }
        .addOnFailureListener { e ->
            Log.e("FaceVerification", "Upload failed", e)
            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
}
