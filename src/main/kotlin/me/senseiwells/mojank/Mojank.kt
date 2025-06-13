package me.senseiwells.mojank

import io.github.reactivecircus.cache4k.Cache
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.network.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.io.IOException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Class that provides wrappers for Mojang's api.
 *
 * This class doesn't cache results, as you will be rate limited,
 * it is highly suggested that you cached the results.
 * You can use the [CachedMojank] class for this or implement your own.
 *
 * You can either construct an instance of this class or if you
 * wish to use the default [HttpClient] you can use the companion object.
 * ```kotlin
 * val mojank = Mojank()
 * val result = mojank.usernameToUUID("Notch")
 * // Or
 * val result = Mojank.usernameToUUID("Notch")
 * ```
 *
 * For more information about specifics see the
 * [wiki page](https://wiki.vg/Mojang_API).
 *
 * @param client The [HttpClient] to use for api requests.
 * @param endpoints The endpoints to target.
 */
public open class Mojank(
    private val client: HttpClient = HttpClient(CIO),
    private val endpoints: MojankEndpoints = MojankEndpoints.DEFAULT
) {
    /**
     * Attempts an action a given number of times until the
     * result is conclusive.
     * This should be used when trying to avoid errors that
     * result in inconclusive results, e.i.
     * Mojang server's being down.
     *
     * @param attempts The max number of attempts to run the action.
     * @param action The action to run.
     * @return The result of the actions.
     */
    public inline fun <T: Any> attempt(attempts: Int = 3, action: Mojank.() -> MojankResult<T>): MojankResult<T> {
        repeat(attempts - 1) {
            val result = action.invoke(this)
            if (result.isConclusive) {
                return result
            }
        }
        return action.invoke(this)
    }

    /**
     * Tries to fetch the [UUID] from a player's [username], case-insensitive.
     *
     * This method will return a successful result if the
     * username is valid, and you haven't been rate limited.
     *
     * @param username The player's username.
     * @return The [MojankResult].
     */
    public open suspend fun usernameToUUID(username: String): MojankResult<UUID> {
        return usernameToSimpleProfile(username).map(SimpleMojankProfile::id)
    }

    /**
     * Tries to fetch a [SimpleMojankProfile] from a player's username,
     * case-insensitive.
     *
     * This method will return a successful result if the
     * username is valid, and you haven't been rate limited.
     *
     * @param username The player's username.
     * @return The [MojankResult].
     * @see SimpleMojankProfile
     */
    public open suspend fun usernameToSimpleProfile(username: String): MojankResult<SimpleMojankProfile> {
        return request("Couldn't find any profile with that name") {
            client.get(endpoints.usernameToSimpleProfile.invoke(username))
        }
    }

    /**
     * Tries to Fetch [SimpleMojankProfile]s in batches, case-insensitive.
     *
     * This method will return a successful result if **all** the usernames
     * are valid, and you haven't been rate limited.
     * It returns a partial result if *some* of the usernames were valid,
     * any invalid usernames will be omitted in the partial.
     * And it will return a failure if none of the usernames are valid,
     * or another error occurred.
     *
     * **Important Note**: The order in which you provide the usernames
     * is not guaranteed to be the order of the returned result.
     *
     * @param usernames The collection of usernames to fetch.
     * @return The [MojankResult].
     */
    public open suspend fun usernamesToSimpleProfiles(usernames: Collection<String>): MojankResult<List<SimpleMojankProfile>> {
        if (usernames.size <= 10) {
            return le10UsernamesToSimpleProfile(usernames)
        }

        val deferred = coroutineScope {
            usernames.chunked(10).map { chunk ->
                async { le10UsernamesToSimpleProfile(chunk) }
            }
        }

        val profiles = ArrayList<SimpleMojankProfile>()
        var message: String? = null
        var conclusive = true
        for (result in deferred.awaitAll()) {
            if (!result.isSuccess) {
                message = result.getReason()
                conclusive = conclusive and result.isConclusive
                continue
            }
            profiles.addAll(result.get())
        }

        if (profiles.isEmpty()) {
            return MojankResult.failure(message!!, conclusive)
        }
        return MojankResult.successOrPartial(profiles, message, conclusive)
    }

    /**
     * Tries to fetch a [MojankProfile] from a player's username, case-insensitive.
     *
     * This method will return a successful result if the
     * username is valid, and you haven't been rate limited.
     *
     * @param username The player's username.
     * @return The [MojankResult].
     * @see MojankProfile
     */
    public open suspend fun usernameToProfile(username: String): MojankResult<MojankProfile> {
        val result = usernameToUUID(username)
        if (result.isFailure) {
            return result.into()
        }

        return uuidToProfile(result.get())
    }

    /**
     * Tries to fetch a player's username from a [UUID].
     *
     * This method will return a successful result if the
     * uuid is valid, and you haven't been rate limited.
     *
     * @param uuid The player's uuid.
     * @return The [MojankResult].
     */
    public open suspend fun uuidToUsername(uuid: UUID): MojankResult<String> {
        return uuidToProfile(uuid).map(MojankProfile::name)
    }

    /**
     * Tries to fetch a [MojankProfile] from a player's [UUID].
     *
     * This method will return a successful result if the
     * uuid is valid, and you haven't been rate limited.
     *
     * @param uuid The player's uuid.
     * @return The [MojankResult].
     * @see MojankProfile
     */
    public open suspend fun uuidToProfile(uuid: UUID): MojankResult<MojankProfile> {
        return request("Couldn't find any profile with that uuid") {
            client.get(endpoints.uuidToProfile.invoke(uuid))
        }
    }

    private suspend fun le10UsernamesToSimpleProfile(usernames: Collection<String>): MojankResult<List<SimpleMojankProfile>> {
        require(usernames.size <= 10) { "Cannot request more than 10 usernames at at time!" }
        if (usernames.isEmpty()) {
            return MojankResult.success(listOf())
        }
        return request<List<SimpleMojankProfile>>(
            "Couldn't find any profiles with those names",
            handler = { profiles ->
                if (profiles.isEmpty()) {
                    MojankResult.failure("Couldn't resolve profiles for all names", true)
                } else if (profiles.size != usernames.size) {
                    MojankResult.partial(profiles, "Couldn't resolve profiles for all names", true)
                } else {
                    MojankResult.success(profiles)
                }
            },
            request = {
                client.post(endpoints.usernameToSimpleProfileBulk) {
                    contentType(ContentType.Application.Json)
                    setBody(Json.encodeToString(usernames))
                }
            }
        )
    }

    private suspend inline fun <reified T: Any> request(
        invalid: String,
        handler: (T) -> MojankResult<T> = { value -> MojankResult.success(value) },
        request: () -> HttpResponse,
    ): MojankResult<T> {
        try {
            val response = request.invoke()
            if (response.status == HttpStatusCode.NoContent) {
                return MojankResult.failure(invalid, true)
            }
            // Sometimes Mojang decides to return HTML when their servers are down
            if (response.contentType() == ContentType.Application.Json) {
                if (response.status == HttpStatusCode.OK) {
                    return handler.invoke(response.body<T>())
                }
                return MojankResult.failure(response.body<MojankError>().errorMessage, true)
            }
            return MojankResult.failure("Service unavailable", false)
        } catch (e: IOException) {
            return MojankResult.failure("Failed to decode response body", e)
        } catch (e: UnresolvedAddressException) {
            return MojankResult.failure("Failed to resolve address", e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> HttpResponse.body(): T {
        return bodyAsChannel().toInputStream().use {
            Json.decodeFromStream(it)
        }
    }

    public companion object: Mojank()
}

/**
 * Class that provides cached wrappers for Mojang's api.
 *
 * This class caches the results for a specified duration.
 * You can either construct an instance of this class or if you
 * wish to use the default [HttpClient] you can use the companion object.
 * ```kotlin
 * val mojank = CachedMojank()
 * val result = mojank.usernameToUUID("Notch")
 * // Or
 * val result = CachedMojank.usernameToUUID("Notch")
 * ```
 *
 * For more information about specifics see the
 * [wiki page](https://wiki.vg/Mojang_API).
 *
 * @param duration The duration to cache the results for, by default 20 minutes.
 * @param client The [HttpClient] to use it for api requests.
 * @param endpoints The endpoints to target.
 */
public open class CachedMojank(
    duration: Duration = 20.minutes,
    client: HttpClient = HttpClient(CIO),
    endpoints: MojankEndpoints = MojankEndpoints.DEFAULT
): Mojank(client, endpoints) {
    private val usernameToSimpleProfile = Cache.Builder<String, MojankResult<SimpleMojankProfile>>()
        .expireAfterWrite(duration).build()
    private val usernameToProfile = Cache.Builder<String, MojankResult<MojankProfile>>()
        .expireAfterWrite(duration).build()
    private val uuidToProfile = Cache.Builder<UUID, MojankResult<MojankProfile>>()
        .expireAfterWrite(duration).build()

    override suspend fun usernameToSimpleProfile(username: String): MojankResult<SimpleMojankProfile> {
        val cached = cachedUsernameToSimpleProfile(username)
        if (cached != null) {
            return cached
        }

        val result = super.usernameToSimpleProfile(username)
        if (result.isConclusive) {
            usernameToSimpleProfile.put(username.lowercase(), result)
        }
        return result
    }

    override suspend fun usernamesToSimpleProfiles(usernames: Collection<String>): MojankResult<List<SimpleMojankProfile>> {
        val known = ArrayList<SimpleMojankProfile>()
        val unknown = ArrayList<String>()
        var message: String? = null
        for (username in usernames) {
            val cache = cachedUsernameToSimpleProfile(username)
            if (cache == null) {
                unknown.add(username)
                continue
            }

            if (cache.isSuccess) {
                known.add(cache.get())
            } else {
                message = cache.getReason()
            }
        }

        if (unknown.isEmpty()) {
            return MojankResult.successOrPartial(known, message, true)
        }

        val result = super.usernamesToSimpleProfiles(unknown)
        if (!result.isSuccess) {
            message = result.getReason()
            if (!result.isPartial && known.isEmpty()) {
                return result
            }
        }
        known.addAll(result.getOrElse { listOf() })
        return MojankResult.successOrPartial(known, message, result.isConclusive)
    }

    override suspend fun usernameToProfile(username: String): MojankResult<MojankProfile> {
        val normalized = username.lowercase()
        val cached = usernameToProfile.get(normalized)
        if (cached != null) {
            return cached
        }

        val result = super.usernameToProfile(username)
        if (result.isConclusive) {
            usernameToProfile.put(normalized, result)
        }
        result.ifSuccess { profile ->
            uuidToProfile.put(profile.id, result)
        }
        return result
    }

    override suspend fun uuidToProfile(uuid: UUID): MojankResult<MojankProfile> {
        val cached = uuidToProfile.get(uuid)
        if (cached != null) {
            return cached
        }

        val result = super.uuidToProfile(uuid)
        if (result.isConclusive) {
            uuidToProfile.put(uuid, result)
        }
        result.ifSuccess { profile ->
            usernameToProfile.put(profile.name.lowercase(), result)
        }
        return result
    }

    private fun cachedUsernameToSimpleProfile(username: String): MojankResult<SimpleMojankProfile>? {
        val normalized = username.lowercase()
        val cached = usernameToSimpleProfile.get(normalized)
        if (cached != null) {
            return cached
        }

        val profile = usernameToProfile.get(normalized)
        if (profile != null) {
            return profile.map(MojankProfile::toSimple)
        }
        return null
    }

    public companion object: CachedMojank()
}