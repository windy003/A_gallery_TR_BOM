package com.example.gallery2;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 显示最近完成日期的桌面小部件
 */
public class CompletedDateWidget extends AppWidgetProvider {
    private static final String PREFS_NAME = "IconManagerPrefs";

    /**
     * 设备配置类 - 根据不同手机型号定制显示参数
     */
    private static class DeviceConfig {
        float countTextSize;      // "本机X个"的字体大小
        float timeTextSize;       // 时间文字的字体大小
        float dateTextSize;       // 日期文字的字体大小

        DeviceConfig(float countTextSize, float timeTextSize, float dateTextSize) {
            this.countTextSize = countTextSize;
            this.timeTextSize = timeTextSize;
            this.dateTextSize = dateTextSize;
        }
    }

    /**
     * 根据设备型号获取最佳显示配置（针对用户的四部手机定制）
     */
    private static DeviceConfig getDeviceConfig() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        String brand = Build.BRAND.toLowerCase();

        Log.d("CompletedDateWidget", "设备信息 - 品牌: " + brand + ", 制造商: " + manufacturer + ", 型号: " + model);

        // LG Wing 配置（两台）
        if (model.contains("wing") || model.contains("lm-f100")) {
            Log.d("CompletedDateWidget", "检测到LG Wing，使用LG Wing专用配置");
            return new DeviceConfig(20f, 14f, 14f);  // 所有字体都调大
        }

        // 小米12 Pro 配置
        if (model.contains("2201122c") || model.contains("mi 12 pro") || model.contains("2201122")) {
            Log.d("CompletedDateWidget", "检测到小米12 Pro，使用小米12 Pro专用配置");
            return new DeviceConfig(8f, 8f, 12f);
        }

        // 小米MIX Fold 2 配置
        if (model.contains("22061218c") || model.contains("mix fold 2") || model.contains("mixfold2")) {
            Log.d("CompletedDateWidget", "检测到小米MIX Fold 2，使用MIX Fold 2专用配置");
            return new DeviceConfig(11f, 11f, 11f);  // 折叠屏幕更大，用更大的字体
        }

        // 默认配置（其他设备）
        Log.d("CompletedDateWidget", "未识别的设备，使用默认配置");
        return new DeviceConfig(12f, 8f, 8f);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有的widget实例
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * 更新单个widget
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // 获取当前设备的配置
        DeviceConfig config = getDeviceConfig();

        // 检测是否有过期的文件夹
        PhotoManager photoManager = new PhotoManager(context);
        boolean hasExpired = photoManager.hasExpiredFolders();

        // 统计需要阅读消化的图片数量
        int photoCount = photoManager.getExpiredPhotoCount();

        // 构建widget的RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_completed_date);

        if (hasExpired) {
            // 有过期文件夹：显示图片数量 "本机X个" 和检查时间
            views.setViewVisibility(R.id.widget_date_text, android.view.View.GONE);

            // 使用HTML标签，"本机"和"个"为黑色，数字为红色
            String countText = String.format(Locale.getDefault(),
                "<font color='#000000'>本机</font><font color='#FF0000'>%d</font><font color='#000000'>个</font>", photoCount);
            views.setTextViewText(R.id.widget_photo_count, android.text.Html.fromHtml(countText, android.text.Html.FROM_HTML_MODE_LEGACY));
            views.setTextViewTextSize(R.id.widget_photo_count, android.util.TypedValue.COMPLEX_UNIT_SP, config.countTextSize);
            views.setViewVisibility(R.id.widget_photo_count, android.view.View.VISIBLE);

            // 显示检查时间
            Calendar now = Calendar.getInstance();
            int month = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DAY_OF_MONTH);
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            // 使用HTML标签，日期和分钟为绿色，小时为红色
            String timeText = String.format(Locale.getDefault(),
                "<font color='#00FF00'>%d/%d </font><font color='#FF0000'>%02d</font><font color='#00FF00'>:%02d</font>",
                month, day, hour, minute);

            views.setTextViewText(R.id.widget_check_time, android.text.Html.fromHtml(timeText, android.text.Html.FROM_HTML_MODE_LEGACY));
            views.setTextViewTextSize(R.id.widget_check_time, android.util.TypedValue.COMPLEX_UNIT_SP, config.timeTextSize);
            views.setViewVisibility(R.id.widget_check_time, android.view.View.VISIBLE);

            Log.d("CompletedDateWidget", "有过期文件夹，显示图片数量: " + photoCount + "，检查时间: " + month + "/" + day + " " + hour + ":" + minute);
        } else {
            // 没有过期文件夹：显示"本机0个"和检查时间
            views.setViewVisibility(R.id.widget_date_text, android.view.View.GONE);

            // 使用HTML标签，"本机"和"个"为黑色，数字0为红色
            String countText = String.format(Locale.getDefault(),
                "<font color='#000000'>本机</font><font color='#FF0000'>%d</font><font color='#000000'>个</font>", photoCount);
            views.setTextViewText(R.id.widget_photo_count, android.text.Html.fromHtml(countText, android.text.Html.FROM_HTML_MODE_LEGACY));
            views.setTextViewTextSize(R.id.widget_photo_count, android.util.TypedValue.COMPLEX_UNIT_SP, config.countTextSize);
            views.setViewVisibility(R.id.widget_photo_count, android.view.View.VISIBLE);

            // 显示检查时间
            Calendar now = Calendar.getInstance();
            int month = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DAY_OF_MONTH);
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            // 使用HTML标签，日期和分钟为绿色，小时为红色
            String timeText = String.format(Locale.getDefault(),
                "<font color='#00FF00'>%d/%d </font><font color='#FF0000'>%02d</font><font color='#00FF00'>:%02d</font>",
                month, day, hour, minute);

            views.setTextViewText(R.id.widget_check_time, android.text.Html.fromHtml(timeText, android.text.Html.FROM_HTML_MODE_LEGACY));
            views.setTextViewTextSize(R.id.widget_check_time, android.util.TypedValue.COMPLEX_UNIT_SP, config.timeTextSize);
            views.setViewVisibility(R.id.widget_check_time, android.view.View.VISIBLE);

            Log.d("CompletedDateWidget", "没有过期文件夹，显示图片数量: 0，检查时间: " + month + "/" + day + " " + hour + ":" + minute);
        }

        // 设置点击事件 - 点击widget打开应用
        Intent intent = new Intent(context, MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // 更新widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * 静态方法：从外部触发所有widget的更新（直接更新，不使用广播）
     */
    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, CompletedDateWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            Log.d("CompletedDateWidget", "更新小部件，数量: " + appWidgetIds.length);

            // 直接调用更新方法，不使用广播（避免被小米系统拦截）
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
                Log.d("CompletedDateWidget", "已更新widget ID: " + appWidgetId);
            }
        } catch (Exception e) {
            Log.e("CompletedDateWidget", "更新小部件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
