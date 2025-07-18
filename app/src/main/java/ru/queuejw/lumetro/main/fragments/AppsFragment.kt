package ru.queuejw.lumetro.main.fragments

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.adapters.AppAdapter
import ru.queuejw.lumetro.components.adapters.decor.BottomOffsetDecoration
import ru.queuejw.lumetro.components.adapters.decor.HeaderItemDecoration
import ru.queuejw.lumetro.components.adapters.viewtypes.AppViewTypes
import ru.queuejw.lumetro.components.core.AppManager
import ru.queuejw.lumetro.components.core.AppManager.Companion.launchApp
import ru.queuejw.lumetro.components.core.Lumetro.Companion.isOtherAppOpened
import ru.queuejw.lumetro.components.core.TileManager
import ru.queuejw.lumetro.components.core.base.BaseMainFragment
import ru.queuejw.lumetro.components.core.receivers.AppReceiver
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.components.ui.recyclerview.MetroRecyclerView
import ru.queuejw.lumetro.databinding.AppsBottomSheetBinding
import ru.queuejw.lumetro.databinding.AppsFragmentBinding
import ru.queuejw.lumetro.main.MainActivity
import ru.queuejw.lumetro.model.App
import ru.queuejw.lumetro.settings.SettingsActivity

class AppsFragment : BaseMainFragment<AppsFragmentBinding>() {

    private var mAdapter: AppAdapter? = null
    private var headerDecorator: HeaderItemDecoration? = null

    private var isKeyboardVisible = false
    private var isSearching = false
    private var isWindowVisible = false

    private var textWatcher: TextWatcher? = null
    private var inputManager: InputMethodManager? = null

