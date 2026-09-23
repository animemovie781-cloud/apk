package com.tom.rv2ide.build.lightweight

/**
 * Interface representing a build backend that can compile and package an Android project.
 */
interface BuildBackend {
    
    /**
     * Executes the build pipeline with the given input parameters.
     * @param input The configuration and parameters for the build.
     * @return The result of the build process.
     */
    fun build(input: BuildInput): BuildResult
    
    /**
     * Cancels the currently running build, if any.
     */
    fun cancel()
    
    /**
     * Checks if this backend supports the given build input configuration.
     * @param input The configuration to check.
     * @return true if this backend can handle the build, false otherwise.
     */
    fun supports(input: BuildInput): Boolean
    
    /**
     * @return The display name of this build backend.
     */
    fun getName(): String
}
