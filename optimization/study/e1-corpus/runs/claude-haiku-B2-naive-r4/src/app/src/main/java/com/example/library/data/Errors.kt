package com.example.library.data

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException

fun Throwable.isUnauthorized(): Boolean = this is HttpException && code() == 401

/** Turns a failure into a message that is safe to show to the member. */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> "The library server returned an error (HTTP ${code()}). Please try again."
    is IOException -> "Cannot reach the library server. Check your connection and try again."
    is JsonParseException -> "The library server sent an unexpected response."
    else -> "Something went wrong. Please try again."
}
