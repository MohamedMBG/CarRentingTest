package com.example.carrentingtest.verification.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object Analytics {
    fun log(context: Context, event: String, params: Bundle = Bundle()) {
        FirebaseAnalytics.getInstance(context).logEvent(event, params)
    }
}


