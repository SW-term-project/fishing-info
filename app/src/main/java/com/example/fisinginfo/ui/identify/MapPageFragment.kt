package com.example.fisinginfo.ui.identify

import android.os.Bundle
import android.content.Intent
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.fisinginfo.R
import com.example.fisinginfo.ui.map.FishingMapFragment

class MapPageFragment : Fragment() {

    companion object {
        private const val ARG_SPECIES_NAME = "arg_species_name"

        fun newInstance(speciesName: String? = null) = MapPageFragment().apply {
            arguments = Bundle().apply { putString(ARG_SPECIES_NAME, speciesName) }
        }
    }

    private var speciesName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speciesName = arguments?.getString(ARG_SPECIES_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSearch = view.findViewById<Button>(R.id.btn_search_on_map)

        // 버튼 텍스트에 어종명 표시
        val buttonText = if (speciesName.isNullOrBlank()) {
            "지도에서\n검색"
        } else {
            "지도에서\n${speciesName}\n검색"
        }
        btnSearch.text = buttonText

        // 버튼 클릭: IdentifyActivity를 종료하고 결과(검색어)를 Main으로 전달
        btnSearch.setOnClickListener {
            if (!speciesName.isNullOrBlank()) {
                val data = Intent().apply { putExtra("SEARCH_KEYWORD", speciesName) }
                requireActivity().setResult(Activity.RESULT_OK, data)
                requireActivity().finish()
            }
        }
    }
}


