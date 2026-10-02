package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ShellResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executionTimeMs: Long
)

object ShellExecutor {

    suspend fun execute(command: String, timeoutSeconds: Long = 10): ShellResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = -1

        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).run {
                var line: String?
                while (stdoutReader.readLine().also { line = it } != null) {
                    stdoutBuilder.append(line).append("\n")
                }
            }

            var errLine: String?
            while (stderrReader.readLine().also { errLine = it } != null) {
                stderrBuilder.append(errLine).append("\n")
            }

            val completed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            } else {
                process.waitFor()
                true
            }

            if (completed) {
                exitCode = process.exitValue()
            } else {
                process.destroy()
                stderrBuilder.append("\nCommand timed out after $timeoutSeconds seconds")
                exitCode = 124
            }
        } catch (e: Exception) {
            stderrBuilder.append("Execution error: ${e.localizedMessage}")
            exitCode = -1
        }

        ShellResult(
            command = command,
            stdout = stdoutBuilder.toString().trimEnd(),
            stderr = stderrBuilder.toString().trimEnd(),
            exitCode = exitCode,
            executionTimeMs = System.currentTimeMillis() - startTime
        )
    }
}
