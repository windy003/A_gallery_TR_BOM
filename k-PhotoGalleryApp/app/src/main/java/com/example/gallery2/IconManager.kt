package com.example.gallery2

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class IconManager(private val context: Context) {
    companion object {
        private const val TAG = "IconManager"
        private const val PREFS_NAME = "IconManagerPrefs"
        private const val KEY_COMPLETED_DATE = "completed_date"
        private const val KEY_IS_COMPLETED = "is_completed"
    }

    private val photoManager: PhotoManager = PhotoManager(context)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 检查并更新应用状态
     * 逻辑：已过期文件夹为空 -> 已完成状态
     *      已过期文件夹有内容 -> 未完成状态
     * 注意：APP图标不再切换，只更新小部件显示
     */
    fun updateAppIcon() {
        Log.d(TAG, "开始更新应用图标状态")

        val shouldShowCompleted = isExpiredFolderEmpty()
        val isCurrentlyCompleted = getCompletedStatus()

        Log.d(TAG, "应该显示已完成: $shouldShowCompleted, 当前状态: $isCurrentlyCompleted")

        // 只在状态改变时才更新
        when {
            shouldShowCompleted && !isCurrentlyCompleted -> {
                Log.d(TAG, "状态改变: 设置为已完成")
                setCompletedStatus(true)
            }
            !shouldShowCompleted && isCurrentlyCompleted -> {
                Log.d(TAG, "状态改变: 设置为未完成")
                setCompletedStatus(false)
            }
            else -> {
                Log.d(TAG, "状态未改变")
            }
        }

        // 无论状态是否改变，都更新Widget以确保显示正确
        Log.d(TAG, "调用更新小部件方法")
        CompletedDateWidget.updateAllWidgets(context)
    }

    /**
     * 获取当前完成状态
     */
    private fun getCompletedStatus(): Boolean {
        return prefs.getBoolean(KEY_IS_COMPLETED, false)
    }

    /**
     * 获取公开的完成状态（供Widget使用）
     */
    fun isCompleted(): Boolean {
        return prefs.getBoolean(KEY_IS_COMPLETED, false)
    }

    /**
     * 检查已过期文件夹是否为空
     * 如果已过期文件夹为空，说明没有需要处理的过期照片，返回true（已完成）
     */
    private fun isExpiredFolderEmpty(): Boolean {
        val allPhotos = photoManager.getAllPhotos()

        // 遍历所有照片，检查是否有已过期的
        for (photo in allPhotos) {
            val singlePhotoList = listOf(photo)
            if (photoManager.isFolderExpired(singlePhotoList)) {
                // 发现已过期的照片，未完成
                return false
            }
        }

        // 没有已过期的照片，已完成
        return true
    }

    /**
     * 设置完成状态
     * @param completed true表示已完成，false表示未完成
     */
    private fun setCompletedStatus(completed: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_COMPLETED, completed)

        if (completed) {
            // 保存完成日期（格式: MM/dd，供小部件直接显示）
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            val currentDate = sdf.format(Date())
            editor.putString(KEY_COMPLETED_DATE, currentDate)
            Log.d(TAG, "保存完成日期: $currentDate")
        } else {
            // 未完成时清空日期
            editor.remove(KEY_COMPLETED_DATE)
            Log.d(TAG, "清空完成日期")
        }

        editor.apply()
    }
}
