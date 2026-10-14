package com.aktarjabed.jagallery.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that provides a [TestDispatcher] and installs it as `Dispatchers.Main`
 * for the duration of a test.
 *
 * Example usage in a test class:
 *
 * ```kotlin
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 *
 * The rule exposes the dispatcher via [testDispatcher] so tests can pass it to
 * repository constructors or manually advance time.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    /** The dispatcher that is installed as `Dispatchers.Main`. */
    val testDispatcher: TestDispatcher
        get() = dispatcher

    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description?) {
        // Reset the main dispatcher to the original implementation.
        Dispatchers.resetMain()
    }
}
