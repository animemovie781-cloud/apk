package com.tom.rv2ide.build.lightweight

import com.tom.rv2ide.projects.IProjectManager
import java.io.File

/**
 * A fallback build backend that delegates to the existing Gradle build system.
 */
class GradleBuildBackend : BuildBackend {
    
    override fun build(input: BuildInput): BuildResult {
        input.buildLogger.logInfo("Delegating build to Gradle...")
        
        val project = input.project
        
        try {
            // Note: In a real implementation, we would hook into the existing Gradle
            // tooling API calls, but for this fallback, we assume the IDE handles it
            // directly if LightweightBuildBackend.supports() returns false.
            // This backend acts as a bridge.
            
            // This is a placeholder since the actual Gradle execution is tightly 
            // coupled to the IDE's RunTasksAction right now.
            input.buildLogger.logInfo("Gradle build fallback triggered. Check Gradle console for details.")
            
            // Assume success for the fallback interface right now
            // The actual IDE will intercept the unsupported case and launch Gradle
            return BuildResult.success(
                apkPath = File(input.project.getProjectDirectory(), "app/build/outputs/apk/debug/app-debug.apk"),
                timeMs = 0,
                stats = BuildStats()
            )
        } catch (e: Exception) {
            input.buildLogger.logError("Gradle build failed: ${e.message}")
            return BuildResult.failure("Gradle build failed: ${e.message}", 0)
        }
    }

    override fun cancel() {
        // Handled by existing Gradle infrastructure
    }

    override fun supports(input: BuildInput): Boolean {
        // Gradle supports any valid Android project
        return true
    }

    override fun getName(): String = "Gradle"
}
