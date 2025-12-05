# Photo Gallery App

一个功能丰富的安卓图库应用，支持按日期组织照片。

## 功能特点

### 1. 所有图片浏览
- 显示设备上的所有图片
- 网格视图展示，方便浏览
- 点击图片可进入全屏查看模式

### 2. 日期文件夹
- 今日日期文件夹：显示添加到今天日期的图片
- 历史日期文件夹：显示之前添加的其他日期的图片
- 日期格式：YYYY-MM-DD（例如：2025-01-15）

### 3. 全屏图片查看
- 左右滑动浏览图片
- 点击屏幕显示/隐藏控制栏
- 显示当前图片位置（例如：3 / 10）

### 4. 添加到未来日期
- 在全屏查看时，点击"添加到3天后"按钮
- 图片会被添加到3天后日期的文件夹
- 例如：今天是 2025-01-15，点击后图片会被添加到 2025-01-18 文件夹

## 技术实现

### 项目结构
```
PhotoGalleryApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/photogallery/
│   │   │   ├── MainActivity.java          # 主界面（文件夹列表）
│   │   │   ├── GalleryActivity.java       # 图片网格视图
│   │   │   ├── ImageViewerActivity.java   # 全屏图片查看
│   │   │   ├── Photo.java                 # 图片数据模型
│   │   │   ├── Folder.java                # 文件夹数据模型
│   │   │   ├── PhotoManager.java          # 图片扫描管理
│   │   │   ├── DateFolderManager.java     # 日期文件夹管理
│   │   │   ├── FolderAdapter.java         # 文件夹列表适配器
│   │   │   ├── PhotoAdapter.java          # 图片网格适配器
│   │   │   └── ImagePagerAdapter.java     # 全屏查看适配器
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_gallery.xml
│   │   │   │   ├── activity_image_viewer.xml
│   │   │   │   ├── item_folder.xml
│   │   │   │   └── item_photo.xml
│   │   │   └── values/
│   │   │       └── strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

### 核心技术栈
- **Android SDK**: 最低支持 API 24 (Android 7.0)
- **UI组件**:
  - RecyclerView: 文件夹和图片列表
  - ViewPager2: 全屏图片滑动
  - CardView: 卡片式界面
- **图片加载**: Glide 4.16.0
- **数据存储**: SharedPreferences + Gson (存储日期文件夹映射)
- **权限**: READ_MEDIA_IMAGES (Android 13+) / READ_EXTERNAL_STORAGE (旧版本)

### 数据管理

#### PhotoManager
- 扫描设备上的所有图片
- 使用 MediaStore API 获取图片信息
- 返回 Photo 对象列表（包含路径、名称、添加日期、大小）

#### DateFolderManager
- 使用 SharedPreferences 存储日期到图片路径的映射
- 支持添加/删除图片到特定日期
- 提供日期计算工具方法（获取今日/N天后的日期）

## 安装和运行

### 前置要求
- Android Studio Arctic Fox 或更高版本
- JDK 8 或更高版本
- Android SDK (API 24+)

### 构建步骤
1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮或使用快捷键 Shift+F10

### 权限说明
应用需要以下权限：
- **READ_MEDIA_IMAGES** (Android 13+): 读取设备上的图片
- **READ_EXTERNAL_STORAGE** (Android 12 及以下): 读取外部存储

首次运行时会请求相应权限。

## 使用说明

1. **启动应用**: 授予存储权限后，应用会显示文件夹列表
2. **查看所有图片**: 点击"所有图片"文件夹
3. **查看图片**: 点击任意图片进入全屏模式
4. **添加到未来日期**:
   - 在全屏模式下，点击"添加到3天后"按钮
   - 返回主界面，会看到新的日期文件夹
5. **查看日期文件夹**: 点击日期文件夹查看已添加的图片

## 功能扩展建议

- 添加删除图片功能
- 支持自定义天数（不只是3天）
- 支持分享图片
- 添加图片编辑功能
- 支持视频文件
- 云端同步功能

## 注意事项

- 图片数据存储在 SharedPreferences 中，卸载应用会清除所有日期文件夹数据
- 只是创建虚拟文件夹，不会移动或复制实际的图片文件
- 删除原始图片后，日期文件夹中的引用会失效

## 许可证

本项目仅供学习和参考使用。
