package com.tom.rv2ide.build.lightweight.dex

import com.android.tools.r8.CompilationFailedException
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.Diagnostic
import com.android.tools.r8.DiagnosticsHandler
import com.android.tools.r8.OutputMode
import com.tom.rv2ide.build.lightweight.*
import java.io.File

/**
 * Handles DEX compilation using the D8 API.
 */
class DexCompiler(private val input: BuildInput) {

    fun compile(): Boolean {
        input.buildLogger.logInfo("Starting DEX compilation with D8")
        input.cancellationToken.checkCancelled()

        val dexDir = input.dexDir
        dexDir.mkdirs()

        val androidJar = BuildEnvironment.getAndroidJar()
        
        // 1. Gather class files (compiled by ECJ/Kotlinc)
        val programFiles = mutableListOf<File>()
        if (input.classesDir.exists()) {
            // D8 can accept directories containing class files
            programFiles.add(input.classesDir)
        }
        
        // 2. Gather library jars
        val libsDir = File(input.projectDir, "app/libs")
        if (libsDir.exists()) {
            libsDir.listFiles { file -> file.extension == "jar" }?.forEach { jar ->
                programFiles.add(jar)
            }
        }
        
        // Add extracted AAR classes.jar
        val cacheDir = File(input.cacheDir, "aars")
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.name == "classes.jar" }.forEach { jar ->
                programFiles.add(jar)
            }
        }

        if (programFiles.isEmpty()) {
            input.buildLogger.logInfo("No classes to dex, skipping.")
            return true
        }

        try {
            val mode = if (input.isDebug) CompilationMode.DEBUG else CompilationMode.RELEASE
            
            val builder = D8Command.builder(CustomDiagnosticsHandler(input.buildLogger))
                .setMode(mode)
                // In a real implementation we'd read minSdk from build.gradle or AndroidManifest
                .setMinApiLevel(21) 
                .addLibraryFiles(androidJar.toPath())
                .setOutput(dexDir.toPath(), OutputMode.DexIndexed)

            programFiles.forEach { file ->
                builder.addProgramFiles(file.toPath())
            }

            // Run D8
            input.buildLogger.logInfo("Running D8 on ${programFiles.size} inputs...")
            D8.run(builder.build())
            
            input.buildLogger.logInfo("DEX compilation finished successfully")
            return true

        } catch (e: CompilationFailedException) {
            input.buildLogger.logError("D8 compilation failed")
            // The CustomDiagnosticsHandler already logged the specific errors
            return false
        } catch (e: Exception) {
            input.buildLogger.logError("DEX compilation failed: ${e.message}")
            return false
        }
    }

    /**
     * Bridges D8 diagnostics to our BuildLogger.
     */
    private class CustomDiagnosticsHandler(private val logger: BuildLogger) : DiagnosticsHandler {
        override fun warning(diagnostic: Diagnostic) {
            logger.logWarning(diagnostic.diagnosticMessage)
        }

        override fun info(diagnostic: Diagnostic) {
            logger.logInfo(diagnostic.diagnosticMessage)
        }

        override fun error(diagnostic: Diagnostic) {
            logger.logError(diagnostic.diagnosticMessage)
        }
    }
}
