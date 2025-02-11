package ru.queuejw.mpl

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import com.google.android.material.color.DynamicColors
import ru.queuejw.mpl.content.bsod.BsodDetector
import ru.queuejw.mpl.content.data.Prefs
import ru.queuejw.mpl.helpers.utils.Utils

class Application : Application() {

    override fun onCreate() {
        BsodDetector.setContext(applicationContext)
        Thread.setDefaultUncaughtExceptionHandler(BsodDetector())
        PREFS = Prefs(applicationContext)
        if (PREFS.accentColor == 21 && DynamicColors.isDynamicColorAvailable()) DynamicColors.applyToActivitiesIfAvailable(
            this
        )
        setupFonts()
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            @SuppressLint("SourceLockedOrientationActivity")
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.setTheme(Utils.launcherAccentTheme())
                when (PREFS.orientation) {
                    "p" -> activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT

                    "l" -> activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                super.onActivityPreCreated(activity, savedInstanceState)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        lateinit var PREFS: Prefs

        var isUpdateDownloading = false
        var isAppOpened = false
        var isStartMenuOpened = false

        var customFont: Typeface? = null
        var customLightFont: Typeface? = null
        var customBoldFont: Typeface? = null

        fun setupFonts() {
            customFont = Utils.getCustomFont()
            customLightFont = Utils.getCustomLightFont()
            customBoldFont = Utils.getCustomBoldFont()
        }

    }
}
