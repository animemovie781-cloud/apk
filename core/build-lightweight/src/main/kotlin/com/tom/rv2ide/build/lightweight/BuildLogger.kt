package com.tom.rv2ide.build.lightweight

/**
 * Interface for logging build output.
 */
interface BuildLogger {
    fun logInfo(message: String)
    fun logWarning(message: String)
    fun logError(message: String)
    fun logError(error: BuildError)
    fun getLogs(): List<String>
}

/**
 * Default implementation of BuildLogger.
 */
class DefaultBuildLogger : BuildLogger {
    private val logs = mutableListOf<String>()
    
    override fun logInfo(message: String) {
        val formatted = "[INFO] $message"
        logs.add(formatted)
        println(formatted)
    }

    override fun logWarning(message: String) {
        val formatted = "[WARNING] $message"
        logs.add(formatted)
        println(formatted)
    }

    override fun logError(message: String) {
        val formatted = "[ERROR] $message"
        logs.add(formatted)
        System.err.println(formatted)
    }
    
    override fun logError(error: BuildError) {
        logs.add(error.toString())
        if (error.severity == BuildError.Severity.ERROR) {
            System.err.println(error.toString())
        } else {
            println(error.toString())
        }
    }

    override fun getLogs(): List<String> = logs.toList()
}
