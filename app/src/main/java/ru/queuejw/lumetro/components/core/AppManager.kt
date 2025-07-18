package ru.queuejw.lumetro.components.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import ru.queuejw.lumetro.components.adapters.viewtypes.AppViewTypes
import ru.queuejw.lumetro.components.core.Lumetro.Companion.isOtherAppOpened
import ru.queuejw.lumetro.model.Alphabet
import ru.queuejw.lumetro.model.App

class AppManager() {

    fun getInstalledApps(
        context: Context,
        getOnlyApps: Boolean = false
    ): MutableList<App> {
        val packageManager = context.packageManager
        val tempList = ArrayList<App>()
        val intent = Intent(Intent.ACTION_MAIN, null).also {
            it.addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val intents = packageManager.queryIntentActivities(intent, 0).also {
            it.sortBy { item ->
                item.loadLabel(packageManager).toString()
            }
        }
        for (i in intents) {
            val item = App(
                i.loadLabel(packageManager).toString(),
                i.activityInfo.packageName,
                0
            )
            if (item.mPackage == context.packageName) continue
            tempList.add(item)

        }
        if (getOnlyApps) return tempList

        val (apps, otherApps) = tempList.partition { app ->
            app.mName.first().let { it in 'A'..'Z' || it in 'a'..'z' }
        }

        val groupedApps = apps
            .groupBy { it.mName.first().lowercase() }
            .toSortedMap()

        val result = mutableListOf<App>()

        groupedApps.forEach { (letter, appsInGroup) ->
            result.apply {
                add(App(letter, null, AppViewTypes.TYPE_HEADER.type))
                addAll(appsInGroup)
            }
        }
        if (otherApps.isNotEmpty()) {
            result.apply {
                add(App("#", null, AppViewTypes.TYPE_HEADER.type))
                addAll(otherApps)
            }
        }
        return result
    }

    fun getAlphabet(apps: MutableList<App>): MutableList<Alphabet> {
        val result = mutableListOf<Alphabet>()
        apps.forEachIndexed { index, item ->
            if (item.viewType == AppViewTypes.TYPE_HEADER.type) {
                result.add(Alphabet(item.mName, index))
            }
        }
        return result
    }

    companion object {
        fun launchApp(mPackage: String?, context: Context): Boolean {
            if (mPackage != null) {
                try {
                    isOtherAppOpened = true
                    when (mPackage) {
                        else -> context.startActivity(
                            Intent(
                                context.packageManager.getLaunchIntentForPackage(
                                    mPackage
                                )
                            )
                        )
                    }
                    return true
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    return false
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    return false
                }
            } else {
                return false
            }
        }
    }
}