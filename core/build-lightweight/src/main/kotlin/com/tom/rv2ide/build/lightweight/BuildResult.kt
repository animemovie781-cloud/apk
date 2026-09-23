package com.tom.rv2ide.build.lightweight

import java.io.File

/**
 * Represents the final result of a build process.
 */
data class BuildResult(
    val success: Boolean,
    val apkPath: File? = null,
    val errorMessage: String? = null,
    val buildTimeMs: Long = 0,
    val warnings: List<BuildError> = emptyList(),
    val errors: List<BuildError> = emptyList(),
    val stats: BuildStats = BuildStats(),
    val isCancelled: Boolean = false
) {
    companion object {
        fun success(apkPath: File, timeMs: Long, stats: BuildStats, warnings: List<BuildError> = emptyList()): BuildResult {
            return BuildResult(
                success = true,
                apkPath = apkPath,
                buildTimeMs = timeMs,
                stats = stats,
                warnings = warnings
            )
        }
        
        fun failure(message: String, timeMs: Long, errors: List<BuildError> = emptyList(), stats: BuildStats = BuildStats()): BuildResult {
            return BuildResult(
                success = false,
                errorMessage = message,
                buildTimeMs = timeMs,
                errors = errors,
                stats = stats
            )
        }
        
        fun cancelled(timeMs: Long, stats: BuildStats = BuildStats()): BuildResult {
            return BuildResult(
                success = false,
                errorMessage = "Build cancelled by user",
                buildTimeMs = timeMs,
                stats = stats,
                isCancelled = true
            )
        }
    }
}

/**
 * Records the time taken for various stages of the build.
 */
data class BuildStats(
    var libraryResolutionTimeMs: Long = 0,
    var resourceCompileTimeMs: Long = 0,
    var javaCompileTimeMs: Long = 0,
    var kotlinCompileTimeMs: Long = 0,
    var dexTimeMs: Long = 0,
    var packageTimeMs: Long = 0,
    var signTimeMs: Long = 0,
    var totalTimeMs: Long = 0
)
