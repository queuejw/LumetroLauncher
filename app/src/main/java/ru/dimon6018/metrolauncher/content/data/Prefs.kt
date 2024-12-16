package ru.dimon6018.metrolauncher.content.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Needed to store/modify MPL settings
 * @param context Context
 */
class Prefs(context: Context) {

    val prefs: SharedPreferences

    private var prefsChanged = false

    private val fileName = "Prefs"

    private val accentColorPref = "accentColor"
    private val maxCrashLogsPref = "maxCrashLogs"
    private val updateStatePref = "updateState"
    private val updatePercentagePref = "updateLevel"
    private val autoUpdatePref = "autoUpdateEnabled"
    private val updateMessagePref = "updateMessage"
    private val versionCodePref = "versionCode"
    private val updateNotificationPref = "updateNotificationEnabled"
    private val feedbackPref = "feedbackEnabled"
    private val launcherStatePref = "launcherState"
    private val lightThemePref = "launcherTheme"
    private val navbarColorPref = "navBarColor"
    private val iconPackPref = "iconPack"
    private val autoPinPref = "autoPinApp"
    private val settingsBtnPref = "allAppsSettingsBtnEnabled"
    private val tilesTransparencyPref = "tilesTransparency"
    private val searchBarPref = "searchBarEnabled"
    private val bottomBarIconPref = "bottomBarIcon"
    private val startBlockPref = "isStartBlocked"
    private val transitionAnimationPref = "transitionAnim"
    private val tilesAnimationsPref = "tileAnim"
    private val allAppsAnimationsPref = "allAppsAnim"
    private val liveTilesAnimPref = "liveTileAnim"
    private val searchBarMaxResultsPref = "maxResultsSearchBar"
    private val allAppsScreenPref = "allAppsEnabled"
    private val autoShutdownAnimPref = "autoStdwnAnimations"
    private val bsodOutputPref = "bsodOutputEnabled"
    private val keyboardSearchPref = "showKeyboardWhenSearching"
    private val keyboardAutoSearchPref = "showKeyboardWhenOpeningAllApps"
    private val orientationPref = "orientation"
    private val coloredStrokePref = "coloredStroke"
    private val customFontPref = "isCustomFontChosen"
    private val customFontNamePref = "customFontName"
    private val customFontPathPref = "customFontPath"
    private val customLightFontPathPref = "customLightFontPath"
    private val customLightFontNamePref = "customLightFontName"
    private val customBoldFontPathPref = "customBoldFontPath"
    private val customBoldFontNamePref = "customBoldFontName"
    private val allAppsKeyboardGoActionPref = "allAppsKeyboardActionEnabled"

    init {
        prefs = context.getSharedPreferences(fileName, 0)
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    var maxCrashLogs: Int
        get() = prefs.getInt(maxCrashLogsPref, 1)
        set(value) = prefs.edit().putInt(maxCrashLogsPref, value).apply()

    var appTheme: Int
        get() = prefs.getInt(lightThemePref, 0) // 0 - auto, 1 - dark, 2 - light
        set(value) = prefs.edit().putInt(lightThemePref, value).apply()

    var isFeedbackEnabled: Boolean
        get() = prefs.getBoolean(feedbackPref, true)
        set(value) = prefs.edit().putBoolean(feedbackPref, value).apply()

    var updateState: Int
        get() = prefs.getInt(updateStatePref, 0)
        // 0 - nothing
        // 1 - checking
        // 2 - downloading
        // 3 - up-to-date
        // 4 - ready to install
        // 5 - failed
        // 6 - ready to download
        // 7 - ready to download (beta ver)
        set(value) = prefs.edit().putInt(updateStatePref, value).apply()

    var updateProgressLevel: Int
        get() = prefs.getInt(updatePercentagePref, 0)
        set(value) = prefs.edit().putInt(updatePercentagePref, value).apply()

    var isAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean(autoUpdatePref, false)
        set(value) = prefs.edit().putBoolean(autoUpdatePref, value).apply()

    var isUpdateNotificationEnabled: Boolean
        get() = prefs.getBoolean(updateNotificationPref, false)
        set(value) = prefs.edit().putBoolean(updateNotificationPref, value).apply()

    // is only used to save update data
    var updateMessage: String
        get() = prefs.getString(updateMessagePref, "")!!
        set(value) = prefs.edit().putString(updateMessagePref, value).apply()

    // is only used to save update data
    var versionCode: Int
        get() = prefs.getInt(versionCodePref, 0)
        set(value) = prefs.edit().putInt(versionCodePref, value).apply()

    var accentColor: Int
        get() = prefs.getInt(accentColorPref, 5)
        set(color) {
            // 0 - lime
            // 1 - green
            // 2 - emerald
            // 3 - cyan
            // 4 - teal
            // 5 - cobalt
            // 6 - indigo
            // 7 - violet
            // 8 - pink
            // 9 - magenta
            // 10 - crimson
            // 11 - red
            // 12 - orange
            // 13 - amber
            // 14 - yellow
            // 15 - brown
            // 16 - olive
            // 17 - steel
            // 18 - mauve
            // 19 - taupe
            // 20 - dynamic colors
            prefsChanged = true
            prefs.edit()
                .putInt("previous_accent_color", prefs.getInt(accentColorPref, 5))
                .apply()
            prefs.edit()
                .putInt(accentColorPref, color)
                .apply()
        }
    var launcherState: Int
        get() = prefs.getInt(launcherStatePref, 0)
        // 0 - OOBE
        // 1 - nothing
        // 2 - OOBE - selecting the type of settings (recommended or customize)
        // 3 - waiting for reset (after recovery)
        set(value) = prefs.edit().putInt(launcherStatePref, value).apply()

    var navBarColor: Int
        get() = prefs.getInt(navbarColorPref, 4)
        set(value) = prefs.edit().putInt(navbarColorPref, value).apply()

    var iconPackPackage: String?
        get() = prefs.getString(iconPackPref, "null")
        set(value) = prefs.edit().putString(iconPackPref, value).apply()

    var isPrefsChanged: Boolean
        get() = prefsChanged
        set(value) {
            prefsChanged = value
        }
    var pinNewApps: Boolean
        get() = prefs.getBoolean(autoPinPref, false)
        set(value) = prefs.edit().putBoolean(autoPinPref, value).apply()

    var isSettingsBtnEnabled: Boolean
        get() = prefs.getBoolean(settingsBtnPref, false)
        set(value) = prefs.edit().putBoolean(settingsBtnPref, value).apply()

    var tilesTransparency: Float
        get() = prefs.getFloat(tilesTransparencyPref, 1.0f)
        set(value) = prefs.edit().putFloat(tilesTransparencyPref, value).apply()

    var navBarIconValue: Int
        get() = prefs.getInt(bottomBarIconPref, 0)
        // 0 - windows (default)
        // 1 - windows old
        // 2 - android
        set(value) = prefs.edit().putInt(bottomBarIconPref, value).apply()

    var isSearchBarEnabled: Boolean
        get() = prefs.getBoolean(searchBarPref, false)
        set(value) = prefs.edit().putBoolean(searchBarPref, value).apply()

    var isStartBlocked: Boolean
        get() = prefs.getBoolean(startBlockPref, false)
        set(value) = prefs.edit().putBoolean(startBlockPref, value).apply()

    var isTransitionAnimEnabled: Boolean
        get() = prefs.getBoolean(transitionAnimationPref, true)
        set(value) = prefs.edit().putBoolean(transitionAnimationPref, value).apply()

    var isTilesAnimEnabled: Boolean
        get() = prefs.getBoolean(tilesAnimationsPref, true)
        set(value) = prefs.edit().putBoolean(tilesAnimationsPref, value).apply()

    var isLiveTilesAnimEnabled: Boolean
        get() = prefs.getBoolean(liveTilesAnimPref, true)
        set(value) = prefs.edit().putBoolean(liveTilesAnimPref, value).apply()

    var isAAllAppsAnimEnabled: Boolean
        get() = prefs.getBoolean(allAppsAnimationsPref, true)
        set(value) = prefs.edit().putBoolean(allAppsAnimationsPref, value).apply()

    var maxResultsSearchBar: Int
        get() = prefs.getInt(searchBarMaxResultsPref, 4)
        set(value) = prefs.edit().putInt(searchBarMaxResultsPref, value).apply()

    var isAllAppsEnabled: Boolean
        get() = prefs.getBoolean(allAppsScreenPref, true)
        set(value) = prefs.edit().putBoolean(allAppsScreenPref, value).apply()

    //disabling animations if developer mode is enabled (to avoid problems)
    var isAutoShutdownAnimEnabled: Boolean
        get() = prefs.getBoolean(autoShutdownAnimPref, true)
        set(value) = prefs.edit().putBoolean(autoShutdownAnimPref, value).apply()

    var bsodOutputEnabled: Boolean
        get() = prefs.getBoolean(bsodOutputPref, false)
        set(value) = prefs.edit().putBoolean(bsodOutputPref, value).apply()

    var iconPackChanged: Boolean
        get() = prefs.getBoolean("iconPackChanged", false)
        set(value) = prefs.edit().putBoolean("iconPackChanged", value).apply()

    var showKeyboardWhenSearching: Boolean
        get() = prefs.getBoolean(keyboardSearchPref, true)
        set(value) = prefs.edit().putBoolean(keyboardSearchPref, value).apply()

    var showKeyboardWhenOpeningAllApps: Boolean
        get() = prefs.getBoolean(keyboardAutoSearchPref, false)
        set(value) = prefs.edit().putBoolean(keyboardAutoSearchPref, value).apply()

    var orientation: String?
        get() = prefs.getString(orientationPref, "default")
        set(value) = prefs.edit().putString(orientationPref, value).apply()

    var coloredStroke: Boolean
        get() = prefs.getBoolean(coloredStrokePref, false)
        set(value) = prefs.edit().putBoolean(coloredStrokePref, value).apply()

    var customFontInstalled: Boolean
        get() = prefs.getBoolean(customFontPref, false)
        set(value) = prefs.edit().putBoolean(customFontPref, value).apply()

    var customFontPath: String?
        get() = prefs.getString(customFontPathPref, null)
        set(value) = prefs.edit().putString(customFontPathPref, value).apply()

    var customFontName: String?
        get() = prefs.getString(customFontNamePref, null)
        set(value) = prefs.edit().putString(customFontNamePref, value).apply()

    var customLightFontPath: String?
        get() = prefs.getString(customLightFontPathPref, null)
        set(value) = prefs.edit().putString(customLightFontPathPref, value).apply()

    var customLightFontName: String?
        get() = prefs.getString(customLightFontNamePref, null)
        set(value) = prefs.edit().putString(customLightFontNamePref, value).apply()

    var customBoldFontPath: String?
        get() = prefs.getString(customBoldFontPathPref, null)
        set(value) = prefs.edit().putString(customBoldFontPathPref, value).apply()

    var customBoldFontName: String?
        get() = prefs.getString(customBoldFontNamePref, null)
        set(value) = prefs.edit().putString(customBoldFontNamePref, value).apply()

    var allAppsKeyboardActionEnabled: Boolean
        get() = prefs.getBoolean(allAppsKeyboardGoActionPref, false)
        set(value) = prefs.edit().putBoolean(allAppsKeyboardGoActionPref, value).apply()
}
