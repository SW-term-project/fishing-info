package com.example.fisinginfo.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.fisinginfo.R
import com.example.fisinginfo.data.remote.RetrofitClient
import com.example.fisinginfo.ui.detail.DetailActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ✨ 1. OnMapReadyCallback 인터페이스를 상속받아야 지도를 쓸 수 있음!
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // ✨ 2. 네이버 맵 객체를 담아둘 전역 변수 선언
    private lateinit var naverMap: NaverMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ✨ 3. 화면(xml)에 있는 FragmentContainerView에서 네이버 지도 프래그먼트를 불러옴
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        // 지도가 준비되면 비동기로 onMapReady() 함수를 실행하라는 명령
        mapFragment.getMapAsync(this)
    }

    // ✨ 4. 지도가 완벽하게 로딩되면 이 함수가 자동으로 실행됨
    override fun onMapReady(map: NaverMap) {
        // 준비된 지도 객체를 전역 변수에 저장!
        this.naverMap = map

        // 지도가 켜졌으니, 이제 낚시 데이터를 서버에서 불러오자!
        fetchFishingData()
    }

    private fun fetchFishingData() {
        val myApiKey = "lOyH2X4PGUTMbUx3dDYSgfntYrFqfbgQAm%2FvC2Nvmd7RT9xJ24fvAwD%2BbXwfq9K%2FZbe%2BmanRy40vsFFf4oSBqA%3D%3D"

        // Coroutine 시작 (백그라운드 스레드에서 API 통신)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. API 통신 요청
                val response = RetrofitClient.apiService.getFishingInfo(serviceKey = myApiKey)

                if (response.isSuccessful) {
                    Log.w("API_TEST", "서버가 보낸 원본 데이터: ${response.errorBody()?.string() ?: response.body()}")
                    // 2. 통신 성공 시 JSON 데이터 파싱
                    val fishingItems = response.body()?.body?.items?.item

                    // 3. UI 업데이트 (마커 찍기)는 반드시 Main 스레드에서!
                    withContext(Dispatchers.Main) {
                        fishingItems?.forEach { item ->
                            // 이제 naverMap 변수가 정상적으로 살아있으니 마커가 잘 찍힘!
                            val marker = Marker()
                            marker.position = LatLng(item.lat, item.lot)
                            marker.captionText = item.seafsPstnNm
                            marker.map = naverMap

                            // 마커 클릭 이벤트
                            marker.setOnClickListener {
                                val intent = Intent(this@MainActivity, DetailActivity::class.java).apply {
                                    putExtra("PLACE_NAME", item.seafsPstnNm)
                                    putExtra("TIDE", item.tdlvHrCn ?: "정보 없음")
                                    putExtra("TARGET_FISH", item.seafsTgfshNm ?: "정보 없음")
                                    putExtra("TOTAL_INDEX", item.totalIndex ?: "정보 없음")
                                }
                                startActivity(intent)
                                true
                            }
                        }
                    }
                } else {
                    Log.e("API_ERROR", "응답 실패: 코드 ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_CRASH", "통신 중 오류 발생: ${e.message}")
            }
        }
    }
}
