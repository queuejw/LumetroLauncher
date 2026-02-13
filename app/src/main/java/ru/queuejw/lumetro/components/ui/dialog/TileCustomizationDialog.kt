package ru.queuejw.lumetro.components.ui.dialog

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import coil3.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.ColorManager
import ru.queuejw.lumetro.databinding.TileCustomizationBinding
import ru.queuejw.lumetro.model.TileEntity

interface TileCustomizationDialogInterface {

    fun onTileChanged(newEntity: TileEntity)

    fun onTileIconUpdate(newIcon: Bitmap?, entity: TileEntity)

    fun onTileIconReset(entity: TileEntity, imageView: AppCompatImageView)

}

class TileCustomizationDialog(
    private val entity: TileEntity,
    private val icon: Bitmap?,
    private val dialogInterface: TileCustomizationDialogInterface
) : BottomSheetDialogFragment() {

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context?.let {
                val size = it.resources.getDimensionPixelSize(R.dimen.icon_size)
                val userIcon = Icon.createWithContentUri(uri).loadDrawable(it)?.toBitmap(size, size)
                updateIcon(userIcon)
                dialogInterface.onTileIconUpdate(userIcon, entity)
            }
        }
    }

    private var binding: TileCustomizationBinding? = null
    private var editLabelLayoutVisible = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Metro_BottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = TileCustomizationBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private fun updateIcon(newIcon: Bitmap?) {
        newIcon?.let {
            binding?.appIcon?.setImageBitmap(it)
        }
    }

    private fun changeLabel(newLabel: String) {
        entity.tileLabel = newLabel
        binding?.appLabel?.text = newLabel
        dialogInterface.onTileChanged(entity)
    }

    private fun controlEditLabelLayout() {
        editLabelLayoutVisible = !editLabelLayoutVisible
        if (!editLabelLayoutVisible) {
            binding?.let {
                it.changeLabelLayout.visibility = View.GONE
                it.labelEditText.setText(null)
            }
        } else {
            binding?.let {
                it.changeLabelLayout.visibility = View.VISIBLE
                it.labelEditText.setText(entity.tileLabel)
            }
        }
    }

    private fun changeColor(context: Context) {
        val d = ColorDialog(context)
        d.show(childFragmentManager, "color")
        childFragmentManager.setFragmentResultListener("color", viewLifecycleOwner) { _, bundle ->
            bundle.getString("color_value")?.let {
                entity.tileColor = it
                dialogInterface.onTileChanged(entity)
            }
        }
    }

    private fun updateTileCornerValue(newValue: Int) {
        entity.tileCornerRadius = newValue
        dialogInterface.onTileChanged(entity)
    }

    private fun setUi() {
        binding?.let { view ->
            view.appIcon.apply {
                this.load(icon)
                this.setOnClickListener {
                    activity?.let {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                this.setOnLongClickListener {
                    // Icon reset
                    dialogInterface.onTileIconReset(entity, this)
                    Toast.makeText(
                        this.context, this.context.resources.getString(android.R.string.ok),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnLongClickListener true
                }
            }
            view.appLabel.text = entity.tileLabel
            view.cornerRadiusSlider.apply {
                val accentColor =
                    ColorStateList.valueOf(ColorManager().getAccentColor(this.context))
                trackTintList = accentColor
                thumbTintList = accentColor
                value =
                    if (entity.tileCornerRadius != -1) entity.tileCornerRadius.toFloat() / 4 else 0f
            }

            view.changeLabelBtn.setOnClickListener {
                controlEditLabelLayout()
            }
            view.changeColorBtn.setOnClickListener {
                changeColor(it.context)
            }
            view.saveNewLabel.setOnClickListener {
                changeLabel(view.labelEditText.text.toString())
                controlEditLabelLayout()
            }
            view.cornerRadiusSlider.addOnChangeListener { _, value, _ ->
                updateTileCornerValue(value.toInt() * 4)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}