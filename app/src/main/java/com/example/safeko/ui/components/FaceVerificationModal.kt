package com.example.safeko.ui.components

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.safeko.services.FaceVerificationService
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

/**
 * Composable for face verification with camera capture
 */
@Composable
fun FaceVerificationModal(
    onDismiss: () -> Unit,
    onFaceVerified: (faceEmbedding: String, bitmap: Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var hasCameraPermission by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureStatus by remember { mutableStateOf("Position your face in the frame") }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    val faceVerificationService = remember { FaceVerificationService() }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onError("Camera permission denied")
            onDismiss()
        }
    }
    
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        previewView = this
                        setupCameraPreview(ctx, this, lifecycleOwner) { capture ->
                            imageCapture = capture
                        }
                    }
                }
            )
            
            // Face Frame Overlay
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 4.dp,
                        color = Color(0xFF29B6F6),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(40.dp)
                    )
            )
            
            // Top Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Face Verification",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                FloatingActionButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            
            // Status Text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = captureStatus,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (!isCapturing) {
                            Text(
                                "• Ensure good lighting",
                                color = Color(0xFFB0BEC5),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• Face must be clearly visible",
                                color = Color(0xFFB0BEC5),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• Minimize head rotation",
                                color = Color(0xFFB0BEC5),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 8.dp),
                                color = Color(0xFF29B6F6),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                // Capture Button
                FloatingActionButton(
                    onClick = {
                        if (!isCapturing && imageCapture != null) {
                            isCapturing = true
                            captureStatus = "Capturing face..."
                            captureFaceImage(
                                imageCapture!!,
                                context,
                                lifecycleOwner
                            ) { bitmap ->
                                scope.launch {
                                    try {
                                        val faceResult = faceVerificationService.detectFaces(bitmap)
                                        
                                        faceResult.onSuccess { faces ->
                                            if (faces.isNotEmpty()) {
                                                val faceData = faces[0]
                                                
                                                // Check confidence
                                                if (faceData.confidenceScore >= 0.5f) {
                                                    val embeddingString = faceVerificationService.embeddingToString(
                                                        faceData.embedding
                                                    )
                                                    captureStatus = "✅ Face verified! Processing..."
                                                    
                                                    kotlinx.coroutines.delay(500)
                                                    onFaceVerified(embeddingString, bitmap)
                                                    onDismiss()
                                                } else {
                                                    captureStatus = "❌ Face quality too low. Try again."
                                                    isCapturing = false
                                                }
                                            }
                                        }
                                        
                                        faceResult.onFailure { error ->
                                            captureStatus = "❌ ${error.message}"
                                            isCapturing = false
                                            Log.e("FaceVerification", "Error: ${error.message}")
                                        }
                                    } catch (e: Exception) {
                                        captureStatus = "❌ Error processing face"
                                        isCapturing = false
                                        Log.e("FaceVerification", "Exception: ${e.message}")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    containerColor = Color(0xFF29B6F6),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "Capture",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Setup camera preview with CameraX
 */
private fun setupCameraPreview(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onImageCapture: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // ImageCapture
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            onImageCapture(imageCapture)
            
            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("FaceVerification", "Use case binding failed: ${exc.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Capture image from camera
 */
private fun captureFaceImage(
    imageCapture: ImageCapture,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onCaptureComplete: (Bitmap) -> Unit
) {
    val outputFile = File(context.cacheDir, "face_capture.jpg")
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(outputFile.absolutePath)
                    if (bitmap != null) {
                        onCaptureComplete(bitmap)
                    } else {
                        Log.e("FaceVerification", "Failed to decode bitmap")
                    }
                } catch (e: Exception) {
                    Log.e("FaceVerification", "Error processing captured image: ${e.message}")
                }
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e("FaceVerification", "Photo capture error: ${exception.message}")
            }
        }
    )
}
