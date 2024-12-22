package ru.queuejw.mpl.content

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.arasthel.spannedgridlayoutmanager.SpanSize
import com.arasthel.spannedgridlayoutmanager.SpannedGridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Main
import ru.queuejw.mpl.MainViewModel
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.app.App
import ru.queuejw.mpl.content.data.tile.Tile
import ru.queuejw.mpl.content.data.tile.TileDao
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.LauncherStartScreenBinding
import ru.queuejw.mpl.databinding.SpaceTileBinding
import ru.queuejw.mpl.databinding.TileBinding
import ru.queuejw.mpl.helpers.dragndrop.ItemTouchCallback
import ru.queuejw.mpl.helpers.dragndrop.ItemTouchHelperAdapter
import ru.queuejw.mpl.helpers.dragndrop.ItemTouchHelperViewHolder
import ru.queuejw.mpl.helpers.receivers.PackageChangesReceiver
import ru.queuejw.mpl.helpers.ui.MetroRecyclerView
import ru.queuejw.mpl.helpers.utils.Utils
import kotlin.random.Random

// Start screen
class Start : Fragment() {

    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private var mSpannedLayoutManager: SpannedGridLayoutManager? = null
    private var mAdapter: NewStartAdapter? = null
    private var tiles: MutableList<Tile>? = null
    private var packageBroadcastReceiver: BroadcastReceiver? = null

    private var isBroadcasterRegistered = false

    private var _binding: LauncherStartScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel

    private var screenLoaded = false

