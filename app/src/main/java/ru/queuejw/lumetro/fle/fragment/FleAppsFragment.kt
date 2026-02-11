package ru.queuejw.lumetro.fle.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.adapters.AppFLEAdapter
import ru.queuejw.lumetro.components.core.AppManager
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.core.db.tile.TileDatabase
import ru.queuejw.lumetro.components.core.icons.IconLoader
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.FleAppsBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity
import ru.queuejw.lumetro.model.App
import kotlin.random.Random

class FleAppsFragment : BaseFragment<FleAppsBinding>() {

    private var appAdapter: AppFLEAdapter? = null
    private val colorManager = ColorManager()
    private var fragmentLoaded = false
    private var dialogShown = false
    private val selectedApps by lazy {
        ArrayList<App>()
    }
    private var appCount = 0

    private var isActionsBlocked = false

    private var iconLoader: IconLoader? = null

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleAppsBinding {
        return FleAppsBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs.fleProgress = 5
    }

    private fun updateCounter() {
        binding.counter.text = getString(R.string.fle_apps_counter, selectedApps.size, appCount)
    }

    private fun onCheckboxChanged(app: App, value: Boolean) {
        selectedApps.apply {
            if (value) {
                add(app)
            } else {
                remove(app)
            }
        }
        updateCounter()
    }

    private fun blockUi() {
        (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(false)
        binding.apply {
            progressBar.showProgressBar()
            pinAll.isEnabled = false
            unpinAll.isEnabled = false
            recyclerView.isScrollEnabled = false
            pinAll.alpha = 0.5f
            unpinAll.alpha = 0.5f
            recyclerView.alpha = 0.5f
        }
    }

    private fun getRandomTileSize(prevValue: Int): Int {
        val result = Random.nextInt(0, 3)
        return if (result == prevValue) getRandomTileSize(prevValue)
        else result
    }

    private fun saveData() {
        appAdapter ?: return
        isActionsBlocked = true
        blockUi()
        val size = appAdapter!!.data.size * 2 + selectedApps.size * 2
        val tileManager = TileManager()
        lifecycleScope.launch(Dispatchers.IO) {
            context?.let {
                tileManager.generatePlaceholders(size, it, true)
                val db = TileDatabase.getTileData(it)
                val list = db.getTilesDao().getTilesData()
                var prevTileSize = -1
                selectedApps.forEachIndexed { i, item ->
                    val item = list[i].apply {
                        tilePosition = i
                        tileType = 0
                        tileLabel = item.mName
                        tilePackage = item.mPackage!!
                        tileSize = getRandomTileSize(prevTileSize)
                        tileColor = null
                    }
                    db.getTilesDao().insertTile(item)
                    prevTileSize = item.tileSize
                }
            }
            withContext(Dispatchers.Main) {
                destroy()
                (activity as FirstLaunchExperienceActivity?)?.apply {
                    setFragment(nextFragment)
                }
            }
        }
    }

    private fun showEmptyListDialog(context: Context) {
        val dialog = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.warning))
            setMessage(context.getString(R.string.fle_apps_empty_list_warn))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                dialogShown = false
                saveData()
                this.dismiss()
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                dialogShown = false
                this.dismiss()
            }
            setDismissListener {
                dialogShown = false
            }
        }
        dialogShown = true
        dialog.show(childFragmentManager, "empty_list")
    }

    private fun prepareScreen() {
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(getString(R.string.apps))
            nextFragment = 6
            previousFragment = 4
            animateBottomBar(true)
            setNextButtonOnClickListener {
                if (selectedApps.isEmpty()) {
                    if (dialogShown) return@setNextButtonOnClickListener
                    showEmptyListDialog(this.baseContext)
                } else {
                    saveData()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            progressBar.apply {
                setIndicatorColor(colorManager.getAccentColor(progressBar.context))
                setIndicatorRadius(8)
                showProgressBar()
            }
            pinAll.setOnClickListener {
                if (fragmentLoaded && !isActionsBlocked) {
                    selectedApps.clear()
                    appAdapter?.pinAll()?.forEach {
                        selectedApps.add(it)
                    }
                    updateCounter()
                }
            }
            unpinAll.setOnClickListener {
                if (fragmentLoaded && !isActionsBlocked) {
                    selectedApps.clear()
                    appAdapter?.unpinAll()
                    updateCounter()
                }
            }
        }
        prepareScreen()
        lifecycleScope.launch(Dispatchers.IO) {
            val appManager = AppManager()
            context?.let {
                iconLoader = IconLoader(prefs.iconPackPackage != null, prefs.iconPackPackage)
                val list = appManager.getInstalledApps(it, true)
                appCount = list.size
                withContext(Dispatchers.Main) {
                    updateCounter()
                }
                iconLoader?.cacheAllIcons(list, it)
                appAdapter = AppFLEAdapter(
                    list,
                    iconLoader!!,
                    colorManager.getAccentColor(it)
                ) { app, bool ->
                    onCheckboxChanged(app, bool)
                }
                val lm = LinearLayoutManager(it)
                withContext(Dispatchers.Main) {
                    binding.recyclerView.apply {
                        layoutManager = lm
                        adapter = appAdapter
                    }
                }
            }
            withContext(Dispatchers.Main) {
                binding.progressBar.hideProgressBar()
                fragmentLoaded = true
            }
        }
    }

    private fun destroy() {
        (activity as FirstLaunchExperienceActivity?)?.setNextButtonOnClickListener(null)
        binding.apply {
            pinAll.setOnClickListener(null)
            unpinAll.setOnClickListener(null)
            progressBar.hideProgressBar()
            progressBar.destroyView()
            recyclerView.apply {
                layoutManager = null
                adapter = null
            }
        }
        iconLoader = null
        appAdapter = null
    }

    override fun onDestroyView() {
        iconLoader?.resetIconLoader()
        iconLoader = null
        fragmentLoaded = false
        destroy()
        super.onDestroyView()
    }
}