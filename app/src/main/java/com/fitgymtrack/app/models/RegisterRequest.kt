package com.fitgymtrack.app.models

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val name: String
)