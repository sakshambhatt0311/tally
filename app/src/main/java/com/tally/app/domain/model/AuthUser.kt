package com.tally.app.domain.model

data class AuthUser(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String?,
    val displayName: String? = null,
    val photoUrl: String? = null
)
