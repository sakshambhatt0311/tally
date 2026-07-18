package com.tally.app.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = "",
    
    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String? = null,
    
    @get:PropertyName("displayName")
    @set:PropertyName("displayName")
    var displayName: String? = null,
    
    @get:PropertyName("photoUrl")
    @set:PropertyName("photoUrl")
    var photoUrl: String? = null,
    
    @get:PropertyName("isGuest")
    @set:PropertyName("isGuest")
    var isGuest: Boolean = false
)
