package com.supermemo.app.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.entity.ImageAttachmentEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.data.repository.NoteRepository
import com.supermemo.app.domain.engine.BackupEngine
import com.supermemo.app.domain.engine.MarkdownParser
import com.supermemo.app.util.ImageStorageHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditNoteUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val categoryId: Long? = null,
    val tagNames: List<String> = emptyList(),
    val attachments: List<ImageAttachmentEntity> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val colorHex: String? = null,
    val isPreviewMode: Boolean = false,
    val wordCount: Int = 0,
    val isSaved: Boolean = true
)

class EditNoteViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditNoteUiState(noteId = initialNoteId))
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var autoSaveJob: Job? = null

    init {
        if (initialNoteId > 0L) {
            loadNote(initialNoteId)
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val details = repository.getNoteByIdSync(id)
            if (details != null) {
                _uiState.value = EditNoteUiState(
                    noteId = details.note.id,
                    title = details.note.title,
                    content = details.note.content,
                    categoryId = details.note.categoryId,
                    tagNames = details.tags.map { it.name },
                    attachments = details.attachments,
                    isPinned = details.note.isPinned,
                    isLocked = details.note.isLocked,
                    colorHex = details.note.colorHex,
                    wordCount = details.note.content.length,
                    isSaved = true
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle, isSaved = false)
        scheduleAutoSave()
    }

    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(
            content = newContent,
            wordCount = newContent.length,
            isSaved = false
        )
        scheduleAutoSave()
    }

    fun convertToTodoList() {
        val currentContent = _uiState.value.content
        val newContent = MarkdownParser.convertContentToTodoList(currentContent, _uiState.value.title)
        updateContent(newContent)
    }

    fun setAllChecklistStatus(isCompleted: Boolean) {
        val currentContent = _uiState.value.content
        val newContent = MarkdownParser.setAllChecklistStatus(currentContent, isCompleted, _uiState.value.title)
        updateContent(newContent)
    }

    fun setCategory(catId: Long?) {
        _uiState.value = _uiState.value.copy(categoryId = catId, isSaved = false)
        scheduleAutoSave()
    }

    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = newPinned, isSaved = false)
        scheduleAutoSave()
    }

    fun toggleLock() {
        val newLocked = !_uiState.value.isLocked
        _uiState.value = _uiState.value.copy(isLocked = newLocked, isSaved = false)
        scheduleAutoSave()
    }

    fun setColorHex(color: String?) {
        _uiState.value = _uiState.value.copy(colorHex = color, isSaved = false)
        scheduleAutoSave()
    }

    fun togglePreviewMode() {
        _uiState.value = _uiState.value.copy(isPreviewMode = !_uiState.value.isPreviewMode)
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotBlank() && !_uiState.value.tagNames.contains(trimmed)) {
            val updatedTags = _uiState.value.tagNames + trimmed
            _uiState.value = _uiState.value.copy(tagNames = updatedTags, isSaved = false)
            scheduleAutoSave()
        }
    }

    fun removeTag(tag: String) {
        val updatedTags = _uiState.value.tagNames.filter { it != tag }
        _uiState.value = _uiState.value.copy(tagNames = updatedTags, isSaved = false)
        scheduleAutoSave()
    }

    fun addImageAttachment(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = ImageStorageHelper.saveImageToInternalStorage(context, uri)
            if (result != null) {
                val (relativePath, fileSize) = result
                val newAttachment = ImageAttachmentEntity(
                    noteId = _uiState.value.noteId,
                    relativePath = relativePath,
                    fileName = uri.lastPathSegment ?: "image.jpg",
                    fileSize = fileSize
                )
                val updatedAttachments = _uiState.value.attachments + newAttachment
                _uiState.value = _uiState.value.copy(attachments = updatedAttachments, isSaved = false)
                saveNoteImmediately()
            }
        }
    }

    fun removeAttachment(attachment: ImageAttachmentEntity) {
        viewModelScope.launch {
            if (attachment.id > 0L) {
                repository.deleteAttachment(attachment.id)
            }
            val updatedAttachments = _uiState.value.attachments.filter { it.id != attachment.id && it.relativePath != attachment.relativePath }
            _uiState.value = _uiState.value.copy(attachments = updatedAttachments, isSaved = false)
            scheduleAutoSave()
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500) // 1.5秒防抖自动保存
            saveNoteImmediately()
        }
    }

    fun saveNoteImmediately() {
        val state = _uiState.value
        // 若标题正文均为空且没有图片，则不保存空白草稿
        if (state.title.isBlank() && state.content.isBlank() && state.attachments.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val noteEntity = NoteEntity(
                id = state.noteId,
                title = state.title,
                content = state.content,
                categoryId = state.categoryId,
                isPinned = state.isPinned,
                isArchived = false,
                isDeleted = false,
                isLocked = state.isLocked,
                colorHex = state.colorHex
            )
            val savedId = repository.saveNote(
                note = noteEntity,
                tagNames = state.tagNames,
                attachments = state.attachments
            )
            _uiState.value = _uiState.value.copy(noteId = savedId, isSaved = true)
        }
    }

    fun getExportMarkdown(): String {
        val state = _uiState.value
        val catName = categories.value.find { it.id == state.categoryId }?.name
        return buildString {
            appendLine("# ${state.title.ifBlank { "备忘录" }}")
            if (!catName.isNullOrBlank()) appendLine("> 分类: $catName")
            if (state.tagNames.isNotEmpty()) appendLine("> 标签: [${state.tagNames.joinToString(", ")}]")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(state.content)
        }
    }
}
