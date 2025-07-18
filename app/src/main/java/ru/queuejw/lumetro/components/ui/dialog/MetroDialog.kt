package ru.queuejw.lumetro.components.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.databinding.DialogLayoutBinding


/**
 * Simple Windows Phone-style dialog.
 */
class MetroDialog : DialogFragment() {

    private var gravity: Int? = null
    private var binding: DialogLayoutBinding? = null
    private var mTitle: String? = null
    private var mMessage: String? = null

    private var positiveButtonText: String? = null
    private var neutralButtonText: String? = null
    private var negativeButtonText: String? = null

    private var positiveListener: View.OnClickListener? = null
    private var negativeListener: View.OnClickListener? = null
    private var neutralListener: View.OnClickListener? = null

    private var dismissListener: DialogInterface.OnDismissListener? = null

    private var backgroundColor: Int? = null

    fun setTitle(string: String) {
        mTitle = string
    }

    fun setMessage(string: String) {
        mMessage = string
    }

    fun setDialogCancelable(boolean: Boolean) {
        isCancelable = boolean
    }

    fun setPositiveDialogListener(string: String, listener: View.OnClickListener?): MetroDialog {
        positiveButtonText = string
        positiveListener = listener
        return this
    }

    fun setNeutralDialogListener(string: String, listener: View.OnClickListener?): MetroDialog {
        neutralButtonText = string
        neutralListener = listener
        return this
    }

    fun setNegativeDialogListener(string: String, listener: View.OnClickListener?): MetroDialog {
        negativeButtonText = string
        negativeListener = listener
        return this
    }

    fun setDismissListener(listener: DialogInterface.OnDismissListener) {
        dismissListener = listener
    }

    fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    private fun setWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Metro_Dialog)
        super.onCreate(savedInstanceState)
        gravity = arguments?.getInt("dialog_gravity")
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            window?.apply {
                gravity?.let {
                    setGravity(it)
                } ?: {
                    setGravity(Gravity.TOP)
                }
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            setOnDismissListener(dismissListener)
        }

    }

    private fun configureButtons(binding: DialogLayoutBinding) {
        if (positiveListener == null) {
            binding.positiveButton.visibility = View.GONE
        } else {
            binding.positiveButton.setOnClickListener(positiveListener)
            positiveButtonText?.let {
                binding.positiveButton.text = it
            }
        }
        if (neutralListener == null) {
            binding.neutralButton.visibility = View.GONE
        } else {
            binding.neutralButton.setOnClickListener(neutralListener)
            neutralButtonText?.let {
                binding.neutralButton.text = it
            }
        }
        if (negativeListener == null) {
            binding.negativeButton.visibility = View.GONE
        } else {
            binding.negativeButton.setOnClickListener(negativeListener)
            negativeButtonText?.let {
                binding.negativeButton.text = it
            }
        }

    }

    private fun initDialog() {
        binding?.apply {
            backgroundColor?.let {
                root.setBackgroundColor(it)
            }
            setWindowInsets(root)
            configureButtons(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DialogLayoutBinding.inflate(inflater, container, false)
        initDialog()
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            title.text = mTitle
            message.text = mMessage
            root.translationY = when (gravity) {
                Gravity.TOP -> root.context.resources.getDimensionPixelSize(R.dimen.dialog_translation_y_top)
                    .toFloat()

                Gravity.BOTTOM -> root.context.resources.getDimensionPixelSize(R.dimen.dialog_translation_y)
                    .toFloat()

                else -> 0f
            }
            root.animate().alpha(1f).translationY(0f).setDuration(300).start()
        }
    }

    override fun onDestroyView() {
        binding?.apply {
            positiveButton.setOnClickListener(null)
            neutralButton.setOnClickListener(null)
            negativeButton.setOnClickListener(null)
        }
        positiveListener = null
        neutralListener = null
        negativeListener = null
        dismissListener = null
        binding = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(gravity: Int): MetroDialog {
            return MetroDialog().apply {
                arguments = Bundle().apply {
                    putInt("dialog_gravity", gravity)
                }
            }
        }
    }
}