package ru.queuejw.lumetro.fle

import android.os.Bundle
import android.view.View
import ru.queuejw.lumetro.components.core.base.BaseFLEActivity

class FirstLaunchExperienceActivity : BaseFLEActivity() {

    fun setDefaultButtonOnClickListeners() {
        binding.apply {
            next.setOnClickListener {
                animateBottomBar(false)
                setFragment(nextFragment)
            }
            previous.setOnClickListener {
                animateBottomBar(false)
                setFragment(previousFragment)
            }
        }
    }

    fun setNextButtonOnClickListener(listener: View.OnClickListener?) {
        binding.next.setOnClickListener(listener)
    }

    private fun setUi() {
        setDefaultButtonOnClickListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val value = savedInstanceState.getInt("current_fle_fragment")
            setFragment(value, false)
        } else {
            setFragment(prefs.fleProgress, prefs.fleProgress != 4)
        }
        setUi()
    }
}