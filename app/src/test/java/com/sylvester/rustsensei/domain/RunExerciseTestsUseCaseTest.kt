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
import java.net.UnknownHostException

class RunExerciseTestsUseCaseTest {

    private lateinit var fakeService: FakePlaygroundService
    private lateinit var useCase: RunExerciseTestsUseCase

    @Before
    fun setUp() {
        fakeService = FakePlaygroundService()
        useCase = RunExerciseTestsUseCase(fakeService)
    }

    @Test
    fun `all tests passing emits Completed with correct counts`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(
            success = true,
            stdout = """
                running 3 tests
                test tests::test_bigger_returns_larger ... ok
                test tests::test_bigger_first_is_larger ... ok
                test tests::test_bigger_equal_values ... ok

                test result: ok. 3 passed; 0 failed; 0 ignored
            """.trimIndent(),
            stderr = ""
        )

        useCase("fn bigger(a: i32, b: i32) -> i32 { a }", "#[cfg(test)] mod tests {}").test {
            awaitItem() // Running

            val completed = awaitItem() as RunTestsEvent.Completed
            assertEquals(3, completed.passed)
            assertEquals(3, completed.total)
            assertTrue(completed.testResults.all { it.passed })

            awaitComplete()
        }

        assertTrue(fakeService.lastRequest!!.tests)
    }

    @Test
    fun `partial test failures emits correct pass and fail counts`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(
            success = false,
            stdout = """
                running 3 tests
                test tests::test_a ... ok
                test tests::test_b ... FAILED
                test tests::test_c ... ok

                test result: FAILED. 2 passed; 1 failed
            """.trimIndent(),
            stderr = ""
        )

        useCase("code", "tests").test {
            awaitItem() // Running

            val completed = awaitItem() as RunTestsEvent.Completed
            assertEquals(2, completed.passed)
            assertEquals(3, completed.total)

            val failed = completed.testResults.filter { !it.passed }
            assertEquals(1, failed.size)
            assertEquals("tests::test_b", failed[0].name)

            awaitComplete()
        }
    }

    @Test
    fun `compilation error before tests run emits Completed with zero tests`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(
            success = false,
            stdout = "",
            stderr = "error[E0308]: mismatched types"
        )

        useCase("bad code", "tests").test {
            awaitItem() // Running

            val completed = awaitItem() as RunTestsEvent.Completed
            assertEquals(0, completed.passed)
            assertEquals(0, completed.total)
            assertTrue(completed.rawOutput.contains("E0308"))

            awaitComplete()
        }
    }

    @Test
    fun `no internet emits Error`() = runTest {
        fakeService.shouldThrow = UnknownHostException("No address")

        useCase("code", "tests").test {
            awaitItem() // Running

            val error = awaitItem() as RunTestsEvent.Error
            assertTrue(error.message.contains("No internet"))

            awaitComplete()
        }
    }

    @Test
    fun `code is concatenated with test code in request`() = runTest {
        fakeService.responseToReturn = PlaygroundResponse(true, "", "")

        useCase("fn foo() {}", "#[test] fn test_foo() {}").test {
            awaitItem()
            awaitItem()
            awaitComplete()
        }

        val sentCode = fakeService.lastRequest!!.code
        assertTrue(sentCode.contains("fn foo() {}"))
        assertTrue(sentCode.contains("#[test] fn test_foo() {}"))
    }

    // --- parseTestOutput unit tests ---

    @Test
    fun `parseTestOutput parses standard cargo test output`() {
        val output = """
            test tests::test_add ... ok
            test tests::test_sub ... FAILED
            test tests::test_mul ... ok
        """.trimIndent()

        val results = useCase.parseTestOutput(output)

        assertEquals(3, results.size)
        assertTrue(results[0].passed)
        assertEquals("tests::test_add", results[0].name)
        assertFalse(results[1].passed)
        assertEquals("tests::test_sub", results[1].name)
        assertTrue(results[2].passed)
    }

    @Test
    fun `parseTestOutput returns empty list for no test output`() {
        val results = useCase.parseTestOutput("error[E0308]: mismatched types")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `parseTestOutput handles single test`() {
        val output = "test tests::only_test ... ok"
        val results = useCase.parseTestOutput(output)
        assertEquals(1, results.size)
        assertTrue(results[0].passed)
    }
}
