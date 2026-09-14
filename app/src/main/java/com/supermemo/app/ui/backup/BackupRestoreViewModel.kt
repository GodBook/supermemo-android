package com.supermemo.app.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.data.repository.NoteRepository
import com.supermemo.app.domain.engine.BackupEngine
import com.supermemo.app.domain.model.FullBackupData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class BackupStats(
    val totalNotes: Int = 0,
    val totalCategories: Int = 0,
    val totalAttachments: Int = 0
)

class BackupRestoreViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    val allNotes: StateFlow<List<NoteWithDetails>> = repository.getActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun exportToZip(context: Context, destUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val notes = repository.getAllNotesSync()
            val backupData = BackupEngine.buildBackupData(notes)
            val success = BackupEngine.exportToZip(context, destUri, backupData, context.filesDir)
            _isLoading.value = false
            _statusMessage.value = if (success) "全量 ZIP 备份导出成功！" else "备份导出失败，请重试"
        }
    }

    fun exportToJson(context: Context, destUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val notes = repository.getAllNotesSync()
                val backupData = BackupEngine.buildBackupData(notes)
                val jsonStr = BackupEngine.toJsonString(backupData)
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                _statusMessage.value = "JSON 备份导出成功！"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "JSON 备份导出失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importBackup(context: Context, sourceUri: Uri, mergeMode: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mimeType = context.contentResolver.getType(sourceUri)
                val isZip = mimeType?.contains("zip") == true || sourceUri.toString().endsWith(".zip")

                val backupData: FullBackupData? = if (isZip) {
                    BackupEngine.importFromZip(context, sourceUri, context.filesDir)
                } else {
                    val jsonStr = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.readText()
                    if (jsonStr != null) BackupEngine.parseFromJson(jsonStr) else null
                }

                if (backupData != null) {
                    repository.restoreFromBackup(backupData, mergeMode)
                    _statusMessage.value = "成功恢复 ${backupData.notes.size} 条备忘录与 ${backupData.categories.size} 个分组！"
                } else {
                    _statusMessage.value = "解析备份文件失败，请确保格式正确"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "恢复失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importExternalTextFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "导入文件"
                val title = fileName.removeSuffix(".md").removeSuffix(".txt")
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""

                repository.saveNote(
                    note = NoteEntity(title = title, content = content),
                    tagNames = listOf("外部导入")
                )
                _statusMessage.value = "已成功导入「$title」"
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "导入文件失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
