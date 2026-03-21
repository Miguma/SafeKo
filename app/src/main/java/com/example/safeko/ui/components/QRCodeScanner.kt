package com.example.safeko.ui.components

import android.Manifest
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class) 
@Composable
fun QRCodeScanner(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // Simple flag to prevent duplicate callbacks
    val scannedFlag = remember { AtomicBoolean(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                Log.d("QRCodeScanner", "Creating PreviewView")
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    Log.d("QRCodeScanner", "Camera provider ready")
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            try {
                                // Skip if already scanned - thread-safe check
                                if (scannedFlag.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    val scanner = BarcodeScanning.getClient()
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            // Only trigger once - use atomic flag
                                            if (barcodes.isNotEmpty() && scannedFlag.compareAndSet(false, true)) {
                                                Log.d("QRCodeScanner", "QR code detected: ${barcodes.size} barcodes")
                                                for (barcode in barcodes) {
                                                    barcode.rawValue?.let { value ->
                                                        Log.d("QRCodeScanner", "Sending to callback: $value")
                                                        onQrCodeScanned(value)
                                                        return@addOnSuccessListener
                                                    }
                                                }
                                            } else if (barcodes.isEmpty()) {
                                                Log.d("QRCodeScanner", "No barcode in this frame")
                                            } else {
                                                Log.d("QRCodeScanner", "Already scanned, ignoring")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("QRCodeScanner", "Barcode scanning error", e)
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            } catch (e: Exception) {
                                Log.e("QRCodeScanner", "Image analysis error", e)
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            Log.d("QRCodeScanner", "Camera bound successfully")
                        } catch (exc: Exception) {
                            Log.e("QRCodeScanner", "Camera binding error", exc)
                        }
                    } catch (e: Exception) {
                        Log.e("QRCodeScanner", "Error getting camera provider", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    Log.d("QRCodeScanner", "Camera resources cleanup")
                } catch (e: Exception) {
                    Log.e("QRCodeScanner", "Error during cleanup", e)
                }
            }
        }

        // Target overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
        )

        // Close Button overlay - Fixed to use clickable instead of Surface
        Box(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.TopEnd)
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .clickable {
                    Log.d("QRCodeScanner", "Close button clicked")
                    onClose()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Close Scanner",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
