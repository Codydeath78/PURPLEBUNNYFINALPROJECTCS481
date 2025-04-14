package com.example.purplebunnyteam.fragments


import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("json")
    fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") apiKey: String,
        @Query("opennow") openNow: String? = null
    ): Call<PlacesResponse>

}