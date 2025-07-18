package ru.queuejw.lumetro.components.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.queuejw.lumetro.main.fragments.AppsFragment
import ru.queuejw.lumetro.main.fragments.StartFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    private val adapterItemCount: Int = 2

    override fun createFragment(position: Int): Fragment =
        if (position == 0) StartFragment() else AppsFragment()

    override fun getItemCount(): Int = adapterItemCount
}