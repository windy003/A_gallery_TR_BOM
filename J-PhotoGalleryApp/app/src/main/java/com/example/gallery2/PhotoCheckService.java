package com.example.gallery2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.util.List;

/**
 * 后台服务，每分钟检查一次是否有达标的图片文件
 */
public class PhotoCheckService extends Service {
    private static final String TAG = "PhotoCheckService";
    private static final String CHANNEL_ID = "photo_check_service_channel";
    private static final String ALERT_CHANNEL_ID = "photo_alert_channel";
    private static final int SERVICE_NOTIFICATION_ID = 1001;
    private static final int ALERT_NOTIFICATION_ID = 1002;
    private static final long CHECK_INTERVAL = 60 * 1000; // 1分钟

    private Handler handler;
    private Runnable checkRunnable;
    private PhotoManager photoManager;
    private int lastExpiredCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        try {
            photoManager = new PhotoManager(this);
            handler = new Handler(Looper.getMainLooper());

            // 创建通知渠道
            createNotificationChannels();

            // 启动前台服务
            startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());

            // 初始化最后过期数量
            lastExpiredCount = photoManager.getExpiredPhotoCount();

            // 开始定期检查
            startPeriodicCheck();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
            // 如果启动失败，停止服务
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY; // 服务被杀死后自动重启
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        // 停止定期检查
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不支持绑定
    }

    /**
     * 创建通知渠道（Android 8.0及以上需要）
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 服务运行通知渠道
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "图片检查服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("后台检查过期图片的服务");
            serviceChannel.setShowBadge(false);

            // 过期图片提醒通知渠道
            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "过期图片提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("提醒您有新的过期图片需要处理");
            alertChannel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(alertChannel);
            }
        }
    }

    /**
     * 创建服务运行通知
     */
    private Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("图片检查服务运行中")
                .setContentText("每分钟检查一次过期图片")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // 不可滑动删除
                .build();
    }

    /**
     * 开始定期检查
     */
    private void startPeriodicCheck() {
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkExpiredPhotos();
                // 继续下一次检查
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };

        // 立即执行第一次检查
        handler.post(checkRunnable);
    }

    /**
     * 检查过期图片
     */
    private void checkExpiredPhotos() {
        try {
            Log.d(TAG, "Checking for expired photos...");

            int currentExpiredCount = photoManager.getExpiredPhotoCount();

            Log.d(TAG, "Last expired count: " + lastExpiredCount +
                      ", Current expired count: " + currentExpiredCount);

            // 如果过期图片数量发生变化，更新小部件
            if (currentExpiredCount != lastExpiredCount) {
                Log.d(TAG, "过期图片数量发生变化，更新小部件");
                CompletedDateWidget.updateAllWidgets(this);
            }

            // 如果过期图片数量增加了，发送通知
            if (currentExpiredCount > lastExpiredCount) {
                int newExpiredCount = currentExpiredCount - lastExpiredCount;
                sendExpiredPhotoAlert(newExpiredCount, currentExpiredCount);
            }

            // 更新计数
            lastExpiredCount = currentExpiredCount;

            // 更新服务通知，显示当前过期图片数量
            updateServiceNotification(currentExpiredCount);

        } catch (Exception e) {
            Log.e(TAG, "Error checking expired photos", e);
        }
    }

    /**
     * 发送过期图片提醒通知
     */
    private void sendExpiredPhotoAlert(int newCount, int totalCount) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String contentText = "新增 " + newCount + " 张过期图片，共 " + totalCount + " 张需要处理";

        Notification notification = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setContentTitle("发现新的过期图片")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // 点击后自动消失
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(ALERT_NOTIFICATION_ID, notification);
            Log.d(TAG, "Alert notification sent: " + contentText);
        }
    }

    /**
     * 更新服务通知，显示当前过期图片数量
     */
    private void updateServiceNotification(int expiredCount) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = "每分钟检查一次，当前有 " + expiredCount + " 张过期图片";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("图片检查服务运行中")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
        }
    }
}
