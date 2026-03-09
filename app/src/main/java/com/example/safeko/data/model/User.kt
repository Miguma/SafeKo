package com.example.safeko.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    var profilePhoto: String? = null,
    var verifiedStatus: Boolean = false,
    var plan: String = "Free"
)
