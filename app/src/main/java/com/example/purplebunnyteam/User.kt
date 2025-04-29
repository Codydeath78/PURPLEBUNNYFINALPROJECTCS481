package com.example.purplebunnyteam

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "",
    @ServerTimestamp val joinedAt: Date? = null
)