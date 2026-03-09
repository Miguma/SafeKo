package com.example.safeko.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "", // Added for display
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
