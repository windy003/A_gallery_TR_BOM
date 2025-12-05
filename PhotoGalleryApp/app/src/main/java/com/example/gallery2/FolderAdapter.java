package com.example.gallery2;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {
    private Context context;
    private List<Folder> folders;
    private OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
    }

    public FolderAdapter(Context context, List<Folder> folders, OnFolderClickListener listener) {
        this.context = context;
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder folder = folders.get(position);

        // 为文件夹名称设置颜色
        String displayName = folder.getDisplayName();
        SpannableString spannableString = new SpannableString(displayName);

        // 匹配日期范围中的目标日期（-后面的数字）
        // 格式：yyyy/MM/dd-dd 或 yyyy/MM/dd-yyyy/MM/dd
        Pattern targetDayPattern = Pattern.compile("-(\\d+)(?:\\s|\\n)");
        Matcher targetDayMatcher = targetDayPattern.matcher(displayName);
        if (targetDayMatcher.find()) {
            int start = targetDayMatcher.start(1);
            int end = targetDayMatcher.end(1);
            spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 匹配时间部分（HH:00-HH:00）
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2})");
        Matcher timeMatcher = timePattern.matcher(displayName);
        if (timeMatcher.find()) {
            int start = timeMatcher.start(1);
            int end = timeMatcher.end(1);
            spannableString.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.textViewFolderName.setText(spannableString);
        holder.textViewPhotoCount.setText(folder.getPhotoCount() + " 张照片");

        if (folder.getCoverPhotoPath() != null) {
            Glide.with(context)
                    .load(new File(folder.getCoverPhotoPath()))
                    .centerCrop()
                    .into(holder.imageViewCover);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick(folder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    static class FolderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewCover;
        TextView textViewFolderName;
        TextView textViewPhotoCount;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewCover = itemView.findViewById(R.id.imageViewCover);
            textViewFolderName = itemView.findViewById(R.id.textViewFolderName);
            textViewPhotoCount = itemView.findViewById(R.id.textViewPhotoCount);
        }
    }
}
