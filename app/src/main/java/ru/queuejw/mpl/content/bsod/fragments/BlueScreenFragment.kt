package ru.queuejw.mpl.content.bsod.fragments

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Main
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.databinding.CriticalErrorFragmentBinding
import ru.queuejw.mpl.helpers.utils.Utils

class BlueScreenFragment : Fragment() {

    private var _binding: CriticalErrorFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = CriticalErrorFragmentBinding.inflate(inflater, container, false)
        var counter = PREFS.prefs.getInt("crash_count", 0)
        counter += 1
        PREFS.prefs.edit {
            putBoolean("app_crashed", true)
            putInt("crash_count", counter)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stacktrace = arguments?.getString("stacktrace")
        if (stacktrace != null && context != null) {
            saveError(stacktrace, requireContext())
        }
    }

    private fun saveError(stacktrace: String, context: Context) {
        val model = "\nModel: ${Build.MODEL}\n"
        val brand = "Brand: ${Build.BRAND}\n"
        val mplVerCode = "MPL Ver Code: ${Utils.VERSION_CODE}\n"
        val android = "Android Version: ${Build.VERSION.SDK_INT}\n\n"
        val errCode = "\nIf vou call a support person. aive them this info:\n" +
                "Stop code: $stacktrace"
        val error =
            "Your launcher ran into a problem and needs to restart. We're just collecting some error info, and then we'll restart for you.\n$model$brand$android$mplVerCode$stacktrace$errCode"
        if (PREFS.bsodOutputEnabled) {
            binding.bsodDetailsText.text = error
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val db = BSOD.getData(context)
            Utils.saveError(error, db)
            db.close()

            delay(3000)
            withContext(Dispatchers.Main) {
                restartApplication()
            }
        }
    }

    private fun restartApplication() {
        if (activity != null) {
            val intent = Intent(requireActivity(), Main::class.java)
            intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}