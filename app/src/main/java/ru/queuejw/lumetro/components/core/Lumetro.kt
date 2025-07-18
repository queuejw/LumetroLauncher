package ru.queuejw.lumetro.components.core

import android.app.Application
import com.google.android.material.color.DynamicColors
import ru.queuejw.lumetro.components.core.error.CriticalErrorDetector
import ru.queuejw.lumetro.components.prefs.Prefs

class Lumetro : Application() {

    private var criticalErrorDetector: CriticalErrorDetector? = null

    override fun onCreate() {
        val prefs = Prefs(this)
        if (prefs.dynamicColorEnabled) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        criticalErrorDetector = CriticalErrorDetector(applicationContext)
        Thread.setDefaultUncaughtExceptionHandler(criticalErrorDetector)
        super.onCreate()
    }

    companion object {
        var isOtherAppOpened = false
        var viewPagerUserInputEnabled = false
    }
}