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
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream
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
import android.widget.Toast

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
            val url = imageUrl
            if (url.isNullOrBlank()) {
                Toast.makeText(requireContext(), "이미지 URL이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Glide로 비트맵을 가져와서 MediaStore에 저장 (Android Q+ 권한 필요 없음)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val bitmap = Glide.with(requireContext())
                        .asBitmap()
                        .load(url)
                        .submit()
                        .get()

                    val filename = "fishing_${System.currentTimeMillis()}.jpg"
                    val savedUri = saveBitmapToMediaStore(filename, bitmap)

                    withContext(Dispatchers.Main) {
                        if (savedUri != null) {
                            Toast.makeText(requireContext(), "이미지 저장 완료", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PhotoFragment", "다운로드/저장 실패", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "이미지 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        Log.d("PhotoFragment", "전달받은 URL: $imageUrl") // 이 로그를 확인하세요!

        if (imageUrl.isNullOrBlank()) {
            Log.e("PhotoFragment", "URL이 비어있습니다. DB 확인 필요")
            return
        }
    }

    // MediaStore에 비트맵 저장 (이미지)
    private fun saveBitmapToMediaStore(filename: String, bitmap: android.graphics.Bitmap): Uri? {
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        var imageUri: Uri? = null
        var stream: OutputStream? = null
        try {
            imageUri = resolver.insert(collection, contentValues)
            if (imageUri == null) return null
            stream = resolver.openOutputStream(imageUri)
            if (stream == null) return null
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
        } catch (e: Exception) {
            Log.e("PhotoFragment", "saveBitmapToMediaStore error", e)
            if (imageUri != null) {
                try { resolver.delete(imageUri, null, null) } catch (_: Exception) {}
            }
            return null
        } finally {
            try { stream?.close() } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            try { resolver.update(imageUri!!, contentValues, null, null) } catch (_: Exception) {}
        }
        return imageUri
    }
}

