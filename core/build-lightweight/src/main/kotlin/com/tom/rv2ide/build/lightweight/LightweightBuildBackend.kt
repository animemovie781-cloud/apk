package com.tom.rv2ide.build.lightweight

import com.tom.rv2ide.build.lightweight.apk.ApkPackager
import com.tom.rv2ide.build.lightweight.apk.ApkSigner
import com.tom.rv2ide.build.lightweight.apk.ApkVerifier
import com.tom.rv2ide.build.lightweight.apk.ZipAligner
import com.tom.rv2ide.build.lightweight.dex.DexCompiler
import com.tom.rv2ide.build.lightweight.java.JavaCompiler
import com.tom.rv2ide.build.lightweight.kotlin.KotlinCompiler
import com.tom.rv2ide.build.lightweight.library.LibraryResolver
import com.tom.rv2ide.build.lightweight.resource.ResourceCompiler
import java.io.File
import java.lang.System.currentTimeMillis

/**
 * The core build backend that orchestrates the lightweight build pipeline.
 */
class LightweightBuildBackend : BuildBackend {
    
    // We keep a reference to the cancellation token of the currently running build
    // so we can cancel it from cancel()
    private var currentToken: BuildCancellationToken? = null
    
    override fun build(input: BuildInput): BuildResult {
        currentToken = input.cancellationToken
        val startTime = currentTimeMillis()
        val stats = BuildStats()
        
        try {
            input.buildLogger.logInfo("Starting lightweight build for ${input.projectDir.name}")
            
            // 1. Prepare
            input.prepareDirectories()
            
            // 2. Resolve Libraries
            var stageStart = currentTimeMillis()
            val libResolver = LibraryResolver(input)
            libResolver.resolve() // Unpacks AARs into cache for classpath/resources
            stats.libraryResolutionTimeMs = currentTimeMillis() - stageStart
            
            // 3. Resource Compilation (AAPT2)
            stageStart = currentTimeMillis()
            val resCompiler = ResourceCompiler(input)
            if (!resCompiler.compile()) {
                return BuildResult.failure("Resource compilation failed", currentTimeMillis() - startTime)
            }
            stats.resourceCompileTimeMs = currentTimeMillis() - stageStart
            
            // 3. Java Compilation (ECJ)
            stageStart = currentTimeMillis()
            val javaCompiler = JavaCompiler(input)
            if (!javaCompiler.compile()) {
                return BuildResult.failure("Java compilation failed", currentTimeMillis() - startTime)
            }
            stats.javaCompileTimeMs = currentTimeMillis() - stageStart
            
            // 5. Kotlin Compilation
            stageStart = currentTimeMillis()
            val kotlinCompiler = KotlinCompiler(input)
            if (!kotlinCompiler.compile()) {
                return BuildResult.failure("Kotlin compilation failed", currentTimeMillis() - startTime)
            }
            stats.kotlinCompileTimeMs = currentTimeMillis() - stageStart
            
            // 6. DEX Compilation (D8)
            stageStart = currentTimeMillis()
            val dexCompiler = DexCompiler(input)
            if (!dexCompiler.compile()) {
                return BuildResult.failure("DEX compilation failed", currentTimeMillis() - startTime)
            }
            stats.dexTimeMs = currentTimeMillis() - stageStart
            
            // 6. APK Packaging
            stageStart = currentTimeMillis()
            val packager = ApkPackager(input)
            val tempApk = packager.packageApk()
            if (tempApk == null || !tempApk.exists()) {
                return BuildResult.failure("APK packaging failed", currentTimeMillis() - startTime)
            }
            stats.packageTimeMs = currentTimeMillis() - stageStart
            
            // 7. APK Signing & Zipalign
            stageStart = currentTimeMillis()
            val signer = ApkSigner(input)
            val aligner = ZipAligner(input)
            
            val alignedApk = File(input.tempDir, "aligned.apk")
            if (!aligner.align(tempApk, alignedApk)) {
                return BuildResult.failure("Zipalign failed", currentTimeMillis() - startTime)
            }
            
            val finalApk = input.outputApk
            if (!signer.sign(alignedApk, finalApk)) {
                return BuildResult.failure("APK signing failed", currentTimeMillis() - startTime)
            }
            stats.signTimeMs = currentTimeMillis() - stageStart
            
            // 8. Verify
            val verifier = ApkVerifier(input)
            if (!verifier.verify(finalApk)) {
                return BuildResult.failure("APK verification failed", currentTimeMillis() - startTime)
            }
            
            stats.totalTimeMs = currentTimeMillis() - startTime
            input.buildLogger.logInfo("Build completed successfully in ${stats.totalTimeMs}ms")
            input.buildLogger.logInfo("Output: ${finalApk.absolutePath}")
            
            return BuildResult.success(finalApk, stats.totalTimeMs, stats)
            
        } catch (e: BuildCancelledException) {
            input.buildLogger.logWarning("Build was cancelled.")
            return BuildResult.cancelled(currentTimeMillis() - startTime)
        } catch (e: Exception) {
            input.buildLogger.logError("Build failed with unexpected error: ${e.message}")
            e.printStackTrace()
            return BuildResult.failure("Unexpected error: ${e.message}", currentTimeMillis() - startTime)
        } finally {
            currentToken = null
        }
    }

    override fun cancel() {
        currentToken?.cancel()
    }

    override fun supports(input: BuildInput): Boolean {
        // The lightweight backend currently supports basic Android projects.
        // It might not support complex Gradle projects with NDK, custom flavors, etc.
        // For now, if the toolchain is ready, we support it.
        return ToolchainManager.isReady()
    }

    override fun getName(): String = "Lightweight Builder"
}
