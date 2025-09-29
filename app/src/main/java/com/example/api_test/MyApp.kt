package com.example.api_test


import android.app.Application

/**
 *  class to hold the global AppContainer instance.
 */
class MyApp : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        // Initialize the container immediately when the app starts
        container = AppContainer(applicationContext = applicationContext)
    }
}
