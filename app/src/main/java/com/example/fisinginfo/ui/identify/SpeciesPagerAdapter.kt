package com.example.fisinginfo.ui.identify

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fisinginfo.data.local.SpeciesEntity

class SpeciesPagerAdapter(
    fa: FragmentActivity,
    private val species: SpeciesEntity?
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = 4 // map, photo, basic, fishing info

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MapPageFragment.newInstance(species?.speciesName)
            1 -> PhotoFragment.newInstance(species?.imageUrl)
            2 -> BasicInfoFragment.newInstance(
                species?.avgLength,
                species?.avgWeight,
                species?.depth
            )
            3 -> FishingInfoFragment.newInstance(
                species?.bestSeasons,
                species?.fishingMethods
            )
            else -> Fragment()
        }
    }
}

