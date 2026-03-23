package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.network.PlaygroundRequest
import com.sylvester.rustsensei.network.RustPlaygroundService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class RunExerciseTestsUseCase @Inject constructor(
    private val playgroundService: RustPlaygroundService
) {
    operator fun invoke(
        userCode: String,
        testCode: String
    ): Flow<RunTestsEvent> = flow {
        emit(RunTestsEvent.Running)

        val startTime = System.currentTimeMillis()
        val combinedCode = "$userCode\n\n$testCode"

        try {
            val response = playgroundService.execute(
                PlaygroundRequest(code = combinedCode, tests = true)
            )

            val rawOutput = buildString {
                if (response.stderr.isNotBlank()) append(response.stderr)
                if (response.stdout.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(response.stdout)
                }
            }

            val testResults = parseTestOutput(rawOutput)

            emit(RunTestsEvent.Completed(
                passed = testResults.count { it.passed },
                total = testResults.size,
                testResults = testResults,
                rawOutput = rawOutput,
                elapsedMs = System.currentTimeMillis() - startTime
            ))
        } catch (e: UnknownHostException) {
            emit(RunTestsEvent.Error("No internet connection. Check your network and try again."))
        } catch (e: SocketTimeoutException) {
            emit(RunTestsEvent.Error("Test execution timed out."))
        } catch (e: Exception) {
            emit(RunTestsEvent.Error(e.message ?: "Test execution failed"))
        }
    }

    internal fun parseTestOutput(output: String): List<TestCaseResult> {
        val testLineRegex = Regex("""test (\S+) \.\.\. (\w+)""")
        return testLineRegex.findAll(output).map { match ->
            TestCaseResult(
                name = match.groupValues[1],
                passed = match.groupValues[2] == "ok"
            )
        }.toList()
    }
}
