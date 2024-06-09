package me.senseiwells.mojank

import kotlinx.serialization.Serializable

@Serializable
internal data class MojankError(
    val path: String,
    val errorMessage: String
)