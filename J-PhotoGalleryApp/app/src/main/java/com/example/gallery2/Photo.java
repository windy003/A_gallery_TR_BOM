package com.example.gallery2;

import android.net.Uri;
import java.io.Serializable;

public class Photo implements Serializable {
    public static final int TYPE_IMAGE = 1;

    private long id;
    private String path;
    private String name;
    private long dateAdded;
    private long lastModified; // 文件最后修改时间(毫秒)
    private long size;
    private int mediaType; // 1=图片
    private transient Uri uri; // transient 因为 Uri 不能直接序列化

    public Photo(long id, String path, String name, long dateAdded, long size, int mediaType) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.dateAdded = dateAdded;
        this.size = size;
        this.mediaType = mediaType;
        this.lastModified = 0; // 默认值
    }

    public Photo(long id, String path, String name, long dateAdded, long lastModified, long size, int mediaType) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.dateAdded = dateAdded;
        this.lastModified = lastModified;
        this.size = size;
        this.mediaType = mediaType;
    }

    public long getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public int getMediaType() {
        return mediaType;
    }
}
