package com.example.letslink.fragments

import android.content.res.Resources
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.letslink.activities.Compass
import com.example.letslink.R
import com.example.letslink.nav.HorizontalCoordinator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent
import android.location.Location
import com.google.android.gms.maps.model.LatLng

class FriendMapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var friend1Distance: TextView
    private lateinit var friend1Name: TextView
    private lateinit var friend2Distance: TextView
    private lateinit var friend2Name: TextView
    private lateinit var friend3Distance: TextView
    private lateinit var friend3Name: TextView
    private lateinit var friend4Distance: TextView
    private lateinit var friend4Name: TextView

    private var mMap: GoogleMap? = null
    private lateinit var friendCard: MaterialCardView
    private val database = FirebaseDatabase.getInstance().reference
    private val friendMarkers = mutableMapOf<String, Marker>()
    private var currentEventId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friend_map, container, false)
        friend1Distance = view.findViewById(R.id.friend1_distance)
        friend1Name = view.findViewById(R.id.friend1_name)
        friend2Distance = view.findViewById(R.id.friend2_distance)
        friend2Name = view.findViewById(R.id.friend2_name)
        friend3Distance = view.findViewById(R.id.friend3_distance)
        friend3Name = view.findViewById(R.id.friend3_name)
        friend4Distance = view.findViewById(R.id.friend4_distance)
        friend4Name = view.findViewById(R.id.friend4_name)
        // Get current event ID from arguments or intent
        currentEventId = arguments?.getString("EVENT_ID")

        // Handle system insets for full-screen experience
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply top inset to back button and zoom buttons, let map fill entire screen
            view.findViewById<ImageButton>(R.id.back_button).setPadding(12, systemBars.top + 12, 12, 12)
            view.findViewById<View>(R.id.zoom_buttons).setPadding(12, systemBars.top + 12, 12, 12)
            insets
        }

        // Back button - properly navigate back to home and update nav bar
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            // Get reference to the activity and call the navigation method
            (activity as? HorizontalCoordinator)?.navigateToHome()
        }

        // Initialize friend card
        friendCard = view.findViewById(R.id.friend_list_card)

        // Apply blur and transparency safely
        val friendBg = view.findViewById<View>(R.id.friend_list_bg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            friendBg.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
            )
        }
        friendCard.setCardBackgroundColor(Color.parseColor("#88FFFFFF"))

        // Map fragment (childFragmentManager because this is inside a fragment)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Zoom buttons
        view.findViewById<ImageButton>(R.id.zoom_in).setOnClickListener {
            mMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        view.findViewById<ImageButton>(R.id.zoom_out).setOnClickListener {
            mMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        view.findViewById<ImageButton>(R.id.zoom_normal).setOnClickListener {
            mMap?.animateCamera(CameraUpdateFactory.zoomTo(15f))
        }

        // Open compass
        view.findViewById<Button>(R.id.btn_compass).setOnClickListener {
            val intent = Intent(requireContext(), Compass::class.java)
            currentEventId?.let { eventId ->
                intent.putExtra("EVENT_ID", eventId)
            }
            startActivity(intent)
        }

        // Setup friend item click listeners
        setupFriendClickListeners(view)

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )

            if (!success) {
                Log.e("FriendMap", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("FriendMap", "Can't find style. Error: ", e)
        }

        // Enable current location
        enableMyLocation()

        // Start listening for friend locations
        setupFriendLocationListener()
    }

    private fun enableMyLocation() {
        try {
            mMap?.isMyLocationEnabled = true

            // Default location (Johannesburg) as fallback
            val defaultLocation = LatLng(-26.2041, 28.0473)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        } catch (e: SecurityException) {
            Log.e("FriendMap", "Location permission not granted", e)
        }
    }

    private fun setupFriendLocationListener() {
        database.child("users_location").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateFriendLocations(snapshot)
                updateFriendDistances(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendMap", "Error loading friend locations: ${error.message}")
            }
        })
    }

    private fun updateFriendLocations(snapshot: DataSnapshot) {
        // Clear existing markers
        friendMarkers.values.forEach { it.remove() }
        friendMarkers.clear()

        snapshot.children.forEach { userLocationSnapshot ->
            val userId = userLocationSnapshot.key
            val locationData = userLocationSnapshot.getValue(LocationData::class.java)

            // Skip invalid locations
            if (userId != null && locationData != null && isUserAtEvent(userId)) {
                val friendLocation = LatLng(locationData.latitude, locationData.longitude)

                // Add marker for friend
                val marker = mMap?.addMarker(
                    MarkerOptions()
                        .position(friendLocation)
                        .title("Friend")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                )

                marker?.let {
                    friendMarkers[userId] = it
                }
            }
        }
    }

    private fun updateFriendDistances(snapshot: DataSnapshot) {
        val currentLocation = mMap?.myLocation
        if (currentLocation == null) {
            Log.d("FriendMap", "Current location not available yet")
            return
        }

        val currentUserLocation = Location("current").apply {
            latitude = currentLocation.latitude
            longitude = currentLocation.longitude
        }

        val friendLocations = mutableListOf<Pair<String, LocationData>>()

        snapshot.children.forEach { userLocationSnapshot ->
            val userId = userLocationSnapshot.key
            val locationData = userLocationSnapshot.getValue(LocationData::class.java)
            if (userId != null && locationData != null && isUserAtEvent(userId)) {
                friendLocations.add(userId to locationData)
            }
        }

        // Update UI for each friend
        friendLocations.forEachIndexed { index, (userId, locationData) ->
            val friendLocation = Location("friend").apply {
                latitude = locationData.latitude
                longitude = locationData.longitude
            }

            val distance = currentUserLocation.distanceTo(friendLocation)
            updateFriendDistanceUI(index, distance, userId)
        }
    }

    private fun updateFriendDistanceUI(friendIndex: Int, distance: Float, userId: String) {
        val distanceText = if (distance < 1000) {
            "${distance.toInt()} m"
        } else {
            "${String.format("%.1f", distance / 1000)} km"
        }

        when (friendIndex) {
            0 -> {
                friend1Distance.text = distanceText
                friend1Name.text = getFriendName(userId)
            }
            1 -> {
                friend2Distance.text = distanceText
                friend2Name.text = getFriendName(userId)
            }
            2 -> {
                friend3Distance.text = distanceText
                friend3Name.text = getFriendName(userId)
            }
            3 -> {
                friend4Distance.text = distanceText
                friend4Name.text = getFriendName(userId)
            }
        }
    }


    private fun setupFriendClickListeners(view: View) {
        view.findViewById<View>(R.id.friend1_layout)?.setOnClickListener {
            centerOnFriend(0)
        }
        view.findViewById<View>(R.id.friend2_layout)?.setOnClickListener {
            centerOnFriend(1)
        }
        view.findViewById<View>(R.id.friend3_layout)?.setOnClickListener {
            centerOnFriend(2)
        }
        view.findViewById<View>(R.id.friend4_layout)?.setOnClickListener {
            centerOnFriend(3)
        }
    }

    private fun centerOnFriend(friendIndex: Int) {
        if (friendIndex < friendMarkers.size) {
            val marker = friendMarkers.values.elementAtOrNull(friendIndex)
            marker?.let {
                mMap?.animateCamera(CameraUpdateFactory.newLatLng(it.position))
            }
        }
    }

    private fun isUserAtEvent(userId: String): Boolean {
        // For now, return true for all users
        // Later implement event-based filtering using currentEventId
        return true
    }

    private fun getFriendName(userId: String): String {
        // Implement logic to get friend name from user ID
        // This could query Firebase users collection
        return when (userId) {
            "friend1_id" -> "Zalano"
            "friend2_id" -> "Mpho"
            "friend3_id" -> "Neo"
            "friend4_id" -> "Derrick"
            else -> "Friend"
        }
    }

    private fun findViewById(textViewId: Int): TextView? {
        return view?.findViewById(textViewId)
    }

    // Data class for Firebase location data
    data class LocationData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timestamp: Long = 0
    )

    companion object {
        fun newInstance(eventId: String? = null): FriendMapFragment {
            return FriendMapFragment().apply {
                arguments = Bundle().apply {
                    putString("EVENT_ID", eventId)
                }
            }
        }
    }
}
