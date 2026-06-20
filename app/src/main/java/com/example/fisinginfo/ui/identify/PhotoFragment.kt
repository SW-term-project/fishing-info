package com.example.fisinginfo.ui.identify

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fisinginfo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class PhotoFragment : Fragment() {

    companion object {
        private const val ARG_URL = "arg_url"

        fun newInstance(imageUrl: String?) = PhotoFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, imageUrl) }
        }
    }

    private var imageUrl: String? = null
    private lateinit var ivPhoto: ImageView
    private lateinit var btnDownload: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUrl = arguments?.getString(ARG_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivPhoto = view.findViewById(R.id.iv_species_photo)
        btnDownload = view.findViewById(R.id.btn_download_image)
        progressBar = view.findViewById(R.id.pb_photo)

        // 이미지가 있으면 비동기로 로드
        imageUrl?.let { url ->
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = loadBitmapFromUrl(url)
                    withContext(Dispatchers.Main) {
                        ivPhoto.setImageBitmap(bitmap)
                        progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("PhotoFragment", "이미지 로드 실패", e)
                    withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
                }
            }
        }

        btnDownload.setOnClickListener {
            imageUrl?.let { url ->
                try {
                    val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setAllowedOverMetered(true)
                        .setTitle("${getString(R.string.app_name)} 이미지 다운로드")
                    dm.enqueue(request)
                } catch (e: Exception) {
                    Log.e("PhotoFragment", "다운로드 실패", e)
                }
            }
        }
        Log.d("PhotoFragment", "전달받은 URL: $imageUrl") // 이 로그를 확인하세요!

        if (imageUrl.isNullOrBlank()) {
            Log.e("PhotoFragment", "URL이 비어있습니다. DB 확인 필요")
            return
        }
    }

    private fun loadBitmapFromUrl(src: String): android.graphics.Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(src)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            android.graphics.BitmapFactory.decodeStream(input)
        } finally {
            connection?.disconnect()
        }
    }
}

