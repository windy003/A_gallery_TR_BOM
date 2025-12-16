package com.example.gallery2

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {
    private lateinit var recyclerViewPhotos: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photos: MutableList<Photo>
    private lateinit var folderName: String
    private lateinit var folderDisplayName: String

    private val imageViewerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // ImageViewerActivity返回OK，说明有变化，传递给MainActivity
            setResult(RESULT_OK)
            loadPhotos() // 刷新当前列表
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        folderName = intent.getStringExtra("folder_name") ?: ""
        folderDisplayName = intent.getStringExtra("folder_display_name") ?: ""

        supportActionBar?.apply {
            title = folderDisplayName
            setDisplayHomeAsUpEnabled(true)
        }

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos)
        recyclerViewPhotos.layoutManager = GridLayoutManager(this, 3)

        loadPhotos()
    }

    private fun loadPhotos() {
        photos = mutableListOf()
        val photoManager = PhotoManager(this)

        if (folderName == "expired") {
            // 加载已到期的照片
            val allPhotos = photoManager.getAllPhotos()
            for (photo in allPhotos) {
                val singlePhotoList = listOf(photo)
                if (photoManager.isFolderExpired(singlePhotoList)) {
                    photos.add(photo)
                }
            }
        } else {
            // 加载所有照片
            photos = photoManager.getAllPhotos().toMutableList()
        }

        photoAdapter = PhotoAdapter(this, photos) { position ->
            // 打开图片查看器
            val intent = Intent(this@GalleryActivity, ImageViewerActivity::class.java)
            intent.putExtra("photos", ArrayList(photos))
            intent.putExtra("position", position)
            intent.putExtra("folder_name", folderName)
            imageViewerLauncher.launch(intent)
        }

        recyclerViewPhotos.adapter = photoAdapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }
}
