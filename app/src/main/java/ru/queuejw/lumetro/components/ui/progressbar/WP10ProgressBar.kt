//https://github.com/shadowalker77/wp7progressbar
package ru.queuejw.lumetro.components.ui.progressbar

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.Utils.px2dp

private const val INTERVAL_DEF = 150
private const val INDICATOR_COUNT_DEF = 5
private const val ANIMATION_DURATION_DEF = 1800
private const val INDICATOR_HEIGHT_DEF = 7
private const val INDICATOR_RADIUS_DEF = 0

class WP10ProgressBar : RelativeLayout {
    private var interval = 0
    private var animationDuration = 0
    private var indicatorHeight = 0
    private var indicatorColor = 0
    private var indicatorRadius = 0

    private var isShowing = false
    private var wp10Indicators: ArrayList<WP10Indicator>? = null

    private var handler: Handler? = null
    private var progressBarCount = 0

    constructor(context: Context?) : super(context) {
        initialize(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        this.gravity = Gravity.CENTER
        this.handler = Handler(Looper.getMainLooper())
        this.rotation = -25f
        setAttributes(attrs)
        initializeIndicators()
    }

    private fun setAttributes(attributeSet: AttributeSet?) {
        context.withStyledAttributes(attributeSet, R.styleable.WP7ProgressBar) {
            interval = getInt(
                R.styleable.WP7ProgressBar_interval,
                INTERVAL_DEF
            )
            animationDuration =
                getInt(
                    R.styleable.WP7ProgressBar_animationDuration,
                    ANIMATION_DURATION_DEF
                )
            indicatorHeight =
                getInt(
                    R.styleable.WP7ProgressBar_indicatorHeight,
                    INDICATOR_HEIGHT_DEF
                )
            indicatorRadius =
                getInt(
                    R.styleable.WP7ProgressBar_indicatorRadius,
                    INDICATOR_RADIUS_DEF
                )
            indicatorColor = getColor(
                R.styleable.WP7ProgressBar_indicatorColor,
                ContextCompat.getColor(context, R.color.tile_cobalt)
            )
            recycle()
        }
    }

    private fun showAnimation() {
        for (i in wp10Indicators!!.indices) {
            wp10Indicators!![i]
                .startAnim(animationDuration.toLong(), (5 - i).toLong() * interval)
        }
    }

    private fun initializeIndicators() {
        this.removeAllViews()
        val WP10Indicators = ArrayList<WP10Indicator>()
        for (i in 0..<INDICATOR_COUNT_DEF) {
            val indicator =
                WP10Indicator(context, indicatorHeight, indicatorColor, indicatorRadius, i)
            //                wp10Indicator.setRotation(i * 14);
            WP10Indicators.add(indicator)
            this.addView(indicator)
        }
        this.wp10Indicators = WP10Indicators
    }

    private fun show() {
        if (isShowing) return
        isShowing = true
        showAnimation()
    }

    private fun hide() {
        clearIndicatorsAnimations()
        isShowing = false
    }

    private fun clearIndicatorsAnimations() {
        for (wp10Indicator in wp10Indicators!!) {
            wp10Indicator.removeAnim()
        }
    }

    fun showProgressBar() {
        progressBarCount++
        if (progressBarCount == 1) {
            handler!!.post { this@WP10ProgressBar.show() }
        }
    }

    fun hideProgressBar() {
        progressBarCount--
        handler!!.postDelayed({
            if (progressBarCount == 0) {
                this@WP10ProgressBar.hide()
            }
        }, 50)
    }

    fun setInterval(interval: Int) {
        this.interval = interval
        initializeIndicators()
    }

    fun setAnimationDuration(animationDuration: Int) {
        this.animationDuration = animationDuration
        initializeIndicators()
    }

    fun setIndicatorHeight(indicatorHeight: Int) {
        this.indicatorHeight = indicatorHeight
        initializeIndicators()
    }

    fun setIndicatorColor(indicatorColor: Int) {
        this.indicatorColor = indicatorColor
        initializeIndicators()
    }

    fun setIndicatorRadius(indicatorRadius: Int) {
        this.indicatorRadius = indicatorRadius
        initializeIndicators()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hideProgressBar()
    }
}

internal class WP10Indicator(
    context: Context?,
    indicatorHeight: Int,
    color: Int,
    radius: Int,
    number: Int
) :
    RelativeLayout(context) {
    private var objectAnimator: ObjectAnimator? = null
    private var number = 0

    init {
        initialize(indicatorHeight, color, radius, number)
    }

    private fun initialize(indicatorHeight: Int, color: Int, radius: Int, mNumber: Int) {
        val size = px2dp(context, indicatorHeight * 8)
        this.apply {
            layoutDirection = LAYOUT_DIRECTION_LTR
            number = mNumber
            layoutParams = LayoutParams(size, size)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            addView(Base10Indicator(context, indicatorHeight, color, radius))
        }
        startAnim(0, 0)
        removeAnim()
        this.alpha = 0f
    }

    fun startAnim(animationDuration: Long, delay: Long) {
        objectAnimator = ObjectAnimator.ofFloat(
            this,
            "rotation",
            (number * 15).toFloat(),
            (-360 + (number * 15)).toFloat()
        ).also {
            it.apply {
                interpolator = WPInterpolator()
                duration = animationDuration
                repeatMode = ValueAnimator.RESTART
                repeatCount = 2
                startDelay = delay
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {
                        this@WP10Indicator.alpha = 1f
                        startAlphaAnimation(animationDuration)
                    }

                    override fun onAnimationEnd(animator: Animator) {
                        removeAnim()
                        startAnim(animationDuration, 0)
                    }

                    override fun onAnimationCancel(animator: Animator) {
                    }

                    override fun onAnimationRepeat(animator: Animator) {
                    }
                })
            }
        }
        objectAnimator?.start()
    }

    fun startAlphaAnimation(animationDuration: Long) {
        this.animate().alpha(0f).setDuration(animationDuration / 12).startDelay =
            2 * animationDuration
    }

    fun removeAnim() {
        this.apply {
            animate().alpha(0f).setDuration(0).startDelay = 0
            animate().cancel()
            clearAnimation()
        }
        objectAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        objectAnimator = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAnim()
    }
}
