package com.tom.rv2ide.build.lightweight.resource

import com.tom.rv2ide.build.lightweight.*
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Handles Android resource compilation using AAPT2.
 */
class ResourceCompiler(private val input: BuildInput) {

    fun compile(): Boolean {
        val aapt2 = BuildEnvironment.getAapt2()
        val androidJar = BuildEnvironment.getAndroidJar()
        
        input.buildLogger.logInfo("Starting resource compilation with AAPT2")
        input.cancellationToken.checkCancelled()
        
        val compiledResDir = File(input.resourcesDir, "compiled")
        compiledResDir.mkdirs()
        
        val resDir = File(input.projectDir, "app/src/main/res")
        if (!resDir.exists()) {
            input.buildLogger.logInfo("No resources found at ${resDir.absolutePath}, skipping resource compilation")
            return true
        }

        // 1. AAPT2 Compile
        input.buildLogger.logInfo("Compiling resources...")
        val compileSuccess = runAapt2Compile(aapt2, resDir, compiledResDir)
        if (!compileSuccess) return false
        
        input.cancellationToken.checkCancelled()

        // 2. AAPT2 Link
        input.buildLogger.logInfo("Linking resources...")
        val linkedResDir = File(input.resourcesDir, "linked")
        linkedResDir.mkdirs()
        
        val manifestFile = File(input.projectDir, "app/src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            input.buildLogger.logError("AndroidManifest.xml not found at ${manifestFile.absolutePath}")
            return false
        }
        
        val generatedSrcDir = File(input.generatedDir, "source/r")
        generatedSrcDir.mkdirs()

        val linkSuccess = runAapt2Link(aapt2, androidJar, manifestFile, compiledResDir, linkedResDir, generatedSrcDir)
        if (!linkSuccess) return false

        input.buildLogger.logInfo("Resource compilation finished successfully")
        return true
    }

    private fun runAapt2Compile(aapt2: File, resDir: File, outputDir: File): Boolean {
        val filesToCompile = mutableListOf<File>()
        resDir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.forEach {
            filesToCompile.add(it)
        }

        if (filesToCompile.isEmpty()) return true

        // For simplicity, we compile all files at once. In a real incremental build,
        // this would be done individually or in batches.
        val command = mutableListOf(aapt2.absolutePath, "compile", "--dir", resDir.absolutePath, "-o", outputDir.absolutePath)
        
        return executeCommand(command, "AAPT2 Compile")
    }

    private fun runAapt2Link(
        aapt2: File, 
        androidJar: File, 
        manifestFile: File, 
        compiledResDir: File, 
        outputDir: File,
        generatedSrcDir: File
    ): Boolean {
        val outputFile = File(outputDir, "resources.arsc.flat")
        
        val compiledFiles = compiledResDir.listFiles()?.filter { it.isFile && it.name.endsWith(".flat") } ?: emptyList()
        
        val command = mutableListOf(
            aapt2.absolutePath, 
            "link", 
            "-I", androidJar.absolutePath,
            "--manifest", manifestFile.absolutePath,
            "--java", generatedSrcDir.absolutePath,
            "-o", outputFile.absolutePath,
            "--auto-add-overlay"
        )
        
        compiledFiles.forEach {
            command.add(it.absolutePath)
        }

        return executeCommand(command, "AAPT2 Link")
    }
    
    private fun executeCommand(command: List<String>, stageName: String): Boolean {
        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true) // merge stderr into stdout
            
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                input.cancellationToken.checkCancelled()
                // Check if it's an error based on aapt2 output format
                if (line?.contains("error:", ignoreCase = true) == true) {
                    input.buildLogger.logError(line ?: "")
                } else if (line?.isNotBlank() == true) {
                    input.buildLogger.logInfo(line ?: "")
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                input.buildLogger.logError("$stageName failed with exit code $exitCode")
                return false
            }
            return true
            
        } catch (e: BuildCancelledException) {
            throw e // Re-throw cancellation
        } catch (e: Exception) {
            input.buildLogger.logError("$stageName execution failed: ${e.message}")
            return false
        }
    }
}
