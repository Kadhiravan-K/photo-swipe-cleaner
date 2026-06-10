package com.example

import android.app.Application
import com.example.data.PhotoDatabase
import com.example.repository.PhotoRepository

class PhotoFlowApplication : Application() {
    
    val database by lazy { PhotoDatabase.getDatabase(this) }
    val repository by lazy { PhotoRepository(this, database.photoDao) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PhotoFlowApplication
            private set
    }
}
