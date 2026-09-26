package com.ministerio.jovenes

import android.app.Application
import com.ministerio.jovenes.data.local.AppDatabase
import com.ministerio.jovenes.data.repository.MinistryRepository

class MinisterioApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { MinistryRepository(database, this) }
}
