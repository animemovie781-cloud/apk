package com.tom.rv2ide.build.lightweight

import com.tom.rv2ide.utils.Environment
import java.io.File
import java.io.FileNotFoundException

/**
 * Provides paths to all required tools for the lightweight build pipeline.
 */
object BuildEnvironment {
    
    private val androidideDir = File(Environment.HOME, ".androidide")
    private val toolsDir = File(androidideDir, "tools")
    
    // SDK paths
    private val sdkDir = File(androidideDir, "sdk")
    private val platformsDir = File(sdkDir, "platforms")
    private val buildToolsDir = File(sdkDir, "build-tools")

    // Get the latest Android platform directory
    private fun getLatestPlatformDir(): File? {
        if (!platformsDir.exists()) return null
        return platformsDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("android-") }
            ?.maxByOrNull { it.name.substringAfter("android-").toIntOrNull() ?: 0 }
    }

    // Get the latest build-tools directory
    private fun getLatestBuildToolsDir(): File? {
        if (!buildToolsDir.exists()) return null
        return buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
    }

    fun getAndroidJar(): File {
        val platformDir = getLatestPlatformDir() ?: throw FileNotFoundException("No Android platform found in SDK")
        val androidJar = File(platformDir, "android.jar")
        if (!androidJar.exists()) throw FileNotFoundException("android.jar not found at ${androidJar.absolutePath}")
        return androidJar
    }

    fun getAapt2(): File {
        val buildToolsDir = getLatestBuildToolsDir() ?: throw FileNotFoundException("No build-tools found in SDK")
        val aapt2 = File(buildToolsDir, "aapt2")
        if (!aapt2.exists()) throw FileNotFoundException("aapt2 not found at ${aapt2.absolutePath}")
        return aapt2
    }

    // Note: D8 is typically used via its Java API from the included R8/D8 dependency.
    // If a binary is needed, it would be returned here.
    fun getD8(): File {
        val buildToolsDir = getLatestBuildToolsDir() ?: throw FileNotFoundException("No build-tools found in SDK")
        val d8 = File(buildToolsDir, "d8")
        if (!d8.exists()) throw FileNotFoundException("d8 not found at ${d8.absolutePath}")
        return d8
    }

    fun getZipalign(): File {
        val buildToolsDir = getLatestBuildToolsDir() ?: throw FileNotFoundException("No build-tools found in SDK")
        val zipalign = File(buildToolsDir, "zipalign")
        // If native zipalign is not available, we use the Java zipalign implementation.
        // The Java implementation is the primary path in this project.
        return zipalign
    }

    fun getApkSigner(): File {
        val buildToolsDir = getLatestBuildToolsDir() ?: throw FileNotFoundException("No build-tools found in SDK")
        val apksigner = File(buildToolsDir, "apksigner")
        // Used primarily via Java API (apksig)
        return apksigner
    }

    fun getJdk(): File {
        val jdkDir = File(toolsDir, "jdk")
        if (!jdkDir.exists()) throw FileNotFoundException("JDK not found at ${jdkDir.absolutePath}")
        return jdkDir
    }

    fun getKotlinCompiler(): File {
        val kotlincDir = File(toolsDir, "kotlinc")
        if (!kotlincDir.exists()) throw FileNotFoundException("Kotlin compiler not found at ${kotlincDir.absolutePath}")
        return kotlincDir
    }
}
