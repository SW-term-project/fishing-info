package com.example.fisinginfo.ui.identify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.fisinginfo.R
import org.tensorflow.lite.Interpreter
import java.io.File
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import com.example.fisinginfo.data.local.AppDatabase
import com.example.fisinginfo.data.local.SpeciesEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentifyActivity : AppCompatActivity() {
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val GALLERY_REQUEST_CODE = 101
        private const val PERMISSION_REQUEST_CODE = 102
    }
    val INPUT_SIZE = 224
    val PIXEL_SIZE = 3

    private var currentImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var tfliteInterpreter: Interpreter? = null

    // UI components
    private lateinit var ivSelectedImage: ImageView
    private lateinit var llPlaceholder: LinearLayout
    private lateinit var llMatchBadge: LinearLayout
    private lateinit var tvMatchPercent: TextView
    private lateinit var llButtonInitial: LinearLayout
    private lateinit var llButtonConfirm: LinearLayout
    private lateinit var cvResult: View
    private lateinit var tvResult: TextView
    private lateinit var vpSpecies: ViewPager2

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // 사용자가 이미지를 선택하면 실행되는 콜백
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    selectedBitmap = BitmapFactory.decodeStream(inputStream)
                }
                currentImageUri = uri
                displaySelectedImage()
            } catch (e: Exception) {
                Log.e("Gallery", "이미지 로드 실패", e)
                Toast.makeText(this, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("PhotoPicker", "선택된 이미지 없음")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identify)

        initializeViews()
        setupClickListeners()
        loadTFLiteModel()
        requestPermissions()
        initializeDatabase()
    }

    private fun initializeViews() {
        ivSelectedImage = findViewById(R.id.iv_selected_image)
        llPlaceholder = findViewById(R.id.ll_placeholder)
        llMatchBadge = findViewById(R.id.ll_match_badge)
        tvMatchPercent = findViewById(R.id.tv_match_percent)
        llButtonInitial = findViewById(R.id.ll_button_group_initial)
        llButtonConfirm = findViewById(R.id.ll_button_group_confirm)
        cvResult = findViewById(R.id.cv_result)
        tvResult = findViewById(R.id.tv_result)
        vpSpecies = findViewById(R.id.vp_species)
    }

    private fun setupClickListeners() {
        // 뒤로가기 버튼
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 카메라 버튼
        findViewById<Button>(R.id.btn_camera).setOnClickListener {
            openCamera()
        }

        // 갤러리 버튼
        findViewById<Button>(R.id.btn_gallery).setOnClickListener {
            openGallery()
        }

        // 분석 버튼
        findViewById<Button>(R.id.btn_analyze).setOnClickListener {
            analyzeImage()
        }

        // 취소 버튼
        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            resetImageSelection()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.CAMERA)

        // 안드로이드 13(API 33) 이상 대응
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 1. 이번 요청 목록(permissions)에 카메라 권한이 포함되어 있는지 확인
            val cameraIndex = permissions.indexOf(Manifest.permission.CAMERA)

            // 2. 카메라 권한이 요청 목록에 있었고, 그 결과가 거부(DENIED)인 경우에만 토스트 표시
            if (cameraIndex != -1 && grantResults[cameraIndex] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "카메라 권한이 거부되었습니다. 설정에서 허용해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        // 1. 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions()
            return
        }

        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile = createImageFile()

            if (photoFile != null) {
                // 2. FileProvider 호출 (Manifest의 authorities와 일치해야 함)
                currentImageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.fisinginfo.fileprovider",
                    photoFile
                )

                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
                // 3. startActivityForResult 실행
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.e("CameraError", "카메라 실행 실패: ${e.message}")
            Toast.makeText(this, "카메라를 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        // 이미지 파일만 선택하도록 설정하여 실행
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun createImageFile(): File? {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile("fishing_", ".jpg", storageDir)
        } catch (e: Exception) {
            Log.e("ImageFile", "Failed to create image file", e)
            null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    currentImageUri?.let { uri ->
                        try {
                            // uri.path 대신 스트림을 사용하여 비트맵 생성
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                selectedBitmap = BitmapFactory.decodeStream(inputStream)
                            }
                            displaySelectedImage()
                        } catch (e: Exception) {
                            Log.e("Camera", "이미지 가져오기 실패", e)
                        }
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            selectedBitmap = BitmapFactory.decodeStream(inputStream)
                        }
                        currentImageUri = uri
                        displaySelectedImage()
                    }
                }
            }
        }
    }

    private fun displaySelectedImage() {
        selectedBitmap?.let {
            ivSelectedImage.setImageBitmap(it)
            ivSelectedImage.visibility = View.VISIBLE
            llPlaceholder.visibility = View.GONE
            llMatchBadge.visibility = View.GONE
            llButtonInitial.visibility = View.GONE
            llButtonConfirm.visibility = View.VISIBLE
            cvResult.visibility = View.GONE
        }
    }

    private fun resetImageSelection() {
        ivSelectedImage.setImageBitmap(null)
        ivSelectedImage.visibility = View.GONE
        llPlaceholder.visibility = View.VISIBLE
        llMatchBadge.visibility = View.GONE
        llButtonInitial.visibility = View.VISIBLE
        llButtonConfirm.visibility = View.GONE
        cvResult.visibility = View.GONE
        selectedBitmap = null
        currentImageUri = null
        // 뷰페이저 숨기기 및 어댑터 제거
        try {
            vpSpecies.adapter = null
            vpSpecies.visibility = View.GONE
        } catch (e: Exception) {
            Log.w("IdentifyActivity", "vpSpecies hide error", e)
        }
    }

    private fun loadTFLiteModel() {
        try {

            val assetManager = assets
            val modelFilename = "model.tflite"
            val modelInputStream = assetManager.open(modelFilename)
            val modelFile = File(cacheDir, modelFilename)
            modelInputStream.use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tfliteInterpreter = Interpreter(modelFile)
            Log.d("TFLite", "Model loaded successfully")

        } catch (e: Exception) {
            Log.e("TFLite", "Failed to load model", e)
            Toast.makeText(this, "모델 로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeImage() {
        selectedBitmap?.let { bitmap ->
            tvResult.text = "분석 중..."
            cvResult.visibility = View.VISIBLE
            Thread {
                try {
                    val result = runInference(bitmap)
                    runOnUiThread {
                        displayAnalysisResult(result)
                    }
                } catch (e: Exception) {
                    Log.e("Analysis", "Failed to analyze", e)
                    runOnUiThread {
                        Toast.makeText(this, "분석 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun runInference(bitmap: Bitmap): Pair<String, Float> {
        val interpreter = tfliteInterpreter ?: throw IllegalStateException("Model이 로드되지 않았습니다.")
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
        val outputArray = Array(1) { ByteArray(5) }

        interpreter.run(inputBuffer, outputArray)

        val results = outputArray[0]
        var maxIndex = 0
        var maxConfidence = -1.0f

        for (i in results.indices) {
            val confidence = (results[i].toInt() and 0xFF) / 255.0f
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                maxIndex = i
            }
        }

        val labels = arrayOf(
            "감성돔",
            "조피볼락",
            "광어",
            "참돔",
            "돌돔"
        )

        return Pair(labels[maxIndex], maxConfidence)
    }

    // 비트맵을 바이트버퍼로 변환하는 전처리 함수
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = intValues[pixel++]

                // 2. 255f로 나누지 않고, 0~255 사이의 정수값을 그대로 Byte(1바이트)로 변환해 꽂아 넣습니다.
                val r = ((pixelValue shr 16) and 0xFF).toByte()
                val g = ((pixelValue shr 8) and 0xFF).toByte()
                val b = (pixelValue and 0xFF).toByte()

                byteBuffer.put(r)
                byteBuffer.put(g)
                byteBuffer.put(b)
            }
        }
        return byteBuffer
    }

    private fun displayAnalysisResult(result: Pair<String, Float>) {
        tvResult.text = "${result.first}"
        tvMatchPercent.text = "${String.format("%.1f", result.second * 100)}% match"
        llMatchBadge.visibility = View.VISIBLE

        // 분석된 어종 이름으로 로컬 DB에서 정보를 불러와서 ViewPager를 구성
        val speciesName = result.first
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val species: SpeciesEntity? = try {
                db.speciesDao().getSpeciesByName(speciesName)
            } catch (e: Exception) {
                null
            }

            withContext(Dispatchers.Main) {
                if (species == null) {
                    // DB에 정보가 없으면 사용자에게 알리고 빈 페이지들을 보여줌
                    Toast.makeText(this@IdentifyActivity, "로컬 DB에 ${speciesName} 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }

                // 어종 정보를 adapter에 넣어 ViewPager를 보여줌
                val adapter = SpeciesPagerAdapter(this@IdentifyActivity, species)
                vpSpecies.adapter = adapter

                // 양옆 페이지의 일부가 보이도록 RecyclerView 내부 설정
                vpSpecies.offscreenPageLimit = 1 // 양옆 페이지를 미리 로드

                // RecyclerView(내부) 참조해서 패딩과 클립 설정
                (vpSpecies.getChildAt(0) as? RecyclerView)?.let { recycler ->
                    recycler.clipToPadding = false
                    recycler.clipChildren = false
                    val pageOffsetPx = (resources.displayMetrics.density * 10).toInt()
                    val pageMarginPx = (resources.displayMetrics.density * 4).toInt()
                    recycler.setPadding(pageOffsetPx, 0, pageOffsetPx, 0)
                    recycler.overScrollMode = RecyclerView.OVER_SCROLL_NEVER

                    // Composite transformer: margin + scale
                    val composite = CompositePageTransformer()
                    composite.addTransformer(MarginPageTransformer(pageMarginPx))
                    composite.addTransformer { page, position ->
                        val r = 1 - abs(position)
                        page.scaleY = 0.85f + r * 0.15f
                    }
                    vpSpecies.setPageTransformer(composite)
                } ?: run {
                    // fallback simple transformer
                    vpSpecies.setPageTransformer { page, position ->
                        page.alpha = 0.7f + (1f - abs(position)) * 0.3f
                    }
                }

                vpSpecies.visibility = View.VISIBLE
                // 최초로 떠있는 뷰페이저는 어종사진(인덱스 1)
                vpSpecies.setCurrentItem(1, false)
            }
        }
    }

    private fun initializeDatabase() {
        // SharedPreferences를 사용해 처음 한 번만 DB 초기화
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isDbInitialized = prefs.getBoolean("db_initialized", false)

        if (!isDbInitialized) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val dao = db.speciesDao()

                    val species = listOf(
                        SpeciesEntity(
                            speciesName = "감성돔",
                            avgLength = "30~45 cm",
                            avgWeight = "0.8~1.5 kg",
                            depth = "5~15 m",
                            bestSeasons = "autumn,winter",
                            fishingMethods = "1. 구멍 찌낚시\n2. 막대찌 낚시\n3. 카고 낚시",
                            imageUrl = "https://i.namu.wiki/i/wDl-Uxi_exJb0puOjlIDnNRnLKeAQoWbDX-Tm5Ab1WUTLhrWMoPR85sm3zKrN3tq7OJIixscsJy8t5_Tp4daRg.webp"
                        ),
                        SpeciesEntity(
                            speciesName = "조피볼락",
                            avgLength = "30~40 cm",
                            avgWeight = "0.5~1.2 kg",
                            depth = "10~40 m",
                            bestSeasons = "spring,autumn",
                            fishingMethods = "선상 다운샷\n 릴 찌낚시\n 묶음추 원투",
                            imageUrl = "https://cdn.suhyupnews.co.kr/news/photo/202307/30674_26102_5555.jpg"
                        ),
                        SpeciesEntity(
                            speciesName = "광어",
                            avgLength = "40~60 cm",
                            avgWeight = "1.0~3.0 kg",
                            depth = "10~50 m",
                            bestSeasons = "spring,autumn",
                            fishingMethods = " 루어 다운샷\n 프리리그\n 메탈지그",
                            imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRUY7P3xUSrLE7f6LQ5Fyehrm_Sv6nBj7ksuOflE9ZusA&s=10"
                        ),
                        SpeciesEntity(
                            speciesName = "참돔",
                            avgLength = "40~70 cm",
                            avgWeight = "1.5~4.0 kg",
                            depth = "30~80 m",
                            bestSeasons = "spring,summer,autumn",
                            fishingMethods = " 루어\n 전유동 찌낚시\n 카고",
                            imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSkKQnfdzVKBri_d6qTKRRmxn_3DYVV1FqBfhxlvTl1Tw&s=10"
                        ),
                        SpeciesEntity(
                            speciesName = "돌돔",
                            avgLength = "35~50 cm",
                            avgWeight = "1.5~3.0 kg",
                            depth = "10~30 m",
                            bestSeasons = "summer,autumn",
                            fishingMethods = " 원투낚시\n 민장대 맥낚시",
                            imageUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRnIplGAq0F1mB9IRSJhg6vOii9kpB64K7JxPNo_8wQsg&s=10"
                        )
                    )

                    dao.insertAll(species)
                    prefs.edit().putBoolean("db_initialized", true).apply()
                    Log.d("DB_INIT", "DB 초기화 완료: ${species.size}개 어종 데이터 삽입됨")

                } catch (e: Exception) {
                    Log.e("DB_INIT", "DB 초기화 실패", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tfliteInterpreter?.close()
    }
}

