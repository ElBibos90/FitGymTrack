package com.fitgymtrack.app.models

data class LoginResponse(
    val success: Boolean = false,
    val message: String = "",
    val user: User? = null,
    val token: String? = null,
    val error: String? = null
)

data class User(
    val id: Int,
    val username: String,
    val email: String?,
    val name: String?,
    val role_id: Int,
    val role_name: String,
    val trainer: Trainer? = null
)

data class Trainer(
    val id: Int,
    val username: String,
    val name: String?
)