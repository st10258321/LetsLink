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
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.letslink.R
import com.example.letslink.nav.HorizontalCoordinator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Compass : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var currentDegree = 0f
    private lateinit var compass: ImageView
    private lateinit var friend1: ImageView
    private lateinit var friend2: ImageView
    private lateinit var friend3: ImageView

    private val database = FirebaseDatabase.getInstance().reference
    private var currentEventId: String? = null
    private val friendBearings = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)

        compass = findViewById(R.id.compass)
        friend1 = findViewById(R.id.friend4)
        friend2 = findViewById(R.id.friend5)
        friend3 = findViewById(R.id.friend3)

        // Get event ID from intent
        currentEventId = intent.getStringExtra("EVENT_ID")

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

        // Start listening for friend locations
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

        // Update friend positions based on real bearings
        updateFriendPositions()
    }

    private fun setupFriendLocationListener() {
        database.child("users_location").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                calculateFriendBearings(snapshot)
                updateFriendPositions()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Compass", "Error loading friend locations: ${error.message}")
            }
        })
    }

    private fun calculateFriendBearings(snapshot: DataSnapshot) {
        // For demo purposes, using simulated current location
        // In real app, get this from Location Services
        val currentLocation = Location("current").apply {
            latitude = -26.2041 // Johannesburg latitude
            longitude = 28.0473 // Johannesburg longitude
        }

        friendBearings.clear()

        snapshot.children.forEachIndexed { index, userLocationSnapshot ->
            val userId = userLocationSnapshot.key
            val locationData = userLocationSnapshot.getValue(LocationData::class.java)

            if (userId != null && locationData != null && isUserAtEvent(userId)) {
                val friendLocation = Location("friend").apply {
                    latitude = locationData.latitude
                    longitude = locationData.longitude
                }

                // Calculate bearing to friend
                val bearing = currentLocation.bearingTo(friendLocation)
                friendBearings[userId] = bearing

                Log.d("Compass", "Friend $userId bearing: $bearing")
            }
        }
    }

    private fun updateFriendPositions() {
        val friendViews = listOf(friend1, friend2, friend3)
        val radius = compass.width / 2.5f

        friendBearings.values.forEachIndexed { index, bearing ->
            if (index < friendViews.size) {
                placeFriend(friendViews[index], bearing, radius)
            }
        }

        // If no real friends, show demo friends
        if (friendBearings.isEmpty()) {
            placeFriend(friend1, 45f, radius)
            placeFriend(friend2, 120f, radius)
            placeFriend(friend3, 250f, radius)
        }
    }

    private fun placeFriend(friend: ImageView, bearing: Float, radius: Float) {
        val centerX = compass.x + compass.width / 2
        val centerY = compass.y + compass.height / 2

        // Adjust bearing based on current compass orientation
        val adjustedBearing = (bearing - currentDegree) % 360

        val rad = Math.toRadians(adjustedBearing.toDouble())
        val x = (centerX + radius * Math.cos(rad)).toFloat()
        val y = (centerY + radius * Math.sin(rad)).toFloat()

        friend.x = x - friend.width / 2
        friend.y = y - friend.height / 2
    }

    private fun isUserAtEvent(userId: String): Boolean {
        // For now, return true for all users
        // Later implement event-based filtering using currentEventId
        return true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Data class for Firebase location data
    data class LocationData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timestamp: Long = 0
    )
}
