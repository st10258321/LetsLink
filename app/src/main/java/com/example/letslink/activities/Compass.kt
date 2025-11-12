package com.example.letslink.activities

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.letslink.R
import com.example.letslink.nav.HorizontalCoordinator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Compass : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var currentDegree = 0f
    private lateinit var compass: ImageView
    private lateinit var friend1: ImageView
    private lateinit var friend2: ImageView
    private lateinit var friend3: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userLocation: Location? = null
    private var friendLocations = mutableMapOf<String, LatLng>()

    // Simple data class for LatLng since we don't have Maps SDK here
    data class LatLng(val latitude: Double, val longitude: Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)

        compass = findViewById(R.id.compass)
        friend1 = findViewById(R.id.friend4)
        friend2 = findViewById(R.id.friend5)
        friend3 = findViewById(R.id.friend3)

        // Pulse compass
        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            compass,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f, 1f),
            PropertyValuesHolder.ofFloat("alpha", 1f, 0.85f, 1f)
        )
        pulse.duration = 1000
        pulse.repeatCount = ValueAnimator.INFINITE
        pulse.start()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            val intent = Intent(this, HorizontalCoordinator::class.java)
            intent.putExtra("showFragment", "map")
            startActivity(intent)
            finish()
        }

        // Load user location and friend locations
        loadUserLocation()
        setupFriendLocationListener()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun loadUserLocation() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users_location")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")
                        if (lat != null && lng != null) {
                            userLocation = Location("user").apply {
                                latitude = lat
                                longitude = lng
                            }
                        }
                    }
                }
        }
    }

    private fun setupFriendLocationListener() {
        // Listen for friend locations
        db.collection("users_location")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.documents?.forEach { document ->
                    val userId = document.id
                    if (userId != auth.currentUser?.uid) { // Exclude current user
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")
                        if (lat != null && lng != null) {
                            friendLocations[userId] = LatLng(lat, lng)
                        }
                    }
                }
            }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val degree = Math.round(event.values[0]).toFloat()

        // Rotate compass base
        val rotateAnimation = RotateAnimation(
            currentDegree,
            -degree,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        rotateAnimation.duration = 210
        rotateAnimation.fillAfter = true
        compass.startAnimation(rotateAnimation)

        currentDegree = -degree

        // Place friends based on real bearings
        placeFriendsBasedOnRealLocations()
    }

    private fun placeFriendsBasedOnRealLocations() {
        if (userLocation == null || friendLocations.isEmpty()) {
            // Use example positions if no real data
            placeFriend(friend1, 45)
            placeFriend(friend2, 120)
            placeFriend(friend3, 250)
            return
        }

        val friends = listOf(friend1, friend2, friend3)
        val friendLocationsList = friendLocations.values.take(3)

        friendLocationsList.forEachIndexed { index, friendLatLng ->
            if (index < friends.size) {
                val bearing = calculateBearing(
                    userLocation!!.latitude,
                    userLocation!!.longitude,
                    friendLatLng.latitude,
                    friendLatLng.longitude
                )
                placeFriend(friends[index], bearing.toInt())
            }
        }

        // Fill remaining slots with example positions if needed
        if (friendLocationsList.size < 3) {
            val exampleAngles = listOf(45, 120, 250)
            for (i in friendLocationsList.size until 3) {
                placeFriend(friends[i], exampleAngles[i])
            }
        }
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLonRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad)
        val bearingRad = atan2(y, x)

        return Math.toDegrees(bearingRad).toFloat()
    }

    private fun placeFriend(friend: ImageView, angle: Int) {
        val radius = compass.width / 2.5f
        val centerX = compass.x + compass.width / 2
        val centerY = compass.y + compass.height / 2

        val rad = Math.toRadians(angle.toDouble())
        val x = (centerX + radius * Math.cos(rad)).toFloat()
        val y = (centerY + radius * Math.sin(rad)).toFloat()

        friend.x = x - friend.width / 2
        friend.y = y - friend.height / 2
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}