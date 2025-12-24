package com.example.gallery2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class PhotoAdapter(
    private val context: Context,
    private val photos: List<Photo>,
    private val listener: OnPhotoClickListener
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    fun interface OnPhotoClickListener {
        fun onPhotoClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // 强制设置item属性，确保点击响应正常
        holder.itemView.isClickable = true
        holder.itemView.isFocusable = false
        holder.itemView.isFocusableInTouchMode = false

        // 加载图片缩略图
        Glide.with(context)
            .load(File(photo.path))
            .centerCrop()
            .into(holder.imageViewPhoto)

        holder.itemView.setOnClickListener {
            android.util.Log.d("PhotoAdapter", "点击图片 position=$position, name=${photo.name}")
            listener.onPhotoClick(position)
        }
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPhoto: ImageView = itemView.findViewById(R.id.imageViewPhoto)
    }
}
