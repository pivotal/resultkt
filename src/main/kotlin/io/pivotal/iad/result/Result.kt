package io.pivotal.iad.result

sealed class Result<out S : Any, F : Any> {
    abstract val success: S
    abstract val failure: F

    abstract fun isSuccess(): Boolean

    fun isFailure(): Boolean {
        return !isSuccess()
    }

    fun success(onSuccess: (S) -> Unit) {
        if (isSuccess()) onSuccess(success)
    }

    fun failure(onFailure: (F) -> Unit) {
        if (isFailure()) onFailure(failure)
    }

    abstract fun <T : Any> map(mapper: (S) -> T): Result<T, F>

    fun <T : Any> flatMap(mapper: (S) -> Result<T, F>): Result<T, F> {
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

    abstract fun <T : Any> mapFailure(mapper: (F) -> T): Result<S, T>

    fun <T : Any> fanout(fn: (S) -> Result<T, F>): Result<Pair<S, T>, F> {
        return flatMap { originalSuccess -> fn(success).map { newSuccess -> Pair(originalSuccess, newSuccess) } }
    }

    fun <T> then(onSuccess: (S) -> T, onFailure: (F) -> T): T {
        return if (isSuccess()) {
            onSuccess(success)
        } else {
            onFailure(failure)
        }
    }

    fun tap(onSuccess: (S) -> Unit = {}, onFailure: (F) -> Unit = {}): Result<S, F> {
        if (isSuccess()) {
            onSuccess(success)
        } else {
            onFailure(failure)
        }

        return this
    }

    abstract fun toNullable(): S?

    companion object {
        fun <S : Any, F : Any> success(value: S): Result<S, F> {
            return Success(value)
        }

        fun <S : Any, F : Any> failure(value: F): Result<S, F> {
            return Failure(value)
        }

        fun <S : Any, F : Any> fromNullable(maybeNull: S?, whenNull: F): Result<S, F> {
            return when (maybeNull) {
                null -> Result.failure(whenNull)
                else -> Result.success(maybeNull)
            }
        }

        fun <S : Any, F : Any> fromNullable(maybeNull: S?, whenNull: () -> F): Result<S, F> {
            return when (maybeNull) {
                null -> Result.failure(whenNull())
                else -> Result.success(maybeNull)
            }
        }

        fun <S : Any, F : Any> partition(results: List<Result<S, F>>): Pair<List<S>, List<F>> {
            val (successes, failures) = results.partition { it.isSuccess() }

            return Pair(
                    successes.map { it.success },
                    failures.map { it.failure }
            )
        }
    }

    data class Success<out S : Any, F : Any>(private val value: S) : Result<S, F>() {
        override val success: S
            get() = value

        override val failure: F
            get() = throw RuntimeException("not a failure")
        override fun isSuccess(): Boolean = true

        override fun <T : Any> map(mapper: (S) -> T): Result<T, F> {
            return Success(mapper(value))
        }

        override fun <T : Any> mapFailure(mapper: (F) -> T): Result<S, T> {
            return Result.success(value)
        }

        override fun toNullable(): S? = success
    }

    data class Failure<out S : Any, F : Any>(private val value: F) : Result<S, F>() {
        override val failure: F
            get() = value
        override val success: S
            get() = throw RuntimeException("not a success")

        override fun isSuccess(): Boolean = false

        override fun <T : Any> map(mapper: (S) -> T): Result<T, F> {
            return Failure(value)
        }

        override fun <T : Any> mapFailure(mapper: (F) -> T): Result<S, T> {
            return Result.failure(mapper(value))
        }

        override fun toNullable(): S? = null
    }
}
