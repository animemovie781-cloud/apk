package com.tom.rv2ide.build.lightweight.library

import com.tom.rv2ide.build.lightweight.BuildInput
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Extracts AAR files so their contents can be used during the build process.
 */
class AarExtractor(private val input: BuildInput) {

    fun extract(aarFile: File): File? {
        val aarsDir = File(input.cacheDir, "aars")
        val extractDir = File(aarsDir, aarFile.nameWithoutExtension)
        
        // Very simplistic caching for extraction
        if (extractDir.exists() && extractDir.lastModified() > aarFile.lastModified()) {
            input.buildLogger.logInfo("Using cached AAR extraction for ${aarFile.name}")
            return extractDir
        }
        
        input.buildLogger.logInfo("Extracting ${aarFile.name}...")
        
        if (extractDir.exists()) {
            extractDir.deleteRecursively()
        }
        extractDir.mkdirs()
        
        try {
            ZipFile(aarFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val outFile = File(extractDir, entry.name)
                    
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                        continue
                    }
                    
                    outFile.parentFile?.mkdirs()
                    
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            return extractDir
        } catch (e: Exception) {
            input.buildLogger.logError("Failed to extract ${aarFile.name}: ${e.message}")
            return null
        }
    }
}
