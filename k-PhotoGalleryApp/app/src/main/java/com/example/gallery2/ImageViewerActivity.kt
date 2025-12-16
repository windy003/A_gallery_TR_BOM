package com.example.gallery2

import android.app.PendingIntent
import android.content.ContentUris
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageViewerActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME = "FloatingButtonPrefs"
        private const val KEY_FLOATING_X = "floating_x"
        private const val KEY_FLOATING_Y = "floating_y"
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var layoutControls: LinearLayout
    private lateinit var layoutFloatingButtons: LinearLayout
    private lateinit var layoutTopButtons: LinearLayout
    private lateinit var dragHandle: View
    private lateinit var buttonAddToThreeDaysLater: Button
    private lateinit var buttonDelete: Button
    private lateinit var buttonUndo: Button
    private lateinit var buttonShowDetails: Button
    private lateinit var textViewPageInfo: TextView
    private lateinit var photos: MutableList<Photo>
    private var currentPosition: Int = 0
    private var controlsVisible = true
    private lateinit var adapter: ImagePagerAdapter
    private lateinit var folderName: String
    private lateinit var fileOperationHelper: FileOperationHelper
    private var photoToDelay: Photo? = null
    private var positionToDelay: Int = 0
    private var newPhotoIdForDelay: Long = 0

    private var dX = 0f
    private var dY = 0f

    private val pendingDeleteManager = PendingDeleteManager()

    private lateinit var prefs: SharedPreferences

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 用户确认删除，清空队列并退出
            pendingDeleteManager.clear()
            setResult(RESULT_OK)
            finish()
        } else {
            // 用户取消删除，恢复所有照片到列表
            Toast.makeText(this, "删除已取消，文件已恢复", Toast.LENGTH_SHORT).show()
            restoreAllPendingDeletes()
        }
    }

    private val delayDeleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        // 暂不使用
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        viewPager = findViewById(R.id.viewPager)
        layoutControls = findViewById(R.id.layoutControls)
        layoutFloatingButtons = findViewById(R.id.layoutFloatingButtons)
        layoutTopButtons = findViewById(R.id.layoutTopButtons)
        dragHandle = findViewById(R.id.dragHandle)
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater)
        buttonDelete = findViewById(R.id.buttonDelete)
        buttonUndo = findViewById(R.id.buttonUndo)
        buttonShowDetails = findViewById(R.id.buttonShowDetails)
        textViewPageInfo = findViewById(R.id.textViewPageInfo)

        photos = (intent.getSerializableExtra("photos") as? ArrayList<Photo>)?.toMutableList()
            ?: mutableListOf()
        currentPosition = intent.getIntExtra("position", 0)
        folderName = intent.getStringExtra("folder_name") ?: ""

        fileOperationHelper = FileOperationHelper(this)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupViewPager()
        setupControls()
        setupFloatingButtonsDrag()
        restoreFloatingButtonPosition()
        updateUndoButton()
    }

    private fun setupViewPager() {
        adapter = ImagePagerAdapter(this, photos)
        adapter.setOnImageClickListener { toggleControls() }
        viewPager.adapter = adapter
        viewPager.setCurrentItem(currentPosition, false)
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updatePageInfo()
            }
        })

        updatePageInfo()
    }

    private fun setupControls() {
        buttonAddToThreeDaysLater.setOnClickListener { addToThreeDaysLater() }
        buttonDelete.setOnClickListener { deleteCurrentPhoto() }
        buttonUndo.setOnClickListener { performUndo() }
        buttonShowDetails.setOnClickListener { showPhotoDetails() }
    }

    private fun setupFloatingButtonsDrag() {
        dragHandle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = layoutFloatingButtons.x - event.rawX
                    dY = layoutFloatingButtons.y - event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY

                    // 限制在屏幕范围内
                    val parent = layoutFloatingButtons.parent as View
                    val maxX = parent.width - layoutFloatingButtons.width
                    val maxY = parent.height - layoutFloatingButtons.height

                    newX = newX.coerceIn(0f, maxX.toFloat())
                    newY = newY.coerceIn(0f, maxY.toFloat())

                    layoutFloatingButtons.x = newX
                    layoutFloatingButtons.y = newY
                    true
                }

                MotionEvent.ACTION_UP -> {
                    // 保存当前位置
                    saveFloatingButtonPosition()
                    true
                }

                else -> false
            }
        }
    }

    private fun addToThreeDaysLater() {
        if (currentPosition >= photos.size) {
            return
        }

        val currentPhoto = photos[currentPosition]
        photoToDelay = currentPhoto
        positionToDelay = currentPosition

        Toast.makeText(this, "正在复制文件...", Toast.LENGTH_SHORT).show()

        // 第一步：复制文件（新文件会有新的DATE_ADDED）
        Thread {
            val newPhotoId = fileOperationHelper.copyImageFile(currentPhoto)

            runOnUiThread {
                if (newPhotoId == -1L) {
                    Toast.makeText(this, "复制文件失败", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                newPhotoIdForDelay = newPhotoId

                // 第二步：软删除原文件（从列表移除，退出时才真正删除）
                pendingDeleteManager.addPendingDelete(
                    PendingDeleteManager.PendingDelete(
                        photoToDelay!!, positionToDelay,
                        PendingDeleteManager.PendingDelete.TYPE_DELAY, newPhotoIdForDelay
                    )
                )
                performDelayCleanup()
            }
        }.start()
    }

    private fun performDelayCleanup() {
        // 从适配器中移除当前照片
        adapter.removePhoto(currentPosition)

        // 如果列表为空，关闭Activity
        if (photos.isEmpty()) {
            Toast.makeText(this, "延迟操作完成", Toast.LENGTH_SHORT).show()
            finishWithPendingDeletes()
            return
        }

        // 调整当前位置
        if (currentPosition >= photos.size) {
            currentPosition = photos.size - 1
        }

        // 更新页面信息
        updatePageInfo()
        updateUndoButton()

        // 设置result，通知上级Activity刷新
        setResult(RESULT_OK)

        Toast.makeText(this, "已移到3天后（可撤销）", Toast.LENGTH_SHORT).show()
    }

    private fun updatePageInfo() {
        textViewPageInfo.text = "${currentPosition + 1} / ${photos.size}"
    }

    private fun toggleControls() {
        if (controlsVisible) {
            layoutControls.visibility = View.GONE
            layoutFloatingButtons.visibility = View.GONE
            layoutTopButtons.visibility = View.GONE
        } else {
            layoutControls.visibility = View.VISIBLE
            layoutFloatingButtons.visibility = View.VISIBLE
            layoutTopButtons.visibility = View.VISIBLE
        }
        controlsVisible = !controlsVisible
    }

    private fun deleteCurrentPhoto() {
        if (currentPosition < photos.size) {
            val photoToDelete = photos[currentPosition]
            val positionToDelete = currentPosition

            // 软删除：先从列表中移除，但不立即删除文件
            pendingDeleteManager.addPendingDelete(
                PendingDeleteManager.PendingDelete(
                    photoToDelete, positionToDelete,
                    PendingDeleteManager.PendingDelete.TYPE_DELETE
                )
            )
            performDeleteCleanup()
            Toast.makeText(this, "已删除（可撤销，退出后永久删除）", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDeleteCleanup() {
        if (currentPosition in 0 until photos.size) {
            adapter.removePhoto(currentPosition)
        }

        if (photos.isEmpty()) {
            finishWithPendingDeletes()
            return
        }

        if (currentPosition >= photos.size) {
            currentPosition = photos.size - 1
        }

        updatePageInfo()
        updateUndoButton()

        setResult(RESULT_OK)
    }

    private fun updateUndoButton() {
        if (pendingDeleteManager.canUndo()) {
            buttonUndo.isEnabled = true
            buttonUndo.text = "撤销(${pendingDeleteManager.getCount()})"
        } else {
            buttonUndo.isEnabled = false
            buttonUndo.text = "撤销"
        }
    }

    private fun performUndo() {
        val pendingDelete = pendingDeleteManager.undo()
        if (pendingDelete == null) {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show()
            return
        }

        val photo = pendingDelete.photo
        val originalPosition = pendingDelete.originalPosition

        if (pendingDelete.actionType == PendingDeleteManager.PendingDelete.TYPE_DELETE) {
            // 撤销删除操作：将照片恢复到列表中
            val position = minOf(originalPosition, photos.size)
            photos.add(position, photo)
            adapter.notifyItemInserted(position)

            currentPosition = position
            viewPager.setCurrentItem(currentPosition, false)

            updatePageInfo()
            Toast.makeText(this, "已撤销删除: ${photo.name}", Toast.LENGTH_SHORT).show()
        } else if (pendingDelete.actionType == PendingDeleteManager.PendingDelete.TYPE_DELAY) {
            // 撤销延迟操作：删除新创建的副本，恢复原照片到列表
            val newPhotoId = pendingDelete.newPhotoId
            if (newPhotoId != -1L) {
                val newPhotoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    newPhotoId
                )
                try {
                    contentResolver.delete(newPhotoUri, null, null)

                    val position = minOf(originalPosition, photos.size)
                    photos.add(position, photo)
                    adapter.notifyItemInserted(position)

                    currentPosition = position
                    viewPager.setCurrentItem(currentPosition, false)

                    updatePageInfo()
                    Toast.makeText(this, "已撤销延迟操作: ${photo.name}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "撤销失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        updateUndoButton()
        setResult(RESULT_OK)
    }

    private fun executePendingDeletes() {
        val pendingDeletes = pendingDeleteManager.getAllPendingDeletes()

        if (pendingDeletes.isEmpty()) {
            setResult(RESULT_OK)
            finish()
            return
        }

        // 收集所有需要删除的URI
        val urisToDelete = mutableListOf<Uri>()
        for (pending in pendingDeletes) {
            val photo = pending.photo
            val photoUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                photo.id
            )
            urisToDelete.add(photoUri)
        }

        // 使用批量删除请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(
                    contentResolver,
                    urisToDelete
                )
                deleteRequestLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                pendingDeleteManager.clear()
                setResult(RESULT_OK)
                finish()
            }
        } else {
            // Android 10 及以下，逐个删除
            var deletedCount = 0
            for (uri in urisToDelete) {
                try {
                    val deleted = contentResolver.delete(uri, null, null)
                    if (deleted > 0) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    // 忽略单个删除错误
                }
            }
            pendingDeleteManager.clear()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun restoreAllPendingDeletes() {
        val pendingDeletes = pendingDeleteManager.getAllPendingDeletes()

        // 按原始位置排序，从后往前恢复
        for (i in pendingDeletes.size - 1 downTo 0) {
            val pending = pendingDeletes[i]

            if (pending.actionType == PendingDeleteManager.PendingDelete.TYPE_DELAY) {
                // 延迟操作：需要删除新创建的副本
                val newPhotoId = pending.newPhotoId
                if (newPhotoId != -1L) {
                    val newPhotoUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        newPhotoId
                    )
                    try {
                        contentResolver.delete(newPhotoUri, null, null)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }

            // 恢复照片到列表
            val photo = pending.photo
            val position = minOf(pending.originalPosition, photos.size)
            photos.add(position, photo)
            adapter.notifyItemInserted(position)
        }

        pendingDeleteManager.clear()
        updatePageInfo()
        updateUndoButton()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishWithPendingDeletes()
    }

    private fun finishWithPendingDeletes() {
        if (pendingDeleteManager.canUndo()) {
            executePendingDeletes()
        } else {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun saveFloatingButtonPosition() {
        val x = layoutFloatingButtons.x
        val y = layoutFloatingButtons.y

        prefs.edit()
            .putFloat(KEY_FLOATING_X, x)
            .putFloat(KEY_FLOATING_Y, y)
            .apply()
    }

    private fun restoreFloatingButtonPosition() {
        layoutFloatingButtons.post {
            val savedX = prefs.getFloat(KEY_FLOATING_X, -1f)
            val savedY = prefs.getFloat(KEY_FLOATING_Y, -1f)

            if (savedX != -1f && savedY != -1f) {
                val parent = layoutFloatingButtons.parent as View
                val maxX = parent.width - layoutFloatingButtons.width
                val maxY = parent.height - layoutFloatingButtons.height

                val x = savedX.coerceIn(0f, maxX.toFloat())
                val y = savedY.coerceIn(0f, maxY.toFloat())

                layoutFloatingButtons.x = x
                layoutFloatingButtons.y = y
            }
        }
    }

    private fun showPhotoDetails() {
        if (currentPosition >= photos.size) {
            return
        }

        val currentPhoto = photos[currentPosition]

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        var createdTime = "未知"
        var lastModified = currentPhoto.lastModified
        if (lastModified > 0) {
            createdTime = dateFormat.format(Date(lastModified))
        } else {
            val file = File(currentPhoto.path)
            if (file.exists()) {
                createdTime = dateFormat.format(Date(file.lastModified()))
            }
        }

        val dateAdded = dateFormat.format(Date(currentPhoto.dateAdded * 1000))
        val path = currentPhoto.path

        val details = StringBuilder()
        details.append("文件名称：\n${currentPhoto.name}\n\n")
        details.append("创建时间：\n$createdTime\n\n")
        details.append("添加时间：\n$dateAdded\n\n")
        details.append("文件路径：\n$path")

        AlertDialog.Builder(this)
            .setTitle("图片详情")
            .setMessage(details.toString())
            .setPositiveButton("确定", null)
            .show()
    }
}
