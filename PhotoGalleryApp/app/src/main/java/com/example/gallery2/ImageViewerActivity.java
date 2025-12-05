package com.example.gallery2;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ImageViewerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutControls;
    private LinearLayout layoutFloatingButtons;
    private LinearLayout layoutTopButtons;
    private View dragHandle;
    private Button buttonAddToThreeDaysLater;
    private Button buttonDelete;
    private Button buttonUndo;
    private Button buttonShowDetails;
    private TextView textViewPageInfo;
    private List<Photo> photos;
    private int currentPosition;
    private boolean controlsVisible = true;
    private ImagePagerAdapter adapter;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private ActivityResultLauncher<IntentSenderRequest> delayDeleteRequestLauncher;
    private String folderName;
    private boolean isDateFolder;
    private FileOperationHelper fileOperationHelper;
    private Photo photoToDelay; // 临时存储待延迟的图片
    private int positionToDelay; // 临时存储待延迟的位置
    private long newPhotoIdForDelay; // 临时存储延迟操作中新创建的照片ID

    // 悬浮按钮拖动相关变量
    private float dX, dY;

    // 撤销管理器
    private PendingDeleteManager pendingDeleteManager = new PendingDeleteManager();

    // SharedPreferences 相关
    private static final String PREFS_NAME = "FloatingButtonPrefs";
    private static final String KEY_FLOATING_X = "floating_x";
    private static final String KEY_FLOATING_Y = "floating_y";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // 初始化删除请求启动器（用于退出时批量删除）
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 用户确认删除，清空队列并退出
                        pendingDeleteManager.clear();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        // 用户取消删除，恢复所有照片到列表
                        Toast.makeText(this, "删除已取消，文件已恢复", Toast.LENGTH_SHORT).show();
                        restoreAllPendingDeletes();
                    }
                }
        );

        // 初始化延迟操作中的删除请求启动器（暂不使用）
        delayDeleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    // 暂不使用
                }
        );

        viewPager = findViewById(R.id.viewPager);
        layoutControls = findViewById(R.id.layoutControls);
        layoutFloatingButtons = findViewById(R.id.layoutFloatingButtons);
        layoutTopButtons = findViewById(R.id.layoutTopButtons);
        dragHandle = findViewById(R.id.dragHandle);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonShowDetails = findViewById(R.id.buttonShowDetails);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);
        folderName = getIntent().getStringExtra("folder_name");
        isDateFolder = getIntent().getBooleanExtra("is_date_folder", false);

        fileOperationHelper = new FileOperationHelper(this);

        // 初始化 SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupViewPager();
        setupControls();
        setupFloatingButtonsDrag();
        restoreFloatingButtonPosition();
        updateUndoButton();
    }

    private void setupViewPager() {
        adapter = new ImagePagerAdapter(this, photos);
        adapter.setOnImageClickListener(() -> toggleControls());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        viewPager.setOffscreenPageLimit(1); // 预加载前后各一张图片

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updatePageInfo();
            }
        });

        updatePageInfo();
    }

    private void setupControls() {
        buttonAddToThreeDaysLater.setOnClickListener(v -> addToThreeDaysLater());
        buttonDelete.setOnClickListener(v -> deleteCurrentPhoto());
        buttonUndo.setOnClickListener(v -> performUndo());
        buttonShowDetails.setOnClickListener(v -> showPhotoDetails());
    }

    private void setupFloatingButtonsDrag() {
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = layoutFloatingButtons.getX() - event.getRawX();
                        dY = layoutFloatingButtons.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // 限制在屏幕范围内
                        View parent = (View) layoutFloatingButtons.getParent();
                        int maxX = parent.getWidth() - layoutFloatingButtons.getWidth();
                        int maxY = parent.getHeight() - layoutFloatingButtons.getHeight();

                        newX = Math.max(0, Math.min(newX, maxX));
                        newY = Math.max(0, Math.min(newY, maxY));

                        layoutFloatingButtons.setX(newX);
                        layoutFloatingButtons.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        // 保存当前位置
                        saveFloatingButtonPosition();
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void addToThreeDaysLater() {
        if (currentPosition >= photos.size()) {
            return;
        }

        Photo currentPhoto = photos.get(currentPosition);
        photoToDelay = currentPhoto; // 保存引用
        positionToDelay = currentPosition; // 保存位置

        Toast.makeText(this, "正在复制文件...", Toast.LENGTH_SHORT).show();

        // 第一步：复制文件（新文件会有新的DATE_ADDED）
        new Thread(() -> {
            long newPhotoId = fileOperationHelper.copyImageFile(currentPhoto);

            runOnUiThread(() -> {
                if (newPhotoId == -1) {
                    Toast.makeText(this, "复制文件失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                newPhotoIdForDelay = newPhotoId;

                // 第二步：软删除原文件（从列表移除，退出时才真正删除）
                // 添加到撤销队列
                pendingDeleteManager.addPendingDelete(
                    new PendingDeleteManager.PendingDelete(
                        photoToDelay, positionToDelay,
                        PendingDeleteManager.PendingDelete.TYPE_DELAY, newPhotoIdForDelay
                    )
                );
                performDelayCleanup();
            });
        }).start();
    }

    /**
     * 从MediaStore中删除照片
     */
    private void deletePhotoFromMediaStore(Photo photo, Runnable onSuccess) {
        Uri photoUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                photo.getId()
        );

        ContentResolver resolver = getContentResolver();

        try {
            int deletedRows = resolver.delete(photoUri, null, null);
            if (deletedRows > 0) {
                onSuccess.run();
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            // Android 10+ 需要用户确认
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    PendingIntent pendingIntent = MediaStore.createDeleteRequest(
                            resolver,
                            Collections.singletonList(photoUri)
                    );
                    delayDeleteRequestLauncher.launch(
                            new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build()
                    );
                } catch (Exception ex) {
                    Toast.makeText(this, "删除失败: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 延迟操作完成后的清理工作
     */
    private void performDelayCleanup() {
        // 从适配器中移除当前照片
        adapter.removePhoto(currentPosition);

        // 如果列表为空，关闭Activity
        if (photos.isEmpty()) {
            Toast.makeText(this, "延迟操作完成", Toast.LENGTH_SHORT).show();
            finishWithPendingDeletes();
            return;
        }

        // 调整当前位置
        if (currentPosition >= photos.size()) {
            currentPosition = photos.size() - 1;
        }

        // 更新页面信息
        updatePageInfo();
        updateUndoButton();

        // 设置result，通知上级Activity刷新
        setResult(RESULT_OK);

        Toast.makeText(this, "已移到3天后（可撤销）", Toast.LENGTH_SHORT).show();
    }

    private void updatePageInfo() {
        textViewPageInfo.setText((currentPosition + 1) + " / " + photos.size());
    }

    private void toggleControls() {
        if (controlsVisible) {
            layoutControls.setVisibility(View.GONE);
            layoutFloatingButtons.setVisibility(View.GONE);
            layoutTopButtons.setVisibility(View.GONE);
        } else {
            layoutControls.setVisibility(View.VISIBLE);
            layoutFloatingButtons.setVisibility(View.VISIBLE);
            layoutTopButtons.setVisibility(View.VISIBLE);
        }
        controlsVisible = !controlsVisible;
    }

    private void deleteCurrentPhoto() {
        if (currentPosition < photos.size()) {
            Photo photoToDelete = photos.get(currentPosition);
            int positionToDelete = currentPosition;

            // 软删除：先从列表中移除，但不立即删除文件
            // 添加到撤销队列
            pendingDeleteManager.addPendingDelete(
                new PendingDeleteManager.PendingDelete(
                    photoToDelete, positionToDelete,
                    PendingDeleteManager.PendingDelete.TYPE_DELETE
                )
            );
            performDeleteCleanup();
            Toast.makeText(this, "已删除（可撤销，退出后永久删除）", Toast.LENGTH_SHORT).show();
        }
    }

    private void performDeleteCleanup() {
        if (currentPosition >= 0 && currentPosition < photos.size()) {
            // 从适配器中移除
            adapter.removePhoto(currentPosition);
        }

        // 如果删除后列表为空，关闭Activity
        if (photos.isEmpty()) {
            finishWithPendingDeletes();
            return;
        }

        // 调整当前位置
        if (currentPosition >= photos.size()) {
            currentPosition = photos.size() - 1;
        }

        // 更新页面信息
        updatePageInfo();
        updateUndoButton();

        // 设置result，通知上级Activity刷新
        setResult(RESULT_OK);
    }

    /**
     * 更新撤销按钮状态
     */
    private void updateUndoButton() {
        if (pendingDeleteManager.canUndo()) {
            buttonUndo.setEnabled(true);
            buttonUndo.setText("撤销(" + pendingDeleteManager.getCount() + ")");
        } else {
            buttonUndo.setEnabled(false);
            buttonUndo.setText("撤销");
        }
    }

    /**
     * 执行撤销操作
     */
    private void performUndo() {
        PendingDeleteManager.PendingDelete pendingDelete = pendingDeleteManager.undo();
        if (pendingDelete == null) {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show();
            return;
        }

        Photo photo = pendingDelete.getPhoto();
        int originalPosition = pendingDelete.getOriginalPosition();

        if (pendingDelete.getActionType() == PendingDeleteManager.PendingDelete.TYPE_DELETE) {
            // 撤销删除操作：将照片恢复到列表中
            int position = Math.min(originalPosition, photos.size());
            photos.add(position, photo);
            adapter.notifyItemInserted(position);

            // 自动跳转到恢复的图片位置
            currentPosition = position;
            viewPager.setCurrentItem(currentPosition, false);

            updatePageInfo();
            Toast.makeText(this, "已撤销删除: " + photo.getName(), Toast.LENGTH_SHORT).show();
        } else if (pendingDelete.getActionType() == PendingDeleteManager.PendingDelete.TYPE_DELAY) {
            // 撤销延迟操作：删除新创建的副本，恢复原照片到列表
            long newPhotoId = pendingDelete.getNewPhotoId();
            if (newPhotoId != -1) {
                // 删除新创建的副本
                Uri newPhotoUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        newPhotoId
                );
                try {
                    getContentResolver().delete(newPhotoUri, null, null);

                    // 恢复原照片到列表
                    int position = Math.min(originalPosition, photos.size());
                    photos.add(position, photo);
                    adapter.notifyItemInserted(position);

                    currentPosition = position;
                    viewPager.setCurrentItem(currentPosition, false);

                    updatePageInfo();
                    Toast.makeText(this, "已撤销延迟操作: " + photo.getName(), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "撤销失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        updateUndoButton();
        setResult(RESULT_OK);
    }

    /**
     * 执行所有待删除的操作（退出时调用）
     */
    private void executePendingDeletes() {
        List<PendingDeleteManager.PendingDelete> pendingDeletes = pendingDeleteManager.getAllPendingDeletes();

        if (pendingDeletes.isEmpty()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        // 收集所有需要删除的URI
        List<Uri> urisToDelete = new ArrayList<>();
        for (PendingDeleteManager.PendingDelete pending : pendingDeletes) {
            Photo photo = pending.getPhoto();
            Uri photoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    photo.getId()
            );
            urisToDelete.add(photoUri);
        }

        // 使用批量删除请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用批量删除请求
            try {
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(
                        getContentResolver(),
                        urisToDelete
                );
                deleteRequestLauncher.launch(
                        new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build()
                );
            } catch (Exception e) {
                Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                pendingDeleteManager.clear();
                setResult(RESULT_OK);
                finish();
            }
        } else {
            // Android 10 及以下，逐个删除
            int deletedCount = 0;
            for (Uri uri : urisToDelete) {
                try {
                    int deleted = getContentResolver().delete(uri, null, null);
                    if (deleted > 0) {
                        deletedCount++;
                    }
                } catch (Exception e) {
                    // 忽略单个删除错误
                }
            }
            pendingDeleteManager.clear();
            setResult(RESULT_OK);
            finish();
        }
    }

    /**
     * 恢复所有待删除的照片到列表（用户取消删除时）
     */
    private void restoreAllPendingDeletes() {
        List<PendingDeleteManager.PendingDelete> pendingDeletes = pendingDeleteManager.getAllPendingDeletes();

        // 按原始位置排序，从后往前恢复
        for (int i = pendingDeletes.size() - 1; i >= 0; i--) {
            PendingDeleteManager.PendingDelete pending = pendingDeletes.get(i);

            if (pending.getActionType() == PendingDeleteManager.PendingDelete.TYPE_DELAY) {
                // 延迟操作：需要删除新创建的副本
                long newPhotoId = pending.getNewPhotoId();
                if (newPhotoId != -1) {
                    Uri newPhotoUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            newPhotoId
                    );
                    try {
                        getContentResolver().delete(newPhotoUri, null, null);
                    } catch (Exception e) {
                        // 忽略错误
                    }
                }
            }

            // 恢复照片到列表
            Photo photo = pending.getPhoto();
            int position = Math.min(pending.getOriginalPosition(), photos.size());
            photos.add(position, photo);
            adapter.notifyItemInserted(position);
        }

        pendingDeleteManager.clear();
        updatePageInfo();
        updateUndoButton();
    }

    @Override
    public void onBackPressed() {
        finishWithPendingDeletes();
    }

    /**
     * 完成Activity并执行待删除操作
     */
    private void finishWithPendingDeletes() {
        if (pendingDeleteManager.canUndo()) {
            // 有待删除的文件，执行删除
            executePendingDeletes();
        } else {
            // 没有待删除的文件，直接退出
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 保存悬浮按钮的位置
     */
    private void saveFloatingButtonPosition() {
        float x = layoutFloatingButtons.getX();
        float y = layoutFloatingButtons.getY();

        prefs.edit()
                .putFloat(KEY_FLOATING_X, x)
                .putFloat(KEY_FLOATING_Y, y)
                .apply();
    }

    /**
     * 恢复悬浮按钮的位置
     */
    private void restoreFloatingButtonPosition() {
        // 等待布局完成后再恢复位置
        layoutFloatingButtons.post(new Runnable() {
            @Override
            public void run() {
                // 获取保存的位置，如果没有保存过，使用默认值 -1
                float savedX = prefs.getFloat(KEY_FLOATING_X, -1);
                float savedY = prefs.getFloat(KEY_FLOATING_Y, -1);

                // 如果之前保存过位置，则恢复
                if (savedX != -1 && savedY != -1) {
                    // 获取父容器尺寸
                    View parent = (View) layoutFloatingButtons.getParent();
                    int maxX = parent.getWidth() - layoutFloatingButtons.getWidth();
                    int maxY = parent.getHeight() - layoutFloatingButtons.getHeight();

                    // 确保位置在有效范围内（防止屏幕尺寸改变导致的问题）
                    float x = Math.max(0, Math.min(savedX, maxX));
                    float y = Math.max(0, Math.min(savedY, maxY));

                    layoutFloatingButtons.setX(x);
                    layoutFloatingButtons.setY(y);
                }
            }
        });
    }

    /**
     * 显示图片详细信息
     */
    private void showPhotoDetails() {
        if (currentPosition >= photos.size()) {
            return;
        }

        Photo currentPhoto = photos.get(currentPosition);

        // 格式化日期时间
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 创建时间（DATE_ADDED）
        String dateAdded = dateFormat.format(new Date(currentPhoto.getDateAdded() * 1000));

        // 修改时间（从文件获取）
        String dateModified = "未知";
        File file = new File(currentPhoto.getPath());
        if (file.exists()) {
            long lastModified = file.lastModified();
            dateModified = dateFormat.format(new Date(lastModified));
        }

        // 文件路径
        String path = currentPhoto.getPath();

        // 构建详情信息
        StringBuilder details = new StringBuilder();
        details.append("文件名称：\n").append(currentPhoto.getName()).append("\n\n");
        details.append("创建时间：\n").append(dateAdded).append("\n\n");
        details.append("修改时间：\n").append(dateModified).append("\n\n");
        details.append("文件路径：\n").append(path);

        // 显示对话框
        new AlertDialog.Builder(this)
                .setTitle("图片详情")
                .setMessage(details.toString())
                .setPositiveButton("确定", null)
                .show();
    }
}
