package com.supermemo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.supermemo.app.data.local.entity.NoteTagCrossRef
import com.supermemo.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(ref: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRefs(refs: List<NoteTagCrossRef>)

    @Delete
    suspend fun deleteNoteTagCrossRef(ref: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_refs WHERE note_id = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    @Query("DELETE FROM note_tag_refs WHERE note_id IN (:noteIds)")
    suspend fun clearTagsForNotes(noteIds: List<Long>)
}
