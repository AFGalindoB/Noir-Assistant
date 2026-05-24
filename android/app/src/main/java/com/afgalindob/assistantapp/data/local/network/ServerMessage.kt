package com.afgalindob.assistantapp.data.local.network

data class ServerMessage(
    val title: String,
    val description: String,
    val isError: Boolean = false
)