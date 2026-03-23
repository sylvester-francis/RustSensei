package com.sylvester.rustsensei.domain

import com.sylvester.rustsensei.network.PlaygroundRequest
import com.sylvester.rustsensei.network.RustPlaygroundService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class CompileCodeUseCase @Inject constructor(
    private val playgroundService: RustPlaygroundService
) {
    operator fun invoke(code: String): Flow<CompileCodeEvent> = flow {
        emit(CompileCodeEvent.Compiling)

        val startTime = System.currentTimeMillis()

        try {
            val response = playgroundService.execute(
                PlaygroundRequest(code = code)
            )

            emit(CompileCodeEvent.Completed(
                success = response.success,
                stdout = response.stdout,
                stderr = response.stderr,
                elapsedMs = System.currentTimeMillis() - startTime
            ))
        } catch (e: UnknownHostException) {
            emit(CompileCodeEvent.Error("No internet connection. Check your network and try again."))
        } catch (e: SocketTimeoutException) {
            emit(CompileCodeEvent.Error("Compilation timed out. The code may be too complex."))
        } catch (e: Exception) {
            emit(CompileCodeEvent.Error(e.message ?: "Compilation failed"))
        }
    }
}
