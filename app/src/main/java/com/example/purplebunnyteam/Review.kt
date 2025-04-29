package com.example.purplebunnyteam

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review(
    val reviewId: String = "",
    val userId: String = "",
    val cafeId: String = "",
    val ratings: Map<String, Double> = emptyMap(),
    val tags: List<String> = emptyList(),
    val comment: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val photos: List<String> = emptyList(),
    var likes: Int = 0,
    var isLiked: Boolean = false
)