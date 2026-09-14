package com.supermemo.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supermemo.app.domain.engine.ReleaseInfo
import com.supermemo.app.domain.engine.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    isAmoledMode: Boolean,
    onToggleAmoledMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = "1.1.0"

    var biometricAppLock by remember { mutableStateOf(false) }

    // 检查更新状态
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // 下载状态
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置与关于", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 版本与软件在线更新卡片
            Text("软件版本与在线更新", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("当前版本：v$currentVersion", fontWeight = FontWeight.SemiBold)
                                Text("支持直接通过 GitHub Releases 在线检测与更新", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            scope.launch {
                                val result = UpdateManager.checkLatestRelease()
                                isCheckingUpdate = false
                                result.onSuccess { info ->
                                    val hasNew = UpdateManager.isNewerVersion(currentVersion, info.versionName)
                                    if (hasNew) {
                                        releaseInfo = info
                                        showUpdateDialog = true
                                    } else {
                                        Toast.makeText(context, "当前已是最新版本 (v$currentVersion)", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure { error ->
                                    Toast.makeText(context, "检查更新失败: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isCheckingUpdate && !isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("正在连接 GitHub 检测更新...")
                        } else {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("检查 GitHub 最新版本")
                        }
                    }

                    // 下载中进度条
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (downloadProgress >= 0f) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = downloadStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 个性化与外观
            Text("个性化与外观", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AMOLED 纯黑深色模式", fontWeight = FontWeight.SemiBold)
                            Text("在深色主题下使用纯黑底色，适合 OLED 屏幕极致省电", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Switch(
                        checked = isAmoledMode,
                        onCheckedChange = onToggleAmoledMode
                    )
                }
            }

            // 隐私与安全
            Text("隐私与安全", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("应用启动生物识别锁", fontWeight = FontWeight.SemiBold)
                            Text("每次打开应用时请求指纹或面部认证", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Switch(
                        checked = biometricAppLock,
                        onCheckedChange = { biometricAppLock = it }
                    )
                }
            }

            // 关于超级备忘录
            Text("关于超级备忘录", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("超级备忘录 Super Memo", fontWeight = FontWeight.Bold)
                            Text("版本 $currentVersion (Android 16 专享版)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "本应用遵循离线优先原则，所有备忘录、分类、图片附件等数据完全保存在设备本地。网络权限严格且唯一用于访问 GitHub 获取版本更新与下载安装包。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 发现新版本弹窗
    if (showUpdateDialog && releaseInfo != null) {
        val info = releaseInfo!!
        val sizeMb = String.format("%.2f", info.apkSize / (1024f * 1024f))

        AlertDialog(
            onDismissRequest = { if (!isDownloading) showUpdateDialog = false },
            title = {
                Text(
                    text = "发现新版本 ${info.tagName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("大小：$sizeMb MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("更新内容：", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (downloadProgress >= 0f) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = downloadStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                if (downloadedApkFile != null) {
                    Button(onClick = { UpdateManager.installApk(context, downloadedApkFile!!) }) {
                        Text("立即安装")
                    }
                } else {
                    Button(
                        onClick = {
                            isDownloading = true
                            downloadStatusText = "正在下载更新包..."
                            scope.launch {
                                val downloadResult = UpdateManager.downloadApk(
                                    context = context,
                                    downloadUrl = info.apkDownloadUrl,
                                    fileName = info.apkFileName
                                ) { progress, downloaded, total ->
                                    downloadProgress = progress
                                    val downloadedMb = String.format("%.1f", downloaded / (1024f * 1024f))
                                    val totalMb = String.format("%.1f", total / (1024f * 1024f))
                                    val percent = if (progress >= 0) "${(progress * 100).toInt()}%" else ""
                                    downloadStatusText = "已下载 $downloadedMb MB / $totalMb MB $percent"
                                }

                                isDownloading = false
                                downloadResult.onSuccess { apkFile ->
                                    downloadedApkFile = apkFile
                                    downloadStatusText = "下载完成，正在准备安装..."
                                    UpdateManager.installApk(context, apkFile)
                                }.onFailure { err ->
                                    downloadStatusText = "下载失败: ${err.message}"
                                    Toast.makeText(context, "下载更新失败: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        Text(if (isDownloading) "正在下载..." else "立即更新")
                    }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("稍后再说")
                    }
                }
            }
        )
    }
}
