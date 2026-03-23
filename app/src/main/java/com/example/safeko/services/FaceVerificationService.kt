package com.example.safeko.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Service for face verification using ML Kit
 * Detects faces and extracts embeddings for verification
 */
class FaceVerificationService {
    
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )
    
    /**
     * Detects faces in a bitmap image
     * Returns face data including landmarks and embeddings
     */
    suspend fun detectFaces(bitmap: Bitmap): Result<List<FaceData>> = withContext(Dispatchers.Default) {
        return@withContext try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Use Tasks.await() to convert async callback to suspend
            val faces = Tasks.await(detector.process(inputImage))
            
            Log.d("FaceVerification", "✅ Detected ${faces.size} face(s)")
            
            if (faces.isEmpty()) {
                return@withContext Result.failure(Exception("No face detected in image"))
            }
            
            if (faces.size > 1) {
                return@withContext Result.failure(Exception("Multiple faces detected. Please ensure only one face is in the frame."))
            }
            
            val faceList = faces.map { face ->
                FaceData(
                    face = face,
                    embedding = extractFaceEmbedding(face),
                    confidenceScore = calculateFaceConfidence(face)
                )
            }
            
            Result.success(faceList)
        } catch (e: Exception) {
            Log.e("FaceVerification", "Exception during face detection: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Compares two face embeddings to determine if they're the same person
     * Returns similarity score between 0 and 1
     */
    fun compareFaceEmbeddings(embedding1: List<Float>, embedding2: List<Float>): Float {
        if (embedding1.size != embedding2.size) {
            return 0f
        }
        
        // Calculate cosine similarity
        val dotProduct = embedding1.zip(embedding2).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
        val norm1 = sqrt(embedding1.sumOf { (it * it).toDouble() }).toFloat()
        val norm2 = sqrt(embedding2.sumOf { (it * it).toDouble() }).toFloat()
        
        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (norm1 * norm2)
        } else {
            0f
        }
    }
    
    /**
     * Determines if two embeddings match (same person)
     * Uses a confidence threshold of 0.6
     */
    fun embeddingsMatch(embedding1: List<Float>, embedding2: List<Float>, threshold: Float = 0.6f): Boolean {
        val similarity = compareFaceEmbeddings(embedding1, embedding2)
        Log.d("FaceVerification", "Similarity score: $similarity (threshold: $threshold)")
        return similarity >= threshold
    }
    
    /**
     * Extract face landmarks as embedding
     * Uses face landmarks to create a simple embedding vector
     */
    private fun extractFaceEmbedding(face: Face): List<Float> {
        val landmarks = face.allLandmarks
        val embedding = mutableListOf<Float>()
        
        // Extract landmark positions (x, y coordinates)
        for (landmark in landmarks) {
            embedding.add(landmark.position.x)
            embedding.add(landmark.position.y)
        }
        
        // Add face bounds and rotation angles for additional context
        embedding.add(face.boundingBox.left.toFloat())
        embedding.add(face.boundingBox.top.toFloat())
        embedding.add(face.boundingBox.width().toFloat())
        embedding.add(face.boundingBox.height().toFloat())
        embedding.add(face.headEulerAngleX)
        embedding.add(face.headEulerAngleY)
        embedding.add(face.headEulerAngleZ)
        
        // Add face tracking ID
        embedding.add(face.trackingId?.toFloat() ?: -1f)
        
        return embedding
    }
    
    /**
     * Calculate a confidence score for the detected face
     * Based on face quality metrics
     */
    private fun calculateFaceConfidence(face: Face): Float {
        var score = 1.0f
        
        // Penalize if face is too small
        val faceArea = face.boundingBox.width() * face.boundingBox.height()
        if (faceArea < 10000) {
            score -= 0.3f
        }
        
        // Penalize if head rotation is too extreme
        val maxRotation = maxOf(
            kotlin.math.abs(face.headEulerAngleX),
            kotlin.math.abs(face.headEulerAngleY),
            kotlin.math.abs(face.headEulerAngleZ)
        )
        if (maxRotation > 45) {
            score -= 0.2f
        }
        
        // Penalize if smiley probability is too high (for liveness check)
        val smilingProb = face.smilingProbability ?: 0f
        if (smilingProb > 0.8f) {
            score -= 0.1f
        }
        
        return maxOf(0f, score)
    }
    
    /**
     * Converts embedding list to a comma-separated string for storage
     */
    fun embeddingToString(embedding: List<Float>): String {
        return embedding.joinToString(",")
    }
    
    /**
     * Converts comma-separated string back to embedding list
     */
    fun stringToEmbedding(embeddingString: String): List<Float> {
        return try {
            embeddingString.split(",").map { it.toFloat() }
        } catch (e: Exception) {
            Log.e("FaceVerification", "Failed to parse embedding string: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Data class holding face detection results
     */
    data class FaceData(
        val face: Face,
        val embedding: List<Float>,
        val confidenceScore: Float
    )
}
