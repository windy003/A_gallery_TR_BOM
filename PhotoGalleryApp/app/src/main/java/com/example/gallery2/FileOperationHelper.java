package com.example.gallery2;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件操作助手类
 * 处理图片文件的复制和删除操作
 */
public class FileOperationHelper {
    private static final String TAG = "FileOperationHelper";
    private Context context;

    public FileOperationHelper(Context context) {
        this.context = context;
    }

    /**
     * 复制图片文件到MediaStore（创建新的副本）
     * 新文件会有新的DATE_ADDED（当前时间）
     *
     * @param sourcePhoto 源图片对象
     * @return 新图片的ID，失败返回-1
     */
    public long copyImageFile(Photo sourcePhoto) {
        try {
            // 准备新文件的元数据
            ContentValues values = new ContentValues();

            // 获取原文件名和路径信息
            File sourceFile = new File(sourcePhoto.getPath());
            String fileName = sourceFile.getName();
            String displayName = fileName;

            // 从原文件路径提取相对路径（相对于Pictures目录）
            String relativePath = extractRelativePath(sourcePhoto.getPath());

            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, getMimeType(fileName));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用相对路径
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);
                values.put(MediaStore.Images.Media.IS_PENDING, 1); // 标记为待处理
            }

            // DATE_ADDED会自动设置为当前时间（这是我们想要的！）

            ContentResolver resolver = context.getContentResolver();
            Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            // 创建新的MediaStore条目
            Uri newImageUri = resolver.insert(collection, values);

            if (newImageUri == null) {
                Log.e(TAG, "Failed to create new MediaStore entry");
                return -1;
            }

            // 复制文件内容
            try (OutputStream out = resolver.openOutputStream(newImageUri);
                 InputStream in = new FileInputStream(sourceFile)) {

                if (out == null) {
                    Log.e(TAG, "Failed to open output stream");
                    resolver.delete(newImageUri, null, null);
                    return -1;
                }

                // 复制数据
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
            }

            // Android 10+ 需要更新IS_PENDING状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(newImageUri, values, null, null);
            }

            // 获取新图片的ID
            long newImageId = ContentUris.parseId(newImageUri);
            Log.d(TAG, "Successfully copied image. New ID: " + newImageId);

            return newImageId;

        } catch (IOException e) {
            Log.e(TAG, "Error copying image file", e);
            return -1;
        }
    }

    /**
     * 从完整路径中提取相对路径
     * 例如: /storage/emulated/0/Pictures/MyFolder/image.jpg -> Pictures/MyFolder/
     */
    private String extractRelativePath(String fullPath) {
        // 尝试从路径中提取Pictures之后的部分
        if (fullPath.contains("/Pictures/")) {
            int picturesIndex = fullPath.indexOf("/Pictures/");
            String afterPictures = fullPath.substring(picturesIndex + 1);
            // 移除文件名，只保留目录路径
            int lastSlash = afterPictures.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterPictures.substring(0, lastSlash + 1);
            }
            return "Pictures/";
        } else if (fullPath.contains("/DCIM/")) {
            int dcimIndex = fullPath.indexOf("/DCIM/");
            String afterDCIM = fullPath.substring(dcimIndex + 1);
            int lastSlash = afterDCIM.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterDCIM.substring(0, lastSlash + 1);
            }
            return "DCIM/Camera/";
        }

        // 默认返回Pictures目录
        return "Pictures/";
    }

    /**
     * 从完整路径中提取视频相对路径
     * 例如: /storage/emulated/0/Movies/MyFolder/video.mp4 -> Movies/MyFolder/
     * 或者: /storage/emulated/0/Pictures/MyFolder/video.mp4 -> Pictures/MyFolder/
     */
    private String extractVideoRelativePath(String fullPath) {
        // 尝试从路径中提取Movies之后的部分
        if (fullPath.contains("/Movies/")) {
            int moviesIndex = fullPath.indexOf("/Movies/");
            String afterMovies = fullPath.substring(moviesIndex + 1);
            // 移除文件名，只保留目录路径
            int lastSlash = afterMovies.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterMovies.substring(0, lastSlash + 1);
            }
            return "Movies/";
        } else if (fullPath.contains("/Pictures/")) {
            int picturesIndex = fullPath.indexOf("/Pictures/");
            String afterPictures = fullPath.substring(picturesIndex + 1);
            int lastSlash = afterPictures.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterPictures.substring(0, lastSlash + 1);
            }
            return "Pictures/";
        } else if (fullPath.contains("/DCIM/")) {
            int dcimIndex = fullPath.indexOf("/DCIM/");
            String afterDCIM = fullPath.substring(dcimIndex + 1);
            int lastSlash = afterDCIM.lastIndexOf('/');
            if (lastSlash > 0) {
                return afterDCIM.substring(0, lastSlash + 1);
            }
            return "DCIM/Camera/";
        }

        // 默认返回Movies目录
        return "Movies/";
    }

    /**
     * 根据文件名获取MIME类型
     */
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "heic":
                return "image/heic";
            case "mp4":
                return "video/mp4";
            case "3gp":
                return "video/3gpp";
            case "mkv":
                return "video/x-matroska";
            case "webm":
                return "video/webm";
            case "avi":
                return "video/x-msvideo";
            default:
                return "image/jpeg";
        }
    }

    /**
     * 生成唯一的文件名（添加时间戳避免重名）
     */
    private String generateUniqueFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String nameWithoutExtension = originalFileName.substring(0, dotIndex);
            String extension = originalFileName.substring(dotIndex);
            long timestamp = System.currentTimeMillis();
            return nameWithoutExtension + "_" + timestamp + extension;
        }
        return originalFileName + "_" + System.currentTimeMillis();
    }

    /**
     * 复制视频文件到MediaStore（创建新的副本）
     * 新文件会有新的DATE_ADDED（当前时间）
     *
     * @param sourceUri 源视频URI
     * @param fileName 文件名
     * @return 新视频的ID，失败返回-1
     */
    public long copyVideoFile(Uri sourceUri, String fileName) {
        return copyVideoFile(sourceUri, fileName, null);
    }

    /**
     * 复制视频文件到MediaStore（创建新的副本）
     * 新文件会有新的DATE_ADDED（当前时间）
     *
     * @param sourceUri 源视频URI
     * @param fileName 文件名
     * @param sourcePath 源视频的完整路径（用于提取相对路径）
     * @return 新视频的ID，失败返回-1
     */
    public long copyVideoFile(Uri sourceUri, String fileName, String sourcePath) {
        Log.d(TAG, "=== copyVideoFile 开始 ===");
        Log.d(TAG, "sourceUri: " + sourceUri);
        Log.d(TAG, "fileName: " + fileName);
        Log.d(TAG, "sourcePath: " + sourcePath);

        try {
            // 准备新文件的元数据
            ContentValues values = new ContentValues();

            // 为文件名添加时间戳，避免重名冲突
            String uniqueFileName = generateUniqueFileName(fileName);
            Log.d(TAG, "生成的唯一文件名: " + uniqueFileName);
            values.put(MediaStore.Video.Media.DISPLAY_NAME, uniqueFileName);
            values.put(MediaStore.Video.Media.MIME_TYPE, getMimeType(fileName));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用相对路径
                String relativePath = "Movies/";
                if (sourcePath != null && !sourcePath.isEmpty()) {
                    relativePath = extractVideoRelativePath(sourcePath);
                }
                Log.d(TAG, "使用相对路径: " + relativePath);
                values.put(MediaStore.Video.Media.RELATIVE_PATH, relativePath);
                values.put(MediaStore.Video.Media.IS_PENDING, 1);
            }

            ContentResolver resolver = context.getContentResolver();
            Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

            // 创建新的MediaStore条目
            Log.d(TAG, "正在创建MediaStore条目...");
            Uri newVideoUri = resolver.insert(collection, values);

            if (newVideoUri == null) {
                Log.e(TAG, "Failed to create new MediaStore entry for video");
                return -1;
            }
            Log.d(TAG, "新视频URI: " + newVideoUri);

            // 复制文件内容
            Log.d(TAG, "正在打开输入输出流...");
            try (OutputStream out = resolver.openOutputStream(newVideoUri);
                 InputStream in = resolver.openInputStream(sourceUri)) {

                if (out == null || in == null) {
                    Log.e(TAG, "Failed to open streams for video copy. out=" + out + ", in=" + in);
                    resolver.delete(newVideoUri, null, null);
                    return -1;
                }

                // 复制数据
                Log.d(TAG, "正在复制数据...");
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                out.flush();
                Log.d(TAG, "复制完成，总字节数: " + totalBytes);
            }

            // Android 10+ 需要更新IS_PENDING状态
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Video.Media.IS_PENDING, 0);
                resolver.update(newVideoUri, values, null, null);
                Log.d(TAG, "已更新IS_PENDING状态为0");
            }

            // 获取新视频的ID
            long newVideoId = ContentUris.parseId(newVideoUri);
            Log.d(TAG, "Successfully copied video. New ID: " + newVideoId);

            return newVideoId;

        } catch (IOException e) {
            Log.e(TAG, "Error copying video file: " + e.getMessage(), e);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error copying video file: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * 删除视频文件
     *
     * @param videoUri 视频URI
     * @param videoId 视频ID
     * @param launcher ActivityResultLauncher用于权限请求
     * @param onSuccess 成功回调
     */
    public void deleteVideo(Uri videoUri, long videoId,
                           androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> launcher,
                           Runnable onSuccess) {
        ContentResolver resolver = context.getContentResolver();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: 需要请求用户权限
                try {
                    int deletedRows = resolver.delete(videoUri, null, null);
                    if (deletedRows > 0) {
                        onSuccess.run();
                    }
                } catch (SecurityException e) {
                    // 需要用户授权
                    android.app.PendingIntent pendingIntent =
                        MediaStore.createDeleteRequest(resolver,
                            java.util.Collections.singletonList(videoUri));
                    androidx.activity.result.IntentSenderRequest request =
                        new androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                    launcher.launch(request);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10: RecoverableSecurityException
                try {
                    int deletedRows = resolver.delete(videoUri, null, null);
                    if (deletedRows > 0) {
                        onSuccess.run();
                    }
                } catch (SecurityException e) {
                    if (e instanceof android.app.RecoverableSecurityException) {
                        android.app.RecoverableSecurityException recoverableException =
                            (android.app.RecoverableSecurityException) e;
                        androidx.activity.result.IntentSenderRequest request =
                            new androidx.activity.result.IntentSenderRequest.Builder(
                                recoverableException.getUserAction().getActionIntent().getIntentSender()).build();
                        launcher.launch(request);
                    }
                }
            } else {
                // Android 9及以下：直接删除
                int deletedRows = resolver.delete(videoUri, null, null);
                if (deletedRows > 0) {
                    onSuccess.run();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting video", e);
        }
    }

    /**
     * 从MediaStore删除图片
     *
     * @param photoId 图片在MediaStore中的ID
     * @return 删除结果回调（需要处理权限请求）
     */
    public interface DeleteCallback {
        void onDeleteSuccess();
        void onDeleteNeedPermission(android.app.PendingIntent pendingIntent);
        void onDeleteFailed(String error);
    }

    public void deleteImage(long photoId, DeleteCallback callback) {
        try {
            Uri photoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    photoId
            );

            ContentResolver resolver = context.getContentResolver();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: 需要请求用户权限
                try {
                    int deletedRows = resolver.delete(photoUri, null, null);
                    if (deletedRows > 0) {
                        callback.onDeleteSuccess();
                    } else {
                        callback.onDeleteFailed("Delete returned 0 rows");
                    }
                } catch (SecurityException e) {
                    // 需要用户授权
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.app.PendingIntent pendingIntent =
                            MediaStore.createDeleteRequest(resolver,
                                java.util.Collections.singletonList(photoUri));
                        callback.onDeleteNeedPermission(pendingIntent);
                    } else {
                        callback.onDeleteFailed("Permission denied");
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10: RecoverableSecurityException
                try {
                    int deletedRows = resolver.delete(photoUri, null, null);
                    if (deletedRows > 0) {
                        callback.onDeleteSuccess();
                    } else {
                        callback.onDeleteFailed("Delete returned 0 rows");
                    }
                } catch (SecurityException e) {
                    if (e instanceof android.app.RecoverableSecurityException) {
                        android.app.RecoverableSecurityException recoverableException =
                            (android.app.RecoverableSecurityException) e;
                        callback.onDeleteNeedPermission(
                            recoverableException.getUserAction().getActionIntent());
                    } else {
                        callback.onDeleteFailed("Permission denied");
                    }
                }
            } else {
                // Android 9及以下：直接删除
                int deletedRows = resolver.delete(photoUri, null, null);
                if (deletedRows > 0) {
                    callback.onDeleteSuccess();
                } else {
                    callback.onDeleteFailed("Delete returned 0 rows");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting image", e);
            callback.onDeleteFailed(e.getMessage());
        }
    }
}
