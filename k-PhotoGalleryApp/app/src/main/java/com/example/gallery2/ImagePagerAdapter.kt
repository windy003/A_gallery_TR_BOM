package com.example.gallery2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File

class ImagePagerAdapter(
    private val context: Context,
    private val photos: MutableList<Photo>
) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    private var onImageClickListener: OnImageClickListener? = null

    fun interface OnImageClickListener {
        fun onImageClick()
    }

    fun setOnImageClickListener(listener: OnImageClickListener) {
        this.onImageClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = TouchImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ImageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val photo = photos[position]
        android.util.Log.d("ImagePagerAdapter", "绑定图片 position=$position, path=${photo.path}, name=${photo.name}")

        holder.imageView.setOnClickListener {
            onImageClickListener?.onImageClick()
        }

        // 获取屏幕尺寸
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        // 限制最大尺寸：考虑5倍缩放，使用屏幕尺寸的5倍，但不超过8192（Canvas最大限制）
        // 对于宽度和高度分别设置限制，避免正方形限制导致的问题
        val maxWidth = minOf(8192, screenWidth * 5)
        val maxHeight = minOf(8192, screenHeight * 5)

        val photoFile = File(photo.path)
        android.util.Log.d("ImagePagerAdapter", "文件是否存在: ${photoFile.exists()}, 可读: ${photoFile.canRead()}")

        Glide.with(context)
            .asBitmap()
            .load(photoFile)
            .override(maxWidth, maxHeight)
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .format(DecodeFormat.PREFER_RGB_565)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    android.util.Log.d("ImagePagerAdapter", "图片加载成功 position=$position, size=${resource.width}x${resource.height}")
                    // 先设置尺寸
                    holder.imageView.setImageBitmap(resource.width, resource.height)
                    // 再设置图片，这会触发重新测量
                    holder.imageView.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    android.util.Log.d("ImagePagerAdapter", "清除图片 position=$position")
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    android.util.Log.e("ImagePagerAdapter", "图片加载失败 position=$position, path=${photo.path}")
                }
            })
    }

    override fun getItemCount(): Int = photos.size

    fun getPhotos(): List<Photo> = photos

    fun removePhoto(position: Int) {
        if (position in 0 until photos.size) {
            photos.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, photos.size)
        }
    }

    class ImageViewHolder(val imageView: TouchImageView) : RecyclerView.ViewHolder(imageView)
}
