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
     * 从 GitHub API 请求最新的发布版本信息
     */
    suspend fun checkLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(LATEST_RELEASE_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SuperMemo-Android-App")
                connectTimeout = 12000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("GitHub API 请求失败，状态码: $responseCode")
                )
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
                return@withContext Result.failure(Exception("最新版本发布中未找到可供下载的 APK 文件"))
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
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 下载 APK 文件到应用私有缓存目录，并回调实时进度
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val destinationFile = File(updatesDir, fileName)

            var currentUrl = downloadUrl
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
                        return@withContext Result.failure(Exception("重定向次数过多"))
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

            Result.success(destinationFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
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
