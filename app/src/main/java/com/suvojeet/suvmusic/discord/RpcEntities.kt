package com.suvojeet.suvmusic.discord

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// OpCodes
@Serializable(with = OpCodeSerializer::class)
enum class OpCode(val value: Int) {
    DISPATCH(0),
    HEARTBEAT(1),
    IDENTIFY(2),
    PRESENCE_UPDATE(3),
    VOICE_STATE_UPDATE(4),
    RESUME(6),
    RECONNECT(7),
    REQUEST_GUILD_MEMBERS(8),
    INVALID_SESSION(9),
    HELLO(10),
    HEARTBEAT_ACK(11)
}

@Serializable
data class Payload(
    @SerialName("op") val op: OpCode,
    @SerialName("d") val d: JsonElement? = null,
    @SerialName("s") val s: Int? = null,
    @SerialName("t") val t: String? = null
)

@Serializable
data class Identify(
    @SerialName("token") val token: String,
    @SerialName("properties") val properties: IdentifyProperties,
    @SerialName("compress") val compress: Boolean? = false,
    @SerialName("large_threshold") val largeThreshold: Int? = null
) {
    companion object {
        fun String.toIdentifyPayload(): Identify {
            return Identify(
                token = this,
                properties = IdentifyProperties(
                    os = "Linux",
                    browser = "Discord Client",
                    device = "Discord Client"
                )
            )
        }
    }
}

@Serializable
data class IdentifyProperties(
    @SerialName("\$os") val os: String,
    @SerialName("\$browser") val browser: String,
    @SerialName("\$device") val device: String
)

@Serializable
data class Heartbeat(
    @SerialName("heartbeat_interval") val heartbeatInterval: Long
)

@Serializable
data class Ready(
    @SerialName("v") val v: Int,
    @SerialName("user") val user: User,
    @SerialName("private_channels") val privateChannels: List<JsonElement>,
    @SerialName("guilds") val guilds: List<JsonElement>,
    @SerialName("session_id") val sessionId: String,
    @SerialName("resume_gateway_url") val resumeGatewayUrl: String
)

@Serializable
data class User(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("discriminator") val discriminator: String,
    @SerialName("avatar") val avatar: String?,
    @SerialName("bot") val bot: Boolean? = false
)

@Serializable
data class Resume(
    @SerialName("token") val token: String,
    @SerialName("session_id") val sessionId: String?,
    @SerialName("seq") val seq: Int
)

// Presence Entities
@Serializable
data class Presence(
    @SerialName("activities") val activities: List<Activity>,
    @SerialName("status") val status: String,
    @SerialName("afk") val afk: Boolean,
    @SerialName("since") val since: Long?
)

@Serializable
data class Activity(
    @SerialName("name") val name: String,
    @SerialName("type") val type: Int,
    @SerialName("url") val url: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("timestamps") val timestamps: Timestamps? = null,
    @SerialName("application_id") val applicationId: String? = null,
    @SerialName("details") val details: String? = null,
    @SerialName("state") val state: String? = null, // Used for artist
    @SerialName("emoji") val emoji: Emoji? = null,
    @SerialName("party") val party: Party? = null,
    @SerialName("assets") val assets: Assets? = null,
    @SerialName("secrets") val secrets: Secrets? = null,
    @SerialName("instance") val instance: Boolean? = null,
    @SerialName("flags") val flags: Int? = null,
    @SerialName("buttons") val buttons: List<String>? = null,
    @SerialName("metadata") val metadata: Metadata? = null
)

@Serializable
data class Timestamps(
    @SerialName("start") val start: Long? = null,
    @SerialName("end") val end: Long? = null
)

@Serializable
data class Emoji(
    @SerialName("name") val name: String,
    @SerialName("id") val id: String? = null,
    @SerialName("animated") val animated: Boolean? = null
)

@Serializable
data class Party(
    @SerialName("id") val id: String? = null,
    @SerialName("size") val size: List<Int>? = null
)

@Serializable
data class Assets(
    @SerialName("large_image") val largeImage: String? = null,
    @SerialName("large_text") val largeText: String? = null,
    @SerialName("small_image") val smallImage: String? = null,
    @SerialName("small_text") val smallText: String? = null
)

@Serializable
data class Secrets(
    @SerialName("join") val join: String? = null,
    @SerialName("spectate") val spectate: String? = null,
    @SerialName("match") val match: String? = null
)

@Serializable
data class Metadata(
    @SerialName("button_urls") val buttonUrls: List<String>? = null
)
object OpCodeSerializer : KSerializer<OpCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OpCode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: OpCode) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): OpCode {
        val v = decoder.decodeInt()
        return OpCode.entries.find { it.value == v } ?: OpCode.DISPATCH
    }
}
