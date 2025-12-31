package com.example.gallery2

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * 文件操作助手类
 * 处理图片文件的复制和删除操作
 */
class FileOperationHelper(private val context: Context) {
    companion object {
        private const val TAG = "FileOperationHelper"
    }

    /**
     * 复制图片文件到MediaStore（创建新的副本）
     * 新文件会有新的DATE_ADDED（当前时间）
     *
     * @param sourcePhoto 源图片对象
     * @return 新图片的ID，失败返回-1
     */
    fun copyImageFile(sourcePhoto: Photo): Long {
        return try {
            // 准备新文件的元数据
            val values = ContentValues()

            // 获取原文件名和路径信息
            val sourceFile = File(sourcePhoto.path)
            val fileName = sourceFile.name
            val displayName = fileName

            // 从原文件路径提取相对路径（相对于Pictures目录）
            val relativePath = extractRelativePath(sourcePhoto.path)

            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.Media.MIME_TYPE, getMimeType(fileName))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用相对路径
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                values.put(MediaStore.Images.Media.IS_PENDING, 1) // 标记为待处理
            }

            // DATE_ADDED会自动设置为当前时间（这是我们想要的！）

            val resolver = context.contentResolver
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            // 创建新的MediaStore条目
            val newImageUri = resolver.insert(collection, values)

            if (newImageUri == null) {
                Log.e(TAG, "Failed to create new MediaStore entry")
                return -1
            }

            // 复制文件内容
            resolver.openOutputStream(newImageUri).use { out ->
                FileInputStream(sourceFile).use { input ->
                    if (out == null) {
                        Log.e(TAG, "Failed to open output stream")
                        resolver.delete(newImageUri, null, null)
                        return -1
                    }

                    // 复制数据
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                    }

                    out.flush()
                }
            }

            // Android 10+ 需要更新IS_PENDING状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(newImageUri, values, null, null)
            }

            // 获取新图片的ID
            val newImageId = ContentUris.parseId(newImageUri)
            Log.d(TAG, "Successfully copied image. New ID: $newImageId")

            newImageId
        } catch (e: IOException) {
            Log.e(TAG, "Error copying image file", e)
            -1
        }
    }

    /**
     * 从完整路径中提取相对路径
     * 例如: /storage/emulated/0/Pictures/MyFolder/image.jpg -> Pictures/MyFolder/
     */
    private fun extractRelativePath(fullPath: String): String {
        // 尝试从路径中提取Pictures之后的部分
        return when {
            fullPath.contains("/Pictures/", ignoreCase = true) -> {
                val picturesIndex = fullPath.indexOf("/Pictures/", ignoreCase = true)
                val afterPictures = fullPath.substring(picturesIndex + 1)
                // 移除文件名，只保留目录路径
                val lastSlash = afterPictures.lastIndexOf('/')
                if (lastSlash > 0) {
                    afterPictures.substring(0, lastSlash + 1)
                } else {
                    "Pictures/"
                }
            }
            fullPath.contains("/DCIM/", ignoreCase = true) -> {
                val dcimIndex = fullPath.indexOf("/DCIM/", ignoreCase = true)
                val afterDCIM = fullPath.substring(dcimIndex + 1)
                val lastSlash = afterDCIM.lastIndexOf('/')
                if (lastSlash > 0) {
                    afterDCIM.substring(0, lastSlash + 1)
                } else {
                    "DCIM/Camera/"
                }
            }
            fullPath.contains("/Download/", ignoreCase = true) -> {
                "Download/"
            }
            fullPath.contains("/Downloads/", ignoreCase = true) -> {
                "Downloads/"
            }
            else -> "Pictures/"
        }
    }

    /**
     * 根据文件名获取MIME类型
     */
    private fun getMimeType(fileName: String): String {
        val extension = fileName.substring(fileName.lastIndexOf('.') + 1).lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            else -> "image/jpeg"
        }
    }
}
