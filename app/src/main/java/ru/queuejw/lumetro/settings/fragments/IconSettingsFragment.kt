package ru.queuejw.lumetro.settings.fragments

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
import ru.queuejw.lumetro.components.adapters.AppAdapter
import ru.queuejw.lumetro.components.adapters.viewtypes.AppViewTypes
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.core.cache.CacheUtils
import ru.queuejw.lumetro.components.core.icons.IconLoader
import ru.queuejw.lumetro.components.core.icons.IconPackManager
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.SettingsIconsBinding
import ru.queuejw.lumetro.model.App
import ru.queuejw.lumetro.settings.SettingsActivity

class IconSettingsFragment : BaseFragment<SettingsIconsBinding>() {

    private var appAdapter: AppAdapter? = null
    private var iconLoader: IconLoader? = null
    private val colorManager by lazy {
        ColorManager()
    }

    private fun createIconPackManager(context: Context): IconPackManager? {
        return IconPackManager(context)
    }

    private fun createAppAdapter(
        data: List<App>,
        context: Context,
        iconLoader: IconLoader
    ): AppAdapter {
        return AppAdapter(
            data,
            iconLoader,
            colorManager.getAccentColor(context),
            onAppClick = { _, app -> onIconPackClick(app) },
            onAppLongClick = { _, _, _ -> }
        )
    }

    private var isIconPackListEmpty = false
    private var recyclerViewVisible = false

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): SettingsIconsBinding? {
        return SettingsIconsBinding.inflate(layoutInflater)
    }

    private fun onIconPackClick(app: App) {
        prefs.apply {
            isRestartRequired = true
            iconPackPackage = app.mPackage
        }
        hideRecyclerView()
        context?.let {
            showDialog(getString(R.string.icon_pack_installed_message, app.mName), it)
            iconLoader?.resetIconLoader()
            iconLoader = null
            clearCache(it)
        }
    }

    private fun clearCache(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            val diskCache = CacheUtils.initDiskCache(context)
            diskCache?.delete()
            CacheUtils.closeDiskCache(diskCache)
            withContext(Dispatchers.Main) {
                prefs.isRestartRequired = true
            }
        }
    }

    private fun showDialog(message: String, context: Context) {
        val d = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.warning))
            setMessage(message)
            setDialogCancelable(false)
            setPositiveDialogListener(context.getString(android.R.string.ok)) {
                this.dismiss()
            }
        }
        d.show(childFragmentManager, "icons")
    }

    private fun showRecyclerView(context: Context) {
        if (iconLoader == null) {
            iconLoader = IconLoader()
        }
        if (iconLoader!!.iconPackManager == null) {
            iconLoader!!.iconPackManager = createIconPackManager(context)
        }
        val iconPacks = iconLoader!!.iconPackManager!!.getAvailableIconPacks(true)
        if (iconPacks.isEmpty()) {
            isIconPackListEmpty = true
            showDialog(getString(R.string.icon_pack_error), context)
        } else {
            isIconPackListEmpty = false
            val list = ArrayList<App>()
            iconPacks.forEach {
                list.add(
                    App(
                        mName = it.name!!,
                        mPackage = it.packageName,
                        viewType = AppViewTypes.TYPE_APP.type
                    )
                )
            }
            binding.iconPackList.visibility = View.VISIBLE
            recyclerViewVisible = true
            updateButtonAndText()
            if (appAdapter == null) {
                iconLoader?.apply {
                    appAdapter = createAppAdapter(
                        list,
                        context,
                        this
                    )
                }
                binding.iconPackList.apply {
                    adapter = appAdapter
                    layoutManager = LinearLayoutManager(context)
                }
            } else {
                appAdapter!!.updateData(list)
            }
        }
    }

    private fun updateButtonAndText() {
        context?.let { updateIconPackTextview(it) }
        updateActionButton()
    }

    private fun hideRecyclerView() {
        binding.iconPackList.visibility = View.GONE
        binding.iconPackActionButton.visibility = View.VISIBLE
        recyclerViewVisible = false
        updateButtonAndText()
    }

    private fun updateIconPackTextview(context: Context) {
        binding.iconPackNameTextview.apply {
            if (prefs.iconPackPackage != null) {
                visibility = View.VISIBLE
                text = getString(
                    R.string.current_icon_pack,
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            prefs.iconPackPackage!!,
                            0
                        )
                    )
                )
            } else {
                visibility = View.GONE
                text = null
            }
        }
    }

    private fun updateActionButton() {
        binding.iconPackActionButton.text = when {
            prefs.iconPackPackage != null && !recyclerViewVisible -> getString(R.string.remove_icon_pack)
            prefs.iconPackPackage == null && !recyclerViewVisible -> getString(R.string.select_icon_pack)
            else -> getString(R.string.cancel)
        }
    }

    private fun deleteIconPack() {
        prefs.apply {
            isRestartRequired = true
            iconPackPackage = null
        }
        context?.let {
            updateButtonAndText()
            showDialog(getString(R.string.icon_pack_removed_message), it)
            iconLoader?.resetIconLoader()
            iconLoader = null
            clearCache(it)
        }
    }

    private fun setUi() {
        binding.clearIconCache.setOnClickListener {
            clearCache(it.context)
        }
        updateButtonAndText()
        binding.iconPackActionButton.setOnClickListener {
            when {
                recyclerViewVisible -> {
                    hideRecyclerView()
                }

                (prefs.iconPackPackage == null) && !recyclerViewVisible -> {
                    showRecyclerView(it.context)
                }

                (prefs.iconPackPackage != null) && !recyclerViewVisible -> {
                    deleteIconPack()
                }
            }
            updateButtonAndText()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as SettingsActivity?)?.setText(getString(R.string.icons))
        setUi()
    }

    override fun onDestroyView() {
        iconLoader?.resetIconLoader()
        iconLoader = null
        binding.iconPackList.apply {
            adapter = null
            layoutManager = null
        }
        appAdapter = null
        super.onDestroyView()
    }
}