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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource
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

        // 이미지가 있으면 Glide로 로드
        imageUrl?.let { url ->
            progressBar.visibility = View.VISIBLE
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("PhotoFragment", "이미지 로드 실패", e)
                        progressBar.post { progressBar.visibility = View.GONE }
                        return false // allow Glide to handle error placeholder if any
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.post { progressBar.visibility = View.GONE }
                        return false
                    }
                })
                .into(ivPhoto)
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

    // Glide 사용으로 더 이상 수동 네트워크 처리 함수가 필요하지 않습니다.
}

