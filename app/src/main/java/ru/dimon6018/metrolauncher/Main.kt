package ru.dimon6018.metrolauncher

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dimon6018.metrolauncher.Application.Companion.PREFS
import ru.dimon6018.metrolauncher.Application.Companion.isAppOpened
import ru.dimon6018.metrolauncher.content.AllApps
import ru.dimon6018.metrolauncher.content.Start
import ru.dimon6018.metrolauncher.content.data.app.App
import ru.dimon6018.metrolauncher.content.data.bsod.BSOD
import ru.dimon6018.metrolauncher.content.oobe.WelcomeActivity
import ru.dimon6018.metrolauncher.content.settings.SettingsActivity
import ru.dimon6018.metrolauncher.databinding.LauncherMainScreenBinding
import ru.dimon6018.metrolauncher.helpers.disklru.CacheUtils.Companion.closeDiskCache
import ru.dimon6018.metrolauncher.helpers.disklru.CacheUtils.Companion.initDiskCache
import ru.dimon6018.metrolauncher.helpers.disklru.CacheUtils.Companion.loadIconFromDiskCache
import ru.dimon6018.metrolauncher.helpers.disklru.CacheUtils.Companion.saveIconToDiskCache
import ru.dimon6018.metrolauncher.helpers.disklru.DiskLruCache
import ru.dimon6018.metrolauncher.helpers.iconpack.IconPackManager
import ru.dimon6018.metrolauncher.helpers.receivers.PackageChangesReceiver
import ru.dimon6018.metrolauncher.helpers.ui.WPDialog
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.VERSION_CODE
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.accentColorFromPrefs
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.applyWindowInsets
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.getDefaultLocale
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.isDevMode
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.launcherAccentColor
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.launcherOnSurfaceColor
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.launcherSurfaceColor
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.registerPackageReceiver
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.sendCrash
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.setUpApps
import ru.dimon6018.metrolauncher.helpers.utils.Utils.Companion.unregisterPackageReceiver

/**
 * Main application screen (tiles, apps)
 * @see Application
 * @see Start
 * @see AllApps
 */
class Main : AppCompatActivity() {

    // 8.0
    private lateinit var pagerAdapter: FragmentStateAdapter
    private lateinit var mainViewModel: MainViewModel

    private val iconPackManager: IconPackManager by lazy { IconPackManager(this) }
    private val packageReceiver: PackageChangesReceiver by lazy { PackageChangesReceiver() }
    private val defaultIconSize: Int by lazy { resources.getDimensionPixelSize(R.dimen.tile_default_size) }

    private val accentColor: Int by lazy { launcherAccentColor(this@Main.theme) }
    private val onSurfaceColor: Int by lazy { launcherOnSurfaceColor(this@Main.theme) }

    private var searchAdapter: SearchAdapter? = null
    private var filteredList = mutableListOf<App>()

    private var bottomViewReady = false
    private var searching = false

    private lateinit var binding: LauncherMainScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        isDarkMode = resources.getBoolean(R.bool.isDark) && PREFS.appTheme != 2
        when (PREFS.appTheme) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        handleDevMode()
        super.onCreate(savedInstanceState)

        // If MPL has never run before, open OOBE
        if (PREFS.launcherState == 0) {
            runOOBE()
            return
        }

        binding = LauncherMainScreenBinding.inflate(layoutInflater)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setContentView(binding.root)
        setupUI()
        lifecycleScope.launch(Dispatchers.Default) {
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
        if (isDevMode(this) && PREFS.isAutoShutdownAnimEnabled) {
            disableAnims()
        }
    }

    private fun runOOBE() {
        val intent = Intent(this, WelcomeActivity::class.java).apply {
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
        pagerAdapter = WinAdapter(this@Main)
        setMainViewModel()
        checkUpdate()
    }

    /**
     * Configures the user interface according to the settings
     */
    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (PREFS.isWallpaperEnabled) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
            )
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainBottomBar.navigationFrame) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = systemBarInsets.bottom,
                left = systemBarInsets.left,
                right = systemBarInsets.right,
            )
            insets
        }
        if (PREFS.isSearchBarEnabled) applyWindowInsets(binding.mainSearchResults.searchBarResultsLayout)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        registerPackageReceiver(this, packageReceiver)
        otherTasks()
    }

    private fun checkUpdate() {
        if (PREFS.prefs.getBoolean(
                "updateInstalled",
                false
            ) && PREFS.versionCode == VERSION_CODE
        ) PREFS.updateState = 3
    }

    private fun disableAnims() {
        PREFS.apply {
            isAAllAppsAnimEnabled = false
            isAlphabetAnimEnabled = false
            isTransitionAnimEnabled = false
            isLiveTilesAnimEnabled = false
            isTilesAnimEnabled = false
        }
    }

    /**
     * Creates OnBackPressedCallback, which is needed to move to the previous ViewPager screen by pressing/gesturing backwards.
     * Or disables the search
     */
    private fun setupBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.mainPager.currentItem != 0) {
                    binding.mainPager.currentItem -= 1
                } else if (searching && PREFS.isSearchBarEnabled) {
                    hideSearch()
                }
            }
        })
    }

    private fun setupViewPager() {
        binding.mainPager.apply {
            adapter = pagerAdapter
            registerOnPageChangeCallback(createPageChangeCallback())
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
            if (!PREFS.isSearchBarEnabled && PREFS.navBarColor != 2) {
                val (startColor, searchColor) = when {
                    position == 0 -> accentColor to onSurfaceColor
                    else -> onSurfaceColor to accentColor
                }
                binding.mainBottomBar.navigationStartBtn.setColorFilter(startColor)
                binding.mainBottomBar.navigationSearchBtn.setColorFilter(searchColor)
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            when (state) {
                ViewPager2.SCROLL_STATE_DRAGGING -> isViewPagerScrolling = true
                ViewPager2.SCROLL_STATE_SETTLING -> isViewPagerScrolling = true
                ViewPager2.SCROLL_STATE_IDLE -> isViewPagerScrolling = false
            }
        }
    }

    override fun onResume() {
        // restart MPL if some settings have been changed
        if (PREFS.isPrefsChanged) restart()

        super.onResume()
    }

    private fun restart() {
        PREFS.isPrefsChanged = false
        val componentName = Intent(this, this::class.java).component
        val intent = Intent.makeRestartActivityTask(componentName)
        startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPackageReceiver(this, packageReceiver)
    }

    private fun runApp(app: String) {
        isAppOpened = true
        val intent = when (app) {
            "ru.dimon6018.metrolauncher" -> Intent(this, SettingsActivity::class.java)
            else -> packageManager.getLaunchIntentForPackage(app)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        startActivity(intent)
    }

    fun configureViewPagerScroll(enabled: Boolean) {
        binding.mainPager.isUserInputEnabled = enabled
    }

    private suspend fun setMainViewModel() {
        mainViewModel.setAppList(setUpApps(packageManager, this))
        regenerateIcons()
    }


    private suspend fun regenerateIcons() {
        val isCustomIconsInstalled = PREFS.iconPackPackage != "null"
        var diskCache = initDiskCache(this)
        if (isCustomIconsInstalled) {
            checkIconPack(diskCache)
        }
        if (PREFS.iconPackChanged) {
            PREFS.iconPackChanged = false
            diskCache?.apply {
                delete()
                close()
            }
            diskCache = null
        }
        withContext(Dispatchers.IO) {
            if (diskCache == null) diskCache = initDiskCache(this@Main)
            mainViewModel.getAppList().forEach { app ->
                if (app.type != 1) {
                    val icon = diskCache?.let { loadIconFromDiskCache(it, app.appPackage!!) }
                    if (icon == null) {
                        generateIcon(app.appPackage!!, isCustomIconsInstalled)
                        saveIconToDiskCache(
                            diskCache,
                            app.appPackage!!,
                            mainViewModel.getIconFromCache(app.appPackage!!)
                        )
                    } else {
                        mainViewModel.addIconToCache(app.appPackage!!, icon)
                    }
                }
            }
            diskCache?.let { closeDiskCache(it) }
        }
    }

    private fun checkIconPack(disk: DiskLruCache?): Boolean {
        return runCatching {
            packageManager.getApplicationInfo(PREFS.iconPackPackage!!, 0)
            true
        }.getOrElse {
            PREFS.iconPackPackage = "null"
            disk?.apply {
                delete()
                close()
            }
            false
        }
    }

    // Icon generation for cache
    fun generateIcon(appPackage: String, isCustomIconsInstalled: Boolean) {
        val icon = if (!isCustomIconsInstalled) {
            packageManager.getApplicationIcon(appPackage)
        } else {
            iconPackManager.getIconPackWithName(PREFS.iconPackPackage)
                ?.getDrawableIconForPackage(appPackage, null)
                ?: packageManager.getApplicationIcon(appPackage)
        }
        mainViewModel.addIconToCache(appPackage, icon.toBitmap(defaultIconSize, defaultIconSize))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun otherTasks() {
        if (PREFS.prefs.getBoolean("tip1Enabled", true)) {
            showTipDialog()
            PREFS.prefs.edit().putBoolean("tip1Enabled", false).apply()
        }
        crashCheck()
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
    private fun crashCheck() {
        if (PREFS.prefs.getBoolean("app_crashed", false)) {
            lifecycleScope.launch(Dispatchers.Default) {
                delay(5000)
                PREFS.prefs.edit().apply {
                    putBoolean("app_crashed", false)
                    putInt("crashCounter", 0)
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
        var pos = (dao.getBsodList().size) - 1
        if (pos < 0) pos = 0
        val text = dao.getBSOD(pos).log
        withContext(Dispatchers.Main) {
            WPDialog(this@Main).apply {
                setTopDialog(true)
                setTitle(getString(R.string.bsodDialogTitle))
                setMessage(getString(R.string.bsodDialogMessage))
                setNegativeButton(getString(R.string.bsodDialogDismiss), null)
                setPositiveButton(getString(R.string.bsodDialogSend)) {
                    sendCrash(text, this@Main)
                }
                show()
            }
        }
    }

    private fun setupNavigationBar() {
        if (bottomViewReady) return
        bottomViewReady = true
        binding.mainBottomBar.navigationFrame.setBackgroundColor(getNavBarColor())
        configureBottomBar()
    }

    private fun getNavBarColor(): Int {
        return when (PREFS.navBarColor) {
            0 -> ContextCompat.getColor(this, android.R.color.background_dark)
            1 -> ContextCompat.getColor(this, android.R.color.background_light)
            2 -> accentColorFromPrefs(this)
            3 -> {
                binding.mainBottomBar.navigationFrame.visibility = View.GONE
                return ContextCompat.getColor(this, android.R.color.transparent)
            }

            else -> launcherSurfaceColor(theme)
        }
    }

    private fun configureBottomBar() {
        if (!PREFS.isSearchBarEnabled) {
            setupNavigationBarButtons()
        } else {
            setupSearchBar()
        }
    }

    private fun setupNavigationBarButtons() {
        binding.mainBottomBar.navigationMain.visibility = View.VISIBLE
        binding.mainBottomBar.navigationStartBtn.apply {
            setImageDrawable(getNavBarIconDrawable())
            setOnClickListener { binding.mainPager.setCurrentItem(0, true) }
        }
        binding.mainBottomBar.navigationSearchBtn.setOnClickListener {
            if (PREFS.isAllAppsEnabled) binding.mainPager.setCurrentItem(1, true)
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

    private fun setupSearchBar() {
        filteredList = mutableListOf()
        binding.mainBottomBar.searchBarLayout.visibility = View.VISIBLE
        searchAdapter = SearchAdapter(filteredList)
        binding.mainSearchResults.searchBarRecyclerview.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = searchAdapter
        }
        setupSearchEditText()
    }

    private fun setupSearchEditText() {
        val editText = binding.mainBottomBar.searchBar.editText as? AutoCompleteTextView
        editText?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                filterSearchText(s.toString(), mainViewModel.getAppList())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
        editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO && filteredList.isNotEmpty()) {
                runApp(filteredList.first().appPackage!!)
                editText.text.clear()
                hideSearchResults()
                true
            } else {
                false
            }
        }
    }

    fun hideSearch() {
        hideSearchResults()
    }

    private fun hideSearchResults() {
        lifecycleScope.launch {
            searching = false
            binding.mainSearchResults.searchBarResultsLayout.apply {
                if (PREFS.isTransitionAnimEnabled) {
                    ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).start()
                }
                visibility = View.GONE
            }
        }
    }

    private fun showSearchResults() {
        searching = true
        binding.mainSearchResults.searchBarResultsLayout.apply {
            if (PREFS.isTransitionAnimEnabled) {
                ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).setDuration(100).start()
            }
            visibility = View.VISIBLE
        }
    }

    private fun filterSearchText(text: String, appList: List<App>) {
        if (text.isEmpty()) hideSearchResults() else showSearchResults()
        val max = PREFS.maxResultsSearchBar
        val defaultLocale = getDefaultLocale()
        filteredList.clear()

        appList.filter {
            it.appLabel!!.lowercase(defaultLocale).contains(text.lowercase(defaultLocale))
        }
            .take(max)
            .let { filteredList.addAll(it) }

        filteredList.sortWith(compareBy { it.appLabel })
        searchAdapter?.setData(filteredList)
    }

    companion object {
        var isLandscape: Boolean = false
        var isDarkMode: Boolean = false
        var isViewPagerScrolling: Boolean = false
    }

    inner class WinAdapter(fragment: FragmentActivity) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = if (PREFS.isAllAppsEnabled) 2 else 1

        override fun createFragment(position: Int): Fragment {
            return when {
                !PREFS.isAllAppsEnabled -> Start()
                position == 1 -> AllApps()
                else -> Start()
            }
        }
    }

    inner class SearchAdapter(private var dataList: MutableList<App>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return AppSearchHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.app, parent, false)
            )
        }

        override fun getItemCount(): Int = dataList.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as AppSearchHolder).bind(dataList[position])
        }

        @SuppressLint("NotifyDataSetChanged")
        fun setData(new: MutableList<App>) {
            dataList = new
            notifyDataSetChanged()
        }

        inner class AppSearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: ImageView = itemView.findViewById(R.id.app_icon)
            private val label: MaterialTextView = itemView.findViewById(R.id.app_label)

            init {
                label.setTextColor(launcherSurfaceColor(theme))
                itemView.setOnClickListener {
                    runApp(dataList[absoluteAdapterPosition].appPackage!!)
                }
            }

            fun bind(app: App) {
                icon.setImageBitmap(mainViewModel.getIconFromCache(app.appPackage!!))
                label.text = app.appLabel
            }
        }
    }
}
