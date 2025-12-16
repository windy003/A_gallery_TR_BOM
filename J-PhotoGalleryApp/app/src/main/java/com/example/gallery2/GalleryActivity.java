package com.example.gallery2;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView recyclerViewPhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> photos;
    private String folderName;
    private String folderDisplayName;
    private ActivityResultLauncher<Intent> imageViewerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // 注册ImageViewerActivity的结果监听器
        imageViewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // ImageViewerActivity返回OK，说明有变化，传递给MainActivity
                        setResult(RESULT_OK);
                        loadPhotos(); // 刷新当前列表
                    }
                }
        );

        folderName = getIntent().getStringExtra("folder_name");
        folderDisplayName = getIntent().getStringExtra("folder_display_name");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderDisplayName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        loadPhotos();
    }

    private void loadPhotos() {
        photos = new ArrayList<>();
        PhotoManager photoManager = new PhotoManager(this);

        if ("expired".equals(folderName)) {
            // 加载已到期的照片
            List<Photo> allPhotos = photoManager.getAllPhotos();
            for (Photo photo : allPhotos) {
                List<Photo> singlePhotoList = new ArrayList<>();
                singlePhotoList.add(photo);
                if (photoManager.isFolderExpired(singlePhotoList)) {
                    photos.add(photo);
                }
            }
        } else {
            // 加载所有照片
            photos = photoManager.getAllPhotos();
        }

        photoAdapter = new PhotoAdapter(this, photos, position -> {
            // 打开图片查看器
            Intent intent = new Intent(GalleryActivity.this, ImageViewerActivity.class);
            intent.putExtra("photos", new ArrayList<>(photos));
            intent.putExtra("position", position);
            intent.putExtra("folder_name", folderName);
            imageViewerLauncher.launch(intent);
        });

        recyclerViewPhotos.setAdapter(photoAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }
}
