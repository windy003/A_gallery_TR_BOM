package com.example.gallery2

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * 后台服务，每分钟检查一次是否有达标的图片文件
 */
class PhotoCheckService : Service() {
    companion object {
        private const val TAG = "PhotoCheckService"
        private const val CHANNEL_ID = "photo_check_service_channel"
        private const val ALERT_CHANNEL_ID = "photo_alert_channel"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val CHECK_INTERVAL = 60 * 1000L // 1分钟
    }

    private var handler: Handler? = null
    private var checkRunnable: Runnable? = null
    private lateinit var photoManager: PhotoManager
    private var lastExpiredCount = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        try {
            photoManager = PhotoManager(this)
            handler = Handler(Looper.getMainLooper())

            // 创建通知渠道
            createNotificationChannels()

            // 启动前台服务
            startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification())

            // 初始化最后过期数量
            lastExpiredCount = photoManager.getExpiredPhotoCount()

            // 开始定期检查
            startPeriodicCheck()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            // 如果启动失败，停止服务
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY // 服务被杀死后自动重启
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        // 停止定期检查
        handler?.let {
            checkRunnable?.let { runnable ->
                it.removeCallbacks(runnable)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 不支持绑定
    }

    /**
     * 创建通知渠道（Android 8.0及以上需要）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 服务运行通知渠道
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "图片检查服务",
                NotificationManager.IMPORTANCE_LOW
            )
            serviceChannel.description = "后台检查过期图片的服务"
            serviceChannel.setShowBadge(false)

            // 过期图片提醒通知渠道
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "过期图片提醒",
                NotificationManager.IMPORTANCE_HIGH
            )
            alertChannel.description = "提醒您有新的过期图片需要处理"
            alertChannel.setShowBadge(true)

            val manager = getSystemService(NotificationManager::class.java)
            manager?.apply {
                createNotificationChannel(serviceChannel)
                createNotificationChannel(alertChannel)
            }
        }
    }

    /**
     * 创建服务运行通知
     */
    private fun createServiceNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("图片检查服务运行中")
            .setContentText("每分钟检查一次过期图片")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 不可滑动删除
            .build()
    }

    /**
     * 开始定期检查
     */
    private fun startPeriodicCheck() {
        checkRunnable = object : Runnable {
            override fun run() {
                checkExpiredPhotos()
                // 继续下一次检查
                handler?.postDelayed(this, CHECK_INTERVAL)
            }
        }

        // 立即执行第一次检查
        handler?.post(checkRunnable!!)
    }

    /**
     * 检查过期图片
     */
    private fun checkExpiredPhotos() {
        try {
            Log.d(TAG, "Checking for expired photos...")

            val currentExpiredCount = photoManager.getExpiredPhotoCount()

            Log.d(
                TAG, "Last expired count: $lastExpiredCount, " +
                        "Current expired count: $currentExpiredCount"
            )

            // 如果过期图片数量发生变化，更新小部件
            if (currentExpiredCount != lastExpiredCount) {
                Log.d(TAG, "过期图片数量发生变化，更新小部件")
                CompletedDateWidget.updateAllWidgets(this)
            }

            // 如果过期图片数量增加了，发送通知
            if (currentExpiredCount > lastExpiredCount) {
                val newExpiredCount = currentExpiredCount - lastExpiredCount
                sendExpiredPhotoAlert(newExpiredCount, currentExpiredCount)
            }

            // 更新计数
            lastExpiredCount = currentExpiredCount

            // 更新服务通知，显示当前过期图片数量
            updateServiceNotification(currentExpiredCount)

        } catch (e: Exception) {
            Log.e(TAG, "Error checking expired photos", e)
        }
    }

    /**
     * 发送过期图片提醒通知
     */
    private fun sendExpiredPhotoAlert(newCount: Int, totalCount: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = "新增 $newCount 张过期图片，共 $totalCount 张需要处理"

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("发现新的过期图片")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 点击后自动消失
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(ALERT_NOTIFICATION_ID, notification)
        Log.d(TAG, "Alert notification sent: $contentText")
    }

    /**
     * 更新服务通知，显示当前过期图片数量
     */
    private fun updateServiceNotification(expiredCount: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "每分钟检查一次，当前有 $expiredCount 张过期图片"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("图片检查服务运行中")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(SERVICE_NOTIFICATION_ID, notification)
    }
}
