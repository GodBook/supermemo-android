# 超级备忘录 (Super Memo) 📝

[![Android 16](https://img.shields.io/badge/Platform-Android%2016%20(API%2036)-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-orange.svg)]()
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)]()

一款专为 **Android 16**（API Level 36, Baklava）深度定制的现代化、高性能、注重隐私与离线体验的超级备忘录应用。采用 **Kotlin + Jetpack Compose + Material 3 + Room FTS5** 官方推荐现代化架构开发。

---

## ✨ 核心特性

### 🔍 超强模糊搜索体系 (Super Search Engine)
- **中文拼音与首字母模糊匹配**：内置轻量高效的 `PinyinEngine`，支持全拼（如输入 `chaoji`）与拼音首字母（如输入 `cjbwl`）瞬间命中“超级备忘录”。
- **SQLite FTS5 全文索引**：结合全文检索引擎，实现标题与正文毫秒级检索。
- **关键词高亮与上下文摘录**：搜索结果实时展示匹配词上下文片段，并以高亮样式着色。
- **多维度复合筛选**：支持按分组、标签、时间范围（今天/近7天/近30天）、待办状态、图片附件组合过滤。

### 📝 Markdown 混排与待办清单 (Checklist)
- **结构化富文本**：支持分级标题 (H1-H3)、粗体、斜体、删除线、行内代码、引用块。
- **待办事项互动**：自动统计完成进度（如 `3/5`），在卡片列表与正文中均可一键点击打勾或取消打勾。
- **待办工作流进化与智能编辑升级 (v1.1.4 重磅)**：
  - **待办智能回车续行与退出**：在编辑器中输入待办清单时，按回车智能自动续行 `- [ ] `，在空白待办行按回车自动清除前缀退出清单模式。
  - **待办项「就地编辑」**：长按待办事项弹窗新增【修改事项内容】对话框，就地修改、即刻保存，无需打断浏览或进入编辑页。
  - **已完成待办自动沉底**：便签快捷弹窗与编辑器右上角菜单新增一键将已完成待办移至底部，保持未完成事项始终处于视觉焦点。
  - **便签快捷换色与换组**：长按便签全功能弹窗新增【便签色彩】色盘与【所属分组】快捷调整。
- **长按弹窗核心「设置为待办」与快捷操作 (v1.1.3)**：
  - 长按任意备忘录或待办项，弹窗顶部最显眼处直接提供【设置为待办】、【标记为已完成】、【删除此事项】三大核心选项！
  - 普通备忘录一键秒转为待办事项清单，立即获得彩色呼吸微光提醒；已完成事项可一键全部重置为待办进行中。
  - 清单单项支持在弹窗内逐项设为待办、完成或删除；编辑器右上角菜单同步支持一键转为待办清单。
- **长按快捷弹窗全面修复与增强 (v1.1.2)**：彻底解决手势冲突问题，长按待办项即刻提供触感震动反馈与卡片化快捷弹窗；待办行右侧新增 `⋯` 操作按钮；长按便签卡片任意区域亦可呼出全功能操作弹窗。
- **待办呼吸微光闪烁与 UI 质感升级 (v1.1.2)**：未完成待办项具备专属呼吸微光流光与发光圆点；设置中提供 6 款活力高亮色盘并带实时动效模拟卡片；卡片边框层次与空状态视觉全面优化。
- **本地图片附件**：适配 Android 13+ 系统 PhotoPicker，图片自动安全复制至应用私有沙盒存储，杜绝原图失效。
- **防丢自动保存**：1.5 秒防抖自动持久化，离开编辑页或退后台自动保存。

### 🗂 灵活分组与多维分类
- **分组分类**：支持新建分组、自定义专属颜色标记与图标、随时重命名分组。
- **多标签系统**：一条备忘录支持关联多个标签，支持标签快速筛选。
- **生命周期保护**：支持便签置顶 (Pin)、归档箱收纳 (Archive) 以及 30 天防误删回收站 (Recycle Bin)。
- **批量多选管理**：长按卡片进入批量操作模式，支持批量移动分组、批量加标签、批量删除或归档。

### 📦 全能导入与导出体系
- **全量 ZIP 打包备份**：一键导出包含 `manifest.json`、`notes.json`、独立 `.md` 文件树以及全部高清图片资源的 `.zip` 备份包。
- **单篇导出与分享**：支持单篇导出为标准 Markdown 文件或纯文本，支持生成长图卡片分享。
- **数据恢复与智能合并**：支持从 `.zip` 或 `.json` 文件恢复数据，提供“合并导入”与“全新覆盖”两种策略。
- **外部文档快捷导入**：支持从系统文件管理器直接导入外部 `.md` 或 `.txt` 文件。

