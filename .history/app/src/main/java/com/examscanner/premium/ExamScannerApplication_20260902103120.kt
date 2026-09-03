package com.examscanner.premium

import android.app.Application
import com.examscanner.premium.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExamScannerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        // Seed initial data including MELCs
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabase()
        }
    }
    
    private suspend fun seedDatabase() {
        val dao = database.examDao()
        
        // Initialize MELCs on first launch
        val existingMelcs = dao.getAllMelcsSync()
        if (existingMelcs.isEmpty()) {
            val melcs = com.examscanner.premium.data.SampleMelcsData.getAllSampleMelcs()
            dao.insertMelcs(melcs)
            android.util.Log.d("ExamScannerApp", "Inserted ${melcs.size} MELCs into database")
        }
    }
}

