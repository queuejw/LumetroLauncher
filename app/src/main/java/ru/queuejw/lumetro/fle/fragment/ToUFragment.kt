package ru.queuejw.lumetro.fle.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.queuejw.lumetro.R
import ru.queuejw.lumetro.components.core.base.BaseFragment
import ru.queuejw.lumetro.databinding.FleTouBinding
import ru.queuejw.lumetro.fle.FirstLaunchExperienceActivity

class ToUFragment : BaseFragment<FleTouBinding>() {

    private var bottomBarVisible = false

    override fun getFragmentViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FleTouBinding? {
        return FleTouBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as FirstLaunchExperienceActivity?)?.apply {
            setAppBarText(getString(R.string.tou_fragment_title))
            nextFragment = 4
            previousFragment = -1
            enableAllButtons()
            updateNextButtonText(getString(R.string.accept))
            updatePreviousButtonText(getString(R.string.reject))
        }
        binding.textView.apply {
            val string = """
${this.context.getString(R.string.app_description)}
    
${this.context.getString(R.string.usage_terms)}
    
${this.context.getString(R.string.security_warning)}
    
${this.context.getString(R.string.data_collection_header)}
${
                this.context.resources.getStringArray(R.array.collected_data_items)
                    .joinToString("\n• ", prefix = "• ")
            }

${this.context.getString(R.string.data_storage_info)}
    
""".trimIndent()

            text = string

        }
        binding.root.apply {
            setOnScrollChangeListener { view, x, y, oldX, oldY ->
                if (this.getChildAt(0).bottom == (this.height + this.scrollY)) {
                    if (!bottomBarVisible) (activity as FirstLaunchExperienceActivity?)?.animateBottomBar(
                        true
                    )
                    bottomBarVisible = true
                }
            }
        }
    }
}