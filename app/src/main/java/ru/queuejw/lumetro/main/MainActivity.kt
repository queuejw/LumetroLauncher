package ru.queuejw.lumetro.main

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.components.adapters.ViewPagerAdapter
import ru.queuejw.lumetro.components.adapters.viewtypes.TileViewTypes
import ru.queuejw.lumetro.components.core.AppManager
import ru.queuejw.lumetro.components.core.Lumetro.Companion.viewPagerUserInputEnabled
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.base.BaseActivity
import ru.queuejw.lumetro.components.viewmodels.MainViewModel
import ru.queuejw.lumetro.databinding.ActivityMainBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var viewModel: MainViewModel
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private val viewPagerCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavigationBarColors(position)
            }

            fun updateNavigationBarColors(position: Int) {
                when {
                    position == 0 -> {
                        binding.bottomBarLayout.apply {
                            navigationStart.setColorFilter(
                                viewModel.getColorManager().getAccentColor(this@MainActivity)
                            )
                            navigationSearch.setColorFilter(
                                viewModel.getColorManager().getOnSurfaceColor(this@MainActivity)
                            )
                        }
                    }

                    else -> {
                        binding.bottomBarLayout.apply {
                            navigationSearch.setColorFilter(
                                viewModel.getColorManager().getAccentColor(this@MainActivity)
                            )
                            navigationStart.setColorFilter(
                                viewModel.getColorManager().getOnSurfaceColor(this@MainActivity)
                            )
                        }
                    }
                }
            }
        }
    }

    private var isActivityLoaded = false

    private fun preInit(context: Context) {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        binding.progressBar.apply {
            visibility = View.VISIBLE
            setIndicatorColor(viewModel.getColorManager().getAccentColor(context))
            showProgressBar()
        }
    }

    private fun configureLayout() {
        binding.apply {
            setWindowInsets(root, false)
        }
    }

    override fun getActivityViewBinding(): ActivityMainBinding? {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.isFirstLaunch) {
            startActivity(
                Intent(
                    this,
                    FirstLaunchExperienceActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            )
            finishAndRemoveTask()
            return
        }
        preInit(this)
        setContentView(binding.root)
        configureLayout()
        startActivity()
    }

    private fun initComponents() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.viewPager.apply {
                    if (currentItem != 0 && !isFakeDragging && viewPagerUserInputEnabled) {
                        currentItem -= 1
                    }
                }
            }
        }
        viewPagerAdapter = ViewPagerAdapter(this)
    }

    private fun createIconLoaderForViewModel(viewModel: MainViewModel) {
        viewModel.createIconLoader(prefs.iconPackPackage != null, prefs.iconPackPackage)
    }

    private fun checkIcons(viewModel: MainViewModel, context: Context): Boolean {
        viewModel.apply {
            getIconLoader()?.let {
                if (!it.isIconPackAvailable(context, prefs.iconPackPackage)) {
                    Log.d("icons", "clean cache")
                    it.apply {
                        removeIconPack(prefs)
                        clearCache(context)
                    }
                    cleanUp()
                    return true
                }
            }
        }
        return false
    }

    private suspend fun configureViewModel(context: Context): Boolean {
        val appManager = AppManager()
        val tileManager = TileManager()
        tileManager.checkAllTiles(context)
        viewModel.apply {
            createIconLoaderForViewModel(this)
            if (checkIcons(this, context)) {
                return false
            }
            Log.d("viewmodel", "continue")
            val apps = appManager.getInstalledApps(context)
            getIconLoader()?.cacheAllIcons(apps, context)
            Log.d("viewmodel", "icons cached")
            val tiles = getDatabase().getTilesDao().getTilesData()
            withContext(Dispatchers.Main) {
                updateAppsList(apps)
                updateTilesList(tiles)
                viewPagerUserInputEnabled =
                    tiles.any { it.tileType != TileViewTypes.TYPE_PLACEHOLDER.type }
            }
        }
        return true
    }

    private fun setNavigationViewButtonOnClick(view: View, page: Int) {
        view.setOnClickListener {
            binding.viewPager.apply {
                if (!isFakeDragging && viewPagerUserInputEnabled) setCurrentItem(page, true)
            }
        }
    }

    private fun initUi() {
        binding.apply {
            setNavigationViewButtonOnClick(bottomBarLayout.navigationSearch, 1)
            setNavigationViewButtonOnClick(bottomBarLayout.navigationStart, 0)
            viewPager.apply {
                alpha = 0f
                adapter = viewPagerAdapter
                registerOnPageChangeCallback(viewPagerCallback)
            }
        }
    }

    // https://stackoverflow.com/questions/57505875/change-viewpager2-scroll-speed-when-sliding-programmatically/59235979#59235979
    private fun ViewPager2.setCurrentItem(
        item: Int,
        mDuration: Long,
        tInterpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
        pagePxWidth: Int = width // Default value taken from getWidth() from ViewPager2 view
    ) {
        var previousValue = 0
        val pxToDrag: Int = pagePxWidth * (item - currentItem)
        val animator = ValueAnimator.ofInt(0, pxToDrag)
        animator.apply {
            addUpdateListener { valueAnimator ->
                val currentValue = valueAnimator.animatedValue as Int
                val currentPxToDrag = (currentValue - previousValue).toFloat()
                fakeDragBy(-currentPxToDrag)
                previousValue = currentValue
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    beginFakeDrag()
                }

                override fun onAnimationEnd(animation: Animator) {
                    endFakeDrag()
                }

                override fun onAnimationCancel(animation: Animator) { /* Ignored */
                }

                override fun onAnimationRepeat(animation: Animator) { /* Ignored */
                }
            })
            interpolator = tInterpolator
            duration = mDuration
        }
        animator.start()
    }

    private fun animateStartScreenLaunch(viewPager2: ViewPager2) {
        viewPager2.apply {
            visibility = View.VISIBLE
            rotationY = 60f
            alpha = 0.75f
            translationX = -125f
            scaleX = 0f
            scaleY = 0f
            pivotX = 0f
            pivotY = height / 2f
        }
        viewPager2.animate().setDuration(600).scaleY(1f).scaleX(1f).alpha(1f).translationX(0f)
            .rotationY(0f).start()
        if (viewModel.getTiles()?.value?.any { it.tileType != TileViewTypes.TYPE_PLACEHOLDER.type } == true) {
            lifecycleScope.launch {
                delay(330)
                viewPager2.setCurrentItem(0, 300)
                cancel()
            }
        } else {
            binding.viewPager.isUserInputEnabled = false
        }
    }

    private fun startActivity() {
        lifecycleScope.launch(Dispatchers.IO) {
            val bool = configureViewModel(this@MainActivity)
            initComponents()
            if (bool) {
                withContext(Dispatchers.Main) {
                    onBackPressedCallback?.let {
                        onBackPressedDispatcher.addCallback(
                            this@MainActivity,
                            it
                        )
                    }
                    initUi()
                    binding.apply {
                        progressBar.apply {
                            hideProgressBar()
                            destroyView()
                            visibility = View.GONE
                        }
                        viewPager.apply {
                            currentItem = 1
                            visibility = View.GONE
                        }
                        isActivityLoaded = true
                        animateStartScreenLaunch(viewPager)
                    }
                }
                cancel()
            } else {
                isActivityLoaded = true
                restartActivity()
            }
        }
    }

    fun restartActivity() {
        destroyComponents()
        startActivity(Intent(this, MainActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    private fun checkPrefs() {
        if (prefs.isRestartRequired) {
            prefs.isRestartRequired = false
            restartActivity()
            return
        }
    }

    override fun onResume() {
        if (isActivityLoaded) {
            if (checkIcons(viewModel, this)) {
                prefs.isRestartRequired = true
            }
        }
        super.onResume()
        if (viewModel.getIconLoader() == null && isActivityLoaded) {
            createIconLoaderForViewModel(viewModel)
        }
        checkPrefs()
        Log.d("Main", "Resume")
    }

    private fun destroyComponents() {
        if (isActivityLoaded) {
            onBackPressedCallback?.remove()
            onBackPressedCallback = null
            viewModel.cleanUp()
            binding.viewPager.unregisterOnPageChangeCallback(viewPagerCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("Main", "Stop")
        viewModel.cleanUp()
    }

    override fun onDestroy() {
        destroyComponents()
        super.onDestroy()
        Log.d("Main", "Destroy")
    }

    fun setViewPagerUserInput(boolean: Boolean) {
        if (viewPagerUserInputEnabled) {
            binding.viewPager.isUserInputEnabled = boolean
        }
    }

    fun changePage(page: Int) {
        if (viewPagerUserInputEnabled) {
            binding.viewPager.setCurrentItem(page, true)
        }
    }
}