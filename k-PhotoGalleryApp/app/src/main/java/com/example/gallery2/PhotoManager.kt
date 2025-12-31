package com.example.gallery2

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.util.*

class PhotoManager(private val context: Context) {
    companion object {
        private const val DELAY_DAYS = 3 // 延迟天数
    }

    /**
     * 从MediaStore获取所有图片
     */
    private fun getImages(): List<Photo> {
        val photos = mutableListOf<Photo>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val path = it.getString(pathColumn)
                val name = it.getString(nameColumn)
                val dateAdded = it.getLong(dateColumn)
                val size = it.getLong(sizeColumn)

                // 只扫描指定的两个目录
                if (isInTargetDirectory(path)) {
                    // 读取文件的最后修改时间
                    val lastModified = try {
                        val file = File(path)
                        if (file.exists()) {
                            file.lastModified()
                        } else {
                            dateAdded * 1000
                        }
                    } catch (e: Exception) {
                        // 如果读取失败，使用 dateAdded 作为备用
                        dateAdded * 1000
                    }

                    val photo = Photo(id, path, name, dateAdded, lastModified, size, Photo.TYPE_IMAGE)
                    photo.uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    photos.add(photo)
                }
            }
        }

        return photos
    }

    /**
     * 获取所有媒体文件（仅图片）
     */
    fun getAllPhotos(): List<Photo> {
        val allMedia = mutableListOf<Photo>()
        allMedia.addAll(getImages())

        // 按日期排序（最新的在前）
        allMedia.sortByDescending { it.dateAdded }

        return allMedia
    }

    /**
     * 判断文件路径是否在目标目录中
     * 只扫描 /DCIM/Screenshots、/Pictures/Screenshots、/Pictures/ImageStitcher 和 /Download(s)/ 四个目录
     * 注意：Download(s) 目录只扫描直接子文件，不包含子目录
     *
     * @param path 文件路径
     * @return 是否在目标目录中
     */
    private fun isInTargetDirectory(path: String?): Boolean {
        if (path == null) {
            return false
        }

        // 转换为小写以进行不区分大小写的比较
        val lowerPath = path.lowercase()

        // 检查是否在 /DCIM/Screenshots、/Pictures/Screenshots 或 /Pictures/ImageStitcher 目录中
        if (lowerPath.contains("/dcim/screenshots") ||
                lowerPath.contains("/pictures/screenshots") ||
                lowerPath.contains("/pictures/imagestitcher")) {
            return true
        }

        // 检查 Download 或 Downloads 目录（不包含子目录）
        val downloadIndex = when {
            lowerPath.contains("/download/") -> lowerPath.indexOf("/download/")
            lowerPath.contains("/downloads/") -> lowerPath.indexOf("/downloads/")
            else -> -1
        }

        if (downloadIndex != -1) {
            // 获取 download/downloads 之后的部分
            val afterDownload = if (lowerPath.contains("/download/")) {
                lowerPath.substring(downloadIndex + "/download/".length)
            } else {
                lowerPath.substring(downloadIndex + "/downloads/".length)
            }

            // 如果之后的部分不包含斜杠，说明是直接在 Download 目录下的文件
            return !afterDownload.contains("/")
        }

        return false
    }

    /**
     * 判断单张照片是否已到期
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @param photo 要判断的照片
     * @return 照片是否已到期
     */
    fun isPhotoExpired(photo: Photo): Boolean {
        // 获取当前时间，并减去3天
        val expirationTime = Calendar.getInstance()
        expirationTime.add(Calendar.DAY_OF_YEAR, -DELAY_DAYS)

        // 将分钟、秒、毫秒设为0，按小时计算
        expirationTime.set(Calendar.MINUTE, 0)
        expirationTime.set(Calendar.SECOND, 0)
        expirationTime.set(Calendar.MILLISECOND, 0)

        // 使用文件最后修改时间（即创建时间）
        var lastModified = photo.lastModified
        if (lastModified == 0L) {
            // 如果 lastModified 为 0，使用 dateAdded 作为备用
            lastModified = photo.dateAdded * 1000
        }

        val photoTime = Calendar.getInstance()
        photoTime.timeInMillis = lastModified

        // 如果照片时间 < 过期时间，说明已到期
        return photoTime.before(expirationTime)
    }

    /**
     * 检测是否有已到期的照片
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @return 是否有过期的照片
     */
    fun hasExpiredFolders(): Boolean {
        val allPhotos = getAllPhotos()
        return allPhotos.any { isPhotoExpired(it) }
    }

    /**
     * 获取需要阅读消化的图片数量
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @return 需要阅读消化的图片总数
     */
    fun getExpiredPhotoCount(): Int {
        val allPhotos = getAllPhotos()
        return allPhotos.count { isPhotoExpired(it) }
    }

}
