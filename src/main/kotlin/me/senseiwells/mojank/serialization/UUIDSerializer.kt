package me.senseiwells.mojank.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

internal object UUIDSerializer: KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        val string = decoder.decodeString()
        try {
            if (string.length == 32) {
                val builder = StringBuilder(string)
                builder.insert(20, '-')
                builder.insert(16, '-')
                builder.insert(12, '-')
                builder.insert(8, '-')
                return UUID.fromString(builder.toString())
            } else if (string.length == 36) {
                return UUID.fromString(string)
            }
        } catch (_: IllegalArgumentException) {

        }
        throw SerializationException("Invalid uuid provided")
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}