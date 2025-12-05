package com.example.gallery2;

import android.net.Uri;
import java.io.Serializable;

public class Photo implements Serializable {
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;

    private long id;
    private String path;
    private String name;
    private long dateAdded;
    private long size;
    private int mediaType; // 1=图片, 2=视频
    private long duration; // 视频时长(毫秒)
    private transient Uri uri; // transient 因为 Uri 不能直接序列化

    public Photo(long id, String path, String name, long dateAdded, long size, int mediaType, long duration) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.dateAdded = dateAdded;
        this.size = size;
        this.mediaType = mediaType;
        this.duration = duration;
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

    public boolean isVideo() {
        return mediaType == TYPE_VIDEO;
    }

    public long getDuration() {
        return duration;
    }
}
