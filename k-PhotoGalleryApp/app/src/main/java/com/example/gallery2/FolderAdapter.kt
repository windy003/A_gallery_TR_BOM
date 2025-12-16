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

class FolderAdapter(
    private val context: Context,
    private val folders: List<Folder>,
    private val listener: OnFolderClickListener
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    fun interface OnFolderClickListener {
        fun onFolderClick(folder: Folder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]

        holder.textViewFolderName.text = folder.displayName
        holder.textViewPhotoCount.text = "${folder.getPhotoCount()} 张照片"

        folder.getCoverPhotoPath()?.let {
            Glide.with(context)
                .load(File(it))
                .centerCrop()
                .into(holder.imageViewCover)
        }

        holder.itemView.setOnClickListener {
            listener.onFolderClick(folder)
        }
    }

    override fun getItemCount(): Int = folders.size

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewCover: ImageView = itemView.findViewById(R.id.imageViewCover)
        val textViewFolderName: TextView = itemView.findViewById(R.id.textViewFolderName)
        val textViewPhotoCount: TextView = itemView.findViewById(R.id.textViewPhotoCount)
    }
}
