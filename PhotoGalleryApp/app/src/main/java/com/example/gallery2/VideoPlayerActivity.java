package com.example.gallery2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class VideoPlayerActivity extends AppCompatActivity {
    private VideoView videoView;
    private SeekBar seekBar;
    private TextView textViewCurrentTime, textViewTotalTime, textViewVideoInfo, textViewPageInfo;
    private Button buttonPlayPause, buttonSystemPlayer, buttonAddToThreeDaysLater, buttonDelete;
    private ImageView imageViewPlayPause;
    private LinearLayout topControlBar, bottomControlBar;

    private ArrayList<Photo> videos;
    private int currentPosition;
    private String folderName;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private boolean controlsVisible = true;

    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private ActivityResultLauncher<IntentSenderRequest> delayDeleteRequestLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // 初始化视图
        videoView = findViewById(R.id.videoView);
        seekBar = findViewById(R.id.seekBar);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);
        textViewVideoInfo = findViewById(R.id.textViewVideoInfo);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonSystemPlayer = findViewById(R.id.buttonSystemPlayer);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        buttonDelete = findViewById(R.id.buttonDelete);
        imageViewPlayPause = findViewById(R.id.imageViewPlayPause);
        topControlBar = findViewById(R.id.topControlBar);
        bottomControlBar = findViewById(R.id.bottomControlBar);

        // 获取Intent数据
        videos = (ArrayList<Photo>) getIntent().getSerializableExtra("videos");
        currentPosition = getIntent().getIntExtra("position", 0);
        folderName = getIntent().getStringExtra("folderName");

        // 设置删除请求启动器
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        performDeleteCleanup();
                        Toast.makeText(this, "已永久删除", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "删除已取消", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 设置延迟操作删除请求启动器
        delayDeleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        performDelayCleanup();
                    } else {
                        Toast.makeText(this, "延迟操作已取消", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 设置按钮监听器
        buttonPlayPause.setOnClickListener(v -> togglePlayPause());
        buttonSystemPlayer.setOnClickListener(v -> openInSystemPlayer());
        buttonAddToThreeDaysLater.setOnClickListener(v -> addToThreeDaysLater());
        buttonDelete.setOnClickListener(v -> deleteCurrentVideo());

        // 点击视频切换控制栏显示/隐藏
        videoView.setOnClickListener(v -> toggleControls());

        // SeekBar进度控制
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 加载视频
        loadVideo();
    }

    private void loadVideo() {
        if (videos == null || videos.isEmpty()) {
            Toast.makeText(this, "没有视频", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Photo currentVideo = videos.get(currentPosition);

        // 设置视频信息
        textViewVideoInfo.setText(currentVideo.getName());
        textViewPageInfo.setText(String.format(Locale.getDefault(), "%d / %d",
                currentPosition + 1, videos.size()));

        // 停止之前的播放
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }

        // 重置状态
        isPlaying = false;
        seekBar.setProgress(0);
        textViewCurrentTime.setText("00:00");
        textViewTotalTime.setText("00:00");

        try {
            // 设置视频URI
            Uri videoUri = currentVideo.getUri();
            if (videoUri == null) {
                Toast.makeText(this, "视频URI为空，尝试使用路径", Toast.LENGTH_SHORT).show();
                // 尝试使用路径
                String path = currentVideo.getPath();
                if (path != null && !path.isEmpty()) {
                    videoUri = Uri.parse(path);
                } else {
                    Toast.makeText(this, "无法获取视频路径", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            android.util.Log.d("VideoPlayer", "Playing video: " + currentVideo.getName());
            android.util.Log.d("VideoPlayer", "URI: " + videoUri);
            android.util.Log.d("VideoPlayer", "Path: " + currentVideo.getPath());

            videoView.setVideoURI(videoUri);

            // 错误监听
            videoView.setOnErrorListener((mp, what, extra) -> {
                String errorMsg = "视频播放出错";
                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    errorMsg += " (未知错误)";
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    errorMsg += " (服务器错误)";
                }
                Toast.makeText(VideoPlayerActivity.this, errorMsg + "\nWhat: " + what + " Extra: " + extra,
                    Toast.LENGTH_LONG).show();
                return true;
            });

            // 准备完成监听
            videoView.setOnPreparedListener(mp -> {
                int duration = mp.getDuration();
                seekBar.setMax(duration);
                textViewTotalTime.setText(formatTime(duration));

                // 自动播放
                videoView.start();
                isPlaying = true;
                updatePlayPauseButton();

                // 开始更新进度
                updateProgress();

                Toast.makeText(VideoPlayerActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
            });

            // 播放完成监听
            videoView.setOnCompletionListener(mp -> {
                isPlaying = false;
                updatePlayPauseButton();

                // 播放下一个视频
                if (currentPosition < videos.size() - 1) {
                    currentPosition++;
                    loadVideo();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "加载视频失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void openInSystemPlayer() {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        Photo currentVideo = videos.get(currentPosition);
        Uri videoUri = currentVideo.getUri();

        if (videoUri == null) {
            Toast.makeText(this, "无法获取视频URI", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(videoUri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开系统播放器: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void togglePlayPause() {
        if (isPlaying) {
            videoView.pause();
            isPlaying = false;
        } else {
            videoView.start();
            isPlaying = true;
            updateProgress();
        }
        updatePlayPauseButton();
        showPlayPauseIcon();
    }

    private void updatePlayPauseButton() {
        buttonPlayPause.setText(isPlaying ? "暂停" : "播放");
    }

    private void showPlayPauseIcon() {
        imageViewPlayPause.setImageResource(isPlaying ?
                android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
        imageViewPlayPause.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> imageViewPlayPause.setVisibility(View.GONE), 500);
    }

    private void toggleControls() {
        if (controlsVisible) {
            topControlBar.setVisibility(View.GONE);
            bottomControlBar.setVisibility(View.GONE);
        } else {
            topControlBar.setVisibility(View.VISIBLE);
            bottomControlBar.setVisibility(View.VISIBLE);
        }
        controlsVisible = !controlsVisible;
    }

    private void updateProgress() {
        if (isPlaying && videoView != null) {
            int currentPos = videoView.getCurrentPosition();
            seekBar.setProgress(currentPos);
            textViewCurrentTime.setText(formatTime(currentPos));

            handler.postDelayed(this::updateProgress, 100);
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void addToThreeDaysLater() {
        Photo currentVideo = videos.get(currentPosition);

        android.util.Log.d("VideoPlayer", "=== 开始3天后操作 ===");
        android.util.Log.d("VideoPlayer", "视频名称: " + currentVideo.getName());
        android.util.Log.d("VideoPlayer", "视频ID: " + currentVideo.getId());
        android.util.Log.d("VideoPlayer", "视频URI: " + currentVideo.getUri());
        android.util.Log.d("VideoPlayer", "视频路径: " + currentVideo.getPath());

        // 确保有有效的URI
        Uri videoUri = currentVideo.getUri();
        if (videoUri == null) {
            // 使用视频ID构建content:// URI
            videoUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    currentVideo.getId()
            );
            android.util.Log.d("VideoPlayer", "URI为空，使用ID构建: " + videoUri);
        }

        final Uri finalVideoUri = videoUri;
        Toast.makeText(this, "正在复制文件...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            FileOperationHelper helper = new FileOperationHelper(this);

            // 复制视频文件到 Movies/ 目录（新文件会有新的 DATE_ADDED）
            // 应用会根据 DATE_ADDED + 3天自动将其归类到正确的日期文件夹
            android.util.Log.d("VideoPlayer", "开始复制视频文件...");
            android.util.Log.d("VideoPlayer", "使用URI: " + finalVideoUri);
            long newId = helper.copyVideoFile(finalVideoUri, currentVideo.getName());
            android.util.Log.d("VideoPlayer", "复制结果 newId: " + newId);

            runOnUiThread(() -> {
                if (newId != -1) {
                    android.util.Log.d("VideoPlayer", "复制成功，开始删除原文件...");
                    // 直接删除原文件
                    deleteVideoFromMediaStore(currentVideo, () -> {
                        android.util.Log.d("VideoPlayer", "删除原文件成功，执行清理...");
                        performDelayCleanup();
                        Toast.makeText(VideoPlayerActivity.this, "已移到3天后", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    android.util.Log.e("VideoPlayer", "复制文件失败!");
                    Toast.makeText(VideoPlayerActivity.this, "复制文件失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 从MediaStore中删除视频
     */
    private void deleteVideoFromMediaStore(Photo video, Runnable onSuccess) {
        android.util.Log.d("VideoPlayer", "=== 删除原视频 ===");
        android.util.Log.d("VideoPlayer", "视频ID: " + video.getId());

        Uri videoUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                video.getId()
        );
        android.util.Log.d("VideoPlayer", "删除URI: " + videoUri);

        ContentResolver resolver = getContentResolver();

        try {
            android.util.Log.d("VideoPlayer", "尝试直接删除...");
            int deletedRows = resolver.delete(videoUri, null, null);
            android.util.Log.d("VideoPlayer", "删除结果: " + deletedRows + " 行");
            if (deletedRows > 0) {
                onSuccess.run();
            } else {
                android.util.Log.e("VideoPlayer", "删除失败，返回0行");
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            android.util.Log.w("VideoPlayer", "SecurityException，需要用户确认: " + e.getMessage());
            // Android 10+ 需要用户确认
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    PendingIntent pendingIntent = MediaStore.createDeleteRequest(
                            resolver,
                            Collections.singletonList(videoUri)
                    );
                    android.util.Log.d("VideoPlayer", "启动删除确认对话框...");
                    delayDeleteRequestLauncher.launch(
                            new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build()
                    );
                } catch (Exception ex) {
                    android.util.Log.e("VideoPlayer", "创建删除请求失败: " + ex.getMessage(), ex);
                    Toast.makeText(this, "删除失败: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                android.util.Log.e("VideoPlayer", "删除失败: " + e.getMessage());
                Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("VideoPlayer", "删除时发生意外错误: " + e.getMessage(), e);
            Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCurrentVideo() {
        Photo currentVideo = videos.get(currentPosition);

        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要永久删除此视频吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    performDeleteVideo(currentVideo);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performDeleteVideo(Photo video) {
        Uri videoUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                video.getId()
        );

        ContentResolver resolver = getContentResolver();

        try {
            int deletedRows = resolver.delete(videoUri, null, null);
            if (deletedRows > 0) {
                performDeleteCleanup();
                Toast.makeText(this, "已永久删除", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            // Android 10+ 需要用户确认
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    PendingIntent pendingIntent = MediaStore.createDeleteRequest(
                            resolver,
                            Collections.singletonList(videoUri)
                    );
                    deleteRequestLauncher.launch(
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

    private void performDelayCleanup() {
        // 移除当前视频
        videos.remove(currentPosition);

        if (videos.isEmpty()) {
            Toast.makeText(this, "已无视频", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        // 调整位置
        if (currentPosition >= videos.size()) {
            currentPosition = videos.size() - 1;
        }

        // 重新加载
        loadVideo();

        // 通知上级Activity刷新
        setResult(RESULT_OK);
    }

    private void performDeleteCleanup() {
        performDelayCleanup();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) {
            videoView.pause();
            isPlaying = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
