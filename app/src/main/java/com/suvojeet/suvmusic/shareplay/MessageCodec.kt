/**
 * SuvMusic Project
 * Licensed under GPL-3.0
 */

package com.suvojeet.suvmusic.shareplay

import android.util.Log
import com.google.protobuf.MessageLite
import com.suvojeet.suvmusic.shareplay.proto.SharePlay
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Codec for encoding and decoding messages using Protocol Buffers
 */
class MessageCodec(
    var compressionEnabled: Boolean = false
) {
    companion object {
        private const val TAG = "MessageCodec"
        private const val COMPRESSION_THRESHOLD = 100 // Only compress if > 100 bytes
    }
    
    /**
     * Encode a message using Protocol Buffers
     */
    fun encode(msgType: String, payload: Any?): ByteArray {
        return encodeProtobuf(msgType, payload)
    }
    
    /**
     * Decode a protobuf message
     */
    fun decode(data: ByteArray): Pair<String, ByteArray> {
        return decodeProtobuf(data)
    }
    
    /**
     * Encode message using Protocol Buffers
     */
    private fun encodeProtobuf(msgType: String, payload: Any?): ByteArray {
        var payloadBytes = byteArrayOf()
        var compressed = false
        
        if (payload != null) {
            val protoMsg = toProtoMessage(payload)
            payloadBytes = protoMsg.toByteArray()
            
            // Compress if enabled and payload is large enough
            if (compressionEnabled && payloadBytes.size > COMPRESSION_THRESHOLD) {
                val compressedBytes = compressData(payloadBytes)
                if (compressedBytes.size < payloadBytes.size) {
                    payloadBytes = compressedBytes
                    compressed = true
                }
            }
        }
        
        val envelope = SharePlay.Envelope.newBuilder()
            .setType(msgType)
            .setPayload(com.google.protobuf.ByteString.copyFrom(payloadBytes))
            .setCompressed(compressed)
            .build()
        
        return envelope.toByteArray()
    }
    
    /**
     * Decode protobuf message
     */
    private fun decodeProtobuf(data: ByteArray): Pair<String, ByteArray> {
        val envelope = SharePlay.Envelope.parseFrom(data)
        
        var payloadBytes = envelope.payload.toByteArray()
        
        if (envelope.compressed) {
            payloadBytes = decompressData(payloadBytes) ?: payloadBytes
        }
        
        return Pair(envelope.type, payloadBytes)
    }
    
    /**
     * Compress data using GZIP
     */
    private fun compressData(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream(data.size)
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(data)
        }
        return outputStream.toByteArray()
    }
    
    /**
     * Decompress GZIP data
     */
    private fun decompressData(data: ByteArray): ByteArray? {
        return try {
            val len = data.size
            val isize = if (len >= 4) {
                (data[len - 4].toInt() and 0xFF) or
                ((data[len - 3].toInt() and 0xFF) shl 8) or
                ((data[len - 2].toInt() and 0xFF) shl 16) or
                ((data[len - 1].toInt() and 0xFF) shl 24)
            } else {
                0
            }
            val inputStream = ByteArrayInputStream(data)
            GZIPInputStream(inputStream).use { gzip ->
                if (isize > 0) {
                    val buffer = ByteArray(isize)
                    var offset = 0
                    while (offset < isize) {
                        val read = gzip.read(buffer, offset, isize - offset)
                        if (read == -1) break
                        offset += read
                    }
                    if (offset == isize && gzip.read() == -1) {
                        buffer
                    } else {
                        val out = ByteArrayOutputStream(isize + 4096)
                        out.write(buffer, 0, offset)
                        val tempBuf = ByteArray(4096)
                        while (true) {
                            val read = gzip.read(tempBuf)
                            if (read == -1) break
                            out.write(tempBuf, 0, read)
                        }
                        out.toByteArray()
                    }
                } else {
                    gzip.readBytes()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decompress data", e)
            null
        }
    }
    
    /**
     * Convert Kotlin objects to protobuf messages
     */
    private fun toProtoMessage(payload: Any): MessageLite {
        return when (payload) {
            is CreateRoomPayload -> SharePlay.CreateRoomPayload.newBuilder()
                .setUsername(payload.username)
                .build()
            is JoinRoomPayload -> SharePlay.JoinRoomPayload.newBuilder()
                .setRoomCode(payload.roomCode)
                .setUsername(payload.username)
                .build()
            is ApproveJoinPayload -> SharePlay.ApproveJoinPayload.newBuilder()
                .setUserId(payload.userId)
                .build()
            is RejectJoinPayload -> SharePlay.RejectJoinPayload.newBuilder()
                .setUserId(payload.userId)
                .setReason(payload.reason ?: "")
                .build()
            is PlaybackActionPayload -> {
                val builder = SharePlay.PlaybackActionPayload.newBuilder()
                    .setAction(payload.action)
                    .setPosition(payload.position ?: 0)
                    .setInsertNext(payload.insertNext ?: false)
                    .setVolume(payload.volume ?: 1f)
                    .setServerTime(payload.serverTime ?: 0)
                
                payload.trackId?.let { builder.setTrackId(it) }
                payload.trackInfo?.let { builder.setTrackInfo(trackInfoToProto(it)) }
                payload.queueTitle?.let { builder.setQueueTitle(it) }
                payload.queue?.forEach { track ->
                    builder.addQueue(trackInfoToProto(track))
                }
                
                builder.build()
            }
            is BufferReadyPayload -> SharePlay.BufferReadyPayload.newBuilder()
                .setTrackId(payload.trackId)
                .build()
            is KickUserPayload -> SharePlay.KickUserPayload.newBuilder()
                .setUserId(payload.userId)
                .setReason(payload.reason ?: "")
                .build()
            is SuggestTrackPayload -> {
                val builder = SharePlay.SuggestTrackPayload.newBuilder()
                payload.trackInfo.let { builder.setTrackInfo(trackInfoToProto(it)) }
                builder.build()
            }
            is ApproveSuggestionPayload -> SharePlay.ApproveSuggestionPayload.newBuilder()
                .setSuggestionId(payload.suggestionId)
                .build()
            is RejectSuggestionPayload -> SharePlay.RejectSuggestionPayload.newBuilder()
                .setSuggestionId(payload.suggestionId)
                .setReason(payload.reason ?: "")
                .build()
            is ReconnectPayload -> SharePlay.ReconnectPayload.newBuilder()
                .setSessionToken(payload.sessionToken)
                .build()
            is TransferHostPayload -> SharePlay.TransferHostPayload.newBuilder()
                .setNewHostId(payload.newHostId)
                .build()
            is SetRoomSettingsPayload -> SharePlay.SetRoomSettingsPayload.newBuilder()
                .setSettings(roomSettingsToProto(payload.settings))
                .build()
            else -> throw IllegalArgumentException("Unsupported payload type: ${payload::class.simpleName}")
        }
    }
    
    /**
     * Decode protobuf payload to Kotlin objects
     */
    fun decodePayload(msgType: String, payloadBytes: ByteArray): Any? {
        if (payloadBytes.isEmpty()) return null
        
        return decodeProtobufPayload(msgType, payloadBytes)
    }
    
    /**
     * Decode protobuf payload
     */
    private fun decodeProtobufPayload(msgType: String, payloadBytes: ByteArray): Any? {
        return when (msgType) {
            MessageTypes.ROOM_CREATED -> {
                val pb = SharePlay.RoomCreatedPayload.parseFrom(payloadBytes)
                RoomCreatedPayload(pb.roomCode, pb.userId, pb.sessionToken)
            }
            MessageTypes.JOIN_REQUEST -> {
                val pb = SharePlay.JoinRequestPayload.parseFrom(payloadBytes)
                JoinRequestPayload(pb.userId, pb.username)
            }
            MessageTypes.JOIN_APPROVED -> {
                val pb = SharePlay.JoinApprovedPayload.parseFrom(payloadBytes)
                JoinApprovedPayload(
                    pb.roomCode,
                    pb.userId,
                    pb.sessionToken,
                    protoToRoomState(pb.state)
                )
            }
            MessageTypes.JOIN_REJECTED -> {
                val pb = SharePlay.JoinRejectedPayload.parseFrom(payloadBytes)
                JoinRejectedPayload(pb.reason)
            }
            MessageTypes.USER_JOINED -> {
                val pb = SharePlay.UserJoinedPayload.parseFrom(payloadBytes)
                UserJoinedPayload(pb.userId, pb.username)
            }
            MessageTypes.USER_LEFT -> {
                val pb = SharePlay.UserLeftPayload.parseFrom(payloadBytes)
                UserLeftPayload(pb.userId, pb.username)
            }
            MessageTypes.SYNC_PLAYBACK -> {
                val pb = SharePlay.PlaybackActionPayload.parseFrom(payloadBytes)
                PlaybackActionPayload(
                    action = pb.action,
                    trackId = pb.trackId.takeIf { it.isNotEmpty() },
                    position = pb.position.takeIf { it > 0 },
                    trackInfo = pb.trackInfo?.let { protoToTrackInfo(it) },
                    insertNext = pb.insertNext.takeIf { it },
                    queue = pb.queueList?.map { protoToTrackInfo(it) },
                    queueTitle = pb.queueTitle.takeIf { it.isNotEmpty() },
                    // Keep volume as-is (do NOT drop 0.0) so a mute (set_volume → 0) syncs.
                    volume = pb.volume,
                    serverTime = pb.serverTime.takeIf { it > 0 }
                )
            }
            MessageTypes.BUFFER_WAIT -> {
                val pb = SharePlay.BufferWaitPayload.parseFrom(payloadBytes)
                BufferWaitPayload(pb.trackId, pb.waitingForList)
            }
            MessageTypes.BUFFER_COMPLETE -> {
                val pb = SharePlay.BufferCompletePayload.parseFrom(payloadBytes)
                BufferCompletePayload(pb.trackId)
            }
            MessageTypes.ERROR -> {
                val pb = SharePlay.ErrorPayload.parseFrom(payloadBytes)
                ErrorPayload(pb.code, pb.message)
            }
            MessageTypes.HOST_CHANGED -> {
                val pb = SharePlay.HostChangedPayload.parseFrom(payloadBytes)
                HostChangedPayload(pb.newHostId, pb.newHostName)
            }
            MessageTypes.KICKED -> {
                val pb = SharePlay.KickedPayload.parseFrom(payloadBytes)
                KickedPayload(pb.reason)
            }
            MessageTypes.SYNC_STATE -> {
                val pb = SharePlay.SyncStatePayload.parseFrom(payloadBytes)
                SyncStatePayload(
                    currentTrack = pb.currentTrack?.let { protoToTrackInfo(it) },
                    isPlaying = pb.isPlaying,
                    position = pb.position,
                    lastUpdate = pb.lastUpdate,
                    queue = pb.queueList?.map { protoToTrackInfo(it) },
                    volume = pb.volume.takeIf { it > 0 }
                )
            }
            MessageTypes.RECONNECTED -> {
                val pb = SharePlay.ReconnectedPayload.parseFrom(payloadBytes)
                ReconnectedPayload(
                    pb.roomCode,
                    pb.userId,
                    protoToRoomState(pb.state),
                    pb.isHost
                )
            }
            MessageTypes.USER_RECONNECTED -> {
                val pb = SharePlay.UserReconnectedPayload.parseFrom(payloadBytes)
                UserReconnectedPayload(pb.userId, pb.username)
            }
            MessageTypes.USER_DISCONNECTED -> {
                val pb = SharePlay.UserDisconnectedPayload.parseFrom(payloadBytes)
                UserDisconnectedPayload(pb.userId, pb.username)
            }
            MessageTypes.SUGGESTION_RECEIVED -> {
                val pb = SharePlay.SuggestionReceivedPayload.parseFrom(payloadBytes)
                SuggestionReceivedPayload(
                    pb.suggestionId,
                    pb.fromUserId,
                    pb.fromUsername,
                    protoToTrackInfo(pb.trackInfo)
                )
            }
            MessageTypes.SUGGESTION_APPROVED -> {
                val pb = SharePlay.SuggestionApprovedPayload.parseFrom(payloadBytes)
                SuggestionApprovedPayload(
                    pb.suggestionId,
                    protoToTrackInfo(pb.trackInfo)
                )
            }
            MessageTypes.SUGGESTION_REJECTED -> {
                val pb = SharePlay.SuggestionRejectedPayload.parseFrom(payloadBytes)
                SuggestionRejectedPayload(pb.suggestionId, pb.reason.takeIf { it.isNotEmpty() })
            }
            MessageTypes.ROOM_SETTINGS_CHANGED -> {
                val pb = SharePlay.RoomSettingsChangedPayload.parseFrom(payloadBytes)
                RoomSettingsChangedPayload(
                    settings = protoToRoomSettings(pb.settings),
                    changedBy = pb.changedBy.takeIf { it.isNotEmpty() }
                )
            }
            else -> null
        }
    }
    
    // Helper conversion functions
    
    private fun trackInfoToProto(track: TrackInfo): SharePlay.TrackInfo {
        return SharePlay.TrackInfo.newBuilder()
            .setId(track.id)
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbum(track.album ?: "")
            .setDuration(track.duration)
            .setThumbnail(track.thumbnail ?: "")
            .setSuggestedBy(track.suggestedBy ?: "")
            .setAudioSource(track.audioSource ?: "")
            .setSourceTrackId(track.sourceTrackId ?: "")
            .setAudioQuality(track.audioQuality ?: "")
            .build()
    }
    
    private fun protoToTrackInfo(proto: SharePlay.TrackInfo): TrackInfo {
        return TrackInfo(
            id = proto.id,
            title = proto.title,
            artist = proto.artist,
            album = proto.album.takeIf { it.isNotEmpty() },
            duration = proto.duration,
            thumbnail = proto.thumbnail.takeIf { it.isNotEmpty() },
            suggestedBy = proto.suggestedBy.takeIf { it.isNotEmpty() },
            audioSource = proto.audioSource.takeIf { it.isNotEmpty() },
            sourceTrackId = proto.sourceTrackId.takeIf { it.isNotEmpty() },
            audioQuality = proto.audioQuality.takeIf { it.isNotEmpty() }
        )
    }
    
    private fun protoToUserInfo(proto: SharePlay.UserInfo): UserInfo {
        return UserInfo(
            userId = proto.userId,
            username = proto.username,
            isHost = proto.isHost,
            isConnected = proto.isConnected
        )
    }
    
    private fun protoToRoomState(proto: SharePlay.RoomState): RoomState {
        return RoomState(
            roomCode = proto.roomCode,
            hostId = proto.hostId,
            users = proto.usersList.map { protoToUserInfo(it) },
            currentTrack = proto.currentTrack?.let { protoToTrackInfo(it) },
            isPlaying = proto.isPlaying,
            position = proto.position,
            lastUpdate = proto.lastUpdate,
            volume = proto.volume,
            queue = proto.queueList.map { protoToTrackInfo(it) },
            settings = protoToRoomSettings(proto.settings)
        )
    }

    private fun roomSettingsToProto(settings: RoomSettings): SharePlay.RoomSettings {
        return SharePlay.RoomSettings.newBuilder()
            .setGuestsCanQueue(settings.guestsCanQueue)
            .setGuestsCanControl(settings.guestsCanControl)
            .build()
    }

    private fun protoToRoomSettings(proto: SharePlay.RoomSettings?): RoomSettings {
        if (proto == null) return RoomSettings()
        return RoomSettings(
            guestsCanQueue = proto.guestsCanQueue,
            guestsCanControl = proto.guestsCanControl
        )
    }
}
