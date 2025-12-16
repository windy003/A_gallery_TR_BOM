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
                    // 读取文件的最后修改时间
                    long lastModified = 0;
                    try {
                        java.io.File file = new java.io.File(path);
                        if (file.exists()) {
                            lastModified = file.lastModified();
                        }
                    } catch (Exception e) {
                        // 如果读取失败，使用 dateAdded 作为备用
                        lastModified = dateAdded * 1000;
                    }

                    Photo photo = new Photo(id, path, name, dateAdded, lastModified, size, Photo.TYPE_IMAGE);
                    photo.setUri(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)));
                    photos.add(photo);
                }
            }
            cursor.close();
        }

        return photos;
    }

    /**
     * 获取所有媒体文件（仅图片）
     */
    public List<Photo> getAllPhotos() {
        List<Photo> allMedia = new ArrayList<>();
        allMedia.addAll(getImages());

        // 按日期排序（最新的在前）
        allMedia.sort((p1, p2) -> Long.compare(p2.getDateAdded(), p1.getDateAdded()));

        return allMedia;
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
     * 检测是否有已到期的照片
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @return 是否有过期的照片
     */
    public boolean hasExpiredFolders() {
        List<Photo> allPhotos = getAllPhotos();

        // 获取当前时间，并减去3天
        Calendar expirationTime = Calendar.getInstance();
        expirationTime.add(Calendar.DAY_OF_YEAR, -DELAY_DAYS);

        // 将分钟、秒、毫秒设为0，按小时计算
        expirationTime.set(Calendar.MINUTE, 0);
        expirationTime.set(Calendar.SECOND, 0);
        expirationTime.set(Calendar.MILLISECOND, 0);

        for (Photo photo : allPhotos) {
            // 使用文件最后修改时间（即创建时间）
            long lastModified = photo.getLastModified();
            if (lastModified == 0) {
                // 如果 lastModified 为 0，使用 dateAdded 作为备用
                lastModified = photo.getDateAdded() * 1000;
            }

            Calendar photoTime = Calendar.getInstance();
            photoTime.setTimeInMillis(lastModified);

            // 如果照片时间 < 过期时间，说明有过期照片
            if (photoTime.before(expirationTime)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取需要阅读消化的图片数量
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @return 需要阅读消化的图片总数
     */
    public int getExpiredPhotoCount() {
        List<Photo> allPhotos = getAllPhotos();

        // 获取当前时间，并减去3天
        Calendar expirationTime = Calendar.getInstance();
        expirationTime.add(Calendar.DAY_OF_YEAR, -DELAY_DAYS);

        // 将分钟、秒、毫秒设为0，按小时计算
        expirationTime.set(Calendar.MINUTE, 0);
        expirationTime.set(Calendar.SECOND, 0);
        expirationTime.set(Calendar.MILLISECOND, 0);

        int count = 0;
        for (Photo photo : allPhotos) {
            // 使用文件最后修改时间（即创建时间）
            long lastModified = photo.getLastModified();
            if (lastModified == 0) {
                // 如果 lastModified 为 0，使用 dateAdded 作为备用
                lastModified = photo.getDateAdded() * 1000;
            }

            Calendar photoTime = Calendar.getInstance();
            photoTime.setTimeInMillis(lastModified);

            // 如果照片时间 < 过期时间，计数加1
            if (photoTime.before(expirationTime)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 判断一个文件夹是否已到期
     * 文件夹到期的定义：文件夹中至少有一张照片已经到期
     * 到期判断：照片创建时间(lastModified) < (当前时间 - 3天)，按小时计算
     * 例如：当前时间 2025/12/7 17:08，则 2025/12/4 17:00 之前创建的照片为已到期
     *
     * @param photos 文件夹中的照片列表
     * @return 文件夹是否已到期
     */
    public boolean isFolderExpired(List<Photo> photos) {
        if (photos == null || photos.isEmpty()) {
            return false;
        }

        // 获取当前时间，并减去3天
        Calendar expirationTime = Calendar.getInstance();
        expirationTime.add(Calendar.DAY_OF_YEAR, -DELAY_DAYS);

        // 将分钟、秒、毫秒设为0，按小时计算
        expirationTime.set(Calendar.MINUTE, 0);
        expirationTime.set(Calendar.SECOND, 0);
        expirationTime.set(Calendar.MILLISECOND, 0);

        for (Photo photo : photos) {
            // 使用文件最后修改时间（即创建时间）
            long lastModified = photo.getLastModified();
            if (lastModified == 0) {
                // 如果 lastModified 为 0，使用 dateAdded 作为备用
                lastModified = photo.getDateAdded() * 1000;
            }

            Calendar photoTime = Calendar.getInstance();
            photoTime.setTimeInMillis(lastModified);

            // 如果照片时间 < 过期时间，说明已到期
            if (photoTime.before(expirationTime)) {
                return true;
            }
        }

        return false;
    }
}
