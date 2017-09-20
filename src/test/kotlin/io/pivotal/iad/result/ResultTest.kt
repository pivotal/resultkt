package io.pivotal.iad.result

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResultTest {
    @Test
    fun `result booleans and readers work as expected`() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        assertThat(success.isSuccess()).isTrue()
        assertThat(success.isFailure()).isFalse()
        assertThat(success.success).isEqualTo("happy string")

        assertThat(failure.isSuccess()).isFalse()
        assertThat(failure.isFailure()).isTrue()
        assertThat(failure.failure).isEqualTo("sad string")
    }

    @Test
    fun `result callbacks yield values to consumers`() {
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
    fun `then yields values to success and failure callbacks and can return a value`() {
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
    fun `tap yields values and returns the original result`(){
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
    fun `tap does nothing if a handler is not specified`() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        success.tap { }
        failure.tap { }
    }

    @Test
    fun `map transforms success values and propagates failure values`() {
        val success: Result<String, Any> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        assertThat(success.map { it.toUpperCase() }).isEqualTo(Result.success<String, Any>("HAPPY STRING"))
        assertThat(failure.map { throw Exception("Should never get here") }).isEqualTo(Result.failure<Any, String>("sad string"))
    }

    @Test
    fun `flat map transforms success values with a mapper that itself returns a result and propagates failure values`(){
        val success: Result<String, OtherType> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        assertThat(success.flatMap { Result.success<Int, OtherType>(3) } ).isEqualTo(Result.success<Int, OtherType>(3))
        assertThat(failure.flatMap<Any> { throw Exception("Should never get here") }).isEqualTo(Result.failure<Any, String>("sad string"))
    }

    @Test
    @Suppress("USELESS_IS_CHECK")
    fun `result success types are covariant`() {
        val stringResult = Result.success<String, OtherType>("foo")
        assertThat(stringResult is Result<Any, OtherType>).isTrue()

        /* Quoth https://kotlinlang.org/docs/reference/generics.html :
         *   In "clever words" they say that the class C is covariant in the parameter T,
         *   or that T is a covariant type parameter. You can think of C as being a
         *   producer of T's, and NOT a consumer of T's.
         */
    }
}

class OtherType
