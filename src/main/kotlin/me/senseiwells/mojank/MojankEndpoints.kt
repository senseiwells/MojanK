package me.senseiwells.mojank

import java.util.UUID

/**
 * The endpoints for the mojang api.
 *
 * You may wish to provide different endpoints
 * in the case that the default ones are unavailable.
 */
public data class MojankEndpoints(
    val uuidToProfile: (UUID) -> String,
    val usernameToSimpleProfile: (String) -> String,
    val usernameToSimpleProfileBulk: String
) {
    public companion object {
        public val DEFAULT: MojankEndpoints = MojankEndpoints(
            { "https://sessionserver.mojang.com/session/minecraft/profile/$it?unsigned=false" },
            { "https://api.mojang.com/users/profiles/minecraft/$it" },
            "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname"
        )

        public val ALTERNATE: MojankEndpoints = DEFAULT.copy(
            usernameToSimpleProfile = { "https://api.minecraftservices.com/minecraft/profile/lookup/name/${it}" }
        )
    }
}
