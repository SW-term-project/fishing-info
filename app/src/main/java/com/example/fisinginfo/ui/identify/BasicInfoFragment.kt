package com.example.fisinginfo.ui.identify

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fisinginfo.R

class BasicInfoFragment : Fragment() {

    companion object {
        private const val ARG_LENGTH = "arg_length"
        private const val ARG_WEIGHT = "arg_weight"
        private const val ARG_DEPTH = "arg_depth"

        fun newInstance(length: String?, weight: String?, depth: String?) = BasicInfoFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_LENGTH, length)
                putString(ARG_WEIGHT, weight)
                putString(ARG_DEPTH, depth)
            }
        }
    }

    private var length: String? = null
    private var weight: String? = null
    private var depth: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            length = it.getString(ARG_LENGTH)
            weight = it.getString(ARG_WEIGHT)
            depth = it.getString(ARG_DEPTH)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_basic_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvLength = view.findViewById<TextView>(R.id.tv_avg_length)
        val tvWeight = view.findViewById<TextView>(R.id.tv_avg_weight)
        val tvDepth = view.findViewById<TextView>(R.id.tv_depth)

        tvLength.text = length ?: "정보 없음"
        tvWeight.text = weight ?: "정보 없음"
        tvDepth.text = depth ?: "정보 없음"
    }
}

