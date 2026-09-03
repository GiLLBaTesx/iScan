package com.examscanner.premium.error

/**
 * RepositoryResult - Uniform result wrapper for the data layer (Requirement 27.2).
 *
 * Repositories return a [RepositoryResult] so callers can react to success, failure,
 * and in-flight states without leaking raw exceptions into the UI. Failed database
 * operations that are rolled back via `database.withTransaction { }` surface here as
 * [Error], which ViewModels map to a [UiError.Database] for display (Req 27.2).
 */
sealed class RepositoryResult<out T> {
    data class Success<out T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Exception, val message: String) : RepositoryResult<Nothing>()
    object Loading : RepositoryResult<Nothing>()

    /** True when this result carries data. */
    val isSuccess: Boolean get() = this is Success

    /** Returns the data if this is a [Success], otherwise null. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Maps a successful payload while preserving [Error]/[Loading] states. */
    inline fun <R> map(transform: (T) -> R): RepositoryResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    companion object {
        /**
         * Runs [block] and wraps its outcome. Any exception becomes an [Error] with a
         * user-safe message; no sensitive values are placed in the message here.
         */
        inline fun <T> catching(errorMessage: String, block: () -> T): RepositoryResult<T> =
            try {
                Success(block())
            } catch (e: Exception) {
                Error(e, errorMessage)
            }
    }
}
