package com.supermemo.app.domain.model

data class BackupManifest(
    val version: Int = 1,
    val appName: String = "超级备忘录",
    val exportTime: Long = System.currentTimeMillis(),
    val totalNotes: Int = 0,
    val totalCategories: Int = 0,
    val totalTags: Int = 0,
    val totalAttachments: Int = 0
)

data class CategoryDto(
    val name: String,
    val colorHex: String,
    val iconName: String,
    val sortOrder: Int
)

data class AttachmentDto(
    val fileName: String,
    val relativePath: String,
    val fileSize: Long
)

data class NoteDto(
    val title: String,
    val content: String,
    val categoryName: String?,
    val tags: List<String>,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isLocked: Boolean,
    val colorHex: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val attachments: List<AttachmentDto>
)

data class FullBackupData(
    val manifest: BackupManifest,
    val categories: List<CategoryDto>,
    val tags: List<String>,
    val notes: List<NoteDto>
)
