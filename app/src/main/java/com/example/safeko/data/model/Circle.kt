package com.example.safeko.data.model

data class Circle(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)
