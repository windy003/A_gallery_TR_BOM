package com.example.gallery2

import android.net.Uri
import java.io.Serializable

data class Photo(
    val id: Long,
    val path: String,
    val name: String,
    val dateAdded: Long,
    var lastModified: Long = 0, // 文件最后修改时间(毫秒)
    val size: Long,
    val mediaType: Int, // 1=图片
    @Transient var uri: Uri? = null // transient 因为 Uri 不能直接序列化
) : Serializable {
    companion object {
        const val TYPE_IMAGE = 1
    }
}
