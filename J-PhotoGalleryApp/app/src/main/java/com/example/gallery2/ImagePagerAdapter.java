package com.example.gallery2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.File;
import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    private Context context;
    private List<Photo> photos;
    private OnImageClickListener onImageClickListener;

    public interface OnImageClickListener {
        void onImageClick();
    }

    public ImagePagerAdapter(Context context, List<Photo> photos) {
        this.context = context;
        this.photos = photos;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TouchImageView imageView = new TouchImageView(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Photo photo = photos.get(position);

        holder.imageView.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick();
            }
        });

        // 获取屏幕尺寸
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

        // 限制最大尺寸：考虑5倍缩放，使用屏幕尺寸的5倍，但不超过8192（Canvas最大限制）
        // 对于宽度和高度分别设置限制，避免正方形限制导致的问题
        int maxWidth = Math.min(8192, screenWidth * 5);
        int maxHeight = Math.min(8192, screenHeight * 5);

        Glide.with(context)
                .asBitmap()
                .load(new File(photo.getPath()))
                .override(maxWidth, maxHeight)
                .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.CENTER_INSIDE)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        holder.imageView.setImageBitmap(resource.getWidth(), resource.getHeight());
                        holder.imageView.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void removePhoto(int position) {
        if (position >= 0 && position < photos.size()) {
            photos.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, photos.size());
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        TouchImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (TouchImageView) itemView;
        }
    }
}