    private var appReceiver: AppReceiver? = null
    private var appReceiverUpdatesBlocked = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): AppsFragmentBinding? {
        return AppsFragmentBinding.inflate(inflater, container, false)
    }

    private fun getHeaderDecorator(mRecyclerView: MetroRecyclerView): HeaderItemDecoration {
        if (headerDecorator == null) {
            headerDecorator = HeaderItemDecoration(mRecyclerView) { itemPosition ->
                mAdapter ?: false
                if (itemPosition >= 0 && itemPosition < mAdapter!!.itemCount) {
                    mAdapter!!.getItemViewType(itemPosition) == AppViewTypes.TYPE_HEADER.type
                } else false
            }
        }
        return headerDecorator!!
    }

    private fun getInputMethod(context: Context): InputMethodManager? {
        return ContextCompat.getSystemService(context, InputMethodManager::class.java)
    }

    private fun controlKeyboard(view: View?, hide: Boolean) {
        if (view != null) {
            if (view.requestFocus()) {
                if (inputManager == null) inputManager = getInputMethod(view.context)
                context?.let {
                    if (hide) {
                        inputManager?.hideSoftInputFromWindow(view.windowToken, 0)
                    } else {
                        inputManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }
    }

    private fun setPadding(
        view: View?,
        paddingStart: Int,
        paddingTop: Int
    ) {
        view?.setPadding(paddingStart, paddingTop, 0, 0)
    }

    private fun search() {
        isSearching = !isSearching
        mAdapter ?: return
        binding.apply {
            search.editText?.text?.clear()
            if (isSearching) {
                buttonPanel.animate().translationX(-100f).alpha(0f).setDuration(200)
                    .withEndAction {
                        buttonPanel.visibility = View.GONE
                    }.start()
                headerDecorator?.let { decoration ->
                    recyclerView.removeItemDecoration(decoration)
                }
                searchLayout.apply {
                    visibility = View.VISIBLE
                    animate().translationY(0f).setDuration(200).start()
                }
                mAdapter!!.updateData(viewModel.getOnlyApps())
                context?.let { context ->
                    setPadding(recyclerView, 0, 0)
                    setPadding(
                        recyclerViewLayout,
                        0,
                        context.resources.getDimensionPixelSize(R.dimen.apps_recyclerview_default_padding)
                    )

                }
            } else {
                noResults.visibility = View.GONE
                buttonPanel.visibility = View.VISIBLE
                buttonPanel.animate().translationX(0f).alpha(1f).setDuration(200).start()
                searchLayout.animate().translationY(-200f).setDuration(200).withEndAction {
                    searchLayout.visibility = View.GONE
                }.start()
                headerDecorator?.let { decoration ->
                    recyclerView.addItemDecoration(decoration)
                }
                mAdapter!!.updateData(viewModel.getLiveAppsList().value!!)
                context?.let { context ->
                    setPadding(
                        recyclerView,
                        context.resources.getDimensionPixelSize(R.dimen.apps_recyclerview_default_padding),
                        0
                    )
                    setPadding(recyclerViewLayout, 0, 0)
                }
            }
            controlKeyboard(searchTextview, !isSearching)
            recyclerView.scrollToPosition(0)
        }
    }

    private fun onAppClick(position: Int, item: App) {
        launchAppWithAnimation(item)
    }

    private fun onAppLongClick(position: Int, item: App, view: View) {
        showPopupWindow(view, item, position)
    }

    private fun applyViewModel(context: Context) {
        viewModel.apply {
            if (getIconLoader() == null) {
                createIconLoader(prefs.iconPackPackage != null, prefs.iconPackPackage)
            }
            this.getLiveAppsList().value?.let { list ->
                mAdapter = AppAdapter(
                    list,
                    viewModel.getIconLoader()!!,
                    this.getColorManager().getAccentColor(context),
                    { int, item ->
                        onAppClick(int, item)
                    },
                    { int, item, view ->
                        onAppLongClick(int, item, view)
                    })
            }
            this.getLiveAppsList().observe(viewLifecycleOwner) {
                mAdapter?.updateData(it)
            }
        }
    }

    private fun filterList(request: String, context: Context) {
        mAdapter ?: return
        val filteredList: List<App> = viewModel.getOnlyApps().filter {
            it.mName.lowercase().contains(request.lowercase())
        }
        mAdapter!!.updateData(filteredList)
        binding.noResults.apply {
            if (filteredList.isEmpty()) {
                visibility = View.VISIBLE
                val string = context.getString(R.string.no_results_for) + request
                val spannable: Spannable = SpannableString(string)
                spannable.setSpan(
                    ForegroundColorSpan(viewModel.getColorManager().getAccentColor(context)),
                    string.indexOf(request),
                    string.indexOf(request) + request.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setText(spannable, TextView.BufferType.SPANNABLE)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun launchAppWithFallback(string: String?, context: Context) {
        if (!launchApp(string, context)) {
            launchErrorDialog(context, context.getString(R.string.launch_app_error))
            clearAppsFragmentAnimation(binding.recyclerView, binding.buttonPanel)
        }
    }

    private fun setSearchView(context: Context) {
        mAdapter ?: return
        (binding.search.editText as? AutoCompleteTextView)?.let {
            it.setOnEditorActionListener { _, actionId, _ ->
                if (!isSearching) false
                if (actionId == EditorInfo.IME_ACTION_GO && mAdapter!!.data.isNotEmpty()) {
                    launchAppWithFallback(
                        mAdapter!!.data.first().mPackage,
                        context
                    )
                    (binding.search.editText as? AutoCompleteTextView)?.text?.clear()
                    search()
                    true
                } else {
                    false
                }
            }
            if (textWatcher == null) {
                textWatcher = object :
                    TextWatcher {
                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (isSearching) {
                            filterList(s.toString(), context)
                            binding.recyclerView.scrollToPosition(0)
                        }
                    }

                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {}
                }
            }
            it.addTextChangedListener(textWatcher)
        }
    }

    private fun configureButtonPanel() {
        binding.searchButton.setOnClickListener {
            search()
        }
        binding.settingsButton.setOnClickListener {
            activity?.let {
                it.startActivity(Intent(it, SettingsActivity::class.java))
            }
        }
    }

    private fun setSearchTextView() {
        binding.searchTextview.setOnApplyWindowInsetsListener { v, insets ->
            isKeyboardVisible = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .isVisible(WindowInsetsCompat.Type.ime())
            if (!isKeyboardVisible && !isWindowVisible) {
                isSearching = true
                search()
            }
            insets
        }
    }

    private fun getBottomOffsetDecoration(context: Context): RecyclerView.ItemDecoration {
        return BottomOffsetDecoration(context.resources.getDimensionPixelSize(R.dimen.bottom_offset))
    }

    private fun configureHeaderDecoration() {
        getHeaderDecorator(binding.recyclerView)
    }

    private fun configureRecyclerView(context: Context) {
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(headerDecorator!!)
            addItemDecoration(getBottomOffsetDecoration(context))
        }
    }

    private fun applyColors(context: Context) {
        viewModel.getColorManager().getAccentColor(context).let { color ->
            binding.search.apply {
                boxStrokeColor = color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursorColor = ColorStateList.valueOf(color)
                }
            }
        }

    }

    private fun prepareFragmentComponents(context: Context) {
        applyViewModel(context)
        configureHeaderDecoration()
        applyColors(context)
    }

    private fun prepareUi(context: Context) {
        setSearchView(context)
        setSearchTextView()
        configureButtonPanel()
        configureRecyclerView(context)
        appReceiver?.let {
            AppReceiver.register(context, it)
        }
    }

    private fun receiverUpdateList(context: Context) {
        if (appReceiverUpdatesBlocked) return
        appReceiverUpdatesBlocked = true
        lifecycleScope.launch(Dispatchers.IO) {
            val appManager = AppManager()
            val list = appManager.getInstalledApps(context)
            withContext(Dispatchers.Main) {
                viewModel.updateAppsList(list)
                appReceiverUpdatesBlocked = false
            }
        }
    }

    private fun checkIconPack(mPackage: String, context: Context): Boolean {
        if (prefs.iconPackPackage == mPackage) {
            viewModel.getIconLoader()?.let {
                it.removeIconPack(prefs)
                it.clearCache(context)
            }
            return true
        } else {
            return false
        }
    }

    private fun launchErrorDialog(context: Context, message: String) {
        val d = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.error))
            setMessage(message)
            setDialogCancelable(false)
            setPositiveDialogListener(context.getString(android.R.string.ok)) {
                this.dismiss()
            }
        }
        d.show(childFragmentManager, "error")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appReceiver = AppReceiver(
            onAppInstalled = {
                context?.let { context ->
                    receiverUpdateList(context)
                }
            },
            onAppRemoved = {
                context?.let { context ->
                    if (checkIconPack(it, context)) {
                        (activity as MainActivity?)?.restartActivity()
                    } else {
                        receiverUpdateList(context)
                    }
                }
            },
            onAppChanged = {
                context?.let { context ->
                    receiverUpdateList(context)
                }
            }
        )
        applyInsets(binding.appsMainLayout)
        prepareFragmentComponents(view.context)
        prepareUi(view.context)
    }

    private fun clearFragmentAnimation() {
        if (isOtherAppOpened) {
            isOtherAppOpened = false
            binding.apply {
                clearAppsFragmentAnimation(
                    recyclerView,
                    buttonPanel
                )
            }

            headerDecorator?.let {
                binding.recyclerView.addItemDecoration(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        clearFragmentAnimation()
    }

    override fun onPause() {
        super.onPause()
        if (isSearching) {
            search()
        }
    }

    override fun onDestroyView() {
        Log.d("Apps", "Destroy view")
        appReceiver?.let {
            context?.let { context ->
                AppReceiver.unregister(context, it)
            }
        }
        appReceiver = null
        binding.recyclerView.apply {
            adapter = null
            layoutManager = null
        }
        (binding.search.editText as? AutoCompleteTextView)?.apply {
            setOnEditorActionListener(null)
            removeTextChangedListener(textWatcher)
        }
        super.onDestroyView()
        mAdapter = null
        headerDecorator = null
        textWatcher = null
        inputManager = null
    }

    private fun isPopupInTop(anchorView: View, popupHeight: Int): Boolean {
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val displayMetrics = anchorView.context.resources.displayMetrics
        return (anchorY - popupHeight) < displayMetrics.heightPixels / 2
    }

    private fun getPopupHeight(popupWindow: PopupWindow): Int {
        val contentView = popupWindow.contentView
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return contentView.measuredHeight
    }

    private fun getPopupWindow(root: View): PopupWindow {
        return PopupWindow(
            root,
            LinearLayoutCompat.LayoutParams.MATCH_PARENT,
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
            true
        )
    }

    private suspend fun pinApp(app: App): Boolean {
        mAdapter ?: return false
        context?.let {
            val r = TileManager().pinNewTile(it, viewModel.getDatabase().getTilesDao().getTilesData(), app.mPackage!!, viewModel.getDatabase().getTilesDao())
            if (r != null) {
                viewModel.updateTilesList(r)
            } else {
                withContext(Dispatchers.Main) {
                    launchErrorDialog(it, getString(R.string.pin_app_error, app.mName))
                }
            }
        }
        return true
    }

    private fun showUninstallDialog(context: Context, app: App) {
        val d = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.uninstall_app_dialog_title))
            setMessage(context.getString(R.string.uninstall_app_dialog_message))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                context.startActivity(Intent(Intent.ACTION_DELETE).setData("package:${app.mPackage}".toUri()))
                this.dismiss()
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                this.dismiss()
            }
        }
        d.show(childFragmentManager, "uninstall_app")
    }

    private fun setPopupOnClick(b: AppsBottomSheetBinding, popupWindow: PopupWindow, app: App) {
        b.pinApp.apply {
            if (viewModel.getTiles()?.value?.any {
                    it.tilePackage == app.mPackage
                } == true) {
                isEnabled = false
                alpha = 0.5f
            } else {
                setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        context?.let {
                            if (pinApp(app)) {
                                withContext(Dispatchers.Main) {
                                    popupWindow.dismiss()
                                    (activity as MainActivity?)?.changePage(0)
                                }
                            }
                        }
                    }
                }
            }
        }
        b.appInfo.setOnClickListener {
            popupWindow.dismiss()
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData("package:${app.mPackage}".toUri())
            )
        }
        b.uninstallApp.setOnClickListener {
            context?.let { context -> showUninstallDialog(context, app) }
            popupWindow.dismiss()
        }
    }

    private fun closeWindow(position: Int) {
        isWindowVisible = false
        binding.recyclerView.apply {
            windowAnimateApps(position, true, this)
            headerDecorator?.let { addItemDecoration(it) }
            isScrollEnabled = true
        }
        (activity as MainActivity?)?.setViewPagerUserInput(true)
    }

    private fun preparePopupWindow(position: Int) {
        binding.recyclerView.apply {
            isScrollEnabled = false
            headerDecorator?.let { removeItemDecoration(it) }
            windowAnimateApps(position, false, this)
        }
        (activity as MainActivity?)?.setViewPagerUserInput(false)
    }

    private fun showPopupWindow(itemView: View, app: App, position: Int) {
        preparePopupWindow(position)
        var windowBinding: AppsBottomSheetBinding? =
            AppsBottomSheetBinding.inflate(LayoutInflater.from(itemView.context))
        val popup = getPopupWindow(windowBinding!!.root).also {
            it.setOnDismissListener {
                windowBinding = null
                closeWindow(position)
            }
        }
        val popupHeight = getPopupHeight(popup)
        val top = isPopupInTop(itemView, getPopupHeight(popup))
        windowBinding!!.root.pivotY = if (top) 0f else popupHeight.toFloat()
        setPopupOnClick(windowBinding!!, popup, app)
        animateAppsPopupWindow(windowBinding!!.root)
        isWindowVisible = true

        popup.showAsDropDown(
            itemView,
            0,
            if (top) 0 else (-popupHeight + -itemView.measuredHeight),
            Gravity.CENTER
        )
    }

    suspend fun animateAppsFragmentAppLaunch(
        mPackage: String,
        recyclerView: MetroRecyclerView,
        buttonPanel: View,
        data: List<App>
    ): Boolean {
        val (first, last) =
            (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition() to (recyclerView.layoutManager as LinearLayoutManager?)?.findLastVisibleItemPosition()
        recyclerView.isScrollEnabled = false
        if (first == null || last == null) {
            return false
        }
        var openedAppPos: Int? = null
        for (i in last downTo first) {
            if (data[i].mPackage == mPackage) {
                openedAppPos = i
                continue
            }
            val view = recyclerView.findViewHolderForAdapterPosition(i)?.itemView ?: continue
            view.animate().translationX(-1000f).alpha(0.75f).rotationY(-90f)
                .setDuration(100 + i * 2L).start()
            delay(20)
        }
        if (openedAppPos != null) {
            buttonPanel.animate().translationX(-1000f).alpha(0.75f).rotationY(-90f)
                .setDuration(150).start()
            val view = recyclerView.findViewHolderForAdapterPosition(openedAppPos)?.itemView
            view?.animate()?.setStartDelay(125)?.translationX(-700f)?.alpha(0.75f)?.rotationY(-90f)
                ?.setDuration(125)
                ?.start()
        }
        delay(250)
        launchAppWithFallback(mPackage, recyclerView.context)
        return true
    }

    fun clearAppsFragmentAnimation(recyclerView: MetroRecyclerView, buttonPanel: View) {
        val (first, last) =
            (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition() to (recyclerView.layoutManager as LinearLayoutManager?)?.findLastVisibleItemPosition()
        if (first == null || last == null) {
            recyclerView.isScrollEnabled = true
            return
        }
        for (i in last downTo first) {
            val view = recyclerView.findViewHolderForAdapterPosition(i)?.itemView ?: continue
            view.animate().translationX(0f).alpha(1f).rotationY(0f).setDuration(400).start()
        }
        buttonPanel.animate().translationX(0f).alpha(1f).rotationY(0f).setDuration(400).start()
        recyclerView.isScrollEnabled = true
    }

    fun windowAnimateApps(
        selectedAppPos: Int,
        restoreViews: Boolean,
        recyclerView: MetroRecyclerView
    ) {
        val (first, last) =
            (recyclerView.layoutManager as LinearLayoutManager?)?.findFirstVisibleItemPosition() to (recyclerView.layoutManager as LinearLayoutManager?)?.findLastVisibleItemPosition()
        if (first == null || last == null) {
            return
        }
        val interpolator = AccelerateInterpolator()
        for (i in last downTo first) {
            val view =
                recyclerView.findViewHolderForAdapterPosition(i)?.itemView ?: continue
            if (!restoreViews) {
                if (i == selectedAppPos) continue
                view.animate().alpha(0.7f).scaleY(0.96f).scaleX(0.96f).setDuration(400)
                    .setInterpolator(interpolator).start()
            } else {
                view.animate().alpha(1f).scaleY(1f).scaleX(1f).setDuration(400)
                    .setInterpolator(interpolator).start()
            }
        }
    }

    fun animateAppsPopupWindow(view: View) {
        val anim =
            ObjectAnimator.ofFloat(view, "scaleY", 0f, 0.01f).setDuration(1)
        val anim2 =
            ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f).setDuration(200)
        val anim3 =
            ObjectAnimator.ofFloat(view, "scaleY", 0.01f, 1f).setDuration(400)
        anim.doOnEnd {
            anim2.doOnEnd {
                anim3.start()
            }
            anim2.start()
        }
        anim.start()
    }

    private fun launchAppWithAnimation(item: App) {
        mAdapter ?: return
        lifecycleScope.launch {
            binding.apply {
                headerDecorator?.let { decoration ->
                    recyclerView.removeItemDecoration(decoration)
                }
                if (animateAppsFragmentAppLaunch(
                        item.mPackage!!,
                        recyclerView,
                        buttonPanel,
                        mAdapter!!.data
                    )
                ) {
                    context?.let {
                        launchAppWithFallback(item.mPackage, it)
                    }
                }
            }
        }
    }
}