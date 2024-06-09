package me.senseiwells.mojank

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.senseiwells.mojank.serialization.UUIDSerializer
import java.util.*

/**
 * The class representing a player's skin.
 *
 * @see MojankProfile.getSkin
 */
@Serializable
public data class MojankSkin(
    /**
     * The timestamp of when the skin was retrieved.
     */
    val timestamp: Long,
    /**
     * The [UUID] of the player.
     */
    @Serializable(with = UUIDSerializer::class)
    val profileId: UUID,
    /**
     * The username of the player.
     */
    val profileName: String,
    /**
     * Whether this property requires a signature.
     */
    val signatureRequired: Boolean = false,
    /**
     * The textures for the skin.
     */
    val textures: Textures
) {
    /**
     * This represents the textures for a player's skin.
     */
    @Serializable
    public data class Textures(
        /**
         * This contains the data for the player's skin.
         */
        @SerialName("SKIN")
        val skin: Skin,
        /**
         * This contains the data for the player's cape.
         */
        @SerialName("CAPE")
        val cape: Cape? = null
    ) {
        /**
         * This represents the player's skin data.
         */
        @Serializable
        public data class Skin(
            /**
             * The url of the player's skin texture.
             */
            val url: String,
            /**
             * The metadata for the player's skin.
             */
            val metadata: Metadata = Metadata()
        ) {
            /**
             * This represents the metadata for the player's skin.
             *
             * Determines whether the skin is rendered in the 'classic' or 'slim' style.
             */
            @Serializable
            public data class Metadata(val model: String = "classic")
        }

        /**
         * This represents the player's cape data.
         *
         * Contains the url for the cape texture.
         */
        @Serializable
        public data class Cape(val url: String)
    }
}