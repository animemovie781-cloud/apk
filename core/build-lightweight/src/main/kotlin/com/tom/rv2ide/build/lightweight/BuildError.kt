package com.tom.rv2ide.build.lightweight

import java.io.File

/**
 * Represents a diagnostic message, warning, or error from the build process.
 */
data class BuildError(
    val severity: Severity,
    val message: String,
    val file: File? = null,
    val line: Int = -1,
    val column: Int = -1
) {
    enum class Severity {
        INFO,
        WARNING,
        ERROR
    }
    
    override fun toString(): String {
        val location = if (file != null) {
            val lineStr = if (line > 0) ":$line" else ""
            val colStr = if (column > 0) ":$column" else ""
            "${file.absolutePath}$lineStr$colStr: "
        } else {
            ""
        }
        
        return "[$severity] $location$message"
    }
}
