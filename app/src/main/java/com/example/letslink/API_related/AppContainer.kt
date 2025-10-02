package com.example.letslink.API_related

import com.example.letslink.API_related.RetrofitClient
import com.example.letslink.SessionManager
import kotlin.getValue
import com.example.letslink.API_related.GroupRepo
import com.example.letslink.API_related.LetsLinkAPI
import android.content.Context
import com.example.letslink.local_database.GroupDao
import com.example.letslink.local_database.LetsLinkDB

/**
 * This class defines what is needed
 */
class AppContainer(applicationContext: Context) {

   //sessionManger
     val sessionManager: SessionManager by lazy {
       SessionManager(applicationContext)
   }

    // Database
    val database: LetsLinkDB by lazy {
        // Initialize Room here
        LetsLinkDB.getDatabase(applicationContext)
    }

    private val groupDao: GroupDao by lazy {
        database.groupDao()
    }

    // Network Dependencies (Retrofit Only)
    // configuration that provides the API interface.
    private val groupApiService: LetsLinkAPI by lazy {
        RetrofitClient.letsLinkAPI
    }

    //3. Repo /The Bridge group repo acts as a bridge between the app and api
    val groupRepository: GroupRepo by lazy {
        GroupRepo(
            groupDao = groupDao,
            groupApiService = groupApiService,
            sessionManager = sessionManager
        )
    }
}
