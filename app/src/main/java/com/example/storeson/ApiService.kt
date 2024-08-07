package com.example.storeson

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/Notification/send")
    fun sendData(@Body data: String): Call<Void>
}

