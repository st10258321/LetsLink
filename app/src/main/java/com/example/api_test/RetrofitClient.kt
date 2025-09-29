package com.example.api_test

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * object to configure and provide the Retrofit client and API services.
 */
object RetrofitClient {

   //link for the api later on
    private const val BASE_URL = "https://letslink-api.onrender.com/"

    // Initialize Retrofit
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Use Gson to convert Kotlin data classes to JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val letsLinkAPI: LetsLinkAPI by lazy {
        retrofit.create(LetsLinkAPI::class.java)
    }
}
