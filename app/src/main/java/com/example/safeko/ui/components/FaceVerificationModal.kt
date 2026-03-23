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
import kotlin.math.abs

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
    var isAnalyzingFrame by remember { mutableStateOf(false) }
    var stableFaceChecks by remember { mutableStateOf(0) }
    var captureStatus by remember { mutableStateOf("Hold your face inside the frame") }
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

    // Auto-scan loop: capture and evaluate snapshots until a stable, high-quality face is found.
    LaunchedEffect(hasCameraPermission, imageCapture, isCapturing) {
        if (!hasCameraPermission || imageCapture == null || isCapturing) {
            return@LaunchedEffect
        }

        while (hasCameraPermission && imageCapture != null && !isCapturing) {
            if (!isAnalyzingFrame) {
                isAnalyzingFrame = true
                captureFaceImage(
                    imageCapture = imageCapture!!,
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onCaptureComplete = { bitmap ->
                        scope.launch {
                            try {
                                val faceResult = faceVerificationService.detectFaces(bitmap)

                                faceResult.onSuccess { faces ->
                                    if (faces.isEmpty()) {
                                        stableFaceChecks = 0
                                        captureStatus = "No face detected. Center your face in the frame."
                                        return@onSuccess
                                    }

                                    val faceData = faces[0]
                                    val qualityResult = evaluateFaceQuality(faceData, bitmap)

                                    if (qualityResult.first) {
                                        stableFaceChecks += 1
                                        captureStatus = "Good. Hold still... ${stableFaceChecks}/3"

                                        if (stableFaceChecks >= 3) {
                                            isCapturing = true
                                            captureStatus = "Face locked. Finalizing verification..."
                                            val embeddingString = faceVerificationService.embeddingToString(faceData.embedding)
                                            onFaceVerified(embeddingString, bitmap)
                                            onDismiss()
                                        }
                                    } else {
                                        stableFaceChecks = 0
                                        captureStatus = qualityResult.second
                                    }
                                }

                                faceResult.onFailure { error ->
                                    stableFaceChecks = 0
                                    captureStatus = "${error.message ?: "Face check failed"}. Try again."
                                    Log.e("FaceVerification", "Error: ${error.message}")
                                }
                            } catch (e: Exception) {
                                stableFaceChecks = 0
                                captureStatus = "Error processing face. Please try again."
                                Log.e("FaceVerification", "Exception: ${e.message}")
                            } finally {
                                isAnalyzingFrame = false
                            }
                        }
                    },
                    onCaptureError = { errorMessage ->
                        stableFaceChecks = 0
                        captureStatus = "Camera capture failed. Adjust lighting and hold still."
                        isAnalyzingFrame = false
                        Log.e("FaceVerification", errorMessage)
                    }
                )
            }

            kotlinx.coroutines.delay(650)
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
                                "• Keep full face inside frame",
                                color = Color(0xFFB0BEC5),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• Hold still for auto scan",
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

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF29B6F6),
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Auto scanning...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Processing...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateFaceQuality(
    faceData: FaceVerificationService.FaceData,
    bitmap: Bitmap
): Pair<Boolean, String> {
    val face = faceData.face

    if (faceData.confidenceScore < 0.6f) {
        return false to "Face quality too low. Improve lighting."
    }

    if (abs(face.headEulerAngleX) > 18 || abs(face.headEulerAngleY) > 18 || abs(face.headEulerAngleZ) > 18) {
        return false to "Keep your head straight and still."
    }

    val box = face.boundingBox
    val width = bitmap.width.toFloat()
    val height = bitmap.height.toFloat()

    val marginX = width * 0.04f
    val marginY = height * 0.04f
    if (box.left < marginX || box.top < marginY || box.right > width - marginX || box.bottom > height - marginY) {
        return false to "Move your face fully inside the frame."
    }

    val faceAreaRatio = (box.width() * box.height()).toFloat() / (width * height)
    if (faceAreaRatio < 0.08f) {
        return false to "Move closer to the camera."
    }
    if (faceAreaRatio > 0.6f) {
        return false to "Move slightly farther from the camera."
    }

    val faceCenterX = box.exactCenterX()
    val faceCenterY = box.exactCenterY()
    val centerDx = abs(faceCenterX - width / 2f)
    val centerDy = abs(faceCenterY - height / 2f)
    if (centerDx > width * 0.2f || centerDy > height * 0.2f) {
        return false to "Center your face in the frame."
    }

    return true to "Good"
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
    onCaptureComplete: (Bitmap) -> Unit,
    onCaptureError: (String) -> Unit
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
                        val error = "Failed to decode captured image"
                        Log.e("FaceVerification", error)
                        onCaptureError(error)
                    }
                } catch (e: Exception) {
                    val error = "Error processing captured image: ${e.message}"
                    Log.e("FaceVerification", error)
                    onCaptureError(error)
                }
            }
            
            override fun onError(exception: ImageCaptureException) {
                val error = "Photo capture error: ${exception.message}"
                Log.e("FaceVerification", error)
                onCaptureError(error)
            }
        }
    )
}
