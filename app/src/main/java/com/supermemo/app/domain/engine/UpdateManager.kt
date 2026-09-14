package com.supermemo.app.domain.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkSize: Long,
    val apkFileName: String
)

object UpdateManager {

    private const val GITHUB_OWNER = "GodBook"
    private const val GITHUB_REPO = "supermemo-android"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val LATEST_RELEASE_WEB = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // 静态 CDN 与镜像加速源（完全避免 GitHub REST API 每小时 60 次的 IP 频率限制与 403 报错）
    private val VERSION_METADATA_URLS = listOf(
        "https://cdn.jsdelivr.net/gh/$GITHUB_OWNER/$GITHUB_REPO@main/version.json",
        "https://fastly.jsdelivr.net/gh/$GITHUB_OWNER/$GITHUB_REPO@main/version.json",
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json",
        "https://raw.gitmirror.com/$GITHUB_OWNER/$GITHUB_REPO/main/version.json"
    )

    /**
     * 检查版本号是否比当前版本更新
     * 例如："1.2.0" > "1.1.0" 返回 true
     */
    fun isNewerVersion(currentVersion: String, remoteVersion: String): Boolean {
        val cleanCurrent = currentVersion.removePrefix("v").trim()
        val cleanRemote = remoteVersion.removePrefix("v").trim()

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }

        val maxParts = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxParts) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * 多级容灾检测最新版本：
     * 1. 优先从 CDN / Raw 获取 version.json（无任何 403 频率限制，全球极速响应）
     * 2. 其次通过 GitHub Web 页面 302 重定向检测最新 Tag（无 API 限制）
     * 3. 最后回退至 GitHub REST API（并妥善处理 403 限频提示）
     */
    suspend fun checkLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        // 策略 1：CDN / Raw 静态文件
        val fromMetadata = fetchFromMetadataJson()
        if (fromMetadata != null) {
            return@withContext Result.success(fromMetadata)
        }

        // 策略 2：Web 302 重定向
        val fromRedirect = fetchFromWebRedirect()
        if (fromRedirect != null) {
            return@withContext Result.success(fromRedirect)
        }

        // 策略 3：GitHub REST API
        val fromApi = fetchFromRestApi()
        if (fromApi.isSuccess) {
            return@withContext fromApi
        }

        val lastError = fromApi.exceptionOrNull()
        val errorMsg = if (lastError?.message?.contains("403") == true) {
            "GitHub 访问频率受限 (403)，镜像服务连接超时，请检查网络后重试"
        } else {
            "检测更新失败：${lastError?.message ?: "无法连接更新服务器，请检查网络"}"
        }
        Result.failure(Exception(errorMsg))
    }

    private fun fetchFromMetadataJson(): ReleaseInfo? {
        for (mirror in VERSION_METADATA_URLS) {
            try {
                val url = URL(mirror)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "SuperMemo-Android-App")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 6000
                    readTimeout = 8000
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    val json = JSONObject(body)
                    val versionName = json.getString("versionName")
                    val tagName = json.optString("tagName", "v$versionName")
                    val title = json.optString("title", "超级备忘录 $tagName")
                    val notes = json.optString("notes", "新版本发布更新")
                    val downloadUrl = json.getString("apkDownloadUrl")
                    val fileName = json.optString("apkFileName", "SuperMemo-Android16-$tagName.apk")
                    val size = json.optLong("apkSize", 0L)
                    val publishedAt = json.optString("publishedAt", "")
                    return ReleaseInfo(
                        tagName = tagName,
                        versionName = versionName,
                        releaseTitle = title,
                        releaseNotes = notes,
                        publishedAt = publishedAt,
                        apkDownloadUrl = downloadUrl,
                        apkSize = size,
                        apkFileName = fileName
                    )
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun fetchFromWebRedirect(): ReleaseInfo? {
        try {
            val url = URL(LATEST_RELEASE_WEB)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == 307 || status == 308
            ) {
                val location = conn.getHeaderField("Location") ?: ""
                if (location.contains("/tag/")) {
                    val tagName = location.substringAfter("/tag/").trimEnd('/')
                    if (tagName.isNotBlank()) {
                        val versionName = tagName.removePrefix("v")
                        val apkFileName = "SuperMemo-Android16-$tagName.apk"
                        val downloadUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tagName/$apkFileName"
                        return ReleaseInfo(
                            tagName = tagName,
                            versionName = versionName,
                            releaseTitle = "超级备忘录 $tagName",
                            releaseNotes = "发现新版本 $tagName，点击立即更新以获取最新特性与优化。",
                            publishedAt = "",
                            apkDownloadUrl = downloadUrl,
                            apkSize = 0L,
                            apkFileName = apkFileName
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun fetchFromRestApi(): Result<ReleaseInfo> {
        return try {
            val url = URL(LATEST_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SuperMemo-Android-App")
                connectTimeout = 8000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode == 403) {
                return Result.failure(Exception("GitHub API 403 频率受限"))
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("GitHub API 请求失败，状态码: $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val json = JSONObject(responseBody)

            val tagName = json.getString("tag_name")
            val versionName = tagName.removePrefix("v")
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "暂无更新日志")
            val publishedAt = json.optString("published_at", "")

            val assets = json.optJSONArray("assets")
            var downloadUrl: String? = null
            var apkSize = 0L
            var apkFileName = "SuperMemo-$versionName.apk"

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.getString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        apkFileName = name
                        break
                    }
                }
            }

            if (downloadUrl.isNullOrBlank()) {
                return Result.failure(Exception("最新版本发布中未找到可供下载的 APK 文件"))
            }

            Result.success(
                ReleaseInfo(
                    tagName = tagName,
                    versionName = versionName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt,
                    apkDownloadUrl = downloadUrl,
                    apkSize = apkSize,
                    apkFileName = apkFileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 下载 APK 文件到应用私有缓存目录，并回调实时进度（支持镜像源容灾加速）
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidateUrls = mutableListOf(downloadUrl)
        if (downloadUrl.startsWith("https://github.com/")) {
            candidateUrls.add("https://ghfast.top/$downloadUrl")
        }

        var lastException: Exception? = null
        for (targetUrl in candidateUrls) {
            val result = executeDownload(context, targetUrl, fileName, onProgress)
            if (result.isSuccess) {
                return@withContext result
            }
            lastException = result.exceptionOrNull() as? Exception
        }

        Result.failure(lastException ?: Exception("下载更新安装包失败，请检查网络设置"))
    }

    private fun executeDownload(
        context: Context,
        targetUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val destinationFile = File(updatesDir, fileName)

            var currentUrl = targetUrl
            var connection: HttpURLConnection
            var redirectCount = 0

            // 循环处理 GitHub Releases 资产重定向 (302 -> AWS S3)
            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "SuperMemo-Android-App")
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newUrl = connection.getHeaderField("Location")
                    currentUrl = newUrl
                    redirectCount++
                    if (redirectCount > 5) {
                        return Result.failure(Exception("重定向次数过多"))
                    }
                    continue
                }
                break
            }

            val totalBytes = connection.contentLengthLong
            val inputStream = BufferedInputStream(connection.inputStream)
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedBytes = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val progress = if (totalBytes > 0) {
                    downloadedBytes.toFloat() / totalBytes.toFloat()
                } else {
                    -1f
                }
                onProgress(progress, downloadedBytes, totalBytes)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return Result.success(destinationFile)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * 调起 Android 16 系统安装器完成 APK 安装更新
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        // 检查 Android 8.0+ 的“安装未知应用”权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        // 使用 FileProvider 生成安全 content:// URI
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(installIntent)
    }
}
