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
    fun `tap yields values and returns the original result`() {
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
    fun `flat map transforms success values with a mapper that itself returns a result and propagates failure values`() {
        val success: Result<String, OtherType> = Result.success("happy string")
        val failure: Result<Any, String> = Result.failure("sad string")

        assertThat(success.flatMap { Result.success<Int, OtherType>(3) }).isEqualTo(Result.success<Int, OtherType>(3))
        assertThat(failure.flatMap<Any> { throw Exception("Should never get here") }).isEqualTo(Result.failure<Any, String>("sad string"))
    }

    @Test
    fun `map failure transforms failure values and propagates success values`() {
        val success: Result<Int, String> = Result.success(123)
        val failure: Result<Int, String> = Result.failure("sad string")

        assertThat(success.mapFailure { throw Exception("Should never get here") }).isEqualTo(Result.success<Int, Boolean>(123))
        assertThat(failure.mapFailure { it.contains("sad") }).isEqualTo(Result.failure<Int, Boolean>(true))
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

    @Test
    fun `fromNullable lets you easily turn a nullable into a Result`() {
        val imNull = null
        val imNotNull = "I'm a real string"

        assertThat(Result.fromNullable(imNotNull, "it was null"))
                .isEqualTo(Result.success<String, String>("I'm a real string"))
        assertThat(Result.fromNullable(imNull, "it was null"))
                .isEqualTo(Result.failure<String, String>("it was null"))
        assertThat(Result.fromNullable(imNull) { "it was null" })
                .isEqualTo(Result.failure<String, String>("it was null"))
    }

    @Test
    fun `toNullable lets you easily turn a Result into a nullable`() {
        val failed = Result.failure<String, String>("nope")
        val succeeded = Result.success<String, String>("ok!")

        assertThat(failed.toNullable()).isNull()
        assertThat(succeeded.toNullable()).isEqualTo("ok!")
    }

    @Test
    fun `partition partitions the list on success and failure`(){
        val mixedList = listOf(
                Result.failure<String, String>("sad"),
                Result.success("happy")
        )

        val (successes, failures) = Result.partition(mixedList)

        assertThat(successes).isEqualTo(listOf("happy"))
        assertThat(failures).isEqualTo(listOf("sad"))

    }
}

class OtherType
