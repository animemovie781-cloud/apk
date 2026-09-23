package com.tom.rv2ide.build.lightweight

import android.os.Build

/**
 * Manages the toolchain required for the lightweight build process.
 */
object ToolchainManager {
    
    /**
     * @return the primary supported ABI (e.g., arm64-v8a)
     */
    fun getPrimaryAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }
    
    /**
     * Checks if all required toolchain components are present.
     */
    fun isReady(): Boolean {
        return getMissingComponents().isEmpty()
    }
    
    /**
     * Returns a list of missing toolchain components.
     */
    fun getMissingComponents(): List<String> {
        val missing = mutableListOf<String>()
        
        try {
            BuildEnvironment.getAndroidJar()
        } catch (e: Exception) {
            missing.add("android.jar (Android SDK Platform)")
        }
        
        try {
            BuildEnvironment.getAapt2()
        } catch (e: Exception) {
            missing.add("aapt2 (Android SDK Build-Tools)")
        }
        
        return missing
    }
}
