package com.tom.rv2ide.build.lightweight.apk

import com.tom.rv2ide.build.lightweight.BuildEnvironment
import com.tom.rv2ide.build.lightweight.BuildInput
import com.android.apksig.ApkSigner
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Handles APK signing using the apksig library.
 */
class ApkSigner(private val input: BuildInput) {

    fun sign(inputApk: File, outputApk: File): Boolean {
        input.buildLogger.logInfo("Signing APK (debug)...")
        input.cancellationToken.checkCancelled()

        try {
            // For now, we only implement the debug signing flow.
            // A real implementation would allow custom JKS for release builds.
            val debugKeystore = getOrCreateDebugKeystore()
            
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(debugKeystore.inputStream(), "android".toCharArray())
            
            val keyAlias = "androiddebugkey"
            val keyPassword = "android".toCharArray()
            
            val privateKey = ks.getKey(keyAlias, keyPassword) as PrivateKey
            val cert = ks.getCertificate(keyAlias) as X509Certificate
            
            val signerConfig = ApkSigner.SignerConfig.Builder("debug", privateKey, listOf(cert)).build()
            
            val signer = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                
            signer.sign()
            
            input.buildLogger.logInfo("APK signed successfully")
            return true
            
        } catch (e: Exception) {
            input.buildLogger.logError("Failed to sign APK: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    private fun getOrCreateDebugKeystore(): File {
        val userHome = System.getProperty("user.home")
        val androidDir = File(userHome, ".android")
        val keystore = File(androidDir, "debug.keystore")
        
        if (!keystore.exists()) {
            input.buildLogger.logInfo("Debug keystore not found at ${keystore.absolutePath}")
            input.buildLogger.logInfo("Please ensure you have generated a debug keystore using keytool.")
            // In a complete implementation, we would generate it programmatically here if missing.
            // Using a stub fallback for demonstration if needed.
            throw IllegalStateException("debug.keystore missing at ~/.android/debug.keystore")
        }
        
        return keystore
    }
}
