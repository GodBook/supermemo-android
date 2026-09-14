package com.supermemo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.supermemo.app.data.local.dao.CategoryDao
import com.supermemo.app.data.local.dao.NoteDao
import com.supermemo.app.data.local.dao.TagDao
import com.supermemo.app.data.local.entity.CategoryEntity
import com.supermemo.app.data.local.entity.ImageAttachmentEntity
import com.supermemo.app.data.local.entity.NoteEntity
import com.supermemo.app.data.local.entity.NoteFtsEntity
import com.supermemo.app.data.local.entity.NoteTagCrossRef
import com.supermemo.app.data.local.entity.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        ImageAttachmentEntity::class,
        NoteFtsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "super_memo.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 初始化预设分类
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.categoryDao()?.let { dao ->
                        val defaultCategories = listOf(
                            CategoryEntity(name = "默认备忘", colorHex = "#6750A4", iconName = "folder", sortOrder = 0),
                            CategoryEntity(name = "工作待办", colorHex = "#006495", iconName = "work", sortOrder = 1),
                            CategoryEntity(name = "生活日常", colorHex = "#386A20", iconName = "home", sortOrder = 2),
                            CategoryEntity(name = "灵感闪念", colorHex = "#9C4146", iconName = "lightbulb", sortOrder = 3),
                            CategoryEntity(name = "学习笔记", colorHex = "#7D5260", iconName = "book", sortOrder = 4)
                        )
                        dao.insertCategories(defaultCategories)
                    }
                }
            }
        }
    }
}
