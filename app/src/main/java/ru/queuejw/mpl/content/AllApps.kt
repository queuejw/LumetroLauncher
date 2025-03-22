package ru.queuejw.mpl.content

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import coil3.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.Application.Companion.customLightFont
import ru.queuejw.mpl.Application.Companion.isAppOpened
import ru.queuejw.mpl.Main
import ru.queuejw.mpl.Main.Companion.isStartScreenEmpty
import ru.queuejw.mpl.MainViewModel
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.app.App
import ru.queuejw.mpl.content.data.tile.Tile
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.AppBinding
import ru.queuejw.mpl.databinding.LauncherAllAppsScreenBinding
import ru.queuejw.mpl.helpers.receivers.PackageChangesReceiver
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.utils.Utils
import kotlin.random.Random

class AllApps : Fragment() {

    private lateinit var recyclerViewLM: LinearLayoutManager

    private var appAdapter: AppAdapter? = null

    private var packageBroadcastReceiver: BroadcastReceiver? = null

    private var isSearching = false
    private var isBroadcasterRegistered = false

    private var popupWindow: PopupWindow? = null
    private var isWindowVisible = false

    private lateinit var mainViewModel: MainViewModel

    private var _binding: LauncherAllAppsScreenBinding? = null
    private val binding get() = _binding!!

    private val accentColor by lazy {
        Utils.launcherAccentColor(requireActivity().theme)
    }

    private val bottomDecor by lazy {
        BottomOffsetDecoration(
            requireContext().resources.getDimensionPixelSize(
                R.dimen.recyclerViewPadding
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LauncherAllAppsScreenBinding.inflate(inflater, container, false)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setUi()
        return binding.root
    }

    private fun setUi() {
        if (PREFS.isSettingsBtnEnabled) {
            binding.settingsBtn.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    activity?.let { it.startActivity(Intent(it, SettingsActivity::class.java)) }
                }
            }
        }
        binding.searchBtn.setOnClickListener {
            searchFunction()
        }
        binding.searchBackBtn.setOnClickListener {
            disableSearch()
        }
        lifecycleScope.launch(Dispatchers.Default) {
            prepareRecyclerView()
            prepareData()
        }
        Utils.applyWindowInsets(binding.root)
    }

