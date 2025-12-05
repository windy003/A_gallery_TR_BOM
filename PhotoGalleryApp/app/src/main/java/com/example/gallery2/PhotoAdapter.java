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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private Context context;
    private List<Photo> photos;
    private OnPhotoClickListener listener;
    private OnPhotoLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;
    private boolean isSelectMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();

    public interface OnPhotoClickListener {
        void onPhotoClick(int position);
    }

    public interface OnPhotoLongClickListener {
        void onPhotoLongClick(int position);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public PhotoAdapter(Context context, List<Photo> photos, OnPhotoClickListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
    }

    public void setOnPhotoLongClickListener(OnPhotoLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setSelectMode(boolean selectMode) {
        this.isSelectMode = selectMode;
        if (!selectMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        return isSelectMode;
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < photos.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedPositions.size());
        }
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
    }

    public List<Photo> getSelectedPhotos() {
        List<Photo> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < photos.size()) {
                selected.add(photos.get(position));
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedPositions.size();
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

        // 处理选择模式
        if (isSelectMode) {
            holder.imageViewCheckMark.setVisibility(
                selectedPositions.contains(position) ? View.VISIBLE : View.GONE
            );
        } else {
            holder.imageViewCheckMark.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelectMode) {
                toggleSelection(position);
            } else if (listener != null) {
                listener.onPhotoClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onPhotoLongClick(position);
                return true;
            }
            return false;
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
        ImageView imageViewCheckMark;
        TextView textViewAdded;
        TextView textViewDuration;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
            imageViewPlayIcon = itemView.findViewById(R.id.imageViewPlayIcon);
            imageViewCheckMark = itemView.findViewById(R.id.imageViewCheckMark);
            textViewAdded = itemView.findViewById(R.id.textViewAdded);
            textViewDuration = itemView.findViewById(R.id.textViewDuration);
        }
    }
}
