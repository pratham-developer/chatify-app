package com.pratham.chatify

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    // Endpoint to verify PIN
    @POST("/api/chatroom/verify-pin")
    fun verifyPin(@Body pinRequest: PinRequest): Call<PinResponse>
}

data class PinRequest(val pin: String)

data class PinResponse(val valid: Boolean)