### 🔒 隐私保险箱与绝对离线
- **零网络权限（Zero Network Permission）**：应用在清单中不声明 `android.permission.INTERNET`，从物理底层彻底杜绝数据外泄与云端跟踪。
- **系统级生物识别**：支持指纹识别、人脸识别或锁屏密码，保护私密备忘录与应用启动安全。

### 🔄 GitHub Releases 在线自动更新 (v1.1.0 新增)
- **应用内一键检测**：通过直连 GitHub Releases API，自动比对语义化版本号，实时掌握最新版本动态。
- **发布日志直观呈现**：直接在应用内展示版本更新说明与更新细节。
- **断点友好带进度下载**：流式下载最新 APK，实时展示百分比与已下载 MB 数，下载完毕后平滑调起 Android 16 系统安装器完成一键升级。
- **安全与权限专有化**：网络权限**严格且仅用于**访问 GitHub 检查更新与下载安装包，备忘录笔记数据依旧 100% 留存在本地设备。

### 📱 Android 16 专属特性
- **Edge-to-Edge 边到边沉浸**：全屏避让 WindowInsets，状态栏与手势导航条全透明。
- **软键盘智能跟随**：Markdown 快捷工具栏在软键盘弹起时平滑吸附于输入法正上方。
- **Material 3 Monet 动态色彩**：跟随系统壁纸实时衍生色彩主题，提供 AMOLED 纯黑夜间模式切换。
- **Glance 桌面微件**：支持在 Android 16 桌面添加备忘录看板微件，一键快速新建记录。

---

## 🛠 技术架构

```
SuperMemo
├── app/src/main/java/com/supermemo/app/
│   ├── MainActivity.kt                  # 单 Activity 沉浸式导航容器
│   ├── SuperMemoApp.kt                  # 数据库与全局仓库初始化
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt           # Room 数据库与 FTS5 虚拟表
│   │   │   ├── entity/                  # Note, Category, Tag, FTS 实体
│   │   │   ├── dao/                     # NoteDao, CategoryDao, TagDao
│   │   │   └── model/                   # 聚合模型与查询映射
│   │   └── repository/
│   │       └── NoteRepository.kt        # 数据仓库与事务管理
│   ├── domain/
│   │   ├── engine/
│   │   │   ├── PinyinEngine.kt          # 汉字拼音与首字母模糊匹配算法
│   │   │   ├── SearchEngine.kt          # 多维复合检索与高亮摘录生成
│   │   │   ├── MarkdownParser.kt        # Checklist 提取与富文本渲染
│   │   │   ├── BackupEngine.kt          # ZIP/JSON 打包备份与解压恢复
│   │   │   └── UpdateManager.kt         # GitHub Release 在线更新与下载管理
│   │   └── model/                       # 业务传输模型与过滤条件
│   ├── ui/
│   │   ├── theme/                       # Material 3 动态色彩与主题
│   │   ├── home/                        # 首页、卡片、抽屉与批量操作栏
│   │   ├── editor/                      # 编辑器、Markdown 工具栏与附件管理
│   │   ├── search/                      # 搜索界面、历史词与结果卡片
│   │   ├── category/                    # 分组分类管理弹窗
│   │   ├── backup/                      # 导入导出与备份恢复界面
│   │   └── settings/                    # 设置与 AMOLED 切换
│   ├── util/
│   │   ├── BiometricHelper.kt           # 生物识别与凭据认证辅助类
│   │   └── ImageStorageHelper.kt        # 本地图片沙盒安全复制
│   └── widget/
│       ├── SuperMemoWidget.kt           # Android Glance 桌面小部件
│       └── SuperMemoWidgetReceiver.kt   # 小部件广播接收器
```

---

## 🚀 编译与构建

### 环境要求
- **Android SDK**: `platforms;android-36` (Android 16 / Baklava)
- **Build Tools**: `36.0.0`
- **JDK**: Java 21 / Java 24
- **Gradle**: 8.14.2+ (内置 Wrapper)

### 编译 Debug APK
```bash
./gradlew assembleDebug
```
生成的 APK 路径为：
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 协议开源。
