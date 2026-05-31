package com.example.tagspotter

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class TagSpotterTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestTagSpotterApplication::class.java.name, context)
    }
}
