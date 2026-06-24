package com.example.logist_tech.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://38.250.116.214:8080/"

    private val cliente = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()

    val api: LogistTechApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LogistTechApi::class.java)
    }
}