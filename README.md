# NovelReaderDual 📖

**双端小说阅读器** — Compose Multiplatform (Android + Windows Desktop)

基于 Kotlin Multiplatform + Compose 构建，共享约 90% 代码，支持 Legado 兼容书源引擎。

## ✨ 功能特性

- 📚 **多格式导入** — TXT 自动编码检测 (UTF-8/GBK)，Desktop 端支持 EPUB
- 🎨 **5 套阅读皮肤** — 纸白 / 羊皮纸 / 护眼绿 / 水墨黑 / 深夜蓝
- 🌡️ **暖色温 + 亮度调节**
- 🌓 **日夜间模式** — 自动跟随系统 / 手动切换
- 📖 **自定义排版** — 字体大小、行间距、段间距、页边距
- 🔖 **书签系统** — 添加 / 查看 / 删除书签
- 🔍 **全书搜索** — 关键词定位到段落
- 💾 **阅读进度** — 自动保存，跨会话恢复
- 🌐 **Legado 书源引擎** — Rhino 1.7.15 + Jsoup，兼容 legado.getHttp/getDom
- 🔄 **书源管理** — 导入/导出 JS 书源，在线搜书，健康检测

## 🏗️ 项目结构

```
NovelReaderDual/
├── shared/          ← 核心共享代码 (~90%)
│   ├── commonMain/  # 数据模型、业务逻辑、Compose UI、书源引擎接口
│   ├── androidMain/ # Android 平台实现 (SharedPreferences, ContentResolver)
│   └── desktopMain/ # Desktop 平台实现 (JSON文件, ZipFile EPUB解析)
├── androidApp/      ← Android 壳 (ComponentActivity)
├── desktopApp/      ← Desktop 壳 (Compose Window)
└── .github/         ← CI 自动构建
```

## 🔧 技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 2.0.21 |
| Compose Multiplatform | 1.7.1 |
| AGP | 8.7.3 |
| Gradle | 8.9 |
| Rhino (JS引擎) | 1.7.15 |
| Jsoup | 1.18.3 |
| Min SDK (Android) | 26 |
| Target SDK | 35 |

## 🚀 构建

### Android
```bash
# 在 Android Studio 中打开项目根目录，运行 :androidApp
# 或命令行构建 APK
./gradlew :androidApp:assembleDebug
```

### Windows Desktop
```bash
./gradlew :desktopApp:run
# 打包 MSI/EXE
./gradlew :desktopApp:packageDistributionForCurrentOS
```

## 📦 下载

APK 通过 GitHub Actions 自动构建，前往 [Releases](https://github.com/xiaoyebaba/NovelReaderDual/releases) 下载。
