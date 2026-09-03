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
        // Seed initial data
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabase()
        }
    }
    
    private suspend fun seedDatabase() {
        val dao = database.examDao()
        
        // Check if we need to seed
        val exams = dao.getAllExams()
        // Database starts empty, seed will happen on first launch
    }
}
