package com.example.gallery2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private Context context;
    private List<Photo> photos;
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    public PhotoAdapter(Context context, List<Photo> photos, OnPhotoClickListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);

        // 加载缩略图
        if (photo.isVideo()) {
            // 视频：加载视频缩略图
            Glide.with(context)
                    .load(photo.getUri())
                    .centerCrop()
                    .into(holder.imageViewPhoto);

            // 显示播放图标
            holder.imageViewPlayIcon.setVisibility(View.VISIBLE);

            // 显示视频时长
            if (photo.getDuration() > 0) {
                holder.textViewDuration.setText(formatDuration(photo.getDuration()));
                holder.textViewDuration.setVisibility(View.VISIBLE);
            } else {
                holder.textViewDuration.setVisibility(View.GONE);
            }
        } else {
            // 图片
            Glide.with(context)
                    .load(new File(photo.getPath()))
                    .centerCrop()
                    .into(holder.imageViewPhoto);

            // 隐藏视频相关元素
            holder.imageViewPlayIcon.setVisibility(View.GONE);
            holder.textViewDuration.setVisibility(View.GONE);
        }

        // 隐藏日期标签（现在完全基于真实创建时间归类，不需要显示）
        holder.textViewAdded.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(position);
            }
        });
    }

    private String formatDuration(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPhoto;
        ImageView imageViewPlayIcon;
        TextView textViewAdded;
        TextView textViewDuration;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
            imageViewPlayIcon = itemView.findViewById(R.id.imageViewPlayIcon);
            textViewAdded = itemView.findViewById(R.id.textViewAdded);
            textViewDuration = itemView.findViewById(R.id.textViewDuration);
        }
    }
}
