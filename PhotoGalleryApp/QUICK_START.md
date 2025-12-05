# 快速开始指南

## 项目已创建完成！✅

### 核心功能
✅ **所有图片文件夹** - 显示设备上的所有图片
✅ **今日日期文件夹** - 格式如 2025-01-15
✅ **全屏图片查看** - 左右滑动浏览
✅ **添加到3天后** - 一键将图片添加到未来日期的文件夹

### 重要：首次设置

#### 1. 配置 Android SDK 路径（仅命令行需要）
如果使用 Android Studio，会自动配置。如果使用命令行，需要：

```bash
# 复制示例文件
cp local.properties.example local.properties

# 编辑 local.properties，设置你的 SDK 路径
# Windows: sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
# macOS: sdk.dir=/Users/YourName/Library/Android/sdk
# Linux: sdk.dir=/home/YourName/Android/Sdk
```

**或者使用 Android Studio 会自动创建此文件。**

### 如何运行

#### 方法1：使用 Android Studio（推荐）✨
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `PhotoGalleryApp` 文件夹
4. **第一次打开时**，Android Studio 会自动创建 `local.properties` 文件
5. 等待 Gradle 同步完成（可能需要几分钟下载依赖）
6. 连接 Android 设备或启动模拟器
7. 点击运行按钮 ▶️

#### 方法2：使用命令行
```bash
cd PhotoGalleryApp
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 测试流程

1. **授予权限**
   - 启动应用后会请求读取图片权限
   - 点击"允许"

2. **查看所有图片**
   - 点击"所有图片"文件夹
   - 浏览设备上的所有图片

3. **全屏查看**
   - 点击任意图片
   - 左右滑动切换图片
   - 点击屏幕显示/隐藏控制栏

4. **添加到未来日期**
   - 在全屏模式下
   - 点击"添加到3天后"按钮
   - 返回主界面
   - 查看新创建的日期文件夹

### 最低系统要求
- Android 7.0 (API 24) 或更高版本
- 约 15MB 存储空间

### 权限说明
- **Android 13+**: READ_MEDIA_IMAGES
- **Android 12-**: READ_EXTERNAL_STORAGE

### 文件说明

#### 核心 Java 文件
- `MainActivity.java` - 文件夹列表主界面
- `GalleryActivity.java` - 图片网格浏览
- `ImageViewerActivity.java` - 全屏查看和添加功能
- `DateFolderManager.java` - 管理日期文件夹逻辑

#### 布局文件
- `activity_main.xml` - 主界面布局
- `activity_gallery.xml` - 图片网格布局
- `activity_image_viewer.xml` - 全屏查看布局
- `item_folder.xml` - 文件夹列表项
- `item_photo.xml` - 图片网格项

### 常见问题

**Q: 图片不显示？**
A: 确保已授予存储权限，并且设备上有图片文件。

**Q: 日期文件夹不出现？**
A: 需要先使用"添加到3天后"功能，才会创建日期文件夹。

**Q: 卸载应用后数据会丢失吗？**
A: 是的，日期文件夹的映射关系存储在 SharedPreferences 中，卸载会清除。但原始图片不受影响。

**Q: 可以修改天数吗？**
A: 可以修改 `ImageViewerActivity.java:71` 行的数字（当前是3）。

### 下一步优化建议
- 添加删除图片功能
- 支持自定义天数选择
- 添加图片分享功能
- 支持视频文件
- 添加搜索功能
- 支持多选操作

---

**祝使用愉快！** 如有问题，请查看 README.md 获取更多详细信息。
