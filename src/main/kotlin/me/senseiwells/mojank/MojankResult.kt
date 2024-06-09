package me.senseiwells.mojank

/**
 * Class representing a result from the [Mojank] api.
 *
 * The result can be in one of three states; [success],
 * [partial], or [failure].
 * - Success indicates a completely successful api call.
 * This holds a value.
 * - Partial indicates an api call where some data was
 * invalid but others were not, this holds a partial value,
 * as well as a reason for any failures.
 * - Failure indicates there was some error in the api call.
 * There will be a reason message for the failure and
 * possibly an exception.
 *
 * @see Mojank
 */
public sealed class MojankResult<T: Any> {
    /**
     * Whether the result was successful.
     */
    public abstract val isSuccess: Boolean

    /**
     * Whether the result was a partial success.
     */
    public abstract val isPartial: Boolean

    /**
     * Whether the result was at least a partial success.
     *
     * This indicates you can call [get] without fault.
     */
    public val isSuccessOrPartial: Boolean
        get() = isSuccess || isPartial

    /**
     * Tries to get the value wrapped in the result.
     * This will fail and throw an exception for [failure]s.
     *
     * @return The resulting value.
     */
    public abstract fun get(): T

    /**
     * Tries to get the value wrapped in the result,
     * or null if this was a [failure].
     *
     * @return The resulting value, or null.
     */
    public abstract fun getOrNull(): T?

    /**
     * Tries to get the reason for a partial or failed result.
     * This will throw an exception if called on a [success].
     *
     * @return The reason for the failure.
     */
    public abstract fun getReason(): String

    /**
     * Gets the exception for the result. This will return
     * null unless an exception occurred during the api call.
     *
     * @return The exception during the api call.
     */
    public abstract fun getException(): Throwable?

    /**
     * Gets the value or a specified value if it was a failure.
     *
     * @param supplier The supplier to call if it was a failure.
     * @return The value or the specified default.
     */
    public fun getOrElse(supplier: () -> T): T {
        return getOrNull() ?: supplier()
    }

    /**
     * Maps this result into a different type. This will
     * keep any state that the result had previously.
     *
     * @param mapper The mapper to map the value.
     * @return The mapped result.
     */
    public fun <S: Any> map(mapper: (T) -> S): MojankResult<S> {
        return if (isSuccess) {
            success(mapper(get()))
        } else if (isPartial) {
            partial(mapper(get()), getReason())
        } else {
            failure(getReason(), getException())
        }
    }

    /**
     * Runs a body if the result was successful providing the value.
     *
     * @param body The function to run.
     */
    public inline fun ifSuccess(body: (T) -> Unit) {
        if (isSuccess) {
            body(get())
        }
    }

    /**
     * Runs a body if the result was partial, providing the value
     * and the reason for the partial result.
     *
     * @param body The function to run.
     */
    public inline fun ifPartial(body: (T, String) -> Unit) {
        if (isPartial) {
            body(get(), getReason())
        }
    }

    /**
     * Runs a body if the result was a failure, providing the reason
     * and the exception if there was one.
     *
     * @param body The function to run.
     */
    public inline fun ifFailure(body: (String, Throwable?) -> Unit) {
        if (!isSuccess && !isPartial) {
            body(getReason(), getException())
        }
    }

    public companion object {
        /**
         * Creates a successful result.
         *
         * @param value The value to wrap.
         * @return The successful result.
         */
        public fun <T: Any> success(value: T): MojankResult<T> {
            return Success(value)
        }

        /**
         * Creates a partial result.
         *
         * @param value The value to wrap.
         * @param reason The reason for the partial result.
         * @return The partial result.
         */
        public fun <T: Any> partial(value: T, reason: String): MojankResult<T> {
            return Partial(value, reason)
        }

        /**
         * Creates a failed result.
         *
         * @param reason The reason for the failure.
         * @param error The exception that occurred.
         * @return The failed result.
         */
        public fun <T: Any> failure(reason: String, error: Throwable? = null): MojankResult<T> {
            return Failure(reason, error)
        }

        /**
         * Creates a successful result or a partial result.
         *
         * @param value The value to wrap.
         * @param reason The reason for the partial result,
         *  if null, a successful result will be created instead.
         * @return The successful or partial result.
         */
        public fun <T: Any> successOrPartial(value: T, reason: String?): MojankResult<T> {
            return if (reason == null) success(value) else partial(value, reason)
        }
    }

    private class Success<T: Any>(private val value: T): MojankResult<T>() {
        override val isSuccess: Boolean get() = true
        override val isPartial: Boolean get() = false

        override fun get(): T {
            return value
        }

        override fun getOrNull(): T {
            return value
        }

        override fun getReason(): String {
            throw IllegalArgumentException("Tried to get reason for success")
        }

        override fun getException(): Throwable? {
            return null
        }

        override fun toString(): String {
            return "SuccessfulMojankResult(value=$value)"
        }
    }

    private class Partial<T: Any>(private val value: T, private val reason: String): MojankResult<T>() {
        override val isSuccess: Boolean get() = false
        override val isPartial: Boolean get() = true

        override fun get(): T {
            return value
        }

        override fun getOrNull(): T {
            return value
        }

        override fun getReason(): String {
            return reason
        }

        override fun getException(): Throwable? {
            return null
        }

        override fun toString(): String {
            return "PartialMojankResult(value=$value, reason=$reason)"
        }
    }

    private class Failure<T: Any>(private val reason: String, private val error: Throwable?): MojankResult<T>() {
        override val isSuccess: Boolean get() = false
        override val isPartial: Boolean get() = false

        override fun get(): T {
            throw IllegalArgumentException("Tried to get failed result")
        }

        override fun getOrNull(): T? {
            return null
        }

        override fun getReason(): String {
            return reason
        }

        override fun getException(): Throwable? {
            return error
        }

        override fun toString(): String {
            return "FailedMojankResult(reason=$reason" + if (error != null) ", error=$error)" else ")"
        }
    }
}