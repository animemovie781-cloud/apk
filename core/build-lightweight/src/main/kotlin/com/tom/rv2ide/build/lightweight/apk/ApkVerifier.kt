package com.tom.rv2ide.build.lightweight.apk

import com.tom.rv2ide.build.lightweight.BuildInput
import java.io.File
import java.util.zip.ZipFile

/**
 * Validates the final APK before declaring the build successful.
 */
class ApkVerifier(private val input: BuildInput) {

    fun verify(apkPath: File): Boolean {
        input.buildLogger.logInfo("Verifying APK...")
        
        if (!apkPath.exists() || !apkPath.canRead()) {
            input.buildLogger.logError("APK does not exist or is not readable: ${apkPath.absolutePath}")
            return false
        }
        
        try {
            var hasManifest = false
            var hasDex = false
            var hasResources = false
            
            ZipFile(apkPath).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    when {
                        entry.name == "AndroidManifest.xml" -> hasManifest = true
                        entry.name.endsWith(".dex") -> hasDex = true
                        entry.name == "resources.arsc" -> hasResources = true
                    }
                }
            }
            
            if (!hasManifest) {
                input.buildLogger.logError("APK verification failed: Missing AndroidManifest.xml")
                return false
            }
            
            if (!hasDex) {
                input.buildLogger.logError("APK verification failed: Missing classes.dex")
                return false
            }
            
            // In a real implementation we would also check signature validity using apksig
            // ApkVerifier.Builder(...).build().verify()
            
            input.buildLogger.logInfo("APK verification passed")
            return true
            
        } catch (e: Exception) {
            input.buildLogger.logError("APK verification failed with exception: ${e.message}")
            return false
        }
    }
}
