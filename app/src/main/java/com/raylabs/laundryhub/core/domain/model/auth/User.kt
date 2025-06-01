package com.raylabs.laundryhub.core.domain.model.auth

data class User(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val urlPhoto: String?
)