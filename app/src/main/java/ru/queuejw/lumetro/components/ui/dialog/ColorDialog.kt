package ru.queuejw.lumetro.components.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.components.core.DefaultAccentColors
import ru.queuejw.lumetro.components.prefs.Prefs
import ru.queuejw.lumetro.components.ui.metro.MetroButton

/**
 * Dialog in which the user can select a color
 */
class ColorDialog(val mContext: Context) : DialogFragment() {

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
    private var colorManager: ColorManager? = null
    private var mColor: String? = null
    private var preview: AppCompatImageView? = null
    private var saveButton: MetroButton? = null
    private var scroll: ScrollView? = null
    private var colorPicker: ColorPickerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.Lumetro_FullScreenDialog)
        colorManager = ColorManager()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.color_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val back = view.findViewById<MaterialCardView>(R.id.back_button)
        back.setOnClickListener { dismiss() }

        for (i in 0..<viewIds.size) {
            setOnClick(view.findViewById(viewIds[i]), i)
        }

        scroll = view.findViewById(R.id.scrollview)
        preview = view.findViewById(R.id.color_preview)


        colorPicker = view.findViewById(R.id.color_picker)
        val brightnessSlider = view.findViewById<BrightnessSlideBar>(R.id.brightness_slidebar)

        val prefs = Prefs(mContext)

        saveButton = view.findViewById(R.id.save_color)
        saveButton?.setOnClickListener {
            mColor?.let { value ->
                parentFragmentManager.setFragmentResult("color", bundleOf("color_value" to value))
            }
            dismiss()
        }

        colorPicker?.apply {
            setInitialColor(prefs.accentColorValue.toColorInt())
            setColorListener(object : ColorEnvelopeListener {

                override fun onColorSelected(envelope: ColorEnvelope, fromUser: Boolean) {
                    mColor = "#${envelope.hexCode}"
                    envelope.color.apply {
                        preview?.setColorFilter(this)
                        saveButton?.updateColor(this)
                    }
                }
            })
            attachBrightnessSlider(brightnessSlider!!)
        }
    }

    private fun setOnClick(colorView: View, colorValue: Int) {
        colorView.setOnClickListener {
            mColor = "#${
                ContextCompat.getColor(mContext, DefaultAccentColors.entries[colorValue].colorResId)
                    .toDrawable().color.toHexString()
            }"
            val colorInt = mColor!!.toColorInt()
            preview?.setColorFilter(colorInt)
            colorPicker?.setInitialColor(colorInt)
            saveButton?.updateColor(colorInt)
            scroll?.smoothScrollTo(0, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        colorManager = null
        scroll = null
        colorPicker?.apply {
            setColorListener(null)
        }
        colorPicker = null
        saveButton = null
        preview = null
    }

}