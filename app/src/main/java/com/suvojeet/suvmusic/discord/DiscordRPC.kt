package com.suvojeet.suvmusic.discord

import android.util.Log
import com.suvojeet.suvmusic.discord.Identify.Companion.toIdentifyPayload
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DiscordRPC(
    private val token: String,
) : CoroutineScope {
    private val tag = "DiscordRPC"
    private val gatewayUrl = "wss://gateway.discord.gg/?v=10&encoding=json"
    private var websocket: DefaultClientWebSocketSession? = null
    private var sequence = 0
    private var sessionId: String? = null
    private var heartbeatInterval = 0L
    private var resumeGatewayUrl: String? = null
    private var heartbeatJob: Job? = null
    private var connected = false
    private var client: HttpClient = HttpClient {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
    }
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var reconnectionJob: Job? = null
    private var currentReconnectDelay = INITIAL_RECONNECT_DELAY
    private var lastPresence: Presence? = null

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    fun connect() {
        if (connected) {
            Log.i(tag, "Gateway already connected.")
            return
        }
        reconnectionJob?.cancel()
        reconnectionJob = launch {
            try {
                val url = resumeGatewayUrl ?: gatewayUrl
                Log.i(tag, "Connecting to Discord Gateway at $url")
                websocket = client.webSocketSession(url)
                connected = true
                Log.i(tag, "Successfully connected to Discord Gateway.")
                currentReconnectDelay = INITIAL_RECONNECT_DELAY
                // start receiving messages
                websocket!!.incoming.receiveAsFlow()
                    .collect {
                        when (it) {
                            is Frame.Text -> {
                                val jsonString = it.readText()
                                try {
                                    onMessage(json.decodeFromString(jsonString))
                                } catch (e: Exception) {
                                     Log.e(tag, "Error decoding message: $jsonString", e)
                                }
                            }

                            else -> {}
                        }
                    }
                handleClose()
            } catch (e: Exception) {
                Log.e(tag, "Gateway connection error", e)
                scheduleReconnection()
            }
        }
    }

    private fun scheduleReconnection() {
        if (reconnectionJob?.isActive == true) {
            return
        }
        heartbeatJob?.cancel()
        connected = false
        reconnectionJob = launch {
            delay(currentReconnectDelay)
            Log.i(tag, "Attempting to reconnect...")
            connect()
            currentReconnectDelay = (currentReconnectDelay * 2).coerceAtMost(MAX_RECONNECT_DELAY)
        }
    }


    private suspend fun handleClose() {
        heartbeatJob?.cancel()
        connected = false
        val close = websocket?.closeReason?.await()
        Log.w(tag, "Gateway closed with code: ${close?.code}, reason: ${close?.message}")
        
        // 4004: Authentication Failed - Do not reconnect
        if (close?.code?.toInt() == 4004) {
             Log.e(tag, "Authentication Failed. Please check your token.")
             return
        }
        
        if (close?.code?.toInt() == 4000) {
            delay(200.milliseconds)
            connect()
        } else
            scheduleReconnection()
    }

    private suspend fun onMessage(payload: Payload) {
        // Log.v(tag, "Gateway received: op=${payload.op}, seq=${payload.s}, event=${payload.t}")
        payload.s?.let {
            sequence = it
        }
        when (payload.op) {
            OpCode.DISPATCH -> payload.handleDispatch()
            OpCode.HEARTBEAT -> sendHeartBeat()
            OpCode.RECONNECT -> {
                Log.i(tag, "Received RECONNECT OpCode")
                reconnectWebSocket()
            }
            OpCode.INVALID_SESSION -> handleInvalidSession()
            OpCode.HELLO -> payload.handleHello()
            OpCode.HEARTBEAT_ACK -> {
                // Heartbeat acknowledged
            }
            else -> {}
        }
    }

    private fun Payload.handleDispatch() {
        when (this.t.toString()) {
            "READY" -> {
                val ready = json.decodeFromJsonElement<Ready>(this.d!!)
                sessionId = ready.sessionId
                resumeGatewayUrl = ready.resumeGatewayUrl + "/?v=10&encoding=json"
                Log.i(tag, "Gateway READY: resume_gateway_url updated to $resumeGatewayUrl, session_id updated to $sessionId")
                connected = true
                
                // Resend presence if we have one
                lastPresence?.let { 
                    launch { sendActivity(it) }
                }
                return
            }

            "RESUMED" -> {
                Log.i(tag, "Gateway: Session Resumed")
            }

            else -> {}
        }
    }

    private suspend inline fun handleInvalidSession() {
        Log.w(tag, "Gateway: Handling Invalid Session. Sending Identify after 150ms")
        delay(150)
        sendIdentify()
    }

    private suspend inline fun Payload.handleHello() {
        if (sequence > 0 && !sessionId.isNullOrBlank()) {
            sendResume()
        } else {
            sendIdentify()
        }
        heartbeatInterval = json.decodeFromJsonElement<Heartbeat>(this.d!!).heartbeatInterval
        Log.i(tag, "Gateway: Setting heartbeatInterval=$heartbeatInterval")
        startHeartbeatJob(heartbeatInterval)
    }

    private suspend fun sendHeartBeat() {
        // Log.v(tag, "Gateway: Sending HEARTBEAT with seq: $sequence")
        send(
            op = OpCode.HEARTBEAT,
            d = if (sequence == 0) null else sequence,
        )
    }

    private suspend inline fun reconnectWebSocket() {
        websocket?.close(
            CloseReason(
                code = 4000,
                message = "Attempting to reconnect"
            )
        )
    }

    private suspend fun sendIdentify() {
        Log.i(tag, "Gateway: Sending IDENTIFY")
        send(
            op = OpCode.IDENTIFY,
            d = token.toIdentifyPayload()
        )
    }

    private suspend fun sendResume() {
        Log.i(tag, "Gateway: Sending RESUME")
        send(
            op = OpCode.RESUME,
            d = Resume(
                seq = sequence,
                sessionId = sessionId,
                token = token
            )
        )
    }

    private fun startHeartbeatJob(interval: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = launch {
            while (isActive) {
                sendHeartBeat()
                delay(interval)
            }
        }
    }

    private fun isSocketConnectedToAccount(): Boolean {
        return connected && websocket?.isActive == true
    }

    fun isWebSocketConnected(): Boolean {
        return websocket?.incoming != null && websocket?.outgoing?.isClosedForSend == false
    }

    private suspend inline fun <reified T> send(op: OpCode, d: T?) {
        if (websocket?.isActive == true) {
            val payload = json.encodeToString(
                Payload(
                    op = op,
                    d = json.encodeToJsonElement(d),
                )
            )
            // Log.v(tag, "Gateway sending payload: $payload")
            websocket?.send(Frame.Text(payload))
        }
    }

    fun close() {
        reconnectionJob?.cancel()
        heartbeatJob?.cancel()
        heartbeatJob = null
        this.cancel()
        resumeGatewayUrl = null
        sessionId = null
        connected = false
        runBlocking {
            try {
                websocket?.close()
                Log.i(tag, "Gateway: Connection to gateway closed")
            } catch (e: Exception) {
                Log.e(tag, "Error closing gateway", e)
            }
        }
    }

    fun updateActivity(
        name: String,
        state: String?, // Artist
        details: String?, // Song Title
        imageUrl: String? = null,
        largeText: String? = null,
        smallText: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Int = 2, // Listening
        buttonLabel: String? = null,
        buttonUrl: String? = null
    ) {
         val presence = Presence(
            activities = listOf(
                Activity(
                    name = name,
                    state = state,
                    details = details,
                    type = type, // Listening
                    timestamps = Timestamps(startTime, endTime),
                    assets = Assets(
                        largeImage = imageUrl, // Use direct URL, client often handles it if valid or proxy might be needed but mp: prefix is unreliable
                        // NOTE: For external images to work without an app ID, we ideally need a proxy or they might not show up.
                        // Standard Discord RPC requires an Application ID and uploaded assets or specific external URL capability.
                        // Assuming 'mp:external' works or we might need to rely on generic icons if not using a specific App ID.
                        // Actually, Kizzy uses a specific repository to resolve images. We might skip complex image resolution for now
                        // and just try direct URL or a default one if this fails.
                        // For user tokens, custom images are tricky.
                        largeText = largeText
                    ),
                    buttons = if (buttonLabel != null && buttonUrl != null) listOf(buttonLabel) else null,
                    metadata = if (buttonUrl != null) Metadata(buttonUrls = listOf(buttonUrl)) else null,
                    applicationId = null // We are using User Token, usually doesn't bind to specific App ID unless we want it to.
                )
            ),
            afk = false,
            since = System.currentTimeMillis(),
            status = "online"
        )
        
        lastPresence = presence
        
        launch {
            sendActivity(presence)
        }
    }
    
    suspend fun sendActivity(presence: Presence) {
        if (!isSocketConnectedToAccount()) {
             // If not connected, we should try to connect or wait.
             // But for now, just logging it.
             if(!connected) connect()
             return
        }
        
        try {
            Log.i(tag, "Gateway: Sending PRESENCE_UPDATE")
            send(
                op = OpCode.PRESENCE_UPDATE,
                d = presence
            )
        } catch (e: Exception) {
            Log.e(tag, "Error sending activity", e)
        }
    }
    
    companion object {
        private val INITIAL_RECONNECT_DELAY = 1.seconds
        private val MAX_RECONNECT_DELAY = 60.seconds
    }
}
