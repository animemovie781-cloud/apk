package com.tom.rv2ide.build.lightweight

import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import org.json.JSONArray

/**
 * A simple incremental build cache that uses file hashing to determine
 * if a build stage needs to be re-run.
 */
class IncrementalCache(private val input: BuildInput) {
    
    private val cacheFile = File(input.cacheDir, "hashes.json")
    private val currentHashes = mutableMapOf<String, String>()
    private var savedHashes = mutableMapOf<String, String>()
    
    init {
        loadSavedHashes()
    }
    
    private fun loadSavedHashes() {
        if (!cacheFile.exists()) return
        
        try {
            val content = cacheFile.readText()
            val json = JSONObject(content)
            
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                savedHashes[key] = json.getString(key)
            }
        } catch (e: Exception) {
            input.buildLogger.logWarning("Failed to load incremental cache: ${e.message}")
        }
    }
    
    fun save() {
        try {
            input.cacheDir.mkdirs()
            val json = JSONObject(currentHashes as Map<*, *>)
            cacheFile.writeText(json.toString(2))
        } catch (e: Exception) {
            input.buildLogger.logWarning("Failed to save incremental cache: ${e.message}")
        }
    }
    
    /**
     * Checks if the contents of a directory have changed since the last build.
     */
    fun hasDirectoryChanged(stageKey: String, dir: File, extensionFilter: String? = null): Boolean {
        if (!dir.exists()) {
            val hash = "empty"
            currentHashes[stageKey] = hash
            return savedHashes[stageKey] != hash
        }
        
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            
            val files = dir.walkTopDown()
                .filter { it.isFile }
                .filter { extensionFilter == null || it.extension == extensionFilter }
                .sortedBy { it.absolutePath }
                .toList()
                
            for (file in files) {
                // Hash the relative path and last modified time as a fast heuristic
                val relativePath = file.absolutePath.substring(dir.absolutePath.length)
                digest.update(relativePath.toByteArray())
                
                // Read a chunk of the file for actual content hashing
                file.inputStream().use {
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (it.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            }
            
            val hashBytes = digest.digest()
            val hashStr = hashBytes.joinToString("") { "%02x".format(it) }
            
            currentHashes[stageKey] = hashStr
            
            return savedHashes[stageKey] != hashStr
            
        } catch (e: Exception) {
            input.buildLogger.logWarning("Hash calculation failed for $stageKey, assuming changed")
            currentHashes[stageKey] = System.currentTimeMillis().toString()
            return true
        }
    }
    
    fun hasFileChanged(stageKey: String, file: File): Boolean {
        if (!file.exists()) {
            val hash = "missing"
            currentHashes[stageKey] = hash
            return savedHashes[stageKey] != hash
        }
        
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            
            file.inputStream().use {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (it.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val hashStr = digest.digest().joinToString("") { "%02x".format(it) }
            
            currentHashes[stageKey] = hashStr
            return savedHashes[stageKey] != hashStr
            
        } catch (e: Exception) {
            currentHashes[stageKey] = System.currentTimeMillis().toString()
            return true
        }
    }
}
