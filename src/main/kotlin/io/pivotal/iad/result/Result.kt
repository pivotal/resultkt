package io.pivotal.iad.result

sealed class Result<out T_SUCCESS : Any, T_FAILURE : Any> {
    abstract val success: T_SUCCESS
    abstract val failure: T_FAILURE
    abstract fun isSuccess(): Boolean
    fun isFailure(): Boolean {
        return !isSuccess()
    }

    abstract fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE>

    fun <T_MAPPED : Any> flatMap(mapper: (T_SUCCESS) -> Result<T_MAPPED, T_FAILURE>): Result<T_MAPPED, T_FAILURE> {
        return then(
                onSuccess = { success ->
                    mapper(success).then(
                            onSuccess = { Result.success(it) },
                            onFailure = { Result.failure(it) }
                    )
                },
                onFailure = { failure -> Result.failure(failure) }
        )
    }

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

    abstract fun toNullable(): T_SUCCESS?

    companion object {
        fun <T_SUCCESS : Any, T_FAILURE : Any> success(value: T_SUCCESS): Result<T_SUCCESS, T_FAILURE> {
            return SuccessResult(value)
        }

        fun <T_SUCCESS : Any, T_FAILURE : Any> failure(value: T_FAILURE): Result<T_SUCCESS, T_FAILURE> {
            return FailureResult(value)
        }

        fun <T_SUCCESS : Any, T_FAILURE : Any> fromNullable(maybeNull: T_SUCCESS?, whenNull: T_FAILURE): Result<T_SUCCESS, T_FAILURE> {
            return when (maybeNull) {
                null -> Result.failure(whenNull)
                else -> Result.success(maybeNull)
            }
        }

        fun <T_SUCCESS : Any, T_FAILURE : Any> fromNullable(maybeNull: T_SUCCESS?, whenNull: () -> T_FAILURE): Result<T_SUCCESS, T_FAILURE> {
            return when (maybeNull) {
                null -> Result.failure(whenNull())
                else -> Result.success(maybeNull)
            }
        }
    }

    data class SuccessResult<out T_SUCCESS : Any, T_FAILURE : Any>(private val value: T_SUCCESS) : Result<T_SUCCESS, T_FAILURE>() {
        override val success: T_SUCCESS
            get() = value

        override val failure: T_FAILURE
            get() = throw RuntimeException("not a failure")
        override fun isSuccess(): Boolean = true

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return SuccessResult(mapper(value))
        }

        override fun toNullable(): T_SUCCESS? = success
    }

    data class FailureResult<out T_SUCCESS : Any, T_FAILURE : Any>(private val value: T_FAILURE) : Result<T_SUCCESS, T_FAILURE>() {
        override val failure: T_FAILURE
            get() = value
        override val success: T_SUCCESS
            get() = throw RuntimeException("not a success")

        override fun isSuccess(): Boolean = false

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return FailureResult(value)
        }

        override fun toNullable(): T_SUCCESS? = null
    }
}
