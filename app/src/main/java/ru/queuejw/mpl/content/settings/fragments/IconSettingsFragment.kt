package ru.queuejw.mpl.content.settings.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.AppBinding
import ru.queuejw.mpl.databinding.SettingsIconsBinding
import ru.queuejw.mpl.helpers.iconpack.IconPackManager
import ru.queuejw.mpl.helpers.ui.WPDialog

class IconSettingsFragment : Fragment() {

    private var _binding: SettingsIconsBinding? = null
    private val binding get() = _binding!!

    private val iconPackManager: IconPackManager by lazy {
        IconPackManager(requireActivity())
    }
    private var iconPackArrayList: ArrayList<IconPackManager.IconPack> = ArrayList()

    private var mAdapter: IconPackAdapterList? = null

    private var isIconPackListEmpty = false
    private var isListVisible = false
    private var isError = false

    private var dialog: WPDialog? = null

    private var appList = ArrayList<IconPackItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsIconsBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.icon_packs))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createDialog()
        setupFont()
        setUi()
        setOnClickers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialog = null
    }

    private fun setupFont() {
        customFont?.let {
            binding.currentIconPackText.typeface = it
            binding.currentIconPackError.typeface = it
            binding.removeIconPack.typeface = it
            binding.chooseIconPack.typeface = it
            binding.downloadIconPacks.typeface = it
            binding.lawniconsMpl.typeface = it
        }
    }

    private fun createDialog() {
        dialog = WPDialog(requireActivity()).setTopDialog(true)
            .setTitle(getString(R.string.tip))
            .setMessage(getString(R.string.tipIconPackError))
            .setPositiveButton(getString(android.R.string.ok), null)
    }

    private fun setOnClickers() {
        binding.chooseIconPack.setOnClickListener {
            setIconPacks()
            if (!isIconPackListEmpty) {
                if (!isListVisible) {
                    isListVisible = true
                    binding.iconPackList.visibility = View.VISIBLE
                } else {
                    isListVisible = false
                    binding.iconPackList.visibility = View.GONE
                }
            } else {
                dialog?.show()
            }
            setUi()
        }
        binding.removeIconPack.setOnClickListener {
            PREFS.apply {
                iconPackPackage = "null"
                iconPackChanged = true
                isPrefsChanged = true
            }
            setUi()
        }
        binding.downloadIconPacks.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/queuejw/mpl_updates/releases/download/release/Lawnicons.apk".toUri()
                )
            )
        }
    }

    private fun setIconPacks() {
        isError = false
        iconPackArrayList = iconPackManager.getAvailableIconPacks(true)
        isIconPackListEmpty = iconPackArrayList.isEmpty()
        setUi()
        appList.clear()
        if (iconPackArrayList.isNotEmpty()) {
            for (i in iconPackArrayList) {
                val app = IconPackItem()
                app.appPackage = i.packageName!!
                app.name = i.name!!
                appList.add(app)
            }
        }
        if (mAdapter != null) {
            mAdapter = null
        }
        mAdapter = IconPackAdapterList(appList, requireContext())
        binding.iconPackList.apply {
            layoutManager = LinearLayoutManager(this@IconSettingsFragment.requireContext())
            adapter = mAdapter
        }
    }

    private fun setUi() {
        if (isIconPackListEmpty) {
            binding.currentIconPackText.visibility = View.GONE
            binding.currentIconPackError.apply {
                visibility = View.VISIBLE
                text =
                    if (isError) getString(R.string.error) else getString(R.string.iconpack_error)
            }
            binding.removeIconPack.visibility = View.GONE
        } else {
            val label = if (PREFS.iconPackPackage == "null") {
                binding.currentIconPackText.visibility = View.GONE
                binding.removeIconPack.visibility = View.GONE
                binding.currentIconPackError.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.iconpack_error)
                }
                "null"
            } else {
                runCatching {
                    binding.currentIconPackText.visibility = View.VISIBLE
                    binding.currentIconPackError.visibility = View.GONE
                    binding.removeIconPack.visibility = View.VISIBLE
                    requireActivity().packageManager.getApplicationLabel(
                        requireActivity().packageManager.getApplicationInfo(
                            PREFS.iconPackPackage!!,
                            0
                        )
                    ).toString()
                }.getOrElse {
                    binding.currentIconPackText.visibility = View.GONE
                    binding.currentIconPackError.apply {
                        visibility = View.VISIBLE
                        text = getString(R.string.iconpack_error)
                    }
                    "null"
                }
            }
            binding.currentIconPackText.text =
                getString(R.string.current_iconpack, label)
        }
        binding.chooseIconPack.text =
            if (isListVisible) getString(android.R.string.cancel) else getString(R.string.choose_icon_pack)
    }

    inner class IconPackAdapterList(
        private var list: MutableList<IconPackItem>,
        private val context: Context
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var iconSize = resources.getDimensionPixelSize(R.dimen.iconAppsListSize)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return IconPackHolder(
                AppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder as IconPackHolder
            val item = list[position]
            holder.holderBinding.appLabel.text = item.name
            holder.holderBinding.appIcon.setImageBitmap(
                context.packageManager.getApplicationIcon(item.appPackage)
                    .toBitmap(iconSize, iconSize)
            )
            holder.itemView.setOnClickListener {
                PREFS.apply {
                    iconPackPackage = item.appPackage
                    iconPackChanged = true
                    isPrefsChanged = true
                }
                binding.iconPackList.visibility = View.GONE
                isListVisible = false
                setUi()
            }
        }
    }
}

class IconPackHolder(val holderBinding: AppBinding) : RecyclerView.ViewHolder(holderBinding.root)

class IconPackItem {
    var name: String = ""
    var appPackage: String = ""
}