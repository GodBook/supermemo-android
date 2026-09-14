package com.supermemo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "pinyin_full")
    val pinyinFull: String,

    @ColumnInfo(name = "pinyin_initial")
    val pinyinInitial: String
)
