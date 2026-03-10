package com.example.safeko.utils

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PhoneAuthManager(
    private val auth: FirebaseAuth,
    private val activity: Activity
) {
    private var verificationId = ""
    private var currentPhone = ""

    // Callback interfaces for the UI
    interface PhoneAuthCallback {
        fun onCodeSent()
        fun onVerificationComplete()
        fun onVerificationFailed(exception: Exception)
        fun onOtpInvalid()
        fun onSuccess()
    }

    fun initiateOTP(phoneNumber: String, callback: PhoneAuthCallback) {
        currentPhone = phoneNumber
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity)                 // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // This callback will be invoked in two situations:
                    // 1 - Instant verification. In some cases the phone number can be instantly
                    //     verified without needing to send or enter a verification code.
                    // 2 - Auto-retrieval. On some devices Google Play services can automatically
                    //     detect the incoming verification SMS and perform verification without
                    //     user action.
                    Log.d("PhoneAuthManager", "onVerificationCompleted:$credential")
                    linkCredential(credential, callback)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w("PhoneAuthManager", "onVerificationFailed", e)
                    callback.onVerificationFailed(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    // The SMS verification code has been sent to the provided phone number, we
                    // now need to ask the user to enter the code and then construct a credential
                    // by combining the code with a verification ID.
                    Log.d("PhoneAuthManager", "onCodeSent:$verificationId")

                    // Save verification ID and resending token so we can use them later
                    this@PhoneAuthManager.verificationId = verificationId
                    callback.onCodeSent()
                }
            })          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOTP(code: String, callback: PhoneAuthCallback) {
        if (verificationId.isEmpty()) {
             callback.onOtpInvalid()
             return
        }
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            linkCredential(credential, callback)
        } catch (e: Exception) {
             callback.onOtpInvalid()
        }
    }

    private fun linkCredential(credential: PhoneAuthCredential, callback: PhoneAuthCallback) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        val user = task.result?.user
                        Log.d("PhoneAuthManager", "Successfully linked phone to user: ${user?.uid}")
                        // Update Firestore
                        updateFirestoreWithPhone(callback)
                    } else {
                        val exception = task.exception
                        if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException || 
                            exception?.message?.contains("already been linked") == true ||
                            exception?.message?.contains("PROVIDER_ALREADY_LINKED") == true) {
                            Log.d("PhoneAuthManager", "Provider already linked, updating firestore anyway")
                            updateFirestoreWithPhone(callback)
                        } else {
                            Log.e("PhoneAuthManager", "Failed to link credential", exception)
                            callback.onVerificationFailed(exception ?: Exception("Verification Failed"))
                        }
                    }
                }
        } else {
            callback.onVerificationFailed(Exception("No user logged in"))
        }
    }

    private fun updateFirestoreWithPhone(callback: PhoneAuthCallback) {
        val uid = auth.currentUser?.uid ?: return
        val db = Firebase.firestore
        
        db.collection("users").document(uid)
            .update(
                mapOf(
                    "phoneNumber" to currentPhone,
                    "phoneVerified" to true,
                    "verificationLevel" to 1 // Example level bump
                )
            )
            .addOnSuccessListener {
                callback.onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("PhoneAuthManager", "Error updating Firestore", e)
                callback.onVerificationFailed(e)
            }
    }
}
