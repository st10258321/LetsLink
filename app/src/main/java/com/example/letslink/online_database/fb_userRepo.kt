package com.example.letslink.online_database
import com.example.letslink.model.User
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging


class fb_userRepo(
    private val auth:FirebaseAuth = FirebaseAuth.getInstance(),
    private val database : DatabaseReference = com.google.firebase.database.FirebaseDatabase.getInstance().reference
) {
    fun register(user : User, callback :(Boolean, String?, User?) -> Unit){
        auth.createUserWithEmailAndPassword(user.email,user.password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid
                    FirebaseMessaging.getInstance().token.addOnCompleteListener{tokenTask ->
                        if(tokenTask.isSuccessful){
                            user.fcmToken = tokenTask.result ?: ""
                        }
                    }
                database.child("users").child(uid!!).setValue(user)
                    .addOnCompleteListener{ task ->
                        if(task.isSuccessful){
                            callback(true,null,user)
                        }else{
                            callback(false,task.exception?.message,null)
                        }
                    }
                }else{
                    callback(false,task.exception?.message,null)
                    }
            }

    }

}