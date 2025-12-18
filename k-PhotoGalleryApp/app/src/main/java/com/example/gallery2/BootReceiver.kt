package com.example.gallery2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 设备重启后自动恢复小部件定时更新任务
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "设备重启完成，重新启动小部件定时更新")
            // 重新启动小部件的定时更新任务
            CompletedDateWidget.startAutoUpdate(context)
        }
    }
}
