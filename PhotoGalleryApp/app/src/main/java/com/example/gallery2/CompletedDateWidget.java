package com.example.gallery2;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
        // 检测是否有过期的文件夹
        PhotoManager photoManager = new PhotoManager(context);
        boolean hasExpired = photoManager.hasExpiredFolders();

        // 统计需要阅读消化的图片数量
        int photoCount = photoManager.getExpiredPhotoCount();

        // 构建widget的RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_completed_date);

        if (hasExpired) {
            // 有过期文件夹：显示图片数量 "本机X个"
            views.setViewVisibility(R.id.widget_icon, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_date_text, android.view.View.GONE);

            String countText = String.format(Locale.getDefault(), "本机%d个", photoCount);
            views.setTextViewText(R.id.widget_photo_count, countText);
            views.setViewVisibility(R.id.widget_photo_count, android.view.View.VISIBLE);

            Log.d("CompletedDateWidget", "有过期文件夹，显示图片数量: " + photoCount);
        } else {
            // 没有过期文件夹：显示当前时间，小时为红色
            views.setViewVisibility(R.id.widget_icon, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_photo_count, android.view.View.GONE);

            // 获取当前时间
            Calendar now = Calendar.getInstance();
            int month = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DAY_OF_MONTH);
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            // 使用HTML标签，"本机"为红色，日期和分钟为绿色，小时为红色
            String timeText = String.format(Locale.getDefault(),
                "<font color='#FF0000'>本机</font><br><font color='#00FF00'>%d/%d </font><font color='#FF0000'>%02d</font><font color='#00FF00'>:%02d</font>",
                month, day, hour, minute);

            views.setTextViewText(R.id.widget_date_text, android.text.Html.fromHtml(timeText, android.text.Html.FROM_HTML_MODE_LEGACY));
            views.setViewVisibility(R.id.widget_date_text, android.view.View.VISIBLE);

            Log.d("CompletedDateWidget", "没有过期文件夹，显示时间: " + month + "/" + day + " " + hour + ":" + minute);
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
