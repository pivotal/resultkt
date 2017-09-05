package io.pivotal.iad.result

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResultTest {
    @Test
    fun successfulResults_canBeAccessed() {
        val success: Result<String, Any> = Result.success("happy string")
        assertThat(success.isSuccess()).isTrue()
        assertThat(success.isFailure()).isFalse()
        assertThat(success.success).isEqualTo("happy string")
        var called = false
        success.success {
            called = true
            assertThat(it).isEqualTo("happy string")
        }
        assertThat(called).isTrue()
    }

    @Test
    fun failedResults_canBeAccessed() {
        val failure: Result<Any, String> = Result.failure("sad string")
        assertThat(failure.isSuccess()).isFalse()
        assertThat(failure.isFailure()).isTrue()
        assertThat(failure.failure).isEqualTo("sad string")
        var called = false
        failure.failure {
            called = true
            assertThat(it).isEqualTo("sad string")
        }
        assertThat(called).isTrue()
    }

    @Test
    fun resultCallbacksWorkAsExpected() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        var successString: String? = null
        var failureString: String? = null
        var hasBeenCalled = false

        success.success { successString = it }
        success.failure { hasBeenCalled = true }
        assertThat(successString).isEqualTo("happy string")
        assertThat(hasBeenCalled).isFalse()

        failure.success { hasBeenCalled = true }
        failure.failure { failureString = it }
        assertThat(failureString).isEqualTo("sad string")
        assertThat(hasBeenCalled).isFalse()
    }

    @Test
    fun thenWorksAsExpected() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        var returned = success.then(
                onSuccess = { it.toUpperCase() },
                onFailure = { throw Exception("should never happen") }
        )
        assertThat(returned).isEqualTo("HAPPY STRING")

        returned = failure.then(
                onSuccess = { throw Exception("should never happen") },
                onFailure = { it.toUpperCase() }
        )
        assertThat(returned).isEqualTo("SAD STRING")

        // can also return nothing
        val emptyReturn = success.then(
                onSuccess = {},
                onFailure = {}
        )
        assertThat(emptyReturn).isEqualTo(Unit)
    }

    @Test
    fun tapWorksAsExpected(){
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        var tapped = ""
        val returnedSuccess = success.tap(
                onSuccess = { tapped = it },
                onFailure = { throw Exception("should never happen") }
        )
        assertThat(tapped).isEqualTo("happy string")
        assertThat(returnedSuccess).isEqualTo(success)

        val returnedFailure = failure.tap(
                onSuccess = { throw Exception("should never happen") },
                onFailure = { tapped = it }
        )
        assertThat(tapped).isEqualTo("sad string")
        assertThat(returnedFailure).isEqualTo(failure)
    }

    @Test
    fun tapDoesNothingIfAHandlerIsNotSpecified() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        success.tap { }
        failure.tap { }
    }

    @Test
    fun successfulResults_canBeTransformed_andFailuresAreIgnored() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        assertThat(success.map { it.toUpperCase() }).isEqualTo(Result.success<String, Any>("HAPPY STRING"))
        assertThat(failure.map { throw Exception("Should never get here") }).isEqualTo(Result.failure<Any, String>("sad string"))
    }

    @Test
    fun resultTypes_areCovariant() {
        val stringResult = Result.success<String, String>("foo")
        assertThat(stringResult is Result<Any, Any>).isTrue()

        /* Quoth https://kotlinlang.org/docs/reference/generics.html :
         *   In "clever words" they say that the class C is covariant in the parameter T,
         *   or that T is a covariant type parameter. You can think of C as being a
         *   producer of T's, and NOT a consumer of T's.
         */
    }
}
