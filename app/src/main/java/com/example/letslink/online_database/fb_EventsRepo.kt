package com.example.letslink.online_database

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.letslink.local_database.LetsLinkDB
import com.example.letslink.local_database.UserDao
import com.example.letslink.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class fb_EventsRepo(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private  val userDao : UserDao = LetsLinkDB.getDatabase(context).userDao()
    fun createEvent(title:String, description:String, location:String, startTime:String, endTime:String, date:String,userid : String , callback :(Boolean) -> Unit){
        val eventId = database.child("events").push().key ?: ""
        val event =
            Event(eventId, userid, title, description, location, startTime, endTime, date)
        database.child("events").child(eventId).setValue(event)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    callback(true)
                }else{
                    callback(false)
                }
            }
    }
    suspend fun getEventsThatBelongToUser(): List<Event>{
        val user = auth.currentUser
        var events = mutableListOf<Event>()
        if(user != null){
            val userEmail = user.email
            val localUser = userDao.getUserByEmail(userEmail!!)
            if(localUser != null){
                val userId = localUser.userId
                val snapshot = database.child("events")
                    .orderByChild("ownerId")
                    .equalTo(userId)
                    .get()
                    .await()

                for(child in snapshot.children){
                    Log.d("__fb__--", "Child key: ${child.key}")
                    val event = child.getValue(Event::class.java)
                    if(event != null){
                        events.add(event)
                    }
                }
            }
        }

        return events
    }
}