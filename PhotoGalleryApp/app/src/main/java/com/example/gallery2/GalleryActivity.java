package com.example.gallery2;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView recyclerViewPhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> photos;
    private String folderName;
    private String folderDisplayName;
    private boolean isDateFolder;
    private ActivityResultLauncher<Intent> imageViewerLauncher;
    private ActivityResultLauncher<Intent> videoPlayerLauncher;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

    // 选择模式相关
    private LinearLayout layoutToolbar;
    private Button buttonSelectAll;
    private Button buttonDelete;
    private Button buttonCancel;
    private Button buttonEnterSelectMode;
    private List<Photo> photosToDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // 注册ImageViewerActivity的结果监听器
        imageViewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // ImageViewerActivity返回OK，说明有变化，传递给MainActivity
                        setResult(RESULT_OK);
                        loadPhotos(); // 刷新当前列表
                    }
                }
        );

        // 注册VideoPlayerActivity的结果监听器
        videoPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        setResult(RESULT_OK);
                        loadPhotos(); // 刷新当前列表
                    }
                }
        );

        // 注册删除请求启动器
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "删除完成", Toast.LENGTH_SHORT).show();
                        exitSelectMode();
                        setResult(RESULT_OK);
                        loadPhotos();
                    } else {
                        Toast.makeText(this, "删除已取消", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        folderName = getIntent().getStringExtra("folder_name");
        folderDisplayName = getIntent().getStringExtra("folder_display_name");
        isDateFolder = getIntent().getBooleanExtra("is_date_folder", false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderDisplayName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        // 初始化工具栏
        layoutToolbar = findViewById(R.id.layoutToolbar);
        buttonSelectAll = findViewById(R.id.buttonSelectAll);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonCancel = findViewById(R.id.buttonCancel);
        buttonEnterSelectMode = findViewById(R.id.buttonEnterSelectMode);

        setupToolbarButtons();

        loadPhotos();
    }

    private void setupToolbarButtons() {
        buttonEnterSelectMode.setOnClickListener(v -> enterSelectMode());

        buttonSelectAll.setOnClickListener(v -> {
            if (photoAdapter != null) {
                if (photoAdapter.getSelectedCount() == photos.size()) {
                    photoAdapter.clearSelection();
                    buttonSelectAll.setText("全选");
                } else {
                    photoAdapter.selectAll();
                    buttonSelectAll.setText("取消全选");
                }
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (photoAdapter != null && photoAdapter.getSelectedCount() > 0) {
                showDeleteConfirmDialog();
            }
        });

        buttonCancel.setOnClickListener(v -> exitSelectMode());
    }

    private void enterSelectMode() {
        if (photoAdapter != null) {
            photoAdapter.setSelectMode(true);
            layoutToolbar.setVisibility(View.VISIBLE);
            buttonEnterSelectMode.setVisibility(View.GONE);
            buttonDelete.setEnabled(false);
            buttonSelectAll.setText("全选");
        }
    }

    private void exitSelectMode() {
        if (photoAdapter != null) {
            photoAdapter.setSelectMode(false);
            layoutToolbar.setVisibility(View.GONE);
            if (isDateFolder) {
                buttonEnterSelectMode.setVisibility(View.VISIBLE);
            }
            buttonDelete.setEnabled(false);
            buttonSelectAll.setText("全选");
        }
    }

    private void showDeleteConfirmDialog() {
        int count = photoAdapter.getSelectedCount();
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要永久删除选中的 " + count + " 个文件吗？此操作不可撤销。")
            .setPositiveButton("删除", (dialog, which) -> {
                deleteSelectedPhotos();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteSelectedPhotos() {
        photosToDelete = photoAdapter.getSelectedPhotos();
        if (photosToDelete.isEmpty()) {
            return;
        }

        List<Uri> urisToDelete = new ArrayList<>();
        for (Photo photo : photosToDelete) {
            Uri uri;
            if (photo.isVideo()) {
                uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        photo.getId()
                );
            } else {
                uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        photo.getId()
                );
            }
            urisToDelete.add(uri);
        }

        ContentResolver resolver = getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用批量删除请求
            try {
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(
                        resolver,
                        urisToDelete
                );
                deleteRequestLauncher.launch(
                        new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build()
                );
            } catch (Exception e) {
                Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Android 10 及以下，逐个删除
            int deletedCount = 0;
            for (Uri uri : urisToDelete) {
                try {
                    int deleted = resolver.delete(uri, null, null);
                    if (deleted > 0) {
                        deletedCount++;
                    }
                } catch (Exception e) {
                    // 忽略单个删除错误
                }
            }
            Toast.makeText(this, "已删除 " + deletedCount + " 个文件", Toast.LENGTH_SHORT).show();
            exitSelectMode();
            setResult(RESULT_OK);
            loadPhotos();
        }
    }

    private void loadPhotos() {
        photos = new ArrayList<>();
        PhotoManager photoManager = new PhotoManager(this);

        if (isDateFolder) {
            // 加载日期文件夹中的照片（基于DATE_ADDED+3天）
            photos = photoManager.getPhotosForDate(folderName);
        } else {
            // 加载所有照片
            photos = photoManager.getAllPhotos();
        }

        photoAdapter = new PhotoAdapter(this, photos, position -> {
            Photo clickedPhoto = photos.get(position);

            if (clickedPhoto.isVideo()) {
                // 打开视频播放器
                // 只传递视频列表
                ArrayList<Photo> videos = new ArrayList<>();
                int videoPosition = 0;
                for (int i = 0; i < photos.size(); i++) {
                    if (photos.get(i).isVideo()) {
                        if (i == position) {
                            videoPosition = videos.size();
                        }
                        videos.add(photos.get(i));
                    }
                }

                Intent intent = new Intent(GalleryActivity.this, VideoPlayerActivity.class);
                intent.putExtra("videos", videos);
                intent.putExtra("position", videoPosition);
                intent.putExtra("folderName", folderName);
                videoPlayerLauncher.launch(intent);
            } else {
                // 打开图片查看器
                // 只传递图片列表
                ArrayList<Photo> images = new ArrayList<>();
                int imagePosition = 0;
                for (int i = 0; i < photos.size(); i++) {
                    if (!photos.get(i).isVideo()) {
                        if (i == position) {
                            imagePosition = images.size();
                        }
                        images.add(photos.get(i));
                    }
                }

                Intent intent = new Intent(GalleryActivity.this, ImageViewerActivity.class);
                intent.putExtra("photos", images);
                intent.putExtra("position", imagePosition);
                intent.putExtra("folder_name", folderName);
                intent.putExtra("is_date_folder", isDateFolder);
                imageViewerLauncher.launch(intent);
            }
        });

        // 设置长按监听器（进入选择模式）
        photoAdapter.setOnPhotoLongClickListener(position -> {
            if (!photoAdapter.isSelectMode() && isDateFolder) {
                enterSelectMode();
                photoAdapter.toggleSelection(position);
            }
        });

        // 设置选择变化监听器
        photoAdapter.setOnSelectionChangedListener(selectedCount -> {
            buttonDelete.setEnabled(selectedCount > 0);
            buttonDelete.setText("删除(" + selectedCount + ")");
            if (selectedCount == photos.size() && photos.size() > 0) {
                buttonSelectAll.setText("取消全选");
            } else {
                buttonSelectAll.setText("全选");
            }
        });

        recyclerViewPhotos.setAdapter(photoAdapter);

        // 只在日期文件夹中显示选择按钮
        if (isDateFolder && !photos.isEmpty()) {
            buttonEnterSelectMode.setVisibility(View.VISIBLE);
        } else {
            buttonEnterSelectMode.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (photoAdapter != null && photoAdapter.isSelectMode()) {
            exitSelectMode();
            return true;
        }
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (photoAdapter != null && photoAdapter.isSelectMode()) {
            exitSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }
}
