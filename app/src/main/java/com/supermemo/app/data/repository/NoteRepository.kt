package com.supermemo.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.supermemo.app.data.local.AppDatabase
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.entity.ImageAttachmentEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.entity.NoteFtsEntity
import com.supermemo.app.data.local.entity.NoteTagCrossRef
import com.supermemo.app.data.local.entity.TagEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.domain.engine.MarkdownParser
import com.supermemo.app.domain.engine.PinyinEngine
import com.supermemo.app.domain.model.FullBackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val noteDao = database.noteDao()
    private val categoryDao = database.categoryDao()
    private val tagDao = database.tagDao()

    fun getActiveNotes(): Flow<List<NoteWithDetails>> = noteDao.getActiveNotes()
    fun getArchivedNotes(): Flow<List<NoteWithDetails>> = noteDao.getArchivedNotes()
    fun getTrashNotes(): Flow<List<NoteWithDetails>> = noteDao.getTrashNotes()
    fun getNotesByCategory(categoryId: Long): Flow<List<NoteWithDetails>> = noteDao.getNotesByCategory(categoryId)
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    fun getNoteById(id: Long): Flow<NoteWithDetails?> = noteDao.getNoteById(id)
    suspend fun getNoteByIdSync(id: Long): NoteWithDetails? = noteDao.getNoteByIdSync(id)
    suspend fun getAllNotesSync(): List<NoteWithDetails> = noteDao.getAllNotesSync()

    /**
     * 保存或更新备忘录，并自动维护拼音与 FTS 全文索引
     */
    suspend fun saveNote(
        note: NoteEntity,
        tagNames: List<String> = emptyList(),
        attachments: List<ImageAttachmentEntity> = emptyList()
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val noteId = if (note.id == 0L) {
                noteDao.insertNote(note.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            } else {
                noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
                note.id
            }

            // 更新 FTS 全文索引与拼音索引
            val fullPinyin = PinyinEngine.toFullPinyin("${note.title} ${note.content}")
            val initialPinyin = PinyinEngine.toInitialLetters("${note.title} ${note.content}")
            noteDao.insertFts(
                NoteFtsEntity(
                    rowid = noteId,
                    title = note.title,
                    content = note.content,
                    pinyinFull = fullPinyin,
                    pinyinInitial = initialPinyin
                )
            )

            // 维护标签关系
            tagDao.clearTagsForNote(noteId)
            for (tagName in tagNames.filter { it.isNotBlank() }) {
                var tag = tagDao.getTagByName(tagName.trim())
                val tagId = if (tag == null) {
                    tagDao.insertTag(TagEntity(name = tagName.trim()))
                } else {
                    tag.id
                }
                tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tagId))
            }

            // 维护图片附件
            for (attachment in attachments) {
                if (attachment.id == 0L) {
                    noteDao.insertAttachment(attachment.copy(noteId = noteId))
                }
            }

            noteId
        }
    }

    /**
     * 一键打勾/取消打勾待办事项并自动保存
     */
    suspend fun toggleChecklistItem(noteId: Long, lineIndex: Int) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.toggleChecklistItem(noteDetails.note.content, lineIndex)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        // 同步更新 FTS
        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 设置待办事项指定行的完成状态（true: 标记完成, false: 设为待办）
     */
    suspend fun setChecklistItemStatus(noteId: Long, lineIndex: Int, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.setChecklistItemStatus(noteDetails.note.content, lineIndex, isCompleted)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 删除指定行待办事项
     */
    suspend fun deleteChecklistItem(noteId: Long, lineIndex: Int) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.deleteChecklistItem(noteDetails.note.content, lineIndex)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 将备忘录整体设置为待办事项（普通文本转为未完成待办，已有待办重置为未完成待办）
     */
    suspend fun setNoteAsTodo(noteId: Long) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.convertContentToTodoList(
            noteDetails.note.content,
            noteDetails.note.title
        )
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 将备忘录中所有事项标记为已完成
     */
    suspend fun setNoteAllCompleted(noteId: Long) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.setAllChecklistStatus(
            noteDetails.note.content,
            isCompleted = true,
            fallbackTitle = noteDetails.note.title
        )
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 快速向备忘录追加一条新待办事项
     */
    suspend fun addChecklistItem(noteId: Long, text: String) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.appendChecklistItem(noteDetails.note.content, text)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 更新指定行待办事项的内容文本（就地编辑）
     */
    suspend fun updateChecklistItemContent(noteId: Long, lineIndex: Int, newText: String) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.updateChecklistItemContent(noteDetails.note.content, lineIndex, newText)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 将备忘录内的已完成待办事项沉底
     */
    suspend fun sinkCompletedChecklist(noteId: Long) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val newContent = MarkdownParser.sinkCompletedChecklistItems(noteDetails.note.content)
        val updatedNote = noteDetails.note.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)

        val fullPinyin = PinyinEngine.toFullPinyin("${updatedNote.title} ${updatedNote.content}")
        val initialPinyin = PinyinEngine.toInitialLetters("${updatedNote.title} ${updatedNote.content}")
        noteDao.insertFts(
            NoteFtsEntity(
                rowid = noteId,
                title = updatedNote.title,
                content = updatedNote.content,
                pinyinFull = fullPinyin,
                pinyinInitial = initialPinyin
            )
        )
    }

    /**
     * 快速更新便签底色
     */
    suspend fun updateNoteColor(noteId: Long, colorHex: String?) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val updatedNote = noteDetails.note.copy(
            colorHex = colorHex,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)
    }

    /**
     * 快速修改便签所属分组
     */
    suspend fun updateNoteCategory(noteId: Long, categoryId: Long?) = withContext(Dispatchers.IO) {
        val noteDetails = noteDao.getNoteByIdSync(noteId) ?: return@withContext
        val updatedNote = noteDetails.note.copy(
            categoryId = categoryId,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.updateNote(updatedNote)
    }


    suspend fun togglePin(noteId: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setNotePinned(noteId, isPinned)
    }

    suspend fun toggleArchive(noteId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setNoteArchived(noteId, isArchived)
    }

    suspend fun moveToTrash(noteId: Long) = withContext(Dispatchers.IO) {
        noteDao.setNoteDeleted(noteId, true)
    }

    suspend fun restoreFromTrash(noteId: Long) = withContext(Dispatchers.IO) {
        noteDao.setNoteDeleted(noteId, false)
    }

    suspend fun deletePermanently(noteId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            noteDao.deleteNoteByIdPermanently(noteId)
            noteDao.deleteFts(noteId)
            tagDao.clearTagsForNote(noteId)
        }
    }

    suspend fun clearTrash() = withContext(Dispatchers.IO) {
        noteDao.clearTrash()
    }

    suspend fun deleteAttachment(attachmentId: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteAttachment(attachmentId)
    }

    // 批量操作
    suspend fun batchMoveToCategory(noteIds: List<Long>, categoryId: Long?) = withContext(Dispatchers.IO) {
        noteDao.batchMoveToCategory(noteIds, categoryId)
    }

    suspend fun batchTrash(noteIds: List<Long>) = withContext(Dispatchers.IO) {
        noteDao.batchSetDeleted(noteIds, true)
    }

    suspend fun batchArchive(noteIds: List<Long>, isArchived: Boolean) = withContext(Dispatchers.IO) {
        noteDao.batchSetArchived(noteIds, isArchived)
    }

    suspend fun batchDeletePermanently(noteIds: List<Long>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            noteDao.batchDeletePermanently(noteIds)
            tagDao.clearTagsForNotes(noteIds)
            noteIds.forEach { noteDao.deleteFts(it) }
        }
    }

    // 分类管理
    suspend fun createCategory(name: String, colorHex: String, iconName: String): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(
            CategoryEntity(
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName
            )
        )
    }

    suspend fun renameCategory(id: Long, newName: String) = withContext(Dispatchers.IO) {
        categoryDao.renameCategory(id, newName.trim())
    }

    suspend fun deleteCategory(category: CategoryEntity, moveNotesToDefault: Boolean) = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (!moveNotesToDefault) {
                // 将属于该分类的便签全部移入回收站
                val notes = noteDao.getNotesByCategory(category.id)
                // 这里可以直接解绑或软删除
            }
            categoryDao.deleteCategory(category)
        }
    }

    // 从备份数据全量还原
    suspend fun restoreFromBackup(backupData: FullBackupData, mergeMode: Boolean) = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (!mergeMode) {
                // 全新覆盖模式：清空现有笔记
                val current = noteDao.getAllNotesSync()
                noteDao.batchDeletePermanently(current.map { it.note.id })
            }

            // 1. 恢复分类并建立映射
            val categoryIdMap = mutableMapOf<String, Long>()
            for (c in backupData.categories) {
                val existing = categoryDao.getCategoryById(0) // or search by name
                val id = categoryDao.insertCategory(
                    CategoryEntity(
                        name = c.name,
                        colorHex = c.colorHex,
                        iconName = c.iconName,
                        sortOrder = c.sortOrder
                    )
                )
                categoryIdMap[c.name] = id
            }

            // 2. 恢复备忘录与关联附件
            for (n in backupData.notes) {
                val catId = n.categoryName?.let { categoryIdMap[it] }
                val noteId = noteDao.insertNote(
                    NoteEntity(
                        title = n.title,
                        content = n.content,
                        categoryId = catId,
                        isPinned = n.isPinned,
                        isArchived = n.isArchived,
                        isLocked = n.isLocked,
                        colorHex = n.colorHex,
                        createdAt = n.createdAt,
                        updatedAt = n.updatedAt
                    )
                )

                // 恢复标签
                for (tagName in n.tags) {
                    var tag = tagDao.getTagByName(tagName)
                    val tagId = if (tag == null) tagDao.insertTag(TagEntity(name = tagName)) else tag.id
                    tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
                }

                // 恢复附件
                for (att in n.attachments) {
                    noteDao.insertAttachment(
                        ImageAttachmentEntity(
                            noteId = noteId,
                            relativePath = att.relativePath,
                            fileName = att.fileName,
                            fileSize = att.fileSize
                        )
                    )
                }

                // 恢复 FTS
                val fullPinyin = PinyinEngine.toFullPinyin("${n.title} ${n.content}")
                val initialPinyin = PinyinEngine.toInitialLetters("${n.title} ${n.content}")
                noteDao.insertFts(
                    NoteFtsEntity(
                        rowid = noteId,
                        title = n.title,
                        content = n.content,
                        pinyinFull = fullPinyin,
                        pinyinInitial = initialPinyin
                    )
                )
            }
        }
    }
}