    private suspend fun prepareRecyclerView() {
        recyclerViewLM = LinearLayoutManager(requireActivity())
        withContext(Dispatchers.Main) {
            binding.appList.apply {
                layoutManager = recyclerViewLM
                addItemDecoration(bottomDecor)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PREFS.customFontInstalled) customFont?.let {
            binding.searchTextview.typeface = it
            binding.noResults.typeface = it
        }
        if (PREFS.prefs.getBoolean("tip2Enabled", true)) {
            tipDialog()
            PREFS.prefs.edit { putBoolean("tip2Enabled", false) }
        }
    }

    private suspend fun prepareData() {
        appAdapter = AppAdapter(mainViewModel.getAppList())
        withContext(Dispatchers.Main) {
            configureRecyclerView()
        }
    }

    private fun tipDialog() {
        WPDialog(requireContext()).setTopDialog(true)
            .setTitle(getString(R.string.tip))
            .setMessage(getString(R.string.tip2))
            .setPositiveButton(getString(android.R.string.ok), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configureRecyclerView() {
        binding.appList.apply {
            adapter = appAdapter
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "InlinedApi")
    private fun registerBroadcast() {
        if (!isBroadcasterRegistered) {
            isBroadcasterRegistered = true
            packageBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val packageName = intent.getStringExtra("package")
                    // End early if it has anything to do with us.
                    if (packageName.isNullOrEmpty()) return
                    val action = intent.getIntExtra("action", 42)
                    packageName.apply {
                        when (action) {
                            PackageChangesReceiver.PACKAGE_INSTALLED -> {
                                val bool = PREFS.iconPackPackage != "null"
                                mainViewModel.addIconToCache(
                                    packageName,
                                    (requireActivity() as Main).generateIcon(packageName, bool)
                                )
                                broadcastListUpdater(context)
                            }

                            PackageChangesReceiver.PACKAGE_REMOVED -> {
                                //I don't think that's gonna work.
                                mainViewModel.removeIconFromCache(packageName)
                                broadcastListUpdater(context)
                            }

                            else -> {
                                broadcastListUpdater(context)
                            }
                        }
                    }
                }
            }
            // We want this fragment to receive the package change broadcast,
            // since otherwise it won't be notified when there are changes to that.
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
        }
    }

    private fun broadcastListUpdater(context: Context) {
        mainViewModel.setAppList(Utils.setUpApps(context))
        appAdapter?.setData(mainViewModel.getAppList())
    }

    private fun unregisterBroadcast() {
        isBroadcasterRegistered = false
        packageBroadcastReceiver?.apply {
            requireActivity().unregisterReceiver(packageBroadcastReceiver)
            packageBroadcastReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBroadcast()
    }

    override fun onPause() {
        if (isSearching && !PREFS.showKeyboardWhenOpeningAllApps) disableSearch()
        if (isWindowVisible == true) {
            popupWindow?.dismiss()
            popupWindow = null
        }
        super.onPause()
    }

    override fun onResume() {
        registerBroadcast()
        super.onResume()
        if (PREFS.showKeyboardWhenOpeningAllApps) searchFunction()
        if (isAppOpened) {
            if (PREFS.isAAllAppsAnimEnabled) clearAnimation()
            isAppOpened = false
        }
        if (!binding.appList.isScrollEnabled) {
            binding.appList.isScrollEnabled = true
        }
    }

    private fun clearAnimation() {
        val first =
            (binding.appList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last =
            (binding.appList.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        binding.appList.isScrollEnabled = false
        lifecycleScope.launch {
            for (i in last downTo first) {
                val view = binding.appList.findViewHolderForAdapterPosition(i)?.itemView ?: continue
                view.animate().translationX(0f).alpha(1f).rotationY(0f).setDuration(400).start()
            }
        }
    }

    private fun disableSearch() {
        if (!isSearching) return
        isSearching = false
        binding.noResults.visibility = View.GONE
        binding.apply {
            hideKeyboard(search.editText as? AutoCompleteTextView)
            (search.editText as? AutoCompleteTextView)?.apply {
                text.clear()
                clearFocus()
            }
            searchLayout.animate().translationY(-300f).setDuration(200).setInterpolator(
                DecelerateInterpolator()
            ).withEndAction {
                searchLayout.visibility = View.GONE
                searchBtn.apply {
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(200).setInterpolator(
                        DecelerateInterpolator()
                    ).start()
                }
                if (PREFS.isSettingsBtnEnabled) {
                    settingsBtn.visibility = View.VISIBLE
                    settingsBtn.animate().alpha(1f).setDuration(200).setInterpolator(
                        DecelerateInterpolator()
                    ).start()
                }
            }.start()
            appList.apply {
                alpha = 0.5f
                animate().translationX(0f).setDuration(200).setInterpolator(
                    DecelerateInterpolator()
                ).start()
                isVerticalScrollBarEnabled = true
            }
        }
        appAdapter?.setData(mainViewModel.getAppList())
        binding.appList.alpha = 1f
        binding.appList.smoothScrollToPosition(0)
    }

    private fun searchFunction() {
        if (isSearching) return
        appAdapter ?: return
        isSearching = true
        binding.apply {
            searchLayout.apply {
                visibility = View.VISIBLE
                animate().translationY(0f).setDuration(200).start()
            }
            searchBtn.animate().alpha(0f).setDuration(100).withEndAction {
                searchBtn.visibility = View.GONE
            }.start()
            if (PREFS.isSettingsBtnEnabled) {
                settingsBtn.animate().alpha(0f).setDuration(100).setInterpolator(
                    DecelerateInterpolator()
                ).withEndAction {
                    settingsBtn.visibility = View.GONE
                }
            }
            appList.apply {
                animate().translationX(
                    -requireContext().resources.getDimensionPixelSize(R.dimen.recyclerViewSearchPadding)
                        .toFloat()
                ).setDuration(200).start()
                isVerticalScrollBarEnabled = false
            }
        }
        if (PREFS.showKeyboardWhenSearching) {
            showKeyboard(binding.search.editText as? AutoCompleteTextView)
        }
        if (PREFS.allAppsKeyboardActionEnabled) {
            (binding.search.editText as? AutoCompleteTextView)?.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO && appAdapter!!.list.isNotEmpty()) {
                    runApp(
                        appAdapter!!.list.first().appPackage,
                        requireActivity().packageManager
                    )
                    (binding.search.editText as? AutoCompleteTextView)?.text!!.clear()
                    disableSearch()
                    true
                } else {
                    false
                }
            }
        }
        (binding.search.editText as? AutoCompleteTextView)?.addTextChangedListener(object :
            TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                filterText(s.toString())
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun showKeyboard(view: View?) {
        if (view != null) {
            if (view.requestFocus()) {
                val input =
                    ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                input?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun hideKeyboard(view: View?) {
        if (view != null) {
            if (view.requestFocus()) {
                val input =
                    ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                input?.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun filterText(searchText: String) {
        val filteredList: MutableList<App> = ArrayList()
        val locale = Utils.getDefaultLocale()
        mainViewModel.getAppList().forEach {
            if (it.appLabel.lowercase(locale).contains(searchText.lowercase(locale))) {
                filteredList.add(it)
            }
        }
        appAdapter?.setData(filteredList)
        binding.noResults.apply {
            if (filteredList.isEmpty()) {
                visibility = View.VISIBLE
                val string = context.getString(R.string.no_results_for) + " " + searchText
                val spannable: Spannable = SpannableString(string)
                spannable.setSpan(
                    ForegroundColorSpan(accentColor),
                    string.indexOf(searchText),
                    string.indexOf(searchText) + searchText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setText(spannable, TextView.BufferType.SPANNABLE)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun runAppWithAnimation(app: String, pm: PackageManager) {
        val first =
            (binding.appList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val last =
            (binding.appList.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        binding.appList.isScrollEnabled = false
        if (appAdapter == null) {
            runApp(app, pm)
            return
        }
        lifecycleScope.launch {
            var openedAppPos: Int? = null
            for (i in last downTo first) {
                val item = appAdapter!!.list[i]
                if (item.appPackage == app) {
                    openedAppPos = i
                    continue
                }
                val view = binding.appList.findViewHolderForAdapterPosition(i)?.itemView ?: continue
                view.animate().translationX(-1000f).alpha(0.75f).rotationY(-90f)
                    .setDuration(100 + i * 2L).start()
                delay(20)
            }
            if (openedAppPos != null) {
                delay(125)
                val view = binding.appList.findViewHolderForAdapterPosition(openedAppPos)?.itemView
                view!!.animate().translationX(-700f).alpha(0.75f).rotationY(-90f).setDuration(125)
                    .start()
                delay(50)
            }
            delay(200)
            runApp(app, pm)
        }
    }


    private fun runApp(app: String, pm: PackageManager) {
        isAppOpened = true
        when (app) {
            context?.packageName -> activity?.apply {
                startActivity(
                    Intent(
                        this,
                        SettingsActivity::class.java
                    )
                )
            }

            else -> startActivity(Intent(pm.getLaunchIntentForPackage(app)))
        }
    }

    private fun isPopupInTop(anchorView: View, popupHeight: Int): Boolean {
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val displayMetrics = anchorView.context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val popupY = anchorY - popupHeight
        return popupY < screenHeight / 2
    }

    private fun getPopupHeight(popupWindow: PopupWindow): Int {
        val contentView = popupWindow.contentView
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return contentView.measuredHeight
    }

    private fun showPopupWindow(view: View, app: App) {
        binding.appList.isScrollEnabled = false
        (requireActivity() as Main).configureViewPagerScroll(false)
        val inflater =
            view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.all_apps_window, binding.appList, false)
        popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow!!.isFocusable = true
        val popupHeight = getPopupHeight(popupWindow!!)
        val top = isPopupInTop(view, popupHeight)
        popupView.pivotY = if (top) 0f else popupHeight.toFloat()
        val pinLabel = popupView.findViewById<MaterialTextView>(R.id.pin_app_label)
        val infoLabel = popupView.findViewById<MaterialTextView>(R.id.app_info_label)
        val uninstallLabel = popupView.findViewById<MaterialTextView>(R.id.uninstall_label)
        (if (PREFS.customLightFontPath != null) customLightFont else customFont)?.let {
            pinLabel.typeface = it
            infoLabel.typeface = it
            uninstallLabel.typeface = it
        }
        val anim = ObjectAnimator.ofFloat(popupView, "scaleY", 0f, 0.01f).setDuration(1)
        val anim2 = ObjectAnimator.ofFloat(popupView, "scaleX", 0f, 1f).setDuration(200)
        val anim3 = ObjectAnimator.ofFloat(popupView, "scaleY", 0.01f, 1f).setDuration(400)
        anim.doOnEnd {
            anim2.doOnEnd {
                anim3.start()
            }
            anim2.start()
        }
        anim.start()
        popupWindow!!.showAsDropDown(
            view,
            0,
            if (top) 0 else (-popupHeight + -view.height),
            Gravity.CENTER
        )
        isWindowVisible = true

        val pin = popupView.findViewById<MaterialCardView>(R.id.pin_app)
        val info = popupView.findViewById<MaterialCardView>(R.id.infoApp)
        val uninstall = popupView.findViewById<MaterialCardView>(R.id.uninstallApp)

        var isAppAlreadyPinned = false
        lifecycleScope.launch(Dispatchers.Default) {
            mainViewModel.getViewModelTileDao().getTilesList().forEach {
                if (it.tilePackage == app.appPackage && !isAppAlreadyPinned) isAppAlreadyPinned =
                    true
            }

            withContext(Dispatchers.Main) {
                if (isAppAlreadyPinned) {
                    pin.apply {
                        isEnabled = false
                        alpha = 0.5f
                    }
                } else {
                    pin.apply {
                        isEnabled = true
                        alpha = 1f
                        setOnClickListener {
                            insertNewApp(app)
                            popupWindow?.dismiss()
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    }
                }
            }
        }
        uninstall.setOnClickListener {
            popupWindow?.dismiss()
            startActivity(Intent(Intent.ACTION_DELETE).setData("package:${app.appPackage}".toUri()))
        }

        info.setOnClickListener {
            isAppOpened = true
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${app.appPackage}".toUri())
            )
        }
        popupWindow?.setOnDismissListener {
            (requireActivity() as Main).configureViewPagerScroll(true)
            binding.appList.isScrollEnabled = true
            isWindowVisible = false
            popupWindow = null
        }
    }

    private fun insertNewApp(app: App) {
        lifecycleScope.launch(Dispatchers.Default) {
            val list = mainViewModel.getViewModelTileDao().getTilesList()
            var isAppPinned = false
            var position: Int? = null
            list.forEachIndexed { index, item ->
                if (item.tilePackage == app.appPackage && !isAppPinned) {
                    isAppPinned = true
                }
                if (!isAppPinned) {
                    if (item.tileType == -1 && position == null) {
                        position = index
                    }
                }
            }
            if (!isAppPinned) {
                if (position == null) position = Random.nextInt(0, 25)
                val item = Tile(
                    id = Random.nextLong(1000, 2000000),
                    tilePosition = position,
                    tileColor = -1,
                    tileType = 0,
                    isSelected = false,
                    tileSize = Utils.generateRandomTileSize(true),
                    tileLabel = app.appLabel,
                    tilePackage = app.appPackage
                )
                mainViewModel.getViewModelTileDao().addTile(item)
                isStartScreenEmpty = false
            }
        }
    }

    open inner class AppAdapter(var list: MutableList<App>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val appHolder = 0

        inner class AppHolder(val binding: AppBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                Utils.setViewInteractAnimation(itemView)
                if (PREFS.customFontInstalled) customFont?.let { binding.appLabel.typeface = it }

                itemView.setOnClickListener {
                    click()
                }
                itemView.setOnLongClickListener {
                    showPopupWindow(itemView, list[absoluteAdapterPosition])
                    true
                }
            }

            private fun click() {
                activity?.let {
                    if (PREFS.isAAllAppsAnimEnabled) {
                        runAppWithAnimation(
                            list[absoluteAdapterPosition].appPackage,
                            it.packageManager
                        )
                    } else {
                        runApp(
                            list[absoluteAdapterPosition].appPackage,
                            it.packageManager
                        )
                    }
                }
            }
        }

        fun setData(new: MutableList<App>) {
            val diffCallback = AppDiffCallback(list, new)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            list = new
            diffResult.dispatchUpdatesTo(this)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return AppHolder(AppBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            bindAppHolder(holder as AppHolder, list[position])
        }

        private fun bindAppHolder(holder: AppHolder, app: App) {
            holder.binding.appIcon.load(mainViewModel.getIconFromCache(app.appPackage))
            holder.binding.appLabel.text = app.appLabel
            setAnim(holder.itemView)
        }

        private fun setAnim(view: View) {
            if (view.alpha != 1f) {
                if (!PREFS.isAAllAppsAnimEnabled) {
                    view.apply {
                        alpha = 1f
                        rotationY = 0f
                        translationX = 0f
                    }
                } else {
                    view.animate().alpha(1f).translationX(0f).rotationY(0f).setDuration(100).start()
                }
            }
        }

        override fun getItemCount(): Int = list.size

        override fun getItemViewType(position: Int): Int {
            return appHolder
        }

        inner class AppDiffCallback(
            private val oldList: List<App>,
            private val newList: List<App>
        ) : DiffUtil.Callback() {

            override fun getOldListSize() = oldList.size

            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                return oldList[old].appPackage == newList[new].appPackage
            }

            override fun areContentsTheSame(old: Int, new: Int): Boolean {
                return oldList[old] == newList[new]
            }
        }
    }
}

class BottomOffsetDecoration(private val bottomOffset: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
            outRect.bottom = bottomOffset
        }
    }
}