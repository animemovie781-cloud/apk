package com.tom.rv2ide.build.lightweight.library

import com.tom.rv2ide.build.lightweight.BuildInput
import java.io.File

/**
 * Resolves libraries (JARs and AARs) required by the project.
 */
class LibraryResolver(private val input: BuildInput) {

    fun resolve(): List<File> {
        val libraries = mutableListOf<File>()
        
        input.buildLogger.logInfo("Resolving local libraries...")
        
        // 1. Scan app/libs directory
        val libsDir = File(input.projectDir, "app/libs")
        if (libsDir.exists()) {
            libsDir.listFiles()?.forEach { file ->
                if (file.extension == "jar" || file.extension == "aar") {
                    libraries.add(file)
                    input.buildLogger.logInfo("Found local library: ${file.name}")
                }
            }
        }
        
        // 2. Extract AARs
        val aars = libraries.filter { it.extension == "aar" }
        if (aars.isNotEmpty()) {
            val extractor = AarExtractor(input)
            aars.forEach { aar ->
                extractor.extract(aar)
            }
        }
        
        return libraries
    }
}
