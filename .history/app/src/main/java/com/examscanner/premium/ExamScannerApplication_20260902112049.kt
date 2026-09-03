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
        android.util.Log.d("ExamScannerApp", "Checking MELC database: ${existingMelcs.size} MELCs found")
        
        if (existingMelcs.isEmpty()) {
            val melcs = com.examscanner.premium.data.SampleMelcsData.getAllSampleMelcs()
            android.util.Log.d("ExamScannerApp", "Database empty, inserting ${melcs.size} MELCs...")
            dao.insertMelcs(melcs)
            
            // Verify insertion
            val verifyCount = dao.getAllMelcsSync().size
            android.util.Log.d("ExamScannerApp", "✅ Successfully inserted MELCs. Verified count: $verifyCount")
        } else {
            android.util.Log.d("ExamScannerApp", "✅ MELCs already in database: ${existingMelcs.size} found")
        }
    }
}

