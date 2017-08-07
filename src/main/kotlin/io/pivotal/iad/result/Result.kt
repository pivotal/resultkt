package io.pivotal.iad.result

sealed class Result<out T_SUCCESS : Any, out T_FAILURE : Any> {
    abstract val success: T_SUCCESS
    abstract val failure: T_FAILURE
    abstract fun isSuccess(): Boolean
    fun isFailure(): Boolean {
        return !isSuccess()
    }

    abstract fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE>

    fun success(onSuccess: (T_SUCCESS) -> Unit) {
        if (isSuccess()) onSuccess(success)
    }

    fun failure(onFailure: (T_FAILURE) -> Unit) {
        if (isFailure()) onFailure(failure)
    }

    fun <T_RETURN> then(onSuccess: (T_SUCCESS) -> T_RETURN, onFailure: (T_FAILURE) -> T_RETURN): T_RETURN {
        return if (isSuccess()) {
            onSuccess(success)
        } else {
            onFailure(failure)
        }
    }

    fun tap(onSuccess: (T_SUCCESS) -> Unit = {}, onFailure: (T_FAILURE) -> Unit = {}): Result<T_SUCCESS, T_FAILURE> {
        if (isSuccess()) {
            onSuccess(success)
        } else {
            onFailure(failure)
        }

        return this
    }

    companion object {
        fun <T_SUCCESS : Any, T_FAILURE : Any> success(value: T_SUCCESS): Result<T_SUCCESS, T_FAILURE> {
            return SuccessResult(value)
        }

        fun <T_SUCCESS : Any, T_FAILURE : Any> failure(value: T_FAILURE): Result<T_SUCCESS, T_FAILURE> {
            return FailureResult(value)
        }
    }

    data class SuccessResult<out T_SUCCESS : Any, out T_FAILURE : Any>(private val value: T_SUCCESS) : Result<T_SUCCESS, T_FAILURE>() {
        override val success: T_SUCCESS
            get() = value
        override val failure: T_FAILURE
            get() = throw RuntimeException("not a failure")

        override fun isSuccess(): Boolean = true

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return SuccessResult(mapper(value))
        }
    }

    data class FailureResult<out T_SUCCESS : Any, out T_FAILURE : Any>(private val value: T_FAILURE) : Result<T_SUCCESS, T_FAILURE>() {
        override val failure: T_FAILURE
            get() = value
        override val success: T_SUCCESS
            get() = throw RuntimeException("not a success")

        override fun isSuccess(): Boolean = false

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return FailureResult(value)
        }
    }
}
