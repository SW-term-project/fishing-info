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
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile != null) {
                currentImageUri = FileProvider.getUriForFile(this, "com.example.fisinginfo.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
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
                        selectedBitmap = BitmapFactory.decodeFile(uri.path)
                        displaySelectedImage()
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
        var maxConfidence = 0.0f

        for (i in results.indices) {
            if (results[i] > maxConfidence) {
                maxConfidence = results[i].toFloat()
                maxIndex = i
            }
        }

        val labels = arrayOf(
            "Black porgy (감성돔)",
            "Korea rockfish (조피볼락)",
            "Olive flounder (광어)",
            "Red seabream (참돔)",
            "Rock bream (돌돔)"
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
        tvResult.text = "${result.first} (해당 확률: ${String.format("%.1f", result.second * 100)}%)"
        tvMatchPercent.text = "${String.format("%.1f", result.second * 100)}% match"
        llMatchBadge.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        tfliteInterpreter?.close()
    }
}

