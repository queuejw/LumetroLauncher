package ru.queuejw.lumetro.fle.fragment

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.components.ui.dialog.MetroDialog
import ru.queuejw.lumetro.databinding.FleUpdatesBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity

class FleUpdatesFragment : BaseFragment<FleUpdatesBinding>() {

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleUpdatesBinding? {
        return FleUpdatesBinding.inflate(inflater, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs.fleProgress = 3
    }

    private fun prepareScreen() {
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(getString(R.string.updates))
            nextFragment = 4
            previousFragment = 2
            updateNextButtonText(this.getString(R.string.next))
            updatePreviousButtonText(this.getString(R.string.back))
            setButtonState(0, true)
        }
    }

    private suspend fun checkUpdateAvailability(context: Context): Int {
        delay(4000)
        return -1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareScreen()
        binding.apply {
            progressBar.apply {
                val colorManager = ColorManager()
                setIndicatorColor(colorManager.getAccentColor(context))
                showProgressBar()
            }
        }
        context?.let {
            showUpdateDialog(it)
        }
    }

    private fun showUpdateDialog(context: Context) {
        val dialog = MetroDialog.newInstance(Gravity.TOP).apply {
            setTitle(context.getString(R.string.update_checking))
            setMessage(context.getString(R.string.updates_req))
            setPositiveDialogListener(context.getString(R.string.yes)) {
                runCheckUpdates(context)
                this.dismiss()
            }
            setNegativeDialogListener(context.getString(R.string.no)) {
                updateUi(-2)
                this.dismiss()
            }
            setDialogCancelable(false)
        }
        dialog.show(childFragmentManager, "fle_check_updates")
    }

    private fun disableUpdateCheckUi() {
        binding.apply {
            progressBar.hideProgressBar()
            fleUpdateHint.visibility = View.GONE
        }
    }

    private fun updateUi(int: Int) {
        when (int) {
            -2 -> {
                disableUpdateCheckUi()
                binding.apply {
                    updateStatus.text =
                        getString(R.string.update_check_cancelled_by_user)
                }
                (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(true)
            }

            1 -> {

            }

            0 -> {

            }

            else -> {
                disableUpdateCheckUi()
                binding.apply {
                    updateStatus.text =
                        getString(R.string.update_service_unavailable)
                }
            }
        }
    }

    private fun runCheckUpdates(context: Context) {
        lifecycleScope.launch(Dispatchers.IO) {
            val i = checkUpdateAvailability(context)
            withContext(Dispatchers.Main) {
                updateUi(i)
                (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(true)
            }
        }
    }

    override fun onDestroyView() {
        binding.progressBar.hideProgressBar()
        super.onDestroyView()
    }
}