package net.maiatoday.spotcache

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class SpotCacheTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TestSpotCacheApplication::class.java.name, context)
    }
}
