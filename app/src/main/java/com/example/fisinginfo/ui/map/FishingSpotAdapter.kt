package com.example.fisinginfo.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisinginfo.R
import com.example.fisinginfo.data.model.FishingItem

class FishingSpotAdapter(
    private var spots: List<FishingItem>,
    private val onItemClick: (FishingItem) -> Unit // 클릭 이벤트 처리용
) : RecyclerView.Adapter<FishingSpotAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_item_name)
        val tvFish: TextView = view.findViewById(R.id.tv_item_fish)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fishing_spot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = spots[position]
        holder.tvName.text = item.seafsPstnNm
        holder.tvFish.text = "대상 어종: ${item.seafsTgfshNm ?: "정보 없음"}"

        // 리스트 아이템 클릭 시 이벤트 실행
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = spots.size

    // 검색 결과가 바뀔 때마다 리스트를 새로고침 해주는 함수
    fun updateData(newSpots: List<FishingItem>) {
        spots = newSpots
        notifyDataSetChanged()
    }
}