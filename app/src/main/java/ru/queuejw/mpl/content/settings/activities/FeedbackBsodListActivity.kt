package ru.queuejw.mpl.content.settings.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customBoldFont
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.data.bsod.BSODEntity
import ru.queuejw.mpl.databinding.BsodItemBinding
import ru.queuejw.mpl.databinding.LauncherSettingsFeedbackBsodsBinding
import ru.queuejw.mpl.helpers.utils.Utils

class FeedbackBsodListActivity : AppCompatActivity() {

    private lateinit var binding: LauncherSettingsFeedbackBsodsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = LauncherSettingsFeedbackBsodsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = BSOD.getData(this@FeedbackBsodListActivity)
            val mAdapter = BSODadapter(db.getDao().getBsodList(), db)
            withContext(Dispatchers.Main) {
                binding.settingsInclude.bsodlistRecycler.apply {
                    layoutManager = LinearLayoutManager(
                        this@FeedbackBsodListActivity,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    adapter = mAdapter
                }
            }
        }
        setupFont()
    }

    private fun setupFont() {
        customFont?.let {
            binding.settingsSectionLabel.typeface = it
            binding.settingsLabel.typeface = it
        }
        customBoldFont?.let {
            binding.settingsLabel.typeface = it
        }
    }

    private fun enterAnimation(exit: Boolean) {
        if (!PREFS.isTransitionAnimEnabled) return

        val main = binding.root
        val animatorSet = AnimatorSet().apply {
            playTogether(
                createObjectAnimator(
                    main,
                    "translationX",
                    if (exit) 0f else -300f,
                    if (exit) -300f else 0f
                ),
                createObjectAnimator(
                    main,
                    "rotationY",
                    if (exit) 0f else 90f,
                    if (exit) 90f else 0f
                ),
                createObjectAnimator(main, "alpha", if (exit) 1f else 0f, if (exit) 0f else 1f),
                createObjectAnimator(
                    main,
                    "scaleX",
                    if (exit) 1f else 0.5f,
                    if (exit) 0.5f else 1f
                ),
                createObjectAnimator(main, "scaleY", if (exit) 1f else 0.5f, if (exit) 0.5f else 1f)
            )
            duration = 400
        }
        animatorSet.start()
    }

    private fun createObjectAnimator(
        target: Any,
        property: String,
        startValue: Float,
        endValue: Float
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(target, property, startValue, endValue)
    }

    override fun onResume() {
        enterAnimation(false)
        super.onResume()
    }

    override fun onPause() {
        enterAnimation(true)
        super.onPause()
    }

    inner class BSODadapter(
        private var data: List<BSODEntity>,
        private val db: BSOD
    ) :
        RecyclerView.Adapter<BSODadapter.ViewHolder>() {

        inner class ViewHolder(val holderBinding: BsodItemBinding) :
            RecyclerView.ViewHolder(holderBinding.root) {
            init {
                if (PREFS.customFontInstalled) {
                    customFont?.let {
                        holderBinding.date.typeface = it
                        holderBinding.log.typeface = it
                    }
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun updateList() {
            lifecycleScope.launch(Dispatchers.IO) {
                val newList = db.getDao().getBsodList()
                withContext(Dispatchers.Main) {
                    data = newList
                    notifyDataSetChanged()
                }
            }
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                BsodItemBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val item = data[position]
            viewHolder.holderBinding.date.text = item.date.toString()
            viewHolder.holderBinding.log.text = item.log
            viewHolder.holderBinding.delete.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.getDao().removeLog(item)
                    viewHolder.itemView.post {
                        updateList()
                    }
                }
            }
            viewHolder.itemView.setOnClickListener {
                showDialog(item.log)
            }
            viewHolder.holderBinding.share.setOnClickListener {
                Utils.sendCrash(item.log, this@FeedbackBsodListActivity)
            }
        }

        private fun showDialog(text: String) {
            MaterialAlertDialogBuilder(this@FeedbackBsodListActivity)
                .setMessage(text)
                .setPositiveButton(getString(R.string.copy)) { _: DialogInterface?, _: Int ->
                    copyError(text)
                }.setNegativeButton(getString(android.R.string.cancel), null).show()
        }

        private fun copyError(error: String) {
            val clipbrd = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("MPL_Error", error)
            clipbrd.setPrimaryClip(clip)
        }

        override fun getItemCount() = data.size
    }
}