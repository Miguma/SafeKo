package com.example.safeko.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.max

data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val stepInfo: String // e.g. "1/3"
)

@Composable
fun TutorialOverlay(
    targetRect: androidx.compose.ui.geometry.Rect?,
    step: TutorialStep,
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // Ensure high z-index
            .pointerInput(Unit) {
                // Consumes all touches to prevent interaction with underlying app
                detectTapGestures(onTap = { /* Consume tap */ })
            }
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeight = maxHeight

        // 1. Semi-transparent Scrim with Cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = .99f // Create a new compositing layer
                }
        ) {
            // Draw dark scrim
            drawRect(Color.Black.copy(alpha = 0.6f))

            // Cut out the hole for the target
            if (targetRect != null) {
                drawCircle(
                    center = targetRect.center,
                    radius = max(targetRect.width, targetRect.height) / 1.5f,
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
        }

        // 2. Tutorial Card
        if (targetRect != null) {
            // Determine position: above or below target
            // Default to below, unless target is too low
            val targetBottomDp = with(density) { targetRect.bottom.toDp() }
            val targetTopDp = with(density) { targetRect.top.toDp() }
            val isTargetLow = targetBottomDp > screenHeight * 0.7f

            val cardModifier = if (isTargetLow) {
                val bottomPadding = (screenHeight - targetTopDp) + 16.dp
                val safeBottomPadding = if (bottomPadding < 0.dp) 16.dp else bottomPadding
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = safeBottomPadding)
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = targetBottomDp + 16.dp)
            }

            Card(
                modifier = cardModifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Title and Step Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = step.stepInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onBack != null) {
                            TextButton(
                                onClick = onBack,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Back", color = Color.Gray)
                            }
                        }

                        Button(
                            onClick = onNext,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                        ) {
                            Text("Next", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
