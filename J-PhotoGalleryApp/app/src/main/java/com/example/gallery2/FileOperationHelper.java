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
            default:
                return "image/jpeg";
        }
    }

}
