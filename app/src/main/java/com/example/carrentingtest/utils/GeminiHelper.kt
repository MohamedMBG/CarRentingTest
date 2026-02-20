package com.example.carrentingtest.utils

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeminiHelper {
    private val apiKey = "AIzaSyC2IIXmAoUt511zuU7JaZkg18HdTyNG0bY" // Replace with actual key
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    fun generateRecommendation(userPrompt: String, callback: RecommendationCallback) {
        val fullPrompt = "You are a helpful car rental concierge. " +
                "Recommend a car from our inventory based on this request: $userPrompt. " +
                "Keep your answer short and friendly."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = model.generateContent(fullPrompt)
                withContext(Dispatchers.Main) {
                    response.text?.let {
                        callback.onSuccess(it)
                    } ?: callback.onFailure(Exception("Empty response"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onFailure(e)
                }
            }
        }
    }

    interface RecommendationCallback {
        fun onSuccess(result: String)
        fun onFailure(t: Throwable)
    }
}
