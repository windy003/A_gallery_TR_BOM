package com.example.gallery2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var recyclerViewFolders: RecyclerView
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var folders: MutableList<Folder>
    private lateinit var photoManager: PhotoManager
    private lateinit var iconManager: IconManager

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // GalleryActivity返回OK，说明有变化，刷新文件夹列表
            loadFolders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewFolders = findViewById(R.id.recyclerViewFolders)
        recyclerViewFolders.layoutManager = LinearLayoutManager(this)

        photoManager = PhotoManager(this)
        iconManager = IconManager(this)

        if (checkPermissions()) {
            loadFolders()
            // 启动后台图片检查服务（只在有权限时启动）
            startPhotoCheckService()
        } else {
            requestPermissions()
        }
    }

    /**
     * 启动后台图片检查服务
     */
    private fun startPhotoCheckService() {
        val serviceIntent = Intent(this, PhotoCheckService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0及以上使用startForegroundService
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImagePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            hasImagePermission && hasNotificationPermission
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFolders()
                // 权限授予后启动后台服务
                startPhotoCheckService()
            } else {
                Toast.makeText(this, "需要存储权限才能查看照片", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadFolders() {
        folders = mutableListOf()

        // 获取所有图片
        val allPhotos = photoManager.getAllPhotos()

        // 添加"所有图片"文件夹
        val allPhotosFolder = Folder("all_photos", "所有图片")
        for (photo in allPhotos) {
            allPhotosFolder.addPhoto(photo)
        }

        // 创建"已到期"文件夹，只包含已到期的图片
        val expiredFolder = Folder("expired", "已到期")

        // 计算过期时间线（用于调试输出）
        val expirationTime = Calendar.getInstance()
        expirationTime.add(Calendar.DAY_OF_YEAR, -3)
        expirationTime.set(Calendar.MINUTE, 0)
        expirationTime.set(Calendar.SECOND, 0)
        expirationTime.set(Calendar.MILLISECOND, 0)

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        Log.d("MainActivity", "过期时间线: ${sdf.format(expirationTime.time)}")

        // 遍历所有图片，筛选出已到期的图片
        for (photo in allPhotos) {
            val singlePhotoList = listOf(photo)

            // 检查这张照片是否已到期
            if (photoManager.isFolderExpired(singlePhotoList)) {
                // 输出调试信息
                var lastModified = photo.lastModified
                if (lastModified == 0L) {
                    lastModified = photo.dateAdded * 1000
                }
                val photoTime = Calendar.getInstance()
                photoTime.timeInMillis = lastModified
                Log.d(
                    "MainActivity", "已到期图片: ${photo.name}, " +
                            "创建时间: ${sdf.format(photoTime.time)} " +
                            "(添加时间: ${sdf.format(Date(photo.dateAdded * 1000))})"
                )

                expiredFolder.addPhoto(photo)
            }
        }

        // 只添加两个文件夹：所有图片和已到期
        folders.add(allPhotosFolder)
        if (expiredFolder.getPhotoCount() > 0) {
            folders.add(expiredFolder)
        }

        folderAdapter = FolderAdapter(this, folders) { folder ->
            val intent = Intent(this@MainActivity, GalleryActivity::class.java)
            intent.putExtra("folder_name", folder.name)
            intent.putExtra("folder_display_name", folder.displayName)
            galleryLauncher.launch(intent)
        }

        recyclerViewFolders.adapter = folderAdapter

        // 更新应用图标（根据日期文件夹状态）
        iconManager.updateAppIcon()
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            loadFolders()
        }
    }
}
