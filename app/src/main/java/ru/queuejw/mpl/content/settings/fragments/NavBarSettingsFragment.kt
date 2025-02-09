package ru.queuejw.mpl.content.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsNavbarBinding
import ru.queuejw.mpl.databinding.SettingsNavbarIconChooseBinding

class NavBarSettingsFragment : Fragment() {

    private var _binding: SettingsNavbarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsNavbarBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.navigation_bar))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFont()
        setNavBarColorRadioGroup()
        updateCurrentIcon()
        setUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUi() {
        binding.navbarRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            changeNavBarColor(checkedId)
            PREFS.isPrefsChanged = true
        }
        binding.chooseStartIconBtn.setOnClickListener {
            iconsBottomSheet()
        }
    }

    private fun changeNavBarColor(checkedId: Int) {
        when (checkedId) {
            binding.alwaysDark.id -> {
                PREFS.navBarColor = 0
            }

            binding.alwaysLight.id -> {
                PREFS.navBarColor = 1
            }

            binding.byTheme.id -> {
                PREFS.navBarColor = 2
            }

            binding.hidden.id -> {
                PREFS.navBarColor = 3
            }

            binding.auto.id -> {
                PREFS.navBarColor = 4
            }
        }
    }

    private fun iconsBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireActivity())
        val bBidding =
            SettingsNavbarIconChooseBinding.inflate(LayoutInflater.from(requireActivity()))
        bottomSheet.setContentView(bBidding.root)
        bottomSheet.dismissWithAnimation = true
        bBidding.icon0.setOnClickListener {
            updateIconSettings(1, bottomSheet)
        }
        bBidding.icon1.setOnClickListener {
            updateIconSettings(0, bottomSheet)
        }
        bBidding.icon2.setOnClickListener {
            updateIconSettings(2, bottomSheet)
        }
        bottomSheet.show()
    }

    private fun updateIconSettings(value: Int, bottomSheetDialog: BottomSheetDialog) {
        PREFS.navBarIconValue = value
        updateCurrentIcon()
        PREFS.isPrefsChanged = true
        bottomSheetDialog.dismiss()
    }

    private fun setupFont() {
        customFont?.let {
            binding.navigationBar.typeface = it
            binding.auto.typeface = it
            binding.alwaysDark.typeface = it
            binding.alwaysLight.typeface = it
            binding.byTheme.typeface = it
            binding.hidden.typeface = it
            binding.additionalOptions.typeface = it
            binding.currentIconText.typeface = it
            binding.chooseStartIconBtn.typeface = it
            binding.iconChangeLabel.typeface = it
        }
    }

    private fun setNavBarColorRadioGroup() {
        when (PREFS.navBarColor) {
            0 -> {
                binding.alwaysDark.isChecked = true
                binding.alwaysLight.isChecked = false
                binding.byTheme.isChecked = false
                binding.hidden.isChecked = false
                binding.auto.isChecked = false
            }

            1 -> {
                binding.alwaysDark.isChecked = false
                binding.alwaysLight.isChecked = true
                binding.byTheme.isChecked = false
                binding.hidden.isChecked = false
                binding.auto.isChecked = false
            }

            2 -> {
                binding.alwaysDark.isChecked = false
                binding.alwaysLight.isChecked = false
                binding.byTheme.isChecked = true
                binding.hidden.isChecked = false
                binding.auto.isChecked = false
            }

            3 -> {
                binding.alwaysDark.isChecked = false
                binding.alwaysLight.isChecked = false
                binding.byTheme.isChecked = false
                binding.hidden.isChecked = true
                binding.auto.isChecked = false
            }

            4 -> {
                binding.alwaysDark.isChecked = false
                binding.alwaysLight.isChecked = false
                binding.byTheme.isChecked = false
                binding.hidden.isChecked = false
                binding.auto.isChecked = true
            }
        }
    }

    private fun updateCurrentIcon() {
        binding.currentStartIcon.setImageDrawable(
            when (PREFS.navBarIconValue) {
                1 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_os_windows)
                2 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_os_android)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_os_windows_8)
            }
        )
    }
}