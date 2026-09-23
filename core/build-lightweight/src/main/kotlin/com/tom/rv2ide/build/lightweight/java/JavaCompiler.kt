package com.tom.rv2ide.build.lightweight.java

import com.tom.rv2ide.build.lightweight.*
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Handles Java compilation using the Eclipse Java Compiler (ECJ).
 */
class JavaCompiler(private val input: BuildInput) {

    fun compile(): Boolean {
        input.buildLogger.logInfo("Starting Java compilation with ECJ")
        input.cancellationToken.checkCancelled()

        val classesDir = input.classesDir
        classesDir.mkdirs()

        val javaSources = mutableListOf<File>()
        
        // Find main sources
        val srcDir = File(input.projectDir, "app/src/main/java")
        if (srcDir.exists()) {
            srcDir.walkTopDown().filter { it.extension == "java" }.forEach {
                javaSources.add(it)
            }
        }
        
        // Find generated sources (R.java)
        val generatedSrcDir = File(input.generatedDir, "source/r")
        if (generatedSrcDir.exists()) {
            generatedSrcDir.walkTopDown().filter { it.extension == "java" }.forEach {
                javaSources.add(it)
            }
        }

        if (javaSources.isEmpty()) {
            input.buildLogger.logInfo("No Java sources found, skipping compilation")
            return true
        }

        val androidJar = BuildEnvironment.getAndroidJar()
        val classpath = buildClasspath(androidJar)
        
        input.buildLogger.logInfo("Compiling ${javaSources.size} Java files...")

        val args = mutableListOf<String>(
            "-1.8", // Source version
            "-target", "1.8",
            "-d", classesDir.absolutePath,
            "-cp", classpath
        )
        
        // Add all source files
        javaSources.forEach {
            args.add(it.absolutePath)
        }

        val outWriter = StringWriter()
        val errWriter = StringWriter()

        val main = Main(
            PrintWriter(outWriter), 
            PrintWriter(errWriter), 
            false /* systemExitWhenFinished */, 
            null /* customDefaultOptions */, 
            null /* compilationProgress */
        )
        
        // Execute ECJ
        val success = main.compile(args.toTypedArray())

        // Process outputs
        val outOutput = outWriter.toString()
        val errOutput = errWriter.toString()

        if (outOutput.isNotBlank()) {
            outOutput.split("\n").forEach { if (it.isNotBlank()) input.buildLogger.logInfo(it) }
        }
        
        if (errOutput.isNotBlank()) {
            // Note: ECJ can output warnings to stderr even if compilation succeeded
            errOutput.split("\n").forEach { 
                if (it.isNotBlank()) {
                    if (success) {
                        input.buildLogger.logWarning(it)
                    } else {
                        input.buildLogger.logError(it)
                    }
                }
            }
        }

        if (success) {
            input.buildLogger.logInfo("Java compilation finished successfully")
        } else {
            input.buildLogger.logError("Java compilation failed")
        }

        return success
    }

    private fun buildClasspath(androidJar: File): String {
        val cp = StringBuilder(androidJar.absolutePath)
        
        // Add library jars from project
        val libsDir = File(input.projectDir, "app/libs")
        if (libsDir.exists()) {
            libsDir.listFiles { file -> file.extension == "jar" }?.forEach { jar ->
                cp.append(File.pathSeparatorChar).append(jar.absolutePath)
            }
        }
        
        // Add extracted AAR classes.jar from library resolver
        // (This will be fully implemented when LibraryResolver is done)
        val cacheDir = File(input.cacheDir, "aars")
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.name == "classes.jar" }.forEach { jar ->
                cp.append(File.pathSeparatorChar).append(jar.absolutePath)
            }
        }

        return cp.toString()
    }
}
