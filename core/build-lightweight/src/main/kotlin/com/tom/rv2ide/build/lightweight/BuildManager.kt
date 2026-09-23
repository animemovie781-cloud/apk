package com.tom.rv2ide.build.lightweight

import com.tom.rv2ide.projects.IProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Singleton service that manages build executions and provides a reactive stream
 * of build progress updates.
 */
object BuildManager {
    
    private val lightweightBackend = LightweightBuildBackend()
    private val gradleBackend = GradleBuildBackend()
    
    private var isCurrentlyBuilding = false
    private var activeBackend: BuildBackend? = null
    
    // Store logs of the last build
    private val lastBuildLogs = mutableListOf<String>()
    private var lastOutputApk: File? = null

    /**
     * Starts a debug build and returns a Flow of progress updates.
     */
    fun buildDebug(project: IProjectManager): Flow<BuildProgress> = flow {
        if (isCurrentlyBuilding) {
            emit(BuildProgress(BuildState.FAILED, "A build is already in progress.", result = BuildResult.failure("Build already running", 0)))
            return@flow
        }
        
        isCurrentlyBuilding = true
        lastBuildLogs.clear()
        lastOutputApk = null
        
        try {
            emit(BuildProgress(BuildState.PREPARING, "Preparing build..."))
            
            // Flow-aware logger that also stores logs in memory
            val flowLogger = object : BuildLogger {
                override fun logInfo(message: String) { lastBuildLogs.add("[INFO] $message") }
                override fun logWarning(message: String) { lastBuildLogs.add("[WARN] $message") }
                override fun logError(message: String) { lastBuildLogs.add("[ERROR] $message") }
                override fun logError(error: BuildError) { lastBuildLogs.add(error.toString()) }
                override fun getLogs(): List<String> = lastBuildLogs.toList()
            }
            
            val cancelToken = BuildCancellationToken()
            val input = BuildInput(project, true, flowLogger, cancelToken)
            
            // Select backend
            val backend = if (lightweightBackend.supports(input)) {
                lightweightBackend
            } else {
                flowLogger.logWarning("Project not supported by lightweight builder. Falling back to Gradle.")
                gradleBackend
            }
            
            activeBackend = backend
            emit(BuildProgress(BuildState.PREPARING, "Selected backend: ${backend.getName()}"))
            
            // Execute build off the main thread
            val result = withContext(Dispatchers.IO) {
                backend.build(input)
            }
            
            if (result.success) {
                lastOutputApk = result.apkPath
                emit(BuildProgress(BuildState.SUCCESS, "Build completed successfully", result = result))
            } else if (result.isCancelled) {
                emit(BuildProgress(BuildState.CANCELLED, "Build cancelled", result = result))
            } else {
                emit(BuildProgress(BuildState.FAILED, result.errorMessage ?: "Build failed", result = result))
            }
            
        } catch (e: Exception) {
            val result = BuildResult.failure("Unexpected error: ${e.message}", 0)
            emit(BuildProgress(BuildState.FAILED, "Build failed", result = result))
        } finally {
            isCurrentlyBuilding = false
            activeBackend = null
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Cancels the currently running build.
     */
    fun cancelBuild() {
        activeBackend?.cancel()
    }

    /**
     * @return true if a build is currently running.
     */
    fun isBuilding(): Boolean = isCurrentlyBuilding

    /**
     * @return the logs of the last build.
     */
    fun getBuildLogs(): List<String> = lastBuildLogs.toList()

    /**
     * @return the output APK of the last successful build.
     */
    fun getOutputApk(): File? = lastOutputApk
}
