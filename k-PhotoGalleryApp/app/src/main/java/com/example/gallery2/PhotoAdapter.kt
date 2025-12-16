package com.example.gallery2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

        // 加载图片缩略图
        Glide.with(context)
            .load(File(photo.path))
            .centerCrop()
            .into(holder.imageViewPhoto)

        // 隐藏视频相关元素
        holder.imageViewPlayIcon?.visibility = View.GONE
        holder.textViewDuration?.visibility = View.GONE

        // 隐藏日期标签（现在完全基于真实创建时间归类，不需要显示）
        holder.textViewAdded.visibility = View.GONE

        holder.itemView.setOnClickListener {
            listener.onPhotoClick(position)
        }
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPhoto: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        val imageViewPlayIcon: ImageView? = itemView.findViewById(R.id.imageViewPlayIcon)
        val textViewAdded: TextView = itemView.findViewById(R.id.textViewAdded)
        val textViewDuration: TextView? = itemView.findViewById(R.id.textViewDuration)
    }
}
