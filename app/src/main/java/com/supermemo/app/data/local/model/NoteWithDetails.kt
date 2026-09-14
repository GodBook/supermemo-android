package com.supermemo.app.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.entity.ImageAttachmentEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.entity.NoteTagCrossRef
import com.supermemo.app.data.local.entity.TagEntity

data class NoteWithDetails(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "note_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "note_id"
    )
    val attachments: List<ImageAttachmentEntity> = emptyList()
)
