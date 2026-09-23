package com.tom.rv2ide.build.lightweight

/**
 * Represents the states and progress of a build.
 */
enum class BuildState {
    IDLE,
    VALIDATING,
    PREPARING,
    RESOLVING_LIBRARIES,
    COMPILING_RESOURCES,
    GENERATING_R,
    COMPILING_JAVA,
    COMPILING_KOTLIN,
    DEXING,
    PACKAGING,
    SIGNING,
    ZIPALIGN,
    VERIFYING,
    SUCCESS,
    FAILED,
    CANCELLED
}

/**
 * Data class representing a build progress update.
 */
data class BuildProgress(
    val state: BuildState,
    val message: String,
    val progressPercent: Int? = null,
    val isComplete: Boolean = state == BuildState.SUCCESS || state == BuildState.FAILED || state == BuildState.CANCELLED,
    val result: BuildResult? = null
)