    private val backCallback by lazy {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mAdapter?.let { if (it.isEditMode) it.disableEditMode() }
            }
        }
    }
    private val marginDecor by lazy {
        Utils.MarginItemDecoration(14)
    }
    private val mItemTouchHelper: ItemTouchHelper? by lazy {
        mAdapter?.let {
            ItemTouchHelper(ItemTouchCallback(it))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LauncherStartScreenBinding.inflate(inflater, container, false)
        binding.startTiles.setOnClickListener {
            mAdapter?.let { if (it.isEditMode) it.disableEditMode() }
        }
        setRecyclerViewInsets(binding.startTiles)
        return binding.root
    }

    private fun setRecyclerViewInsets(view: MetroRecyclerView) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                bottom = systemBarInsets.bottom,
                left = systemBarInsets.left + view.paddingLeft,
                right = systemBarInsets.right + view.paddingEnd,
                top = systemBarInsets.top
            )
            insets
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (context != null) {
            viewLifecycleOwner.lifecycleScope.launch(defaultDispatcher) {
                mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
                tiles = mainViewModel.getViewModelTileDao().getTilesList()
                setupRecyclerViewLayoutManager(requireContext())
                setupAdapter()
                withContext(mainDispatcher) {
                    configureRecyclerView()
                    registerBroadcast()
                    observe()
                }
                screenLoaded = true
                cancel("done")
            }
        }
    }

    /**
     * Sets up a data observer
     * @see onViewCreated
     */
    private fun observe() {
        if (!screenLoaded || mainViewModel.getViewModelTileDao().getTilesLiveData()
                .hasActiveObservers()
        ) return
        mainViewModel.getViewModelTileDao().getTilesLiveData().observe(viewLifecycleOwner) {
            mAdapter ?: return@observe
            if (mAdapter!!.list != it) {
                Log.w("obs", "set new data")
                mAdapter!!.setData(it)
            }
        }
    }

    /**
     * Stops the data observer
     */
    private fun stopObserver() {
        mainViewModel.getViewModelTileDao().getTilesLiveData().removeObservers(viewLifecycleOwner)
    }

    override fun onDestroyView() {
        stopObserver()
        super.onDestroyView()
        _binding = null
    }

    /**
     * Sets up the adapter and ItemTouchHelper
     * @see onViewCreated
     */
    private fun setupAdapter() {
        mAdapter = NewStartAdapter(requireContext(), tiles!!)
    }

    /**
     * Adds a Callback for Activity, which is needed to exit desktop edit mode using the back button/gesture gesture
     * @see NewStartAdapter.enableEditMode
     */
    private fun addCallback() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backCallback)
    }

    /**
     * Removes Callback for Activity after exiting desktop edit mode
     * @see NewStartAdapter.disableEditMode
     */
    private fun removeCallback() {
        backCallback.remove()
    }

    /**
     * Configures RecyclerView, sets Adapter and LayoutManager
     */
    private fun configureRecyclerView() {
        binding.startTiles.apply {
            layoutManager = mSpannedLayoutManager
            adapter = mAdapter
            mItemTouchHelper?.attachToRecyclerView(this)
            addItemDecoration(marginDecor)
        }
    }

    /**
     * Configures the SpannedLayoutManager using the current settings and screen orientation
     *
     * @param context Context
     */
    private fun setupRecyclerViewLayoutManager(context: Context?) {
        if (mSpannedLayoutManager != null) mSpannedLayoutManager = null
        if (!Main.isLandscape) {
            // phone
            mSpannedLayoutManager = SpannedGridLayoutManager(
                orientation = RecyclerView.VERTICAL,
                rowCount = 8,
                columnCount = 4
            )
        } else {
            // Landscape orientation
            val tablet = context?.resources?.getBoolean(R.bool.isTablet) == true
            mSpannedLayoutManager = if (tablet) {
                // tablet
                SpannedGridLayoutManager(
                    orientation = RecyclerView.VERTICAL,
                    rowCount = 2,
                    columnCount = 4
                )
            } else {
                // phone but landscape
                SpannedGridLayoutManager(
                    orientation = RecyclerView.VERTICAL,
                    rowCount = 3,
                    columnCount = 4
                )
            }
        }
        mSpannedLayoutManager?.apply {
            itemOrderIsStable = true
            spanSizeLookup = SpannedGridLayoutManager.SpanSizeLookup { position ->
                when (tiles!![position].tileSize) {
                    "small" -> SpanSize(1, 1)
                    "medium" -> SpanSize(2, 2)
                    "big" -> SpanSize(4, 2)
                    else -> SpanSize(1, 1)
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Main.isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        setupRecyclerViewLayoutManager(context)
        binding.startTiles.apply {
            layoutManager = mSpannedLayoutManager
        }
    }

    override fun onResume() {
        if (screenLoaded) observe()
        if (Application.isAppOpened) {
            viewLifecycleOwner.lifecycleScope.launch {
                animateTiles(false, null, null)
            }
            Application.isAppOpened = false
        }
        Application.isStartMenuOpened = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mAdapter?.apply {
            if (isEditMode) disableEditMode()
        }
        Application.isStartMenuOpened = false
    }

    override fun onStop() {
        super.onStop()
        Application.isStartMenuOpened = false
    }

    /**
     * Sets BroadcastReceiver, which is needed to update the tile list when uninstalling / installing applications
     */
    @SuppressLint("InlinedApi", "UnspecifiedRegisterReceiverFlag")
    private fun registerBroadcast() {
        Log.d("Start", "reg broadcaster")
        if (!isBroadcasterRegistered) {
            isBroadcasterRegistered = true
            packageBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val packageName = intent.getStringExtra("package")
                    // End early if it has anything to do with us.
                    if (packageName.isNullOrEmpty()) return
                    val action = intent.getIntExtra("action", 42)
                    when (action) {
                        PackageChangesReceiver.PACKAGE_INSTALLED -> {
                            Log.d("Start", "pkg installed")
                            val bool = PREFS.iconPackPackage != "null"
                            (requireActivity() as Main).generateIcon(packageName, bool)
                            mainViewModel.addAppToList(
                                App(
                                    appLabel = context.packageManager.getApplicationInfo(
                                        packageName,
                                        0
                                    ).name,
                                    appPackage = packageName,
                                    id = Random.nextInt()
                                )
                            )
                            if (PREFS.pinNewApps) {
                                pinApp(packageName)
                            }
                        }

                        PackageChangesReceiver.PACKAGE_REMOVED -> {
                            packageName.apply { broadcastListUpdater(this, true) }
                        }

                        else -> {
                            packageName.apply { broadcastListUpdater(this, false) }
                        }
                    }
                }
            }
            IntentFilter().apply {
                addAction("ru.dimon6018.metrolauncher.PACKAGE_CHANGE_BROADCAST")
            }.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().registerReceiver(
                        packageBroadcastReceiver,
                        it,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    requireActivity().registerReceiver(packageBroadcastReceiver, it)
                }
            }
        } else {
            Log.d("Start", "broadcaster already registered")
        }
    }

    /**
     * Called from BroadcastReceiver, updates the tile list
     * @param packageName Application package name
     * @param isDelete if True, removes the application tile with [packageName] package name
     * @see registerBroadcast
     */
    private fun broadcastListUpdater(packageName: String, isDelete: Boolean) {
        packageName.apply {
            Log.d("Start", "update list by broadcaster")
            CoroutineScope(ioDispatcher).launch {
                var newList = mainViewModel.getViewModelTileDao().getTilesList()
                if (isDelete) {
                    newList.forEach {
                        if (it.tilePackage == packageName) {
                            Log.d("Start", "delete")
                            destroyTile(it)
                        }
                    }
                    newList = mainViewModel.getViewModelTileDao().getTilesList()
                }
                withContext(mainDispatcher) {
                    mAdapter?.setData(newList)
                }
            }
        }
    }

    /**
     * Adds an application tile to the desktop
     * @param packageName Application package name
     */
    private fun pinApp(packageName: String) {
        lifecycleScope.launch(ioDispatcher) {
            if (mAdapter != null) {
                var pos = 0
                for (i in 0..<mAdapter!!.list.size) {
                    if (mAdapter!!.list[i].tileType == -1) {
                        pos = i
                        break
                    }
                }
                val id = Random.nextLong(1000, 2000000)
                val item = Tile(
                    pos, id, -1, 0,
                    isSelected = false,
                    tileSize = Utils.generateRandomTileSize(true),
                    tileLabel = activity?.packageManager?.getApplicationInfo(packageName, 0)
                        ?.loadLabel(requireActivity().packageManager!!).toString(),
                    tilePackage = packageName
                )
                mainViewModel.getViewModelTileDao().addTile(item)
            }
        }
    }

    /**
     * Removes BroadcastReceiver
     */
    private fun unregisterBroadcast() {
        isBroadcasterRegistered = false
        packageBroadcastReceiver?.apply {
            requireActivity().unregisterReceiver(packageBroadcastReceiver)
            packageBroadcastReceiver = null
        } ?: run {
            Log.d("Start", "unregisterBroadcast() was called to a null receiver.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcast()
    }

    /**
     * “Removes” a tile (replaces it with a Placeholder)
     * @param tile Tile object
     */
    private suspend fun destroyTile(tile: Tile) {
        tile.apply {
            tileType = -1
            tileSize = "small"
            tilePackage = ""
            tileColor = -1
            tileLabel = ""
            id = this.id!! / 2
            mainViewModel.getViewModelTileDao().updateTile(this)
        }
    }

    private suspend fun animateTiles(launchApp: Boolean, launchAppPos: Int?, packageName: String?) {
        mAdapter ?: return
        mSpannedLayoutManager ?: return
        if (launchApp && launchAppPos != null && packageName != null) {
            enterToAppAnim(launchAppPos, packageName)
        } else if (!launchApp) {
            exitFromAppAnim()
        }
    }

    private suspend fun enterToAppAnim(position: Int, packageName: String) {
        binding.startTiles.isScrollEnabled = false
        val first = mSpannedLayoutManager!!.firstVisiblePosition
        val last = mSpannedLayoutManager!!.lastVisiblePosition
        val interpolator = AccelerateInterpolator()
        var duration = 175L
        for (i in last downTo first) {
            if (tiles!![i] == tiles!![position]) continue
            val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
            if (holder.itemViewType == mAdapter!!.spaceType) continue
            delay(5)
            duration += 10L
            holder.itemView.animate().rotationY(-110f).translationX(-1000f).translationY(-100f)
                .setInterpolator(interpolator).setDuration(duration).start()
        }
        delay(250)
        duration = 150L
        for (i in last downTo first) {
            if (tiles!![i] != tiles!![position]) continue
            val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
            holder.itemView.animate().rotationY(-90f).translationX(-1000f).translationY(-100f)
                .setInterpolator(interpolator).setDuration(duration).withEndAction {
                    binding.startTiles.isScrollEnabled = true
                    startApp(packageName)
                }.start()
        }
    }

    private suspend fun exitFromAppAnim() {
        val first = mSpannedLayoutManager!!.firstVisiblePosition
        val last = mSpannedLayoutManager!!.lastVisiblePosition
        var duration = 300L
        for (i in last downTo first) {
            val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
            delay(5)
            duration += 5L
            holder.itemView.animate().rotationY(0f).translationX(0f).translationY(0f)
                .setInterpolator(AccelerateInterpolator()).setDuration(duration).start()
        }
    }

    /**
     * Launches the application or activity of MPL settings
     * @param packageName Application package name
     */
    private fun startApp(packageName: String) {
        Application.isAppOpened = true
        if (activity != null) {
            val intent = when (packageName) {
                context?.packageName -> Intent(
                    requireActivity(),
                    SettingsActivity::class.java
                )

                else -> requireActivity().packageManager.getLaunchIntentForPackage(packageName)
                    ?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            }
            intent?.let { startActivity(it) }
        }
    }

    /**
     * Tile Screen Adapter
     * @param context Context
     * @param list Current tile list
     * @see setupAdapter
     */
    inner class NewStartAdapter(val context: Context, var list: MutableList<Tile>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemTouchHelperAdapter {

        val defaultTileType: Int = 0
        val spaceType: Int = -1

        var selectedItem: Tile? = null

        private val accentColor: Int by lazy { Utils.launcherAccentColor(requireActivity().theme) }

        private val tileSmallSize by lazy {
            context.resources.getDimensionPixelSize(
                R.dimen.tile_small
            )
        }
        private val tileMediumSize by lazy {
            context.resources.getDimensionPixelSize(
                R.dimen.tile_medium
            )
        }
        private val tileBigSize by lazy {
            context.resources.getDimensionPixelSize(
                R.dimen.tile_big
            )
        }

        var isEditMode = false

        init {
            setHasStableIds(true)
        }

        /**
         * Called to update the data
         * @param newData New tile list
         * @see observe
         */
        fun setData(newData: MutableList<Tile>) {
            val diffUtilCallback = DiffUtilCallback(list, newData)
            val diffResult = DiffUtil.calculateDiff(diffUtilCallback, false)
            diffResult.dispatchUpdatesTo(this)
            list = newData
            tiles = newData
        }

        /**
         * Called to activate the desktop editing mode
         */
        fun enableEditMode() {
            Log.d("EditMode", "enter edit mode")
            (requireActivity() as Main).configureViewPagerScroll(false)
            addCallback()
            isEditMode = true
            for (i in 0..binding.startTiles.childCount) {
                val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
                if (holder.itemViewType == -1) continue
                holder.itemView.animate().scaleX(0.9f).scaleY(0.9f).setDuration(300).start()
            }
            startEditModeAnim()
        }

        fun startEditModeAnim() {
            for (i in 0..binding.startTiles.childCount) {
                val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
                if (holder.itemViewType == -1) continue
                animateItemEditMode(holder.itemView, i)
            }
        }

        /**
         * Called to disable the desktop editing mode
         */
        fun disableEditMode() {
            Log.d("EditMode", "exit edit mode")
            removeCallback()
            (requireActivity() as Main).configureViewPagerScroll(true)
            isEditMode = false

            for (i in 0..binding.startTiles.childCount) {
                val holder = binding.startTiles.findViewHolderForAdapterPosition(i) ?: continue
                if (holder.itemViewType == -1) continue
                holder.itemView.clearAnimation()
                holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                holder.itemView.animate().setDuration(300).translationY(0f).translationX(0f).start()
            }
        }

        // tile animation in edit mode
        fun animateItemEditMode(view: View, position: Int) {
            if (!PREFS.isTilesAnimEnabled || !isEditMode || list[position] == selectedItem) return
            val rad = 5
            val randomX = Random.nextFloat() * 2 * rad - rad
            val randomY = Random.nextFloat() * 2 * rad - rad
            if (view.scaleX != 0.9f) view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(300)
                .start()
            view.animate().setDuration(1000).translationX(randomX).translationY(randomY)
                .withEndAction {
                    animateItemEditMode(view, position)
                }.start()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                defaultTileType -> TileViewHolder(TileBinding.inflate(inflater, parent, false))
                else -> SpaceViewHolder(SpaceTileBinding.inflate(inflater, parent, false))
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun getItemId(position: Int): Long {
            return list[position].id!!
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder.itemViewType) {
                defaultTileType -> bindDefaultTile(
                    holder as TileViewHolder,
                    list[position],
                    position
                )
            }
        }

        /**
         * This function is needed for tile configuration
         * @param holder TileViewHolder
         * @param item Tile object
         * @param position Holder position
         * @see onBindViewHolder
         */
        private fun bindDefaultTile(holder: TileViewHolder, item: Tile, position: Int) {
            setTileColor(holder, item)
            setTileSize(item, holder.binding.tileLabel)
            setTileIconSize(holder.binding.tileIcon, item.tileSize)
            setTileText(holder.binding.tileLabel, item)
            setTileIcon(holder.binding.tileIcon, item)
            setTileAnimation(holder.itemView, position)
        }

        /**
         * Creates an animation in edit mode or restores the default scale and translation settings
         * @param view ItemView
         * @param pos View position
         * @see bindDefaultTile
         */
        private fun setTileAnimation(view: View, pos: Int) {
            if (isEditMode) {
                animateItemEditMode(view, pos)
            } else {
                if (view.scaleX != 1f) view.scaleX = 1f
                if (view.scaleY != 1f) view.scaleY = 1f
                if (view.translationY != 0f) view.translationY = 0f
                if (view.translationX != 0f) view.translationX = 0f
            }
        }

        /**
         * Sets the text of the tile
         * @param textView MaterialTextView of a tile
         * @param item Tile object
         * @see bindDefaultTile
         */
        private fun setTileText(textView: MaterialTextView, item: Tile) {
            textView.text = item.tileLabel
        }

        /**
         * Sets the application icon to the tile from the “cache”.
         * @param imageView ImageView
         * @param item Tile object
         * @see bindDefaultTile
         */
        private fun setTileIcon(imageView: ImageView, item: Tile) {
            imageView.load(mainViewModel.getIconFromCache(item.tilePackage))
        }

        /**
         * Sets the size of the application icon on the tile
         * @param imageView ImageView
         * @param tileSize tileSize value, which is stored in the Tile object
         * @param res Context.resources
         * @see bindDefaultTile
         */
        private fun setTileIconSize(imageView: ImageView, tileSize: String) {
            imageView.layoutParams.apply {
                when (tileSize) {
                    "small" -> {
                        width = tileSmallSize
                        height = tileSmallSize
                    }

                    "big" -> {
                        width = tileBigSize
                        height = tileBigSize
                    }

                    else -> {
                        width = tileMediumSize
                        height = tileMediumSize
                    }
                }
            }
        }

        /**
         * Sets the color of the tile. If the desktop wallpaper is on, makes the tiles transparent
         * @param holder TileViewHolder
         * @param item Tile object
         * @see bindDefaultTile
         */
        private fun setTileColor(holder: TileViewHolder, item: Tile) {
            holder.binding.cardContainer.setCardBackgroundColor(
                if (item.tileColor != -1)
                    Utils.getTileColorFromPrefs(
                        item.tileColor!!,
                        context
                    )
                else
                    accentColor
            )
        }

        /**
         * Sets the size of the application icon on the tile
         * @param item Tile object
         * @param mTextView MaterialTextView of a tile
         * @see bindDefaultTile
         */
        private fun setTileSize(item: Tile, mTextView: MaterialTextView) {
            mTextView.visibility = when (item.tileSize) {
                "small" -> View.GONE
                else -> View.VISIBLE
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (list[position].tileType) {
                0 -> defaultTileType
                else -> spaceType
            }
        }

        /**
         * Called when moving tiles
         */
        override fun onItemMove(fromPosition: Int, toPosition: Int) {
            if (!isEditMode) {
                enableEditMode()
                return
            }
            if (fromPosition == toPosition) return
            Log.d("ItemMove", "from pos: $fromPosition")
            Log.d("ItemMove", "to pos: $toPosition")
            list.add(toPosition, list.removeAt(fromPosition))
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onItemDismiss(position: Int) {
            if (!isEditMode) return
            notifyItemChanged(position)
        }

        override fun onDragAndDropCompleted() {
            if (!isEditMode) return
            lifecycleScope.launch(defaultDispatcher) {
                val newData = ArrayList<Tile>()
                for (i in 0 until list.size) {
                    val item = list[i]
                    item.tilePosition = i
                    newData.add(item)
                }
                mainViewModel.getViewModelTileDao().updateAllTiles(newData)
                withContext(mainDispatcher) {
                    startEditModeAnim()
                }
            }
        }

        /**
         * A popup window that shows the tile buttons (resize, unpin, customize)
         * @param holder TileViewHolder
         * @param item Tile object
         * @param position Tile position
         */
        private fun showTilePopupWindow(holder: TileViewHolder, item: Tile, position: Int) {
            binding.startTiles.isScrollEnabled = false
            holder.itemView.clearAnimation()
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(
                if (item.tileSize == "small") R.layout.tile_window_small else R.layout.tile_window,
                holder.itemView as ViewGroup,
                false
            )
            var width = holder.itemView.width + 50
            var height = holder.itemView.height + 25
            val popupWindow = PopupWindow(popupView, width, height, true)
            val resize = popupView.findViewById<MaterialCardView>(R.id.resize)
            val resizeIcon = popupView.findViewById<ImageView>(R.id.resizeIco)
            val settings = popupView.findViewById<MaterialCardView>(R.id.settings)
            val remove = popupView.findViewById<MaterialCardView>(R.id.remove)
            popupWindow.setOnDismissListener {
                binding.startTiles.isScrollEnabled = true
                selectedItem = null
                animateItemEditMode(holder.itemView, position)
            }
            popupWindow.showAsDropDown(
                holder.itemView,
                -(width / 10),
                -height,
                Gravity.CENTER
            )
            applyResizeIcon(resizeIcon, item)
            resize.setOnClickListener {
                resizeTile(item, position)
                popupWindow.dismiss()
            }
            remove.setOnClickListener {
                removeTile(item, position)
                popupWindow.dismiss()
            }
            settings.setOnClickListener {
                showSettingsBottomSheet(item)
                popupWindow.dismiss()
            }
        }

        private fun applyResizeIcon(view: ImageView, item: Tile) {
            view.apply {
                when (item.tileSize) {
                    "small" -> {
                        rotation = 45f
                        setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_arrow
                            )
                        )
                    }

                    "medium" -> {
                        rotation = 0f
                        setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_arrow
                            )
                        )
                    }

                    "big" -> {
                        rotation = -135f
                        setImageDrawable(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.ic_arrow
                            )
                        )
                    }

                    else -> setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_arrow
                        )
                    )
                }
            }
        }

        private fun removeTile(item: Tile, position: Int) {
            lifecycleScope.launch(ioDispatcher) {
                destroyTile(item)
                withContext(mainDispatcher) {
                    notifyItemChanged(position)
                }
            }
        }

        private fun resizeTile(item: Tile, position: Int) {
            lifecycleScope.launch(ioDispatcher) {
                when (item.tileSize) {
                    "small" -> item.tileSize = "medium"
                    "medium" -> item.tileSize = "big"
                    "big" -> item.tileSize = "small"
                }
                mainViewModel.getViewModelTileDao().updateTile(item)
                withContext(mainDispatcher) {
                    notifyItemChanged(position)
                }
            }
        }

        /**
         * The bottom panel with tile settings, which rises from the bottom
         * @param item Tile object
         */
        fun showSettingsBottomSheet(item: Tile) {
            val bottomSheet = BottomSheetDialog(context)
            bottomSheet.setContentView(R.layout.start_tile_settings_bottomsheet)
            bottomSheet.dismissWithAnimation = true
            val bottomSheetInternal =
                bottomSheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from<View?>(bottomSheetInternal!!).peekHeight =
                context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_size)
            configureTileBottomSheet(bottomSheetInternal, bottomSheet, item)
            bottomSheet.show()
        }

        /**
         * Creating the interface of the bottom panel is done here
         * @param bottomSheetInternal bottomSheet view
         * @param bottomSheet BottomSheetDialog object
         * @param item Tile object
         */
        private fun configureTileBottomSheet(
            bottomSheetInternal: View,
            bottomSheet: BottomSheetDialog,
            item: Tile
        ) {
            val label = bottomSheetInternal.findViewById<MaterialTextView>(R.id.appLabelSheet)
            val colorSub = bottomSheetInternal.findViewById<MaterialTextView>(R.id.chooseColorSub)
            val removeColor =
                bottomSheetInternal.findViewById<MaterialTextView>(R.id.chooseColorRemove)
            val uninstall = bottomSheetInternal.findViewById<MaterialCardView>(R.id.uninstallApp)
            val uninstallLabel =
                bottomSheetInternal.findViewById<MaterialTextView>(R.id.uninstall_label)
            val changeLabel = bottomSheetInternal.findViewById<MaterialCardView>(R.id.editAppLabel)
            val changeColor = bottomSheetInternal.findViewById<MaterialCardView>(R.id.editTileColor)
            val editor = bottomSheetInternal.findViewById<EditText>(R.id.textEdit)
            val textFiled = bottomSheetInternal.findViewById<TextInputLayout>(R.id.textField)
            val labelLayout = bottomSheetInternal.findViewById<LinearLayout>(R.id.changeLabelLayout)
            val labelChangeBtn =
                bottomSheetInternal.findViewById<MaterialCardView>(R.id.labelChange)
            val editLabelText =
                bottomSheetInternal.findViewById<MaterialTextView>(R.id.editAppLabelText)
            val appInfo = bottomSheetInternal.findViewById<MaterialCardView>(R.id.appInfo)
            val chooseTileColor =
                bottomSheetInternal.findViewById<MaterialTextView>(R.id.choose_tile_color)
            val appInfoLabel =
                bottomSheetInternal.findViewById<MaterialTextView>(R.id.app_info_label)

            (if (PREFS.customLightFontPath != null) Application.customLightFont else Application.customFont)?.let {
                label.typeface = it
                colorSub.typeface = it
                removeColor.typeface = it
                uninstallLabel.typeface = it
                editor.typeface = it
                editLabelText.typeface = it
                chooseTileColor.typeface = it
                appInfoLabel.typeface = it
                textFiled.typeface = it
            }
            editLabelText.setOnClickListener {
                labelLayout.visibility = View.VISIBLE
            }
            val originalLabel = context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(
                    item.tilePackage,
                    0
                )
            ).toString()
            label.text = item.tileLabel
            editor.setText(item.tileLabel)
            editor.hint = originalLabel
            if (item.tileColor == -1) {
                colorSub.visibility = View.GONE
                removeColor.visibility = View.GONE
            } else {
                colorSub.setTextColor(Utils.getTileColorFromPrefs(item.tileColor!!, context))
                colorSub.text = getString(
                    R.string.tileSettings_color_sub,
                    Utils.getTileColorName(item.tileColor!!, context)
                )
            }
            changeLabel.setOnClickListener {
                labelLayout.visibility = View.VISIBLE
            }
            labelChangeBtn.setOnClickListener {
                lifecycleScope.launch(ioDispatcher) {
                    item.tileLabel =
                        if (editor.text.toString() == "") originalLabel else editor.text.toString()
                    mainViewModel.getViewModelTileDao().updateTile(item)
                }
                bottomSheet.dismiss()
            }
            removeColor.setOnClickListener {
                lifecycleScope.launch(ioDispatcher) {
                    item.tileColor = -1
                    mainViewModel.getViewModelTileDao().updateTile(item)
                }
                bottomSheet.dismiss()
            }
            changeColor.setOnClickListener {
                val dialog = AccentDialog()
                dialog.configure(item, this@NewStartAdapter, mainViewModel.getViewModelTileDao())
                dialog.show(childFragmentManager, "accentDialog")
                bottomSheet.dismiss()
            }
            uninstall.setOnClickListener {
                startActivity(Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:" + item.tilePackage)))
                bottomSheet.dismiss()
            }
            appInfo.setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                        Uri.parse(
                            "package:" + item.tilePackage
                        )
                    )
                )
                bottomSheet.dismiss()
            }
        }

        /**
         * Default TileViewHolder (user apps)
         * @param binding TileBinding
         */
        @SuppressLint("ClickableViewAccessibility")
        inner class TileViewHolder(val binding: TileBinding) :
            RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

            init {
                binding.cardContainer.apply {
                    if (PREFS.coloredStroke) strokeColor = accentColor
                    alpha = PREFS.tilesTransparency
                    setOnClickListener {
                        handleClick()
                    }
                    setOnLongClickListener {
                        handleLongClick()
                        true
                    }
                }
                if (PREFS.customFontInstalled) Application.customFont?.let {
                    binding.tileLabel.typeface = it
                }
            }

            /**
             * Called by clicking on a tile
             */
            private fun handleClick() {
                val item = list[absoluteAdapterPosition]
                if (isEditMode) {
                    selectedItem = item
                    showTilePopupWindow(this@TileViewHolder, item, absoluteAdapterPosition)
                } else {
                    if (PREFS.isTilesAnimEnabled) {
                        lifecycleScope.launch {
                            animateTiles(true, absoluteAdapterPosition, item.tilePackage)
                        }
                    } else {
                        startApp(item.tilePackage)
                    }
                }
            }

            /**
             * Called by long pressing on a tile
             */
            private fun handleLongClick() {
                if (!isEditMode && !PREFS.isStartBlocked) enableEditMode()
            }

            override fun onItemSelected() {}
            override fun onItemClear() {}
        }

        /**
         * Placeholder tile
         * @param binding SpaceTileBinding
         */
        inner class SpaceViewHolder(binding: SpaceTileBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    if (isEditMode) disableEditMode()
                }
            }
        }
    }

    /**
     * DiffUtilCallback is needed to efficiently update the tile list
     */
    class DiffUtilCallback(private val old: List<Tile>, private val new: List<Tile>) :
        DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return old.size
        }

        override fun getNewListSize(): Int {
            return new.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition] == new[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return old[oldItemPosition].tilePackage == new[newItemPosition].tilePackage
        }
    }

    /**
     * Dialog in which the user can select a color for the tile
     */
    class AccentDialog : DialogFragment() {

        private lateinit var item: Tile
        private lateinit var dao: TileDao
        private lateinit var mAdapter: NewStartAdapter

        //available colors
        private val viewIds = arrayOf(
            R.id.choose_color_lime, R.id.choose_color_green, R.id.choose_color_emerald,
            R.id.choose_color_cyan, R.id.choose_color_teal, R.id.choose_color_cobalt,
            R.id.choose_color_indigo, R.id.choose_color_violet, R.id.choose_color_pink,
            R.id.choose_color_magenta, R.id.choose_color_crimson, R.id.choose_color_red,
            R.id.choose_color_orange, R.id.choose_color_amber, R.id.choose_color_yellow,
            R.id.choose_color_brown, R.id.choose_color_olive, R.id.choose_color_steel,
            R.id.choose_color_mauve, R.id.choose_color_taupe
        )

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        }

        fun configure(i: Tile, a: NewStartAdapter, d: TileDao) {
            item = i
            dao = d
            mAdapter = a
        }

        override fun onStart() {
            super.onStart()
            dialog?.apply {
                window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTitle("TILE COLOR")
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.accent_dialog, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            val back = view.findViewById<FrameLayout>(R.id.back_accent_menu)
            back.setOnClickListener { dismiss() }
            for (i in 0..<viewIds.size) {
                setOnClick(view.findViewById<ImageView>(viewIds[i]), i)
            }
        }

        private fun setOnClick(colorView: View, value: Int) {
            colorView.setOnClickListener {
                updateTileColor(value)
            }
        }

        /**
         * Changes the color of the tile and saves it
         * @param color Color value
         * @see setOnClick
         */
        private fun updateTileColor(color: Int) {
            lifecycleScope.launch(Dispatchers.IO) {
                item.tileColor = color
                dao.updateTile(item)
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }

        override fun dismiss() {
            mAdapter.notifyItemChanged(item.tilePosition!!)
            mAdapter.showSettingsBottomSheet(item)
            super.dismiss()
        }
    }
}