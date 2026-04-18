package com.example.carrentingtest.utils

import com.example.carrentingtest.BuildConfig
import com.example.carrentingtest.network.BackendCallback
import com.example.carrentingtest.network.BackendClient
import org.json.JSONObject

class GeminiHelper {
    fun generateRecommendation(userPrompt: String, inventoryContext: String, callback: RecommendationCallback) {
        val payload = JSONObject()
            .put("query", userPrompt)
            .put("inventoryContext", inventoryContext)

        BackendClient.postJson(BuildConfig.CONCIERGE_ENDPOINT_PATH, payload, object : BackendCallback {
            override fun onSuccess(response: JSONObject) {
                val recommendation = response.optString("recommendation")
                if (recommendation.isNullOrBlank()) {
                    callback.onFailure(IllegalStateException("Empty concierge response"))
                    return
                }
                callback.onSuccess(recommendation)
            }

            override fun onError(errorMessage: String) {
                callback.onFailure(IllegalStateException(errorMessage))
            }
        })
    }

    interface RecommendationCallback {
        fun onSuccess(result: String)
        fun onFailure(t: Throwable)
    }
}
