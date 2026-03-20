package com.example.safeko.data.model

data class Circle(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val memberLimit: Int = 5,
    val type: String = "Group", // "Group" (legacy "Duo" is unsupported)
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null
)
