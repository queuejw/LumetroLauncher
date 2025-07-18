//https://github.com/shadowalker77/wp7progressbar

package ru.queuejw.lumetro.components.ui.progressbar

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.Utils.px2dp

private const val INTERVAL_DEF = 150
private const val INDICATOR_COUNT_DEF = 5
private const val ANIMATION_DURATION_DEF = 2200
private const val INDICATOR_HEIGHT_DEF = 5
private const val INDICATOR_RADIUS_DEF = 0

class WP7ProgressBar : LinearLayoutCompat {
    private var interval = 0
    private var animationDuration = 0
    private var indicatorHeight = 0
    private var indicatorColor = 0
    private var indicatorRadius = 0

    private var isShowing = false
    private var wp7Indicators: ArrayList<WP7Indicator>? = null

    private var handler: Handler? = null
    private var progressBarCount = 0

    private var objectAnimator: ObjectAnimator? = null

    constructor(context: Context) : super(context) {
        initialize(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        this.apply {
            layoutDirection = LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER
            orientation = HORIZONTAL
            handler = Handler(Looper.getMainLooper())
        }
        setAttributes(attrs)
        initializeIndicators()
    }

    private fun setAttributes(attributeSet: AttributeSet?) {
        context.withStyledAttributes(attributeSet, R.styleable.WP7ProgressBar) {
            interval = this.getInt(R.styleable.WP7ProgressBar_interval, INTERVAL_DEF)
            animationDuration =
                getInt(R.styleable.WP7ProgressBar_animationDuration, ANIMATION_DURATION_DEF)
            indicatorHeight =
                getInt(R.styleable.WP7ProgressBar_indicatorHeight, INDICATOR_HEIGHT_DEF)
            indicatorRadius =
                getInt(R.styleable.WP7ProgressBar_indicatorRadius, INDICATOR_RADIUS_DEF)
            indicatorColor = getColor(
                R.styleable.WP7ProgressBar_indicatorColor,
                ContextCompat.getColor(context, R.color.tile_cobalt)
            )
        }
    }

    private fun showAnimation() {
        if (wp7Indicators == null) {
            initializeIndicators()
        }
        wp7Indicators?.apply {
            for (i in this.indices) {
                this[i].startAnim(animationDuration.toLong(), (5 - i).toLong() * interval)
            }
        }
    }

    private fun initializeIndicators() {
        clearIndicatorsAnimations()
        this.removeAllViews()
        val indicators = ArrayList<WP7Indicator>()
        for (i in 0 until INDICATOR_COUNT_DEF) {
            val indicator = WP7Indicator(context, indicatorHeight, indicatorColor, indicatorRadius)
            indicators.add(indicator)
            this.addView(indicator)
        }
        wp7Indicators = indicators
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
        wp7Indicators?.forEach {
            it.removeAnim()
        }
    }

    fun showProgressBar() {
        progressBarCount++
        if (progressBarCount == 1) handler?.post { this@WP7ProgressBar.show() }
    }

    fun hideProgressBar() {
        progressBarCount--
        handler?.postDelayed({
            if (progressBarCount <= 0) {
                clearIndicatorsAnimations()
                this@WP7ProgressBar.hide()
                removeAllViews()
                objectAnimator?.cancel()
                wp7Indicators?.clear()
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

    private fun destroy() {
        clearIndicatorsAnimations()
        wp7Indicators?.clear()
        wp7Indicators = null
        handler = null
        objectAnimator?.cancel()
        objectAnimator = null
    }

    fun destroyView() {
        destroy()
    }

    override fun onDetachedFromWindow() {
        destroy()
        super.onDetachedFromWindow()
    }
}

internal class WP7Indicator(
    context: Context?,
    indicatorHeight: Int,
    private val color: Int,
    radius: Int
) : View(context) {
    private var objectAnimator: ObjectAnimator? = null

    private var animRemoved = false

    init {
        initialize(indicatorHeight, radius)
    }

    private fun initialize(indicatorHeight: Int, radius: Int) {
        this.background = getCube(radius)
        val layoutParams = LinearLayout.LayoutParams(
            px2dp(context, indicatorHeight),
            px2dp(context, indicatorHeight)
        )
        layoutParams.rightMargin = px2dp(context, (1.5f * indicatorHeight).toInt())
        this.layoutParams = layoutParams
        startAnim(0, 0)
        removeAnim()
    }

    private fun getCube(radius: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(color)
        drawable.cornerRadius = px2dp(context, radius).toFloat()
        return drawable
    }

    fun startAnim(animationDuration: Long, delay: Long) {
        animRemoved = false
        objectAnimator = ObjectAnimator.ofFloat(this, "translationX", +1000f, -1000f).apply {
            interpolator = WPInterpolator()
            duration = animationDuration
            repeatMode = ValueAnimator.RESTART
            startDelay = delay
            doOnEnd {
                post {
                    if (!animRemoved) startAnim(animationDuration, delay)
                }
            }
        }
        objectAnimator?.start()
    }

    fun removeAnim() {
        objectAnimator?.apply {
            removeAllListeners()
            cancel()
        }
        animRemoved = true
        objectAnimator = null
    }

    override fun onDetachedFromWindow() {
        removeAnim()
        super.onDetachedFromWindow()
    }
}
