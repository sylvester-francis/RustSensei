package com.sylvester.rustsensei.domain

import app.cash.turbine.test
import com.sylvester.rustsensei.network.PlaygroundResponse
import com.sylvester.rustsensei.testdoubles.FakePlaygroundService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CompileCodeUseCaseTest {

    private lateinit var fakeService: FakePlaygroundService
    private lateinit var useCase: CompileCodeUseCase

    @Before
    fun setUp() {
        fakeService = FakePlaygroundService()
        useCase = CompileCodeUseCase(fakeService)
    }

    @Test
    fun `successful compilation emits Compiling then Completed`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(
            success = true,
            stdout = "Hello, world!\n",
            stderr = ""
        )

        useCase("fn main() {}").test {
            val compiling = awaitItem()
            assertTrue(compiling is CompileCodeEvent.Compiling)

            val completed = awaitItem() as CompileCodeEvent.Completed
            assertTrue(completed.success)
            assertEquals("Hello, world!\n", completed.stdout)
            assertEquals("", completed.stderr)
            assertTrue(completed.elapsedMs >= 0)

            awaitComplete()
        }

        assertEquals(1, fakeService.executeCallCount)
        assertEquals("fn main() {}", fakeService.lastRequest?.code)
        assertFalse(fakeService.lastRequest!!.tests)
    }

    @Test
    fun `compilation failure returns success false with stderr`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(
            success = false,
            stdout = "",
            stderr = "error[E0308]: mismatched types"
        )

        useCase("fn main() { let x: i32 = \"hello\"; }").test {
            awaitItem() // Compiling

            val completed = awaitItem() as CompileCodeEvent.Completed
            assertFalse(completed.success)
            assertEquals("error[E0308]: mismatched types", completed.stderr)

            awaitComplete()
        }
    }

    @Test
    fun `no internet emits Error with user-friendly message`() = runTest {
        fakeService.shouldThrow = UnknownHostException("Unable to resolve host")

        useCase("fn main() {}").test {
            awaitItem() // Compiling

            val error = awaitItem() as CompileCodeEvent.Error
            assertTrue(error.message.contains("No internet"))

            awaitComplete()
        }
    }

    @Test
    fun `timeout emits Error with timeout message`() = runTest {
        fakeService.shouldThrow = SocketTimeoutException("Read timed out")

        useCase("fn main() {}").test {
            awaitItem() // Compiling

            val error = awaitItem() as CompileCodeEvent.Error
            assertTrue(error.message.contains("timed out"))

            awaitComplete()
        }
    }

    @Test
    fun `generic exception emits Error with exception message`() = runTest {
        fakeService.shouldThrow = RuntimeException("Something went wrong")

        useCase("fn main() {}").test {
            awaitItem() // Compiling

            val error = awaitItem() as CompileCodeEvent.Error
            assertEquals("Something went wrong", error.message)

            awaitComplete()
        }
    }
}
