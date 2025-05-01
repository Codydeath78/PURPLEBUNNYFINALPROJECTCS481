package com.example.purplebunnyteam

data class PlaceDetails(
    val name: String,
    val address: String,
    val rating: Double,
    val photoUrl: String,
    val placeId: String,
    val lat: Double,
    val lng: Double
)