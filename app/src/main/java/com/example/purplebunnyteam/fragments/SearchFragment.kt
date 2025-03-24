package com.example.purplebunnyteam.fragments
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val API_KEY = "AIzaSyBl7Ue74Ln14TV2ltZeYmuvs8Gc0S3cth8"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Get the SupportMapFragment and request the map to load
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return view
    }

    // Update the map configuration at runtime.
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Set the map coordinates to CSUSM
        val CSUSM = LatLng(33.1284, -117.1592)
        // Set the map type to Hybrid.
         // googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        // Add a marker on the map coordinates.
        mMap.addMarker(
            MarkerOptions()
                .position(CSUSM)
                .title("CSUSM")
        )
        // Move the camera to the map coordinates and zoom in closer.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CSUSM, 10f))
        // Display traffic.
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = true

        // Fetch nearby coffee shops
        getNearbyPlaces(CSUSM)
    }

    private fun getNearbyPlaces(location: LatLng) {
        val baseUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val requestUrl = "$baseUrl?location=${location.latitude},${location.longitude}&radius=1500&type=cafe&key=$API_KEY"
        Log.d("API_REQUEST", "Request URL: $requestUrl")
        val service = retrofit.create(PlacesApiService::class.java)
        val call = service.getNearbyPlaces(
            "${location.latitude},${location.longitude}",
            1500,  // Search radius in meters
            "cafe", // Type of place
            API_KEY
        )

        call.enqueue(object : Callback<PlacesResponse> {
            override fun onResponse(call: Call<PlacesResponse>, response: Response<PlacesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.results?.forEach { place ->
                        val latLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                        )
                    }
                }
            }

            override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

}