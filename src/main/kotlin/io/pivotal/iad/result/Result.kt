package io.pivotal.iad.result

sealed class Result<out T_SUCCESS : Any, T_FAILURE : Any> {
    abstract val success: T_SUCCESS
    abstract val failure: T_FAILURE
    abstract fun isSuccess(): Boolean
    fun isFailure(): Boolean {
        return !isSuccess()
    }

    fun success(onSuccess: (T_SUCCESS) -> Unit) {
        if (isSuccess()) onSuccess(success)
    }

    fun failure(onFailure: (T_FAILURE) -> Unit) {
        if (isFailure()) onFailure(failure)
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

    abstract fun <T_MAPPED_FAILURE : Any> mapFailure(mapper: (T_FAILURE) -> T_MAPPED_FAILURE): Result<T_SUCCESS, T_MAPPED_FAILURE>

    fun <T_OTHER_SUCCESS : Any> fanout(fn: (T_SUCCESS) -> Result<T_OTHER_SUCCESS, T_FAILURE>): Result<Pair<T_SUCCESS, T_OTHER_SUCCESS>, T_FAILURE> {
        return flatMap { originalSuccess -> fn(success).map { newSuccess -> Pair(originalSuccess, newSuccess) } }
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
            return Success(value)
        }

        fun <T_SUCCESS : Any, T_FAILURE : Any> failure(value: T_FAILURE): Result<T_SUCCESS, T_FAILURE> {
            return Failure(value)
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

        fun <T_SUCCESS : Any, T_FAILURE : Any> partition(results: List<Result<T_SUCCESS, T_FAILURE>>): Pair<List<T_SUCCESS>, List<T_FAILURE>> {
            val (successes, failures) = results.partition { it.isSuccess() }

            return Pair(
                    successes.map { it.success },
                    failures.map { it.failure }
            )
        }
    }

    data class Success<out T_SUCCESS : Any, T_FAILURE : Any>(private val value: T_SUCCESS) : Result<T_SUCCESS, T_FAILURE>() {
        override val success: T_SUCCESS
            get() = value

        override val failure: T_FAILURE
            get() = throw RuntimeException("not a failure")
        override fun isSuccess(): Boolean = true

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return Success(mapper(value))
        }

        override fun <T_MAPPED_FAILURE : Any> mapFailure(mapper: (T_FAILURE) -> T_MAPPED_FAILURE): Result<T_SUCCESS, T_MAPPED_FAILURE> {
            return Result.success(value)
        }

        override fun toNullable(): T_SUCCESS? = success
    }

    data class Failure<out T_SUCCESS : Any, T_FAILURE : Any>(private val value: T_FAILURE) : Result<T_SUCCESS, T_FAILURE>() {
        override val failure: T_FAILURE
            get() = value
        override val success: T_SUCCESS
            get() = throw RuntimeException("not a success")

        override fun isSuccess(): Boolean = false

        override fun <T_MAPPED : Any> map(mapper: (T_SUCCESS) -> T_MAPPED): Result<T_MAPPED, T_FAILURE> {
            return Failure(value)
        }

        override fun <T_MAPPED_FAILURE : Any> mapFailure(mapper: (T_FAILURE) -> T_MAPPED_FAILURE): Result<T_SUCCESS, T_MAPPED_FAILURE> {
            return Result.failure(mapper(value))
        }

        override fun toNullable(): T_SUCCESS? = null
    }
}
