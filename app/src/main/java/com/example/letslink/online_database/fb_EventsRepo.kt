package com.example.letslink.online_database

import com.example.letslink.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class fb_EventsRepo(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database : DatabaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().reference
) {
    fun createEvent(title:String, description:String, location:String, startTime:String, endTime:String, date:String , callback :(Boolean) -> Unit){
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val eventId = database.child("events").push().key ?: ""
            val event =
                Event(eventId, userId, title, description, location, startTime, endTime, date)
            database.child("events").child(eventId).setValue(event)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        callback(true)
                    }else{
                        callback(false)
                    }
                }
        }
    }
}