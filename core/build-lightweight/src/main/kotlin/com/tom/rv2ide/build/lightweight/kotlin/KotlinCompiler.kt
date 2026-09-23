package com.tom.rv2ide.build.lightweight.kotlin

import com.tom.rv2ide.build.lightweight.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

/**
 * Handles Kotlin compilation.
 * Adapted from Sketchware Pro's CodeAssist derivative (GPLv3).
 */
class KotlinCompiler(private val input: BuildInput) {

    fun compile(): Boolean {
        input.buildLogger.logInfo("Checking for Kotlin sources...")
        input.cancellationToken.checkCancelled()

        val ktSources = mutableListOf<File>()
        val srcDir = File(input.projectDir, "app/src/main/java")
        if (srcDir.exists()) {
            srcDir.walkTopDown().filter { it.extension == "kt" }.forEach {
                ktSources.add(it)
            }
        }

        if (ktSources.isEmpty()) {
            input.buildLogger.logInfo("No Kotlin sources found, skipping.")
            return true
        }
        
        input.buildLogger.logInfo("Starting Kotlin compilation for ${ktSources.size} files...")

        val classesDir = input.classesDir
        classesDir.mkdirs()

        val androidJar = BuildEnvironment.getAndroidJar()
        val classpath = buildClasspath(androidJar)
        
        // Prepare arguments
        val arguments = K2JVMCompilerArguments().apply {
            freeArgs = ktSources.map { it.absolutePath }
            destination = classesDir.absolutePath
            this.classpath = classpath
            noStdlib = true
            noReflect = true
            jvmTarget = "1.8"
        }

        val collector = CustomMessageCollector(input.buildLogger)
        
        // Execute compiler
        val compiler = K2JVMCompiler()
        val exitCode = compiler.exec(collector, Services.EMPTY, arguments)
        
        val success = exitCode == org.jetbrains.kotlin.cli.common.ExitCode.OK
        
        if (success) {
            input.buildLogger.logInfo("Kotlin compilation finished successfully")
        } else {
            input.buildLogger.logError("Kotlin compilation failed")
        }
        
        return success
    }

    private fun buildClasspath(androidJar: File): String {
        val cp = StringBuilder(androidJar.absolutePath)
        
        // Include output of Java compiler so Kotlin can see Java classes
        if (input.classesDir.exists()) {
            cp.append(File.pathSeparatorChar).append(input.classesDir.absolutePath)
        }
        
        // Add library jars
        val libsDir = File(input.projectDir, "app/libs")
        if (libsDir.exists()) {
            libsDir.listFiles { file -> file.extension == "jar" }?.forEach { jar ->
                cp.append(File.pathSeparatorChar).append(jar.absolutePath)
            }
        }
        
        // Add AAR extracted classes
        val cacheDir = File(input.cacheDir, "aars")
        if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.name == "classes.jar" }.forEach { jar ->
                cp.append(File.pathSeparatorChar).append(jar.absolutePath)
            }
        }
        
        // In a real implementation we must also add the Kotlin stdlib here
        // cp.append(File.pathSeparatorChar).append(getKotlinStdlib().absolutePath)

        return cp.toString()
    }

    private class CustomMessageCollector(private val logger: BuildLogger) : MessageCollector {
        private var hasErrors = false

        override fun clear() {
            hasErrors = false
        }

        override fun hasErrors(): Boolean = hasErrors

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            if (severity.isError) {
                hasErrors = true
            }

            val buildError = BuildError(
                severity = when {
                    severity.isError -> BuildError.Severity.ERROR
                    severity.isWarning -> BuildError.Severity.WARNING
                    else -> BuildError.Severity.INFO
                },
                message = message,
                file = location?.path?.let { File(it) },
                line = location?.line ?: -1,
                column = location?.column ?: -1
            )
            
            logger.logError(buildError)
        }
    }
}
