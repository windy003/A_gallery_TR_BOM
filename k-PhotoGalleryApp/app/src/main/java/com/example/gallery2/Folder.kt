package com.example.gallery2

class Folder(
    val name: String,
    var displayName: String
) {
    private val photos: MutableList<Photo> = ArrayList()
    private var coverPhotoPath: String? = null

    fun addPhoto(photo: Photo) {
        photos.add(photo)
        if (coverPhotoPath == null) {
            coverPhotoPath = photo.path
        }
    }

    fun getPhotos(): List<Photo> = photos

    fun getCoverPhotoPath(): String? = coverPhotoPath

    fun getPhotoCount(): Int = photos.size
}
