package com.example.fisinginfo.ui.map

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisinginfo.R
import com.example.fisinginfo.data.api.FishingRetrofitClient
import com.example.fisinginfo.data.model.FishingItem
import com.example.fisinginfo.ui.detail.DetailActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.util.FusedLocationSource

class FishingMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var naverMap: NaverMap
    private val markerList = mutableListOf<Marker>() // 마커 메모리 증발 방지용 리스트

    // 서버에서 받아온 전체 원본 데이터를 백업해둘 변수 (검색할 때 꺼내 씀)
    private var originalSpots = listOf<FishingItem>()

    // 검색 결과를 보여줄 리사이클러뷰 어댑터
    private lateinit var adapter: FishingSpotAdapter

    private lateinit var locationSource: FusedLocationSource
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✨ 2. FusedLocationSource 초기화 (권한 팝업을 알아서 띄워주는 꿀템)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fishing_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 네이버 지도 프래그먼트 세팅
        val fm = childFragmentManager
        val mapFragment = fm.findFragmentById(R.id.naver_map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.naver_map, it).commit()
            }

        mapFragment.getMapAsync(this)
    }

    // 지도가 로딩 완료되면 실행되는 함수
    override fun onMapReady(map: NaverMap) {
        this.naverMap = map

        naverMap.locationSource = locationSource

        // 1. ✨ [수정] Follow(강제추적) 대신 NoFollow(파란점만 표시하고 카메라는 안 움직임)로 변경!
        naverMap.locationTrackingMode = LocationTrackingMode.NoFollow

        // 2. ✨ [추가] 남한 전체가 들어오는 대한민국 중심 좌표와 줌 레벨(5.5) 설정
        val initialPosition = com.naver.maps.map.CameraPosition(
            com.naver.maps.geometry.LatLng(35.9078, 127.7669), 5.5
        )
        naverMap.cameraPosition = initialPosition

        // 3. ✨ [추가] 유저가 원할 때 자기 위치로 줌인할 수 있게 네이버 기본 '과녁 모양 버튼' 켜기
        naverMap.uiSettings.isLocationButtonEnabled = true

        setupRecyclerView()
        setupSearch()
        fetchFishingData()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // 1. 리사이클러뷰 세팅
    private fun setupRecyclerView() {
        val rv = requireView().findViewById<RecyclerView>(R.id.rv_search_results)

        adapter = FishingSpotAdapter(emptyList()) { clickedItem ->
            // 리스트에서 항목 클릭 시 상세 화면(DetailActivity)으로 데이터 넘기며 이동
            val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                putExtra("PLACE_NAME", clickedItem.seafsPstnNm)
                putExtra("TIDE", clickedItem.tdlvHrCn ?: "정보 없음")
                putExtra("TARGET_FISH", clickedItem.seafsTgfshNm ?: "정보 없음")
                putExtra("TOTAL_INDEX", clickedItem.totalIndex ?: "정보 없음")
                putExtra("LAT", clickedItem.lat)
                putExtra("LOT", clickedItem.lot) // 날씨 조회를 위한 위경도!
            }
            startActivity(intent)
        }

        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())
    }

    // 2. 검색창 세팅
    private fun setupSearch() {
        val etSearch = requireView().findViewById<EditText>(R.id.et_search_fish)
        val btnSearch = requireView().findViewById<ImageView>(R.id.btn_search)
        val rv = requireView().findViewById<RecyclerView>(R.id.rv_search_results)

        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString().trim()

            // 검색어를 다 지우고 돋보기를 누른 경우
            if (keyword.isEmpty()) {
                rv.visibility = View.GONE
                drawMarkers(originalSpots) // 지도에 전체 마커 다시 그리기
                return@setOnClickListener
            }

            // 검색어로 원본 데이터 필터링 (대상 어종에 포함되는지 확인)
            val searchResult = originalSpots.filter { item ->
                item.seafsTgfshNm?.contains(keyword) == true
            }

            if (searchResult.isEmpty()) {
                rv.visibility = View.GONE
                Toast.makeText(requireContext(), "${keyword} 낚시터가 없습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "${searchResult.size}곳의 포인트를 찾았습니다!", Toast.LENGTH_SHORT).show()

                // 결과가 있으면 리사이클러뷰를 띄우고 데이터 업데이트
                rv.visibility = View.VISIBLE
                adapter.updateData(searchResult)

                // 검색된 낚시터만 지도에 마커로 남기기
                drawMarkers(searchResult, moveCamera = true)
            }
        }
    }

    // 3. 지도에 마커를 그리는 전용 함수 (전체 그리기 & 검색 결과 그리기 공용)
    // ✨ 파라미터에 moveCamera = false 를 기본값으로 달아둠!
    private fun drawMarkers(spots: List<FishingItem>, moveCamera: Boolean = false) {
        // 기존 마커 지우기
        markerList.forEach { it.map = null }
        markerList.clear()

        spots.forEach { item ->
            val marker = com.naver.maps.map.overlay.Marker()
            marker.position = com.naver.maps.geometry.LatLng(item.lat, item.lot)
            marker.captionText = item.seafsPstnNm

            // 마커 크기와 색상 커스텀
            marker.width = 60
            marker.height = 85
            marker.icon = com.naver.maps.map.util.MarkerIcons.BLACK
            marker.iconTintColor = android.graphics.Color.parseColor("#1E3A8A")

            marker.map = naverMap
            markerList.add(marker)

            // 마커 클릭 이벤트
            marker.setOnClickListener {
                val intent = android.content.Intent(requireContext(), DetailActivity::class.java).apply {
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

        // ✨ [핵심 원인 해결] moveCamera가 true일 때만 강제로 화면을 이동시킴!
        if (moveCamera && spots.isNotEmpty()) {
            val firstSpot = spots[0]
            val cameraUpdate = com.naver.maps.map.CameraUpdate
                .scrollAndZoomTo(com.naver.maps.geometry.LatLng(firstSpot.lat, firstSpot.lot), 8.0)
                .animate(com.naver.maps.map.CameraAnimation.Easing)
            naverMap.moveCamera(cameraUpdate)
        }
    }

    // 4. API 서버 통신 (전체 데이터 긁어오기)
    private fun fetchFishingData() {
        val myApiKey = "lOyH2X4PGUTMbUx3dDYSgfntYrFqfbgQAm%2FvC2Nvmd7RT9xJ24fvAwD%2BbXwfq9K%2FZbe%2BmanRy40vsFFf4oSBqA%3D%3D"

        lifecycleScope.launch(Dispatchers.IO) {
            val allItems = mutableListOf<FishingItem>()
            var currentPage = 1
            var isMoreDataAvailable = true

            try {
                // 더 이상 가져올 데이터가 없을 때까지 무한 루프
                while (isMoreDataAvailable) {
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

                // 중복되는 낚시터 이름 1개만 남기고 필터링
                val uniqueFishingSpots = allItems.groupBy { it.seafsPstnNm }.map { entry ->
                    val items = entry.value
                    val baseItem = items.first() // 첫 번째 데이터를 기본 틀로 사용 (위경도, 지수 등)

                    // 해당 낚시터 이름으로 들어온 모든 데이터에서 물고기 이름만 싹 모으기
                    val combinedFish = items.mapNotNull { it.seafsTgfshNm } // null 데이터 제외
                        .flatMap { it.split(",") }                          // 혹시 "감성돔,농어" 처럼 콤마 있으면 쪼개기
                        .map { it.trim() }                                  // 앞뒤 공백 제거
                        .filter { it.isNotEmpty() && it != "정보 없음" }       // 무의미한 텍스트 필터링
                        .distinct()                                         // 물고기 이름 중복 제거 ("감성돔", "감성돔" -> "감성돔")
                        .joinToString(", ")                                 // 최종적으로 "감성돔, 농어, 돌돔" 형태로 합치기

                    // 기본 틀 데이터에 우리가 새로 싹 합친 물고기 리스트를 꽂아넣기
                    // 💡 만약 FishingItem이 data class면 아래처럼 copy를 쓰면 됨!
                    baseItem.copy(seafsTgfshNm = if (combinedFish.isNotEmpty()) combinedFish else "정보 없음")
                }

                // [확인용 로그] 이제 중복 제거 후 데이터가 어떻게 변했는지 로그캣으로 확인!
                Log.d("API_CHECK", "이름 묶기 완료! 총 낚시터 갯수: ${uniqueFishingSpots.size}개")

                // UI와 맵 업데이트는 Main 스레드에서!
                withContext(Dispatchers.Main) {
                    originalSpots = uniqueFishingSpots // 원본 백업
                    drawMarkers(originalSpots)         // 맵에 전체 그리기
                }

            } catch (e: Exception) {
                Log.e("API_CRASH", "통신 중 오류 발생: ${e.message}")
            }
        }
    }
}