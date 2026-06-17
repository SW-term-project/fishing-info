package com.example.fisinginfo.ui.map

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fisinginfo.R
import com.example.fisinginfo.data.api.FishingRetrofitClient
import com.example.fisinginfo.ui.detail.DetailActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FishingMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var naverMap: NaverMap

    // 1. 프래그먼트용 화면(XML)을 연결하는 곳
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 3단계에서 만들 fragment_fishing_map.xml을 가져와서 화면에 그림
        return inflater.inflate(R.layout.fragment_fishing_map, container, false)
    }

    // 2. 화면이 다 그려진 직후에 지도를 세팅
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✨ 핵심: Fragment 안에서 또 다른 Fragment(네이버맵)를 부를 땐 childFragmentManager를 써야 안 터짐!
        val fm = childFragmentManager
        val mapFragment = fm.findFragmentById(R.id.naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.naver_map, it).commit()
            }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: NaverMap) {
        this.naverMap = map
        fetchFishingData()
    }

    // ✨ 클래스 최상단 전역 변수 모음에 이거 한 줄 추가 (마커 메모리 증발 방지)
    private val markerList = mutableListOf<Marker>()

    private fun fetchFishingData() {
        val myApiKey = "lOyH2X4PGUTMbUx3dDYSgfntYrFqfbgQAm%2FvC2Nvmd7RT9xJ24fvAwD%2BbXwfq9K%2FZbe%2BmanRy40vsFFf4oSBqA%3D%3D"

        lifecycleScope.launch(Dispatchers.IO) {
            val allItems = mutableListOf<com.example.fisinginfo.data.model.FishingItem>()
            var currentPage = 1
            var isMoreDataAvailable = true

            try {
                while (isMoreDataAvailable) {
                    // ✨ 서버가 절대 안 뻗게 100개씩만 안전하게 요청
                    val response = FishingRetrofitClient.apiService.getFishingInfo(
                        serviceKey = myApiKey,
                        pageNo = currentPage
                    )

                    if (response.isSuccessful) {
                        val fishingBody = response.body()?.body
                        val currentItems = fishingBody?.items?.item

                        if (!currentItems.isNullOrEmpty()) {
                            allItems.addAll(currentItems)
                            currentPage++

                            val totalCount = fishingBody.totalCount
                            if (allItems.size >= totalCount) {
                                isMoreDataAvailable = false
                            }
                        } else {
                            isMoreDataAvailable = false
                        }
                    } else {
                        Log.e("API_ERROR", "응답 실패: 코드 ${response.code()}")
                        isMoreDataAvailable = false
                    }
                }
                Log.d("API_VERIFY", "==================================================")
                Log.d("API_VERIFY", "1. 서버에서 긁어온 순수 원본 데이터 총 갯수: ${allItems.size}개")
                // 중복 제거
                val uniqueFishingSpots = allItems.distinctBy { it.seafsPstnNm }

                // 🚨 [확인용 로그] 로그캣에 이게 몇 개라고 찍히는지 꼭 봐봐!
                Log.d("API_CHECK", "최종 중복 제거된 낚시터 갯수: ${uniqueFishingSpots.size}개")

                // UI 업데이트
                withContext(Dispatchers.Main) {
                    // 기존에 찍혀있던 마커가 있다면 싹 청소
                    markerList.forEach { it.map = null }
                    markerList.clear()

                    uniqueFishingSpots.forEach { item ->
                        val marker = Marker()
                        marker.position = LatLng(item.lat, item.lot)
                        marker.captionText = item.seafsPstnNm
                        marker.map = naverMap // 지도에 부착

                        // ✨ 전역 리스트에 마커를 담아서 메모리에서 안 날아가게 꽉 잡아둠
                        markerList.add(marker)

                        // 🚨 [확인용 로그 2] 실제로 마커가 화면에 그려지고 있는지 체크
                        Log.d("API_MARKER", "마커 생성됨: ${item.seafsPstnNm} (${item.lat}, ${item.lot})")

                        marker.setOnClickListener {
                            val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                                putExtra("PLACE_NAME", item.seafsPstnNm)
                                putExtra("TIDE", item.tdlvHrCn ?: "정보 없음")
                                putExtra("TARGET_FISH", item.seafsTgfshNm ?: "정보 없음")
                                putExtra("TOTAL_INDEX", item.totalIndex ?: "정보 없음")
                                putExtra("LAT", item.lat)
                                putExtra("LOT", item.lot)
                            }
                            startActivity(intent)
                            true
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("API_CRASH", "통신 중 오류 발생: ${e.message}")
            }
        }
    }
}