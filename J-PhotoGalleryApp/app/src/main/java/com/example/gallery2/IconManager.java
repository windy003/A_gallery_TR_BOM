package com.example.gallery2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IconManager {
    private static final String TAG = "IconManager";
    private static final String PREFS_NAME = "IconManagerPrefs";
    private static final String KEY_COMPLETED_DATE = "completed_date";
    private static final String KEY_IS_COMPLETED = "is_completed";

    private Context context;
    private PhotoManager photoManager;
    private SharedPreferences prefs;

    public IconManager(Context context) {
        this.context = context;
        this.photoManager = new PhotoManager(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 检查并更新应用状态
     * 逻辑：已过期文件夹为空 -> 已完成状态
     *      已过期文件夹有内容 -> 未完成状态
     * 注意：APP图标不再切换，只更新小部件显示
     */
    public void updateAppIcon() {
        Log.d(TAG, "开始更新应用图标状态");

        boolean shouldShowCompleted = isExpiredFolderEmpty();
        boolean isCurrentlyCompleted = getCompletedStatus();

        Log.d(TAG, "应该显示已完成: " + shouldShowCompleted + ", 当前状态: " + isCurrentlyCompleted);

        // 只在状态改变时才更新
        if (shouldShowCompleted && !isCurrentlyCompleted) {
            Log.d(TAG, "状态改变: 设置为已完成");
            setCompletedStatus(true);
        } else if (!shouldShowCompleted && isCurrentlyCompleted) {
            Log.d(TAG, "状态改变: 设置为未完成");
            setCompletedStatus(false);
        } else {
            Log.d(TAG, "状态未改变");
        }

        // 无论状态是否改变，都更新Widget以确保显示正确
        Log.d(TAG, "调用更新小部件方法");
        CompletedDateWidget.updateAllWidgets(context);
    }

    /**
     * 获取当前完成状态
     */
    private boolean getCompletedStatus() {
        return prefs.getBoolean(KEY_IS_COMPLETED, false);
    }

    /**
     * 获取公开的完成状态（供Widget使用）
     */
    public boolean isCompleted() {
        return prefs.getBoolean(KEY_IS_COMPLETED, false);
    }

    /**
     * 检查已过期文件夹是否为空
     * 如果已过期文件夹为空，说明没有需要处理的过期照片，返回true（已完成）
     */
    private boolean isExpiredFolderEmpty() {
        List<Photo> allPhotos = photoManager.getAllPhotos();

        // 遍历所有照片，检查是否有已过期的
        for (Photo photo : allPhotos) {
            List<Photo> singlePhotoList = new java.util.ArrayList<>();
            singlePhotoList.add(photo);
            if (photoManager.isFolderExpired(singlePhotoList)) {
                // 发现已过期的照片，未完成
                return false;
            }
        }

        // 没有已过期的照片，已完成
        return true;
    }

    /**
     * 设置完成状态
     * @param completed true表示已完成，false表示未完成
     */
    private void setCompletedStatus(boolean completed) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_COMPLETED, completed);

        if (completed) {
            // 保存完成日期（格式: MM/dd，供小部件直接显示）
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            editor.putString(KEY_COMPLETED_DATE, currentDate);
            Log.d(TAG, "保存完成日期: " + currentDate);
        } else {
            // 未完成时清空日期
            editor.remove(KEY_COMPLETED_DATE);
            Log.d(TAG, "清空完成日期");
        }

        editor.apply();
    }

    /**
     * 获取今天的日期字符串 (格式: yyyy-MM-dd)
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

}
