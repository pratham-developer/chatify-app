package com.pratham.chatify



data class Message(
    val text: String,
    val senderName: String,
    val senderId: String,
    val timestamp: String
)