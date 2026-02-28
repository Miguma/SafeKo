package com.example.safeko.model

import com.example.safeko.R

data class SearchResult(
    val id: Long,
    val name: String,
    val address: String,
    val status: String,
    val distance: String,
    val imageRes: Int? = null,
    val lat: Double,
    val lon: Double
)
