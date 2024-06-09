package me.senseiwells.mojank

import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.senseiwells.mojank.serialization.UUIDSerializer
import java.util.UUID

/**
 * The profile returned by [Mojank.usernameToSimpleProfile]
 * and [Mojank.usernamesToSimpleProfiles].
 */
@Serializable
public data class SimpleMojankProfile(
    /**
     * The [UUID] of the player.
     */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /**
     * The username of the player, correctly capitalized.
     */
    val name: String,
    /**
     * Whether the player hasn't migrated to a Mojang account yet.
     */
    val legacy: Boolean = false,
    /**
     * Whether the player hasn't bought Minecraft.
     */
    val demo: Boolean = false
)

/**
 * The profile returned by [Mojank.usernameToProfile] and
 * [Mojank.uuidToProfile].
 *
 * This class contains information about the player's skin.
 */
@Serializable
public data class MojankProfile(
    /**
     * The [UUID] of the player.
     */
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    /**
     * The username of the player, correctly capitalized.
     */
    val name: String,
    /**
     * A list of properties for the player, containing the player's skin.
     */
    val properties: List<Property> = listOf(),
    /**
     * A list of moderation actions taken against the player.
     */
    val profileActions: List<String> = listOf(),
    /**
     * Whether the player hasn't migrated to a Mojang account yet.
     */
    val legacy: Boolean = false,
) {
    /**
     * This decodes the property containing the skin for the player.
     *
     * @return The skin for the player.
     */
    public fun getSkin(): MojankSkin {
        val property = properties.find { it.name == "textures" }!!
        return Json.decodeFromString<MojankSkin>(property.value.decodeBase64String())
    }

    /**
     * This converts this profile into a [SimpleMojankProfile].
     *
     * @return The simple version of this profile.
     */
    public fun toSimple(): SimpleMojankProfile {
        return SimpleMojankProfile(id, name, legacy)
    }

    /**
     * A property for the player.
     */
    @Serializable
    public data class Property(
        /**
         * The name of the property.
         */
        val name: String,
        /**
         * The value of the property, usually encoded in base64.
         */
        val value: String,
        /**
         * The signature of the property. May be null if unsigned.
         */
        val signature: String? = null
    )
}