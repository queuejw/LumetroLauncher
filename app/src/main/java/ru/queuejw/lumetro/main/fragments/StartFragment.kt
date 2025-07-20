package ru.queuejw.lumetro.main.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.adapters.TilesAdapter
import ru.queuejw.lumetro.components.adapters.decor.MarginItemDecoration
import ru.queuejw.lumetro.components.adapters.viewtypes.TileViewTypes
import ru.queuejw.lumetro.components.core.AppManager.Companion.launchApp
import ru.queuejw.lumetro.components.core.Lumetro.Companion.viewPagerUserInputEnabled
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.base.BaseMainFragment
import ru.queuejw.lumetro.components.core.receivers.AppReceiver
import ru.queuejw.lumetro.components.itemtouch.ItemTouchCallback
import ru.queuejw.lumetro.components.ui.recyclerview.SpanSize
import ru.queuejw.lumetro.components.ui.recyclerview.SpannedGridLayoutManager
import ru.queuejw.lumetro.databinding.StartFragmentBinding
import ru.queuejw.lumetro.main.MainActivity
import ru.queuejw.lumetro.model.TileEntity

class StartFragment : BaseMainFragment<StartFragmentBinding>() {

    private var tilesAdapter: TilesAdapter? = null
    private var spannedGridLayoutManager: SpannedGridLayoutManager? = null
    private var isPortraitOrientation = false
    private var itemTouchHelper: ItemTouchHelper? = null
    private var handler: Handler? = null

    private var appReceiver: AppReceiver? = null

    private var iconDefaultSize: Int = 0
    private var iconSmallSize: Int = 0
    private var iconBigSize: Int = 0
    private var isFirstUpdate = true
    val isMoreTilesEnabled get() = prefs.showMoreTilesEnabled

    private val backCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                tilesAdapter?.let { if (it.isEditMode) it.setAdapterEditMode(false) }
            }
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): StartFragmentBinding? {
        return StartFragmentBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.resources.apply {
            isPortraitOrientation =
                configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            iconDefaultSize = getDimensionPixelSize(
                R.dimen.icon_size
            )
            iconSmallSize = getDimensionPixelSize(
                R.dimen.icon_size_small
            )
            iconBigSize = getDimensionPixelSize(R.dimen.icon_size_big)
        }
    }

    private fun setObserver() {
        viewModel.getTiles()?.observe(viewLifecycleOwner) {
            tilesAdapter?.apply {
                updateData(it)
                Log.d("update", it.toString())
                if (isFirstUpdate) {
                    isFirstUpdate = false
                }
            }
        }
    }

    private fun animateAllTiles(boolean: Boolean) {
        spannedGridLayoutManager ?: return
        if (!prefs.allowEditModeAnimation) return
        for (i in 0..spannedGridLayoutManager!!.itemCount) {
            binding.recyclerView.findViewHolderForAdapterPosition(i)?.apply {
                if (itemViewType == TileViewTypes.TYPE_PLACEHOLDER.type) continue
                if (!boolean) {
                    itemView.animate()?.cancel()
                    itemView.clearAnimation()
                    itemView.animate()?.translationX(0f)?.translationY(0f)?.setDuration(200)
                        ?.start()
                }
                itemView.animate()?.scaleY(if (boolean) 0.9f else 1f)
                    ?.scaleX(if (boolean) 0.9f else 1f)?.setDuration(200)
                    ?.alpha(if (boolean) 0.7f else 1f)?.start()
            }
        }
    }

    private fun setBackCallback(boolean: Boolean) {
        if (boolean) {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backCallback)
        } else {
            backCallback.remove()
        }
    }

    private fun adapterEditModeFunction(boolean: Boolean) {
        animateAllTiles(boolean)
        (activity as MainActivity?)?.setViewPagerUserInput(!boolean)
        setBackCallback(boolean)
    }

    private fun adapterTileWindowControl(boolean: Boolean) {
        binding.recyclerView.isScrollEnabled = boolean
    }

    private fun getNewTilesAdapter(context: Context): TilesAdapter {
        if (viewModel.getIconLoader() == null) {
            viewModel.createIconLoader(prefs.iconPackPackage != null, prefs.iconPackPackage)
        }
        return object : TilesAdapter(
            data = ArrayList(),
            iconSizes = Triple(iconSmallSize, iconDefaultSize, iconBigSize),
            isMoreTilesEnabled = isMoreTilesEnabled,
            accentColor = viewModel.getColorManager().getAccentColor(context),
            iconProvider = viewModel.getIconLoader()!!,
            editModeAnimation = prefs.allowEditModeAnimation
        ) {

            override fun saveTilesFunction(list: MutableList<TileEntity>) {
                viewModel.updateTilePositions(list)
                if ((list.filter { it.tileType != TileViewTypes.TYPE_PLACEHOLDER.type }).isEmpty()) {
                    (activity as MainActivity?)?.changePage(1)
                    viewPagerUserInputEnabled = false
                }
            }

            override fun editModeFunction(boolean: Boolean) {
                adapterEditModeFunction(boolean)
            }

            override fun tileWindowFunction(boolean: Boolean) {
                adapterTileWindowControl(boolean)
            }

            override fun onTileClick(entity: TileEntity) {
                launchAppWithAnimation(entity, context)
            }
        }
    }

    private fun launchAppWithAnimation(item: TileEntity, context: Context) {
        tilesAdapter ?: {
            launchApp(item.tilePackage, context)
        }.also {
            return
        }
        val duration = 400L
        var appPos: Int? = null
        var lastDelay = 0L
        val inter = FastOutSlowInInterpolator()
        for (i in 0..tilesAdapter!!.itemCount) {
            if (tilesAdapter!!.getTilesList()[i] == item) {
                appPos = i
                continue
            }
            val holder = binding.recyclerView.findViewHolderForAdapterPosition(i) ?: continue
            if (holder.itemViewType == TileViewTypes.TYPE_PLACEHOLDER.type) continue
            lastDelay = i * 10L + 210L
            holder.itemView.animate().setStartDelay(lastDelay).rotationY(-60f).translationX(-2000f)
                .translationY(-30f).scaleX(0.25f).scaleY(0.25f)
                .setDuration(duration).alpha(0f).setInterpolator(inter).start()
        }
        appPos?.let {
            binding.recyclerView.findViewHolderForAdapterPosition(it)?.itemView?.animate()
                ?.setStartDelay(lastDelay + 250L)?.rotationY(-60f)?.translationX(-1000f)
                ?.scaleX(0.25f)?.scaleY(0.25f)
                ?.setDuration(200L)?.alpha(0.1f)?.setInterpolator(inter)?.withEndAction {
                    handler?.postDelayed({
                        launchApp(item.tilePackage, context)
                    }, 100)
                }?.start()
        } ?: {
            launchApp(item.tilePackage, context)
        }
    }

    private fun configure(context: Context) {
        configureLayoutManager()
        tilesAdapter = getNewTilesAdapter(context)
        tilesAdapter?.let {
            itemTouchHelper = ItemTouchHelper(ItemTouchCallback(it))
        }
    }

    private fun getRowCount(): Int = when {
        isPortraitOrientation && isMoreTilesEnabled -> 12
        isPortraitOrientation -> 8
        else -> 12
    }

    private fun getColumnCount() = when {
        isPortraitOrientation && isMoreTilesEnabled -> 6
        isPortraitOrientation -> 4
        else -> 6
    }

    private fun getSpanSizeLookup(): SpannedGridLayoutManager.SpanSizeLookup {
        return SpannedGridLayoutManager.SpanSizeLookup { position ->
            val list = viewModel.getTiles()?.value
            val item = list?.getOrNull(position)
            when (item?.tileSize) {
                1 -> SpanSize(2, 2)
                2 -> SpanSize(4, 2)
                else -> SpanSize(1, 1)
            }
        }
    }

    private fun configureLayoutManager() {
        spannedGridLayoutManager =
            SpannedGridLayoutManager(RecyclerView.VERTICAL, getRowCount(), getColumnCount()).apply {
                itemOrderIsStable = true
                spanSizeLookup = getSpanSizeLookup()
            }
    }

    private fun updateLayoutManager() {
        configureLayoutManager()
        binding.recyclerView.layoutManager = spannedGridLayoutManager
    }

    private fun configureRecyclerView(context: Context) {
        binding.recyclerView.apply {
            adapter = tilesAdapter
            itemAnimator = null
            itemTouchHelper?.attachToRecyclerView(this)
            addItemDecoration(MarginItemDecoration(context.resources.getDimensionPixelSize(R.dimen.tile_margin)))
        }
        appReceiver?.let {
            AppReceiver.register(context, it)
        }
    }

    private fun onNewAppInstalled(mPackage: String, context: Context) {
        if (!prefs.autoPinEnabled) return
        tilesAdapter?.apply {
            lifecycleScope.launch(Dispatchers.IO) {
                val l = TileManager().pinNewTile(
                    context,
                    this@apply.getTilesList(),
                    mPackage,
                    viewModel.getDatabase().getTilesDao()
                )
                viewModel.updateTilesList(l)
            }
        }
    }

    private fun onAppRemoved(mPackage: String) {
        tilesAdapter?.apply {
            val list = getTilesList()
            val item = list.find { it.tilePackage == mPackage }
            if (item != null) {
                list.forEachIndexed { i, tile ->
                    if (tile.tilePackage == mPackage) {
                        list[i].apply {
                            tileType = TileViewTypes.TYPE_PLACEHOLDER.type
                            tilePackage = null
                            tileLabel = null
                            tileColor = null
                            tileSize = 0
                        }
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.getDatabase().getTilesDao().updateTile(list[i])
                            viewModel.updateTilesList(list)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        appReceiver = AppReceiver(
            onAppInstalled = {
                context?.let { context ->
                    onNewAppInstalled(it, context)
                }
            },
            onAppRemoved = {
                onAppRemoved(it)
            },
            onAppChanged = {}
        )
        binding.apply {
            applyInsets(recyclerView)
        }
        configure(view.context)
        updateLayoutManager()
        configureRecyclerView(view.context)
        setObserver()
    }

    private fun restoreTilesWithAnim() {
        val duration = 300L
        var lastDelay = 100L
        val inter = FastOutSlowInInterpolator()
        for (i in 0..binding.recyclerView.childCount) {
            val holder = binding.recyclerView.findViewHolderForAdapterPosition(i) ?: continue
            if (holder.itemViewType == TileViewTypes.TYPE_PLACEHOLDER.type) continue
            holder.itemView.animate().setStartDelay(lastDelay).rotationY(0f).translationX(0f)
                .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(duration).setInterpolator(inter).start()
            lastDelay = i * 20L + 100L
        }
    }

    override fun onResume() {
        restoreTilesWithAnim()
        super.onResume()
        Log.d("Start", "Resume")
    }

    override fun onPause() {
        super.onPause()
        tilesAdapter?.apply {
            if (isEditMode) setAdapterEditMode(false)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("Start", "Stop")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isPortraitOrientation = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
        updateLayoutManager()
    }

    override fun onDestroyView() {
        Log.d("Start", "Destroy view")
        appReceiver?.let {
            context?.let { context ->
                AppReceiver.unregister(context, it)
            }
        }
        appReceiver = null
        itemTouchHelper?.attachToRecyclerView(null)
        binding.recyclerView.apply {
            adapter = null
            layoutManager = null
        }
        tilesAdapter = null
        spannedGridLayoutManager = null
        itemTouchHelper = null
        setBackCallback(false)
        handler = null
        super.onDestroyView()
    }
}
