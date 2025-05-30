package ru.queuejw.mpl

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.content.AllApps
import ru.queuejw.mpl.content.Start
import ru.queuejw.mpl.content.data.app.App
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.data.tile.Tile
import ru.queuejw.mpl.content.oobe.OOBEActivity
import ru.queuejw.mpl.databinding.LauncherMainScreenBinding
import ru.queuejw.mpl.helpers.disklru.CacheUtils
import ru.queuejw.mpl.helpers.disklru.DiskLruCache
import ru.queuejw.mpl.helpers.iconpack.IconPackManager
import ru.queuejw.mpl.helpers.receivers.PackageChangesReceiver
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils
import kotlin.system.exitProcess

/**
 * Main application screen (tiles, apps)
 * @see ru.queuejw.mpl.Application
 * @see Start
 * @see AllApps
 */
class Main : AppCompatActivity() {

    // 8.0
    private lateinit var pagerAdapter: FragmentStateAdapter
    private lateinit var mainViewModel: MainViewModel

    private val iconPackManager: IconPackManager by lazy { IconPackManager(this) }
    private val packageReceiver: PackageChangesReceiver by lazy { PackageChangesReceiver() }
    private val defaultIconSize: Int by lazy { resources.getDimensionPixelSize(R.dimen.tile_small) }

    private val accentColor: Int by lazy { Utils.launcherAccentColor(this@Main.theme) }
    private val onSurfaceColor: Int by lazy { Utils.launcherOnSurfaceColor(this@Main.theme) }

    private lateinit var binding: LauncherMainScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        isDarkMode = resources.getBoolean(R.bool.isDark) && PREFS.appTheme != 2
        handleDevMode()
        super.onCreate(savedInstanceState)

        // If MPL has never run before, open OOBE
        if (PREFS.launcherState == 0) {
            runOOBE()
            return
        }

        binding = LauncherMainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        lifecycleScope.launch(Dispatchers.Default) {
            otherTasks()
            initializeData()
            withContext(Dispatchers.Main) {
                setupNavigationBar()
                setupViewPager()
                setupBackPressedDispatcher()
            }
        }
    }

    /**
     * Turn off animations if developer mode is enabled to prevent some animation issues
     * @see onCreate
     * @see disableAnims
     */
    private fun handleDevMode() {
        if (Utils.isDevMode(this) && PREFS.isAutoShutdownAnimEnabled) {
            disableAnims()
        }
    }

    private fun runOOBE() {
        val intent = Intent(this, OOBEActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        finishAffinity()
        startActivity(intent)
    }

    /**
     * Called when the application is started.
     * Updates the icon cache, application list and checks the application version
     * @see onCreate
     */
    private suspend fun initializeData() {
        setMainViewModel()
        checkUpdate()
        pagerAdapter = WinAdapter(this@Main)
    }

    fun blockStart() {
        isStartScreenEmpty = true
        binding.mainPager.currentItem = 1
        binding.mainPager.isUserInputEnabled = false
    }

    private suspend fun setMainViewModel() {
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.apply {
            val list = Utils.setUpApps(this@Main)
            setAppList(list)
            val tileList = checkTiles(getViewModelTileDao().getTilesList())
            setTileList(tileList)
            checkStartScreen(tileList)
            updateIcons(list, this@Main)
        }
    }

    private suspend fun checkTiles(list: MutableList<Tile>): MutableList<Tile> {
        list.forEachIndexed { index, item ->
            if (!isAppExist(item.tilePackage)) {
                item.apply {
                    tileType = -1
                    tileSize = "small"
                    tilePackage = ""
                    tileColor = -1
                    tileLabel = ""
                    id = this.id!! / 2
                    mainViewModel.getViewModelTileDao().updateTile(this)
                }
                list[index] = item
            }
        }
        return list
    }

    private fun initDiskCache(context: Context): DiskLruCache? {
        return CacheUtils.initDiskCache(context)
    }

    private fun updateIcons(list: MutableList<App>, context: Context) {
        var diskCache = initDiskCache(context)
        val isCustomIconsInstalled = PREFS.iconPackPackage != "null"
        if (updateIconPack(diskCache, isCustomIconsInstalled)) {
            diskCache = initDiskCache(context)
        }
        list.forEach {
            var icon = CacheUtils.loadIconFromDiskCache(diskCache!!, it.appPackage)
            if (icon == null) {
                icon = generateIcon(it.appPackage, isCustomIconsInstalled)
                CacheUtils.saveIconToDiskCache(diskCache, it.appPackage, icon)
            }
            mainViewModel.addIconToCache(it.appPackage, icon)
        }
        diskCache?.let {
            CacheUtils.closeDiskCache(it)
        }
    }

    // Icon generation for cache
    fun generateIcon(appPackage: String, isCustomIconsInstalled: Boolean): Bitmap {
        return if (!isCustomIconsInstalled) {
            packageManager.getApplicationIcon(appPackage)
        } else {
            iconPackManager.getIconPackWithName(PREFS.iconPackPackage)
                ?.getDrawableIconForPackage(appPackage, null)
                ?: packageManager.getApplicationIcon(appPackage)
        }.toBitmap(defaultIconSize, defaultIconSize)
    }

    private fun updateIconPack(
        diskLruCache: DiskLruCache?,
        isCustomIconsInstalled: Boolean
    ): Boolean {
        if (!isCustomIconsInstalled) {
            return false
        }
        if (!isAppExist(PREFS.iconPackPackage!!)) {
            PREFS.iconPackPackage = "null"
            diskLruCache?.let {
                it.delete()
                CacheUtils.closeDiskCache(it)
                return true
            }
            return false
        }
        return false
    }

    private fun isAppExist(packageName: String): Boolean {
        runCatching {
            packageManager.getApplicationInfo(packageName, 0)
            return true
        }.getOrElse {
            return false
        }
    }

    private fun checkStartScreen(list: MutableList<Tile>) {
        val userTiles = ArrayList<Tile>()
        list.forEach {
            if (it.tileType != -1) userTiles.add(it)
        }
        if (userTiles.isNotEmpty()) isStartScreenEmpty = false
    }

    /**
     * Configures the user interface according to the settings
     */
    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainBottomBar.navigationMain) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = systemBarInsets.bottom,
                left = systemBarInsets.left,
                right = systemBarInsets.right,
            )
            insets
        }
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        Utils.registerPackageReceiver(this, packageReceiver)
    }

    private fun checkUpdate() {
        if (PREFS.prefs.getBoolean(
                "updateInstalled",
                false
            ) && PREFS.versionCode == Utils.VERSION_CODE
        ) PREFS.updateState = 3
    }

    private fun disableAnims() {
        PREFS.apply {
            isAAllAppsAnimEnabled = false
            isTransitionAnimEnabled = false
            isLiveTilesAnimEnabled = false
            isTilesAnimEnabled = false
        }
    }

    /**
     * Creates OnBackPressedCallback, which is needed to move to the previous ViewPager screen by pressing/gesturing backwards.
     */
    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.mainPager.currentItem != 0 && !isStartScreenEmpty) {
                    binding.mainPager.currentItem -= 1
                }
            }
        })
    }

    private fun setupViewPager() {
        binding.mainPager.apply {
            adapter = pagerAdapter
            registerOnPageChangeCallback(createPageChangeCallback())
            if (isStartScreenEmpty) {
                binding.mainPager.currentItem = 1
            }
            if (!PREFS.isAllAppsEnabled || isStartScreenEmpty) {
                isUserInputEnabled = false
            }
        }
    }

    /**
     * Creates OnPageChangeCallback for some required actions
     */
    private fun createPageChangeCallback() = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateNavigationBarColors(position)
        }

        private fun updateNavigationBarColors(position: Int) {
            if (PREFS.navBarColor != 2) {
                val (startColor, searchColor) = when {
                    position == 0 -> accentColor to onSurfaceColor
                    else -> onSurfaceColor to accentColor
                }
                binding.mainBottomBar.navigationStartBtn.setColorFilter(startColor)
                binding.mainBottomBar.navigationSearchBtn.setColorFilter(searchColor)
            }
        }
    }

    override fun onResume() {
        // restart MPL if some settings have been changed
        if (PREFS.isPrefsChanged) restartDialog()

        super.onResume()
    }

    private fun restartDialog() {
        WPDialog(this).apply {
            setTopDialog(true)
            setCancelable(false)
            setTitle(getString(R.string.settings_app_title))
            setMessage(getString(R.string.restart_required))
            setPositiveButton(getString(R.string.restart)) {
                dismiss()
                restart()
            }
            setNegativeButton(getString(R.string.later)) {
                dismiss()
            }
            show()
        }
    }

    private fun restart() {
        PREFS.isPrefsChanged = false
        val componentName = Intent(this, this::class.java).component
        val intent = Intent.makeRestartActivityTask(componentName)
        startActivity(intent)
        exitProcess(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.unregisterPackageReceiver(this, packageReceiver)
    }

    fun configureViewPagerScroll(enabled: Boolean) {
        if (PREFS.isAllAppsEnabled && !isStartScreenEmpty) {
            binding.mainPager.isUserInputEnabled = enabled
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private suspend fun otherTasks() {
        withContext(Dispatchers.Main) {
            if (PREFS.prefs.getBoolean("tip1Enabled", true)) {
                showTipDialog()
                PREFS.prefs.edit { putBoolean("tip1Enabled", false) }
            }
        }
        lifecycleScope.launch {
            crashCheck()
        }
    }

    private fun showTipDialog() {
        WPDialog(this@Main).apply {
            setTopDialog(false)
            setTitle(getString(R.string.tip))
            setMessage(getString(R.string.tip1))
            setPositiveButton(getString(android.R.string.ok), null)
            show()
        }
    }

    // If 5 seconds after the crash is successful, display an error message
    private suspend fun crashCheck() {
        if (PREFS.prefs.getBoolean("app_crashed", false)) {
            withContext(Dispatchers.IO) {
                delay(5000)
                PREFS.prefs.edit().apply {
                    putBoolean("app_crashed", false)
                    putInt("crash_count", 0)
                    apply()
                }
                if (PREFS.isFeedbackEnabled) {
                    handleCrashFeedback()
                }
            }
        }
    }

    private suspend fun handleCrashFeedback() {
        val dao = BSOD.getData(this@Main).getDao()
        val list = dao.getBsodList()
        val text = if (list.isNotEmpty()) dao.getBsodList()
            .last().log else "Failed to retrieve error information"
        withContext(Dispatchers.Main) {
            WPDialog(this@Main).apply {
                setTopDialog(true)
                setTitle(getString(R.string.bsodDialogTitle))
                setMessage(getString(R.string.bsodDialogMessage))
                setNegativeButton(getString(R.string.bsodDialogDismiss), null)
                setPositiveButton(getString(R.string.bsodDialogSend)) {
                    Utils.sendCrash(text, this@Main)
                }
                show()
            }
        }
    }

    private fun setupNavigationBar() {
        if (PREFS.navBarColor == 3) {
            binding.mainBottomBar.root.visibility = View.GONE
            return
        }
        if (!PREFS.isAllAppsEnabled && !isStartScreenEmpty) binding.mainBottomBar.navigationSearchBtn.visibility =
            View.GONE
        binding.mainBottomBar.navigationMain.setBackgroundColor(getNavBarColor())
        configureBottomBar()
    }

    private fun getNavBarColor(): Int {
        return when (PREFS.navBarColor) {
            0 -> ContextCompat.getColor(this, android.R.color.background_dark)
            1 -> ContextCompat.getColor(this, android.R.color.background_light)
            2 -> Utils.accentColorFromPrefs(this)
            3 -> ContextCompat.getColor(this, android.R.color.transparent)
            else -> Utils.launcherSurfaceColor(theme)
        }
    }

    private fun configureBottomBar() {
        setupNavigationBarButtons()
    }

    private fun setupNavigationBarButtons() {
        binding.mainBottomBar.navigationMain.visibility = View.VISIBLE
        binding.mainBottomBar.navigationStartBtn.apply {
            setImageDrawable(getNavBarIconDrawable())
            setOnClickListener {
                if (!isStartScreenEmpty) binding.mainPager.setCurrentItem(0, true)
            }
            if (!PREFS.isAllAppsEnabled) setOnLongClickListener {
                binding.mainPager.setCurrentItem(1, true)
                true
            }
        }
        if (PREFS.isAllAppsEnabled) {
            binding.mainBottomBar.navigationSearchBtn.setOnClickListener {
                binding.mainPager.setCurrentItem(1, true)
            }
        }
    }

    private fun getNavBarIconDrawable(): Drawable? {
        return ContextCompat.getDrawable(
            this, when (PREFS.navBarIconValue) {
                0 -> R.drawable.ic_os_windows_8
                1 -> R.drawable.ic_os_windows
                2 -> R.drawable.ic_os_android
                else -> R.drawable.ic_os_windows_8
            }
        )
    }

    companion object {
        var isLandscape: Boolean = false
        var isDarkMode: Boolean = false
        var isStartScreenEmpty: Boolean = true
    }

    class WinAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when {
                position == 1 -> AllApps()
                else -> Start()
            }
        }
    }
}
