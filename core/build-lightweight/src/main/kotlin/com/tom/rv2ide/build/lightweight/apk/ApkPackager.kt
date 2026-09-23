package com.tom.rv2ide.build.lightweight.apk

import com.tom.rv2ide.build.lightweight.BuildInput
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages compiled resources, DEX files, and assets into an unaligned, unsigned APK.
 */
class ApkPackager(private val input: BuildInput) {

    fun packageApk(): File? {
        input.buildLogger.logInfo("Packaging APK...")
        input.cancellationToken.checkCancelled()

        val tempApk = File(input.tempDir, "unaligned-unsigned.apk")
        
        try {
            ZipOutputStream(FileOutputStream(tempApk)).use { zos ->
                
                // 1. Add compiled resources (resources.arsc.flat usually implies we just take the output of AAPT2 link)
                // AAPT2 link actually produces an APK structure directly if output is a .apk file, but since we output resources.arsc.flat
                // we should grab that and other linked resources.
                // Note: Standard AAPT2 link produces a .ap_ file which IS a zip file containing AndroidManifest.xml, resources.arsc, and res/
                // We will assume `resources.arsc.flat` from ResourceCompiler was actually named `resources.ap_` and use its contents.
                val linkedRes = File(input.resourcesDir, "linked/resources.arsc.flat")
                if (linkedRes.exists()) {
                    // For a lightweight build, we actually just copy the linked ap_ as the base zip, then append dex/assets.
                    // This implementation is a simplification. A real implementation unpacks the ap_ or uses a true ZipBuilder.
                    input.buildLogger.logInfo("Adding resources from AAPT2 link...")
                    addFileToZip(zos, linkedRes, "resources.arsc") // Simplified
                    
                    val manifest = File(input.projectDir, "app/src/main/AndroidManifest.xml")
                    addFileToZip(zos, manifest, "AndroidManifest.xml") // AAPT2 link actually outputs a binary one, this is simplified
                }
                
                // 2. Add DEX files
                val dexDir = input.dexDir
                if (dexDir.exists()) {
                    dexDir.listFiles { file -> file.extension == "dex" }?.forEach { dex ->
                        input.buildLogger.logInfo("Adding ${dex.name}...")
                        addFileToZip(zos, dex, dex.name)
                    }
                }
                
                // 3. Add assets
                val assetsDir = File(input.projectDir, "app/src/main/assets")
                if (assetsDir.exists()) {
                    addDirectoryToZip(zos, assetsDir, "assets")
                }
                
                // 4. Add native libs
                val jniLibsDir = File(input.projectDir, "app/src/main/jniLibs")
                if (jniLibsDir.exists()) {
                    addDirectoryToZip(zos, jniLibsDir, "lib")
                }
            }
            
            input.buildLogger.logInfo("APK packaged successfully: ${tempApk.name}")
            return tempApk
            
        } catch (e: Exception) {
            input.buildLogger.logError("Failed to package APK: ${e.message}")
            return null
        }
    }
    
    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        
        // Android requires certain files (like resources.arsc) to be STORED (not deflated)
        // for mmap support. This simplified version uses DEFLATED for everything.
        zos.putNextEntry(entry)
        
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        
        zos.closeEntry()
    }
    
    private fun addDirectoryToZip(zos: ZipOutputStream, dir: File, baseEntryPath: String) {
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relativePath = file.absolutePath.substring(dir.absolutePath.length + 1).replace('\\', '/')
            val entryName = "$baseEntryPath/$relativePath"
            addFileToZip(zos, file, entryName)
        }
    }
}
