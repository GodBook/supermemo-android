package com.supermemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.supermemo.app.data.local.entity.ImageAttachmentEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.entity.NoteFtsEntity
import com.supermemo.app.data.local.model.NoteWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND is_archived = 0 ORDER BY is_pinned DESC, updated_at DESC")
    fun getActiveNotes(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND is_archived = 1 ORDER BY updated_at DESC")
    fun getArchivedNotes(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE is_deleted = 1 ORDER BY deleted_at DESC")
    fun getTrashNotes(): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE category_id = :categoryId AND is_deleted = 0 AND is_archived = 0 ORDER BY is_pinned DESC, updated_at DESC")
    fun getNotesByCategory(categoryId: Long): Flow<List<NoteWithDetails>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<NoteWithDetails?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdSync(id: Long): NoteWithDetails?

    @Transaction
    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getNotesByIdsSync(ids: List<Long>): List<NoteWithDetails>

    @Transaction
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteByIdPermanently(id: Long)

    @Query("UPDATE notes SET is_pinned = :isPinned WHERE id = :id")
    suspend fun setNotePinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notes SET is_archived = :isArchived WHERE id = :id")
    suspend fun setNoteArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notes SET is_deleted = :isDeleted, deleted_at = :deletedAt WHERE id = :id")
    suspend fun setNoteDeleted(id: Long, isDeleted: Boolean, deletedAt: Long? = if (isDeleted) System.currentTimeMillis() else null)

    @Query("UPDATE notes SET is_locked = :isLocked WHERE id = :id")
    suspend fun setNoteLocked(id: Long, isLocked: Boolean)

    @Query("UPDATE notes SET category_id = :categoryId WHERE id = :id")
    suspend fun updateNoteCategory(id: Long, categoryId: Long?)

    // 批量操作
    @Query("UPDATE notes SET category_id = :categoryId WHERE id IN (:noteIds)")
    suspend fun batchMoveToCategory(noteIds: List<Long>, categoryId: Long?)

    @Query("UPDATE notes SET is_deleted = :isDeleted, deleted_at = :deletedAt WHERE id IN (:noteIds)")
    suspend fun batchSetDeleted(noteIds: List<Long>, isDeleted: Boolean, deletedAt: Long? = if (isDeleted) System.currentTimeMillis() else null)

    @Query("UPDATE notes SET is_archived = :isArchived WHERE id IN (:noteIds)")
    suspend fun batchSetArchived(noteIds: List<Long>, isArchived: Boolean)

    @Query("DELETE FROM notes WHERE id IN (:noteIds)")
    suspend fun batchDeletePermanently(noteIds: List<Long>)

    @Query("DELETE FROM notes WHERE is_deleted = 1")
    suspend fun clearTrash()

    // 全文检索 (FTS) 操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFts(fts: NoteFtsEntity)

    @Query("DELETE FROM notes_fts WHERE rowid = :rowid")
    suspend fun deleteFts(rowid: Long)

    @Query("SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query")
    suspend fun searchFtsRowIds(query: String): List<Long>

    @Transaction
    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND (title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%') ORDER BY is_pinned DESC, updated_at DESC")
    suspend fun searchBySqlFuzzy(keyword: String): List<NoteWithDetails>

    // 附件操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: ImageAttachmentEntity): Long

    @Query("DELETE FROM image_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: Long)

    @Query("SELECT * FROM image_attachments WHERE note_id = :noteId")
    suspend fun getAttachmentsForNote(noteId: Long): List<ImageAttachmentEntity>
}
