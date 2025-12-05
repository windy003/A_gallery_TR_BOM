package com.example.gallery2;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PhotoManager {
    private Context context;
    private static final int DELAY_DAYS = 3; // 延迟天数

    public PhotoManager(Context context) {
        this.context = context;
    }

    /**
     * 从MediaStore获取所有图片
     */
    private List<Photo> getImages() {
        List<Photo> photos = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
        };

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String path = cursor.getString(pathColumn);
                String name = cursor.getString(nameColumn);
                long dateAdded = cursor.getLong(dateColumn);
                long size = cursor.getLong(sizeColumn);

                // 只扫描指定的两个目录
                if (isInTargetDirectory(path)) {
                    Photo photo = new Photo(id, path, name, dateAdded, size, Photo.TYPE_IMAGE, 0);
                    photo.setUri(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                    photos.add(photo);
                }
            }
            cursor.close();
        }

        return photos;
    }

    /**
     * 从MediaStore获取所有视频
     */
    private List<Photo> getVideos() {
        List<Photo> videos = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
        };

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String path = cursor.getString(pathColumn);
                String name = cursor.getString(nameColumn);
                long dateAdded = cursor.getLong(dateColumn);
                long size = cursor.getLong(sizeColumn);
                long duration = cursor.getLong(durationColumn);

                // 只扫描指定的两个目录
                if (isInTargetDirectory(path)) {
                    Photo video = new Photo(id, path, name, dateAdded, size, Photo.TYPE_VIDEO, duration);
                    video.setUri(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                    videos.add(video);
                }
            }
            cursor.close();
        }

        return videos;
    }

    /**
     * 获取所有媒体文件（图片+视频）
     */
    public List<Photo> getAllPhotos() {
        List<Photo> allMedia = new ArrayList<>();
        allMedia.addAll(getImages());
        allMedia.addAll(getVideos());

        // 按日期排序（最新的在前）
        allMedia.sort((p1, p2) -> Long.compare(p2.getDateAdded(), p1.getDateAdded()));

        return allMedia;
    }

    /**
     * 根据图片的 DATE_ADDED + DELAY_DAYS 对图片进行分组
     *
     * @return Map<日期字符串, 图片列表>，按日期倒序排列
     */
    public Map<String, List<Photo>> getPhotosByDisplayDate() {
        Map<String, List<Photo>> photoMap = new LinkedHashMap<>();
        List<Photo> allPhotos = getAllPhotos();

        for (Photo photo : allPhotos) {
            String displayDate = getDisplayDate(photo.getDateAdded());

            if (!photoMap.containsKey(displayDate)) {
                photoMap.put(displayDate, new ArrayList<>());
            }
            photoMap.get(displayDate).add(photo);
        }

        return photoMap;
    }

    /**
     * 获取指定日期的所有图片
     *
     * @param dateString 日期字符串，格式：yyyy-MM-dd
     * @return 该日期对应的图片列表
     */
    public List<Photo> getPhotosForDate(String dateString) {
        List<Photo> photos = new ArrayList<>();
        List<Photo> allPhotos = getAllPhotos();

        for (Photo photo : allPhotos) {
            String displayDate = getDisplayDate(photo.getDateAdded());
            if (displayDate.equals(dateString)) {
                photos.add(photo);
            }
        }

        return photos;
    }

    /**
     * 根据图片的DATE_ADDED计算显示日期
     * 显示原始日期到+3天后的日期范围（表示阅读日期），按原始日期的小时段分组
     *
     * @param dateAddedSeconds DATE_ADDED时间戳（秒）
     * @return 日期字符串，格式：yyyy/MM/d-d HH:00-HH:00（同月）或 yyyy/MM/d-yyyy/MM/d HH:00-HH:00（跨月/年）
     */
    public String getDisplayDate(long dateAddedSeconds) {
        Calendar originalCalendar = Calendar.getInstance();
        originalCalendar.setTimeInMillis(dateAddedSeconds * 1000);

        // This calendar will be used to determine the final date, accounting for hour rollover
        Calendar targetCalendar = (Calendar) originalCalendar.clone();
        targetCalendar.add(Calendar.DAY_OF_YEAR, DELAY_DAYS);
        targetCalendar.add(Calendar.HOUR_OF_DAY, 1); // Add 1 hour to handle rollover correctly

        int originalYear = originalCalendar.get(Calendar.YEAR);
        int originalMonth = originalCalendar.get(Calendar.MONTH); // 0-11
        int originalDay = originalCalendar.get(Calendar.DAY_OF_MONTH);
        int hour = originalCalendar.get(Calendar.HOUR_OF_DAY);

        int targetYear = targetCalendar.get(Calendar.YEAR);
        int targetMonth = targetCalendar.get(Calendar.MONTH); // 0-11
        int targetDay = targetCalendar.get(Calendar.DAY_OF_MONTH);

        // The end hour for display should be 0 if the original was 23
        int displayEndHour = (hour + 1) % 24;

        // Check if the date range crosses a month/year boundary for display
        if (originalYear == targetYear && originalMonth == targetMonth) {
            return String.format(Locale.getDefault(), "%d/%d/%d-%d\n%d:00-%d:00",
                originalYear, originalMonth + 1, originalDay, targetDay, hour, displayEndHour);
        } else {
            // Full format for cross-month or cross-year
            return String.format(Locale.getDefault(), "%d/%d/%d-%d/%d/%d\n%d:00-%d:00",
                originalYear, originalMonth + 1, originalDay,
                targetYear, targetMonth + 1, targetDay, hour, displayEndHour);
        }
    }

    /**
     * 获取今天的日期字符串
     */
    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取指定天数后的日期字符串
     */
    public static String getDateAfterDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    /**
     * 获取图片应该显示的日期（用于UI显示）
     *
     * @param photo 图片对象
     * @return 日期字符串
     */
    public String getPhotoDisplayDate(Photo photo) {
        return getDisplayDate(photo.getDateAdded());
    }

    /**
     * 判断文件路径是否在目标目录中
     * 只扫描 /DCIM/Screenshots、/Pictures/Screenshots 和 /Pictures/ImageStitcher 三个目录
     *
     * @param path 文件路径
     * @return 是否在目标目录中
     */
    private boolean isInTargetDirectory(String path) {
        if (path == null) {
            return false;
        }

        // 转换为小写以进行不区分大小写的比较
        String lowerPath = path.toLowerCase();

        // 检查是否在 /DCIM/Screenshots、/Pictures/Screenshots 或 /Pictures/ImageStitcher 目录中
        return lowerPath.contains("/dcim/screenshots") ||
               lowerPath.contains("/pictures/screenshots") ||
               lowerPath.contains("/pictures/imagestitcher");
    }

    /**
     * 检测是否有阅读时间在当前时刻之前的文件夹
     * 比如现在是2025/12/5 12:11，文件夹"2025/12/2-5 11:00-12:00"的阅读时间是12/5 12:00
     * 如果当前时间 > 阅读时间的结束时间，则认为有过期文件夹
     *
     * @return 是否有过期的文件夹
     */
    public boolean hasExpiredFolders() {
        List<Photo> allPhotos = getAllPhotos();
        Calendar now = Calendar.getInstance();

        for (Photo photo : allPhotos) {
            Calendar originalCalendar = Calendar.getInstance();
            originalCalendar.setTimeInMillis(photo.getDateAdded() * 1000);

            // 计算阅读日期（原始日期 + 3天）
            Calendar readCalendar = (Calendar) originalCalendar.clone();
            readCalendar.add(Calendar.DAY_OF_YEAR, DELAY_DAYS);

            // 设置时间为原始时间的下一个小时（时间段的结束时间）
            int hour = originalCalendar.get(Calendar.HOUR_OF_DAY);
            int nextHour = hour + 1; // 移除 % 24, 让Calendar自动处理进位
            readCalendar.set(Calendar.HOUR_OF_DAY, nextHour);
            readCalendar.set(Calendar.MINUTE, 0);
            readCalendar.set(Calendar.SECOND, 0);
            readCalendar.set(Calendar.MILLISECOND, 0); // 确保毫秒为0

            // 如果当前时间 > 阅读时间的结束时间，说明有过期文件夹
            if (now.after(readCalendar)) {
                return true;
            }
        }

        return false;
    }
}
