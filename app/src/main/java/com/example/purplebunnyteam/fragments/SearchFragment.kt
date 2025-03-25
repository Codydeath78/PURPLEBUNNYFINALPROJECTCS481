package com.example.purplebunnyteam.fragments
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.example.purplebunnyteam.CustomInfoWindowAdapter
import com.example.purplebunnyteam.PlaceDetails
import com.example.purplebunnyteam.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val API_KEY = "AIzaSyBl7Ue74Ln14TV2ltZeYmuvs8Gc0S3cth8"
    private val markerMap = mutableMapOf<String, Marker>() //stores Marker references instead of LatLg so we can show info

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        val searchView = view.findViewById<SearchView>(R.id.search_bar)
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
        //set custom info window
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(layoutInflater))
        val csusmMarker = mMap.addMarker(
            MarkerOptions()
                .position(CSUSM)
                .title("CSUSM")
        )
        csusmMarker?.tag = PlaceDetails(
            name = "CSUSM",
            address = "333 S Twin Oaks Valley Rd, San Marcos, CA 92096",
            rating = 5.0
        )
        //markerMap["CSUSM"] = CSUSM

        // Move the camera to the map coordinates and zoom in closer.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CSUSM, 10f))
        // Display traffic.
        mMap.isTrafficEnabled = true
        mMap.isBuildingsEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true //Enable zoom controls
        mMap.uiSettings.isCompassEnabled = true      //Enable compass
        mMap.uiSettings.isMyLocationButtonEnabled = true  //Enable current location button
        mMap.uiSettings.isMapToolbarEnabled = true   //Enable map toolbar

        // Fetch nearby coffee shops
        getNearbyPlaces(CSUSM)
        setupSearchBar()
    }

    private fun setupSearchBar() {
        val searchView = requireView().findViewById<SearchView>(R.id.search_bar)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchForMarker(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun searchForMarker(query: String) { //This searches for marker and moves camera if found.
        val result = markerMap.entries.find { it.key.contains(query, ignoreCase = true)}

        if (result != null) {
            val marker = result.value
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))
            marker.showInfoWindow() // Open the InfoWindow
            Log.d("SEARCH", "Navigating to: ${result.key}")
        } else {
            Log.d("SEARCH", "No match found for: $query")
        }
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
            3000,  // Search radius in meters
            "cafe", // Type of place
            API_KEY
        )


        fun resizeMapIcon(iconResId: Int, width: Int, height: Int): BitmapDescriptor {
            val bitmap = BitmapFactory.decodeResource(resources, iconResId)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
            return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        }

        call.enqueue(object : Callback<PlacesResponse> {
            override fun onResponse(call: Call<PlacesResponse>, response: Response<PlacesResponse>) {
                if (response.isSuccessful) {
                    response.body()?.results?.forEach { place ->
                        val latLng = LatLng(place.geometry.location.lat, place.geometry.location.lng)

                        val placeDetails = PlaceDetails(
                            name = place.name ?: "No name",
                            address = place.vicinity ?: "No address available",
                            rating = place.rating ?: 0.0
                        )

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .icon(resizeMapIcon(R.drawable.coffee_cup, 100, 100)) // Custom size icon
                        )

                        marker?.tag = placeDetails
                        // Store the marker reference instead of just the LatLng to show info
                        marker?.let {
                            markerMap[place.name ?: "Unknown"] = it
                        }

                    }
                }
            }

            override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

}