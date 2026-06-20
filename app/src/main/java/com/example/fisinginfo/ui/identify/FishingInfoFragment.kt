package com.example.fisinginfo.ui.identify

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fisinginfo.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class FishingInfoFragment : Fragment() {

    companion object {
        private const val ARG_SEASONS = "arg_seasons"
        private const val ARG_METHODS = "arg_methods"

        fun newInstance(seasonsCsv: String?, methods: String?) = FishingInfoFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_SEASONS, seasonsCsv)
                putString(ARG_METHODS, methods)
            }
        }
    }

    private var seasonsCsv: String? = null
    private var methods: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            seasonsCsv = it.getString(ARG_SEASONS)
            methods = it.getString(ARG_METHODS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fishing_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cgSeasons = view.findViewById<ChipGroup>(R.id.chipgroup_seasons)
        val tvMethods = view.findViewById<TextView>(R.id.tv_fishing_methods)

        // seasonsCsv는 "spring,summer" 같은 형태라고 가정
        val activeSeasons = seasonsCsv?.split(',')?.map { it.trim().lowercase() } ?: emptyList()

        val seasons = listOf("spring" to "봄", "summer" to "여름", "autumn" to "가을", "winter" to "겨울")

        seasons.forEach { (key, label) ->
            val chip = Chip(requireContext())
            chip.text = label
            chip.isClickable = false
            chip.isCheckable = false

            val isActive = activeSeasons.any { s -> s.contains(key) || s.contains(label) }

            if (isActive) {
                // 활성화 스타일 (파란/흰색)
                chip.setTextColor(Color.WHITE)
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
            } else {
                // 비활성화 스타일 (흰색/검정색)
                chip.setTextColor(Color.DKGRAY)
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#FAFAFA"))
            }

            cgSeasons.addView(chip)
        }

        // methods를 마크다운 리스트처럼 보여주기
        if (!methods.isNullOrBlank()) {
            val lines = methods!!.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            val sb = SpannableStringBuilder()
            lines.forEachIndexed { idx, line ->
                sb.append("• ")
                sb.append(line)
                if (idx < lines.size - 1) sb.append("\n\n")
            }
            tvMethods.text = sb
        } else {
            tvMethods.text = "정보 없음"
        }
    }
}



