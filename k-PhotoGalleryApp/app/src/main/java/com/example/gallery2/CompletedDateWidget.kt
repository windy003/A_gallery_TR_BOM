package com.example.gallery2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.util.*

/**
 * 显示最近完成日期的桌面小部件
 */
class CompletedDateWidget : AppWidgetProvider() {
    companion object {
        private const val PREFS_NAME = "IconManagerPrefs"

        /**
         * 设备配置类 - 根据不同手机型号定制显示参数
         */
        private data class DeviceConfig(
            val countTextSize: Float,  // "本机X个"的字体大小
            val timeTextSize: Float    // 时间文字的字体大小
        )

        /**
         * 根据设备型号获取最佳显示配置（针对用户的四部手机定制）
         */
        private fun getDeviceConfig(): DeviceConfig {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL.lowercase()
            val brand = Build.BRAND.lowercase()

            Log.d("CompletedDateWidget", "设备信息 - 品牌: $brand, 制造商: $manufacturer, 型号: $model")

            // LG Wing 配置（两台）
            if (model.contains("wing") || model.contains("lm-f100")) {
                Log.d("CompletedDateWidget", "检测到LG Wing，使用LG Wing专用配置")
                return DeviceConfig(17f, 17f)  // 所有字体都调大
            }

            // 小米12 Pro 配置
            if (model.contains("2201122c") || model.contains("mi 12 pro") || model.contains("2201122")) {
                Log.d("CompletedDateWidget", "检测到小米12 Pro，使用小米12 Pro专用配置")
                return DeviceConfig(11f, 11f)
            }

            // 小米MIX Fold 2 配置
            if (model.contains("22061218c") || model.contains("mix fold 2") || model.contains("mixfold2")) {
                Log.d("CompletedDateWidget", "检测到小米MIX Fold 2，使用MIX Fold 2专用配置")
                return DeviceConfig(15f, 15f)  // 折叠屏幕更大，用更大的字体
            }

            // 默认配置（其他设备）
            Log.d("CompletedDateWidget", "未识别的设备，使用默认配置")
            return DeviceConfig(12f, 8f)
        }

        /**
         * 更新单个widget
         */
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // 获取当前设备的配置
            val config = getDeviceConfig()

            // 检测是否有过期的文件夹
            val photoManager = PhotoManager(context)
            val hasExpired = photoManager.hasExpiredFolders()

            // 统计需要阅读消化的图片数量
            val photoCount = photoManager.getExpiredPhotoCount()

            // 构建widget的RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_completed_date)

            if (hasExpired) {
                // 有过期文件夹：显示图片数量 "本机X个" 和检查时间
                val countText = String.format(
                    Locale.getDefault(),
                    "<font color='#000000'>本机</font><font color='#FF0000'>%d</font><font color='#000000'>个</font>", photoCount
                )
                views.setTextViewText(
                    R.id.widget_photo_count,
                    Html.fromHtml(countText, Html.FROM_HTML_MODE_LEGACY)
                )
                views.setTextViewTextSize(R.id.widget_photo_count, TypedValue.COMPLEX_UNIT_SP, config.countTextSize)
                views.setViewVisibility(R.id.widget_photo_count, View.VISIBLE)

                // 显示检查时间
                val now = Calendar.getInstance()
                val month = now.get(Calendar.MONTH) + 1
                val day = now.get(Calendar.DAY_OF_MONTH)
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val minute = now.get(Calendar.MINUTE)

                val timeText = String.format(
                    Locale.getDefault(),
                    "<font color='#00FF00'>%d/%d </font><font color='#FF0000'>%02d</font><font color='#00FF00'>:%02d</font>",
                    month, day, hour, minute
                )

                views.setTextViewText(
                    R.id.widget_check_time,
                    Html.fromHtml(timeText, Html.FROM_HTML_MODE_LEGACY)
                )
                views.setTextViewTextSize(R.id.widget_check_time, TypedValue.COMPLEX_UNIT_SP, config.timeTextSize)
                views.setViewVisibility(R.id.widget_check_time, View.VISIBLE)

                Log.d("CompletedDateWidget", "有过期文件夹，显示图片数量: $photoCount，检查时间: $month/$day $hour:$minute")
            } else {
                // 没有过期文件夹：显示"本机0个"和检查时间
                val countText = String.format(
                    Locale.getDefault(),
                    "<font color='#000000'>本机</font><font color='#FF0000'>%d</font><font color='#000000'>个</font>", photoCount
                )
                views.setTextViewText(
                    R.id.widget_photo_count,
                    Html.fromHtml(countText, Html.FROM_HTML_MODE_LEGACY)
                )
                views.setTextViewTextSize(R.id.widget_photo_count, TypedValue.COMPLEX_UNIT_SP, config.countTextSize)
                views.setViewVisibility(R.id.widget_photo_count, View.VISIBLE)

                // 显示检查时间
                val now = Calendar.getInstance()
                val month = now.get(Calendar.MONTH) + 1
                val day = now.get(Calendar.DAY_OF_MONTH)
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val minute = now.get(Calendar.MINUTE)

                val timeText = String.format(
                    Locale.getDefault(),
                    "<font color='#00FF00'>%d/%d </font><font color='#FF0000'>%02d</font><font color='#00FF00'>:%02d</font>",
                    month, day, hour, minute
                )

                views.setTextViewText(
                    R.id.widget_check_time,
                    Html.fromHtml(timeText, Html.FROM_HTML_MODE_LEGACY)
                )
                views.setTextViewTextSize(R.id.widget_check_time, TypedValue.COMPLEX_UNIT_SP, config.timeTextSize)
                views.setViewVisibility(R.id.widget_check_time, View.VISIBLE)

                Log.d("CompletedDateWidget", "没有过期文件夹，显示图片数量: 0，检查时间: $month/$day $hour:$minute")
            }

            // 设置点击事件 - 点击widget打开应用
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 更新widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * 静态方法：从外部触发所有widget的更新（直接更新，不使用广播）
         */
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, CompletedDateWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                Log.d("CompletedDateWidget", "更新小部件，数量: ${appWidgetIds.size}")

                // 直接调用更新方法，不使用广播（避免被小米系统拦截）
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    Log.d("CompletedDateWidget", "已更新widget ID: $appWidgetId")
                }
            } catch (e: Exception) {
                Log.e("CompletedDateWidget", "更新小部件失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // 更新所有的widget实例
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
