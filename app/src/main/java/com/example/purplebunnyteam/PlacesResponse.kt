package com.example.purplebunnyteam.fragments
import com.google.gson.annotations.SerializedName
data class PlacesResponse(
    val results: List<PlaceResult>,
    val status: String
)

data class PlaceResult(
    val place_id: String?,
    val name: String?,
    val geometry: Geometry,
    val vicinity: String?,
    val rating: Double?,
    val photos: List<Photo>?
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Photo(
    @SerializedName("photo_reference")
    val photoReference: String
)
