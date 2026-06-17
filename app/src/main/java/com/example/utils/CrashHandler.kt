package com.example.utils

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            
            Log.e("CrashHandler", "CRASH DETECTED ON THREAD ${thread.name}: $stackTrace")
            
            // Save to SharedPreferences using commit() to block and force write synchronously before process exit
            val sharedPrefs = context.getSharedPreferences("nicole_crash_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("last_crash", stackTrace)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(10)
            }
        }
    }

    companion object {
        fun init(context: Context) {
            CrashHandler(context.applicationContext)
        }
        
        fun getLastCrash(context: Context): String? {
            val sharedPrefs = context.getSharedPreferences("nicole_crash_prefs", Context.MODE_PRIVATE)
            return sharedPrefs.getString("last_crash", null)
        }
        
        fun clearLastCrash(context: Context) {
            val sharedPrefs = context.getSharedPreferences("nicole_crash_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("last_crash").commit()
        }
    }
}
