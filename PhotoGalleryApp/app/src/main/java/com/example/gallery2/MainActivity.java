package com.example.gallery2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private RecyclerView recyclerViewFolders;
    private FolderAdapter folderAdapter;
    private List<Folder> folders;
    private PhotoManager photoManager;
    private IconManager iconManager;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 注册GalleryActivity的结果监听器
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // GalleryActivity返回OK，说明有变化，刷新文件夹列表
                        loadFolders();
                    }
                }
        );

        recyclerViewFolders = findViewById(R.id.recyclerViewFolders);
        recyclerViewFolders.setLayoutManager(new LinearLayoutManager(this));

        photoManager = new PhotoManager(this);
        iconManager = new IconManager(this);

        if (checkPermissions()) {
            loadFolders();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasImagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
            boolean hasVideoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;
            return hasImagePermission && hasVideoPermission;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFolders();
            } else {
                Toast.makeText(this, "需要存储权限才能查看照片", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadFolders() {
        folders = new ArrayList<>();

        // 获取所有图片
        List<Photo> allPhotos = photoManager.getAllPhotos();

        // 添加"所有图片"文件夹
        Folder allPhotosFolder = new Folder("all_photos", "所有图片");
        for (Photo photo : allPhotos) {
            allPhotosFolder.addPhoto(photo);
        }

        // 使用PhotoManager的新方法：根据DATE_ADDED+3天自动分组
        java.util.Map<String, List<Photo>> photosByDate = photoManager.getPhotosByDisplayDate();

        // 为日期文件夹创建一个临时列表
        List<Folder> dateFolders = new ArrayList<>();

        for (String date : photosByDate.keySet()) {
            List<Photo> photos = photosByDate.get(date);

            if (photos != null && !photos.isEmpty()) {
                Folder folder = new Folder(date, date);
                folder.setDateFolder(true);
                for (Photo photo : photos) {
                    folder.addPhoto(photo);
                }
                dateFolders.add(folder);
            }
        }

        // 关键：根据文件夹内照片的实际日期进行排序（降序，最新的在前）
        java.util.Collections.sort(dateFolders, (f1, f2) -> {
            // 每个文件夹里的照片时间都差不多，取第一个作为代表即可
            long d1 = f1.getPhotos().get(0).getDateAdded();
            long d2 = f2.getPhotos().get(0).getDateAdded();
            return Long.compare(d1, d2); // d1 vs d2 实现升序 (最旧的在前)
        });

        // 将“所有图片”文件夹放在首位，然后添加排序后的日期文件夹
        folders.add(allPhotosFolder);
        folders.addAll(dateFolders);

        folderAdapter = new FolderAdapter(this, folders, folder -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            intent.putExtra("folder_name", folder.getName());
            intent.putExtra("folder_display_name", folder.getDisplayName());
            intent.putExtra("is_date_folder", folder.isDateFolder());
            galleryLauncher.launch(intent);
        });

        recyclerViewFolders.setAdapter(folderAdapter);

        // 更新应用图标（根据日期文件夹状态）
        iconManager.updateAppIcon();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            loadFolders();
        }
    }
}
