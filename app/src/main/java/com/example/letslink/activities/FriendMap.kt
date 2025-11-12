package com.example.letslink.fragments

import android.content.Intent
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
import android.widget.Button
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.letslink.R
import com.example.letslink.activities.Compass
import com.example.letslink.nav.HorizontalCoordinator
import com.example.letslink.utils.LocationPermissionHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange

class FriendMapFragment : Fragment(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var friendCard: MaterialCardView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var locationListener: ListenerRegistration? = null
    private var userMarkers = mutableMapOf<String, Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_friend_map, container, false)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.findViewById<ImageButton>(R.id.back_button)
                .setPadding(12, systemBars.top + 12, 12, 12)
            view.findViewById<View>(R.id.zoom_buttons)
                .setPadding(12, systemBars.top + 12, 12, 12)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        // Back button
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            (activity as? HorizontalCoordinator)?.navigateToHome()
        }

        // Friend card style
        friendCard = view.findViewById(R.id.friend_list_card)
        val friendBg = view.findViewById<View>(R.id.friend_list_bg)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            friendBg.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
        }
        friendCard.setCardBackgroundColor(Color.parseColor("#88FFFFFF"))

        // Map fragment
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
            startActivity(Intent(requireContext(), Compass::class.java))
        }

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success) Log.e("FriendMap", "Map style parsing failed.")
        } catch (e: Resources.NotFoundException) {
            Log.e("FriendMap", "Map style error: ", e)
        }

        if (LocationPermissionHelper.hasLocationPermission(requireContext())) {
            setupRealTimeLocationListening()
            centerOnCurrentUser()
        } else {
            LocationPermissionHelper.requestLocationPermission(requireActivity())
        }
    }

    private fun setupRealTimeLocationListening() {
        locationListener = db.collection("users_location")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FriendMap", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    val doc = dc.document
                    val userId = doc.id
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            val lat = doc.getDouble("latitude")
                            val lng = doc.getDouble("longitude")
                            val ts = doc.getLong("timestamp")
                            if (lat != null && lng != null) updateUserMarker(userId, lat, lng, ts)
                        }
                        DocumentChange.Type.REMOVED -> {
                            userMarkers.remove(userId)?.remove()
                        }
                    }
                }
            }
    }

    private fun updateUserMarker(userId: String, lat: Double, lng: Double, timestamp: Long?) {
        val location = LatLng(lat, lng)
        val existingMarker = userMarkers[userId]
        if (existingMarker != null) {
            existingMarker.position = location
        } else {
            val hue = if (userId == auth.currentUser?.uid)
                BitmapDescriptorFactory.HUE_BLUE
            else BitmapDescriptorFactory.HUE_ORANGE

            val markerOptions = MarkerOptions()
                .position(location)
                .title("User: ${userId.take(8)}")
                .icon(BitmapDescriptorFactory.defaultMarker(hue))

            mMap?.addMarker(markerOptions)?.let { marker ->
                userMarkers[userId] = marker
            }
        }
    }

    private fun centerOnCurrentUser() {
        val currentUser = auth.currentUser ?: return
        db.collection("users_location").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    if (lat != null && lng != null) {
                        val userLocation = LatLng(lat, lng)
                        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        updateUserMarker(currentUser.uid, lat, lng, null)
                        return@addOnSuccessListener
                    }
                }
                // Default location (Johannesburg)
                val fallback = LatLng(-26.2041, 28.0473)
                mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 15f))
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationListener?.remove()
    }
}
