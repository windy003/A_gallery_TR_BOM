package com.example.gallery2;

import java.util.ArrayList;
import java.util.List;

public class Folder {
    private String name;
    private String displayName;
    private List<Photo> photos;
    private String coverPhotoPath;
    private boolean isDateFolder;

    public Folder(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.photos = new ArrayList<>();
        this.isDateFolder = false;
    }

    public void addPhoto(Photo photo) {
        photos.add(photo);
        if (coverPhotoPath == null) {
            coverPhotoPath = photo.getPath();
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public String getCoverPhotoPath() {
        return coverPhotoPath;
    }

    public int getPhotoCount() {
        return photos.size();
    }

    public void setDateFolder(boolean dateFolder) {
        isDateFolder = dateFolder;
    }

    public boolean isDateFolder() {
        return isDateFolder;
    }
}
