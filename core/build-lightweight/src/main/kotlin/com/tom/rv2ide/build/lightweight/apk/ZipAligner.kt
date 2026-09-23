package com.tom.rv2ide.build.lightweight.apk

import com.tom.rv2ide.build.lightweight.BuildInput
import java.io.File
import com.iyxan23.zipalignjava.ZipAlign

/**
 * Handles APK zipalign process.
 */
class ZipAligner(private val input: BuildInput) {

    fun align(inputApk: File, outputApk: File): Boolean {
        input.buildLogger.logInfo("Running Zipalign...")
        input.cancellationToken.checkCancelled()

        try {
            // Using the zipalign-java library by iyxan23
            ZipAlign.alignZip(inputApk, outputApk)
            
            input.buildLogger.logInfo("Zipalign finished successfully")
            return true
        } catch (e: Exception) {
            input.buildLogger.logError("Zipalign failed: ${e.message}")
            return false
        }
    }
}
