package com.supermemo.app

import android.app.Application
import com.supermemo.app.data.local.AppDatabase
import com.supermemo.app.data.repository.NoteRepository

class SuperMemoApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        repository = NoteRepository(database, this)
        com.supermemo.app.util.PreferenceManager.init(this)
    }

    companion object {
        lateinit var instance: SuperMemoApp
            private set
    }
}
