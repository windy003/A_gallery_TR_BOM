# 故障排除指南

## 已修复的问题 ✅

### Gradle 配置错误已修复
如果你之前遇到以下错误：
```
'org.gradle.api.artifacts.Dependency org.gradle.api.artifacts.dsl.DependencyHandler.module(java.lang.Object)'
```

**这个问题已经修复！** 我已经更新了以下文件：
- `build.gradle` - 使用现代插件 DSL
- `settings.gradle` - 添加了仓库配置
- `gradle/wrapper/gradle-wrapper.properties` - 配置 Gradle 版本

## 常见问题和解决方案

### 1. Gradle 同步失败

#### 问题：无法下载依赖
**解决方案：**
```bash
# 清理 Gradle 缓存
cd PhotoGalleryApp
./gradlew clean --refresh-dependencies

# Windows 使用：
gradlew.bat clean --refresh-dependencies
```

#### 问题：网络连接问题
**解决方案：**
如果在中国大陆，可能需要配置镜像。编辑 `settings.gradle`：

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        google()
        mavenCentral()
    }
}
```

### 2. Android SDK 未找到

#### 错误信息：
```
SDK location not found. Define location with sdk.dir in the local.properties file
```

**解决方案：**
1. 打开 Android Studio，它会自动创建 `local.properties`
2. 或手动创建文件：
   ```properties
   # Windows
   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk

   # macOS
   sdk.dir=/Users/你的用户名/Library/Android/sdk

   # Linux
   sdk.dir=/home/你的用户名/Android/Sdk
   ```

### 3. 构建工具版本问题

#### 错误信息：
```
Failed to find Build Tools revision 34.0.0
```

**解决方案：**
在 Android Studio 中：
1. Tools → SDK Manager
2. SDK Tools 标签页
3. 勾选 "Android SDK Build-Tools 34"
4. 点击 Apply 安装

### 4. 权限问题

#### 在 Android 13+ 上无法读取图片
**解决方案：**
- 确保在 AndroidManifest.xml 中有 `READ_MEDIA_IMAGES` 权限
- 应用会自动请求权限，请点击"允许"

#### 在旧版本 Android 上无法读取图片
**解决方案：**
- 确保授予了 `READ_EXTERNAL_STORAGE` 权限
- 在系统设置 → 应用 → PhotoGallery → 权限中手动授权

### 5. 图片不显示

#### 问题：RecyclerView 是空的
**检查清单：**
1. ✅ 已授予存储权限
2. ✅ 设备上有图片文件
3. ✅ 图片位于 MediaStore 可访问的位置

**解决方案：**
```java
// 在 MainActivity.java 中添加日志查看
Log.d("PhotoGallery", "Total photos found: " + allPhotos.size());
```

### 6. Gradle Wrapper 无法执行

#### Windows 错误：
```
'gradlew' is not recognized as an internal or external command
```

**解决方案：**
```bash
# 使用完整命令
gradlew.bat assembleDebug

# 或者给予执行权限并运行
.\gradlew.bat assembleDebug
```

#### Linux/Mac 错误：
```
Permission denied: ./gradlew
```

**解决方案：**
```bash
# 给予执行权限
chmod +x gradlew

# 然后运行
./gradlew assembleDebug
```

### 7. ViewBinding 错误

#### 错误信息：
```
Cannot resolve symbol 'ActivityMainBinding'
```

**解决方案：**
1. Build → Clean Project
2. Build → Rebuild Project
3. File → Invalidate Caches / Restart
4. 重启 Android Studio

### 8. Glide 图片加载失败

#### 问题：图片显示为灰色方块
**解决方案：**
确保在 app/build.gradle 中添加了：
```groovy
dependencies {
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

检查图片路径是否有效：
```java
File imageFile = new File(photo.getPath());
Log.d("PhotoGallery", "Image exists: " + imageFile.exists());
```

## 清理和重建项目

如果遇到奇怪的错误，尝试完全清理项目：

### 在 Android Studio 中：
1. Build → Clean Project
2. Build → Rebuild Project
3. File → Invalidate Caches / Restart → Invalidate and Restart

### 在命令行中：
```bash
# Windows
gradlew.bat clean
del /s /q .gradle
del /s /q app\build

# Linux/Mac
./gradlew clean
rm -rf .gradle
rm -rf app/build
```

然后重新构建：
```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

## 获取详细错误信息

如果问题仍然存在，使用以下命令获取详细日志：

```bash
# Windows
gradlew.bat assembleDebug --stacktrace --info

# Linux/Mac
./gradlew assembleDebug --stacktrace --info
```

## 系统要求检查

确保满足以下要求：
- ✅ Java JDK 8 或更高版本
- ✅ Android Studio Arctic Fox (2020.3.1) 或更高版本
- ✅ Android SDK API 24+ 已安装
- ✅ Android SDK Build Tools 34 已安装
- ✅ 至少 4GB RAM
- ✅ 至少 8GB 可用磁盘空间

检查 Java 版本：
```bash
java -version
```

检查 Gradle 版本：
```bash
# Windows
gradlew.bat --version

# Linux/Mac
./gradlew --version
```

## 仍然需要帮助？

如果以上方法都无法解决问题：

1. 检查完整的错误堆栈信息
2. 确认 Android Studio 和 SDK 都是最新版本
3. 尝试创建一个新的 Android 项目测试环境是否正常
4. 查看 Android Studio 的 Event Log（View → Tool Windows → Event Log）

## 成功运行后的验证

应用正常运行时，你应该能看到：
1. ✅ 主界面显示"所有图片"文件夹
2. ✅ 点击后显示设备上的图片网格
3. ✅ 点击图片可全屏查看
4. ✅ 全屏模式有"添加到3天后"按钮
5. ✅ 添加后主界面出现新的日期文件夹

祝你使用顺利！🎉
