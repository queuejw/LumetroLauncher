package ru.queuejw.lumetro.components.core

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

class AnimationUtils {

    companion object {
        @SuppressLint("ClickableViewAccessibility")
        fun setViewInteractAnimation(view: View, strength: Int) {

            fun clearAnim(view: View) {
                view.animate().rotationX(0f).rotationY(0f)
                    .setDuration(200).start()
            }

            view.setOnTouchListener { _, event ->
                val centerX = view.width / 2f
                val centerY = view.height / 2f
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val rotationX = (event.y - centerY) / centerY * strength
                        val rotationY = (centerX - event.x) / centerX * strength
                        view.animate().rotationX(-rotationX).rotationY(-rotationY).setDuration(125)
                            .start()
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearAnim(view)

                }
                false
            }
        }
    }
}