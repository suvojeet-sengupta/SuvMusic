package com.suvojeet.suvmusic.listentogether

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suvojeet.suvmusic.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import android.widget.Toast

// DataStore extension
val Context.dataStore by preferencesDataStore(name = "listen_together_prefs")

// Keys
val ListenTogetherServerUrlKey = stringPreferencesKey("listenTogetherServerUrl")
val ListenTogetherSessionTokenKey = stringPreferencesKey("listenTogetherSessionToken")
val ListenTogetherRoomCodeKey = stringPreferencesKey("listenTogetherRoomCode")
val ListenTogetherUserIdKey = stringPreferencesKey("listenTogetherUserId")
val ListenTogetherIsHostKey = booleanPreferencesKey("listenTogetherIsHost")
val ListenTogetherSessionTimestampKey = longPreferencesKey("listenTogetherSessionTimestamp")
val ListenTogetherUsernameKey = stringPreferencesKey("listenTogetherUsername")


/**
 * Connection state for the Listen Together feature
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Room role for the current user
 */
enum class RoomRole {
    HOST,
    GUEST,
    NONE
}

/**
 * Log entry for debugging
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR,
    DEBUG
}

/**
 * Pending action to execute when connected
 */
sealed class PendingAction {
    data class CreateRoom(val username: String) : PendingAction()
    data class JoinRoom(val roomCode: String, val username: String) : PendingAction()
}

/**
 * WebSocket client for Listen Together feature
 */
@Singleton
class ListenTogetherClient @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ListenTogether"
        // Server provided by: https://nyx.meowery.eu/
        private const val DEFAULT_SERVER_URL = "https://metroserver.meowery.eu/ws" 
        private const val MAX_RECONNECT_ATTEMPTS = 15
        private const val INITIAL_RECONNECT_DELAY_MS = 1000L
        private const val MAX_RECONNECT_DELAY_MS = 120000L
        private const val PING_INTERVAL_MS = 25000L
        private const val MAX_LOG_ENTRIES = 500
        private const val SESSION_GRACE_PERIOD_MS = 10 * 60 * 1000L

        // Notification constants
        private const val NOTIFICATION_CHANNEL_ID = "listen_together_channel"
        const val ACTION_APPROVE_JOIN = "com.suvojeet.suvmusic.LISTEN_TOGETHER_APPROVE_JOIN"
        const val ACTION_REJECT_JOIN = "com.suvojeet.suvmusic.LISTEN_TOGETHER_REJECT_JOIN"
        const val ACTION_APPROVE_SUGGESTION = "com.suvojeet.suvmusic.LISTEN_TOGETHER_APPROVE_SUGGESTION"
        const val ACTION_REJECT_SUGGESTION = "com.suvojeet.suvmusic.LISTEN_TOGETHER_REJECT_SUGGESTION"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_SUGGESTION_ID = "extra_suggestion_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        @Volatile
        private var instance: ListenTogetherClient? = null
        
        fun getInstance(): ListenTogetherClient? = instance
        
        fun setInstance(client: ListenTogetherClient) {
            instance = client
        }
    }
    
    init {
        setInstance(this)
        ensureNotificationChannel()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            loadPersistedSession()
        }
    }
    
    /**
     * Load persisted session information from storage
     */
    private suspend fun loadPersistedSession() {
        try {
            val prefs = context.dataStore.data.first()
            val token = prefs[ListenTogetherSessionTokenKey] ?: ""
            val roomCode = prefs[ListenTogetherRoomCodeKey] ?: ""
            val userId = prefs[ListenTogetherUserIdKey] ?: ""
            val isHost = prefs[ListenTogetherIsHostKey] ?: false
            val timestamp = prefs[ListenTogetherSessionTimestampKey] ?: 0L
            
            // Check if session is still valid (within grace period)
            if (token.isNotEmpty() && roomCode.isNotEmpty() && 
                (System.currentTimeMillis() - timestamp < SESSION_GRACE_PERIOD_MS)) {
                sessionToken = token
                storedRoomCode = roomCode
                _userId.value = userId.ifEmpty { null }
                wasHost = isHost
                sessionStartTime = timestamp
                log(LogLevel.INFO, "Loaded persisted session", "Room: $roomCode, Host: $isHost")
            } else if (token.isNotEmpty()) {
                log(LogLevel.WARNING, "Session expired", "Age: ${System.currentTimeMillis() - timestamp}ms")
                clearPersistedSession()
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to load persisted session", e.message)
        }
    }
    
    /**
     * Save current session information to persistent storage
     */
    private fun savePersistedSession() {
        try {
            scope.launch {
                context.dataStore.edit { preferences ->
                    if (sessionToken != null) {
                        preferences[ListenTogetherSessionTokenKey] = sessionToken!!
                        preferences[ListenTogetherRoomCodeKey] = storedRoomCode ?: ""
                        preferences[ListenTogetherUserIdKey] = _userId.value ?: ""
                        preferences[ListenTogetherIsHostKey] = wasHost
                        preferences[ListenTogetherSessionTimestampKey] = System.currentTimeMillis()
                    }
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to save persisted session", e.message)
        }
    }
    
    /**
     * Clear persisted session information
     */
    private fun clearPersistedSession() {
        try {
            scope.launch {
                context.dataStore.edit { preferences ->
                    preferences.remove(ListenTogetherSessionTokenKey)
                    preferences.remove(ListenTogetherRoomCodeKey)
                    preferences.remove(ListenTogetherUserIdKey)
                    preferences.remove(ListenTogetherIsHostKey)
                    preferences.remove(ListenTogetherSessionTimestampKey)
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Failed to clear persisted session", e.message)
        }
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectAttempts = 0
    
    // Session info for reconnection
    private var sessionToken: String? = null
    private var storedUsername: String? = null
    private var storedRoomCode: String? = null
    private var wasHost: Boolean = false
    private var sessionStartTime: Long = 0
    
    // Pending actions to execute when connected
    private var pendingAction: PendingAction? = null
    
    // Wake lock to keep connection alive when in a room
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Track notification IDs for join requests to dismiss them from both UI and notification actions
    private val joinRequestNotifications = mutableMapOf<String, Int>()

    // Track notification IDs for suggestions to dismiss them similarly
    private val suggestionNotifications = mutableMapOf<String, Int>()

    // State flows
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _role = MutableStateFlow(RoomRole.NONE)
    val role: StateFlow<RoomRole> = _role.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoinRequests = MutableStateFlow<List<JoinRequestPayload>>(emptyList())
    val pendingJoinRequests: StateFlow<List<JoinRequestPayload>> = _pendingJoinRequests.asStateFlow()

    private val _bufferingUsers = MutableStateFlow<List<String>>(emptyList())
    val bufferingUsers: StateFlow<List<String>> = _bufferingUsers.asStateFlow()

    // Suggestions: pending items visible to host
    private val _pendingSuggestions = MutableStateFlow<List<SuggestionReceivedPayload>>(emptyList())
    val pendingSuggestions: StateFlow<List<SuggestionReceivedPayload>> = _pendingSuggestions.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Event flow
    private val _events = MutableSharedFlow<ListenTogetherEvent>()
    val events: SharedFlow<ListenTogetherEvent> = _events.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private suspend fun getServerUrl(): String {
        val prefs = context.dataStore.data.first()
        return prefs[ListenTogetherServerUrlKey] ?: DEFAULT_SERVER_URL
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RECONNECT_DELAY_MS * (2 shl (minOf(attempt - 1, 4)))
        val cappedDelay = minOf(exponentialDelay, MAX_RECONNECT_DELAY_MS)
        val jitter = (cappedDelay * 0.2 * Math.random()).toLong()
        return cappedDelay + jitter
    }

    suspend fun getSavedUsername(): String {
        val prefs = context.dataStore.data.first()
        return prefs[ListenTogetherUsernameKey] ?: ""
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[ListenTogetherUsernameKey] = username
        }
    }

    private fun log(level: LogLevel, message: String, details: String? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        val entry = LogEntry(timestamp, level, message, details)
        
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)
        
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, "$message ${details ?: ""}")
            LogLevel.WARNING -> Log.w(TAG, "$message ${details ?: ""}")
            LogLevel.DEBUG -> Log.d(TAG, "$message ${details ?: ""}")
            LogLevel.INFO -> Log.i(TAG, "$message ${details ?: ""}")
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED || 
            _connectionState.value == ConnectionState.CONNECTING) {
            log(LogLevel.WARNING, "Already connected or connecting")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        
        scope.launch {
            val url = getServerUrl()
            log(LogLevel.INFO, "Connecting to server", url)

            val request = Request.Builder()
                .url(url)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    log(LogLevel.INFO, "Connected to server")
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0
                    startPingJob()
                    
                    if (sessionToken != null && storedRoomCode != null) {
                        log(LogLevel.INFO, "Attempting to reconnect to previous session", "Room: $storedRoomCode")
                        sendMessage(MessageTypes.RECONNECT, ReconnectPayload(sessionToken!!))
                    } else {
                        executePendingAction()
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    log(LogLevel.INFO, "Server closing connection", "Code: $code, Reason: $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    log(LogLevel.INFO, "Connection closed", "Code: $code, Reason: $reason")
                    handleDisconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    log(LogLevel.ERROR, "Connection failure", t.message)
                    handleConnectionFailure(t)
                }
            })
        }
    }
    
    private fun executePendingAction() {
        val action = pendingAction ?: return
        pendingAction = null
        
        when (action) {
            is PendingAction.CreateRoom -> {
                log(LogLevel.INFO, "Executing pending create room", action.username)
                sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(action.username))
            }
            is PendingAction.JoinRoom -> {
                log(LogLevel.INFO, "Executing pending join room", "${action.roomCode} as ${action.username}")
                sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(action.roomCode.uppercase(), action.username))
            }
        }
    }

    fun disconnect() {
        log(LogLevel.INFO, "Disconnecting from server")
        releaseWakeLock()
        pingJob?.cancel()
        pingJob = null
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        
        sessionToken = null
        storedRoomCode = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        clearPersistedSession()
        reconnectAttempts = 0
        
        scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
    }

    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (true) {
                delay(PING_INTERVAL_MS)
                sendMessageNoPayload(MessageTypes.PING)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService<PowerManager>()
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SuvMusic:ListenTogether"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(30 * 60 * 1000L)
            log(LogLevel.DEBUG, "Wake lock acquired")
        }
    }
    
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            log(LogLevel.DEBUG, "Wake lock released")
        }
    }

    private fun ensureNotificationChannel() {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            val existing = nm?.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.listen_together),
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = "Notifications for Listen Together events"
                nm?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            log(LogLevel.WARNING, "Failed to create notification channel", e.message)
        }
    }

    private fun showJoinRequestNotification(payload: JoinRequestPayload) {
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        joinRequestNotifications[payload.userId] = notifId

        val approveIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_APPROVE_JOIN
            putExtra(EXTRA_USER_ID, payload.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_REJECT_JOIN
            putExtra(EXTRA_USER_ID, payload.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(context, payload.userId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rejectPI = PendingIntent.getBroadcast(context, payload.userId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val content = "${payload.username} wants to join the room"

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Approve", approvePI)
            .addAction(0, "Reject", rejectPI)

        if (checkNotificationPermission()) {
             NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    private fun showSuggestionNotification(payload: SuggestionReceivedPayload) {
        val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        suggestionNotifications[payload.suggestionId] = notifId

        val approveIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_APPROVE_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, payload.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val rejectIntent = Intent(context, ListenTogetherActionReceiver::class.java).apply {
            action = ACTION_REJECT_SUGGESTION
            putExtra(EXTRA_SUGGESTION_ID, payload.suggestionId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }

        val approvePI = PendingIntent.getBroadcast(context, payload.suggestionId.hashCode(), approveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rejectPI = PendingIntent.getBroadcast(context, payload.suggestionId.hashCode().inv(), rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val content = "${payload.fromUsername} requested ${payload.trackInfo.title}"

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(context.getString(R.string.listen_together))
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Approve", approvePI)
            .addAction(0, "Reject", rejectPI)

        if (checkNotificationPermission()) {
             NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             return androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun handleDisconnect() {
        pingJob?.cancel()
        pingJob = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        if (sessionToken != null && _roomState.value != null) {
            log(LogLevel.INFO, "Connection lost, will attempt to reconnect")
            handleConnectionFailure(Exception("Connection lost"))
        } else {
            scope.launch { _events.emit(ListenTogetherEvent.Disconnected) }
        }
    }

    private fun handleConnectionFailure(t: Throwable) {
        pingJob?.cancel()
        pingJob = null
        
        val shouldReconnect = sessionToken != null || _roomState.value != null || pendingAction != null
        
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && shouldReconnect) {
            reconnectAttempts++
            _connectionState.value = ConnectionState.RECONNECTING
            
            val delayMs = calculateBackoffDelay(reconnectAttempts)
            
            log(LogLevel.INFO, "Attempting reconnect", 
                "Attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS, waiting ${delayMs/1000}s, reason: ${t.message}")
            
            scope.launch {
                _events.emit(ListenTogetherEvent.Reconnecting(reconnectAttempts, MAX_RECONNECT_ATTEMPTS))
                delay(delayMs)
                
                if (_connectionState.value == ConnectionState.RECONNECTING || _connectionState.value == ConnectionState.DISCONNECTED) {
                    connect()
                }
            }
        } else {
            _connectionState.value = ConnectionState.ERROR
            
            if (sessionToken != null) {
                log(LogLevel.ERROR, "Reconnection failed", 
                    "Max attempts reached, but session preserved for manual reconnect")
                scope.launch { 
                    _events.emit(ListenTogetherEvent.ConnectionError(
                        "Connection failed after $MAX_RECONNECT_ATTEMPTS attempts. ${t.message ?: "Unknown error"}"
                    ))
                }
            } else {
                sessionToken = null
                storedRoomCode = null
                storedUsername = null
                _roomState.value = null
                _role.value = RoomRole.NONE
                clearPersistedSession()
                
                scope.launch { 
                    _events.emit(ListenTogetherEvent.ConnectionError(t.message ?: "Unknown error"))
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString<Message>(text)
            
            when (message.type) {
                MessageTypes.ROOM_CREATED -> {
                    val payload = json.decodeFromJsonElement<RoomCreatedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = RoomRole.HOST
                    sessionToken = payload.sessionToken
                    storedRoomCode = payload.roomCode
                    wasHost = true
                    sessionStartTime = System.currentTimeMillis()
                    
                    _roomState.value = RoomState(
                        roomCode = payload.roomCode,
                        hostId = payload.userId,
                        users = listOf(UserInfo(payload.userId, storedUsername ?: "", true)),
                        isPlaying = false,
                        position = 0,
                        lastUpdate = System.currentTimeMillis()
                    )
                    
                    savePersistedSession()
                    acquireWakeLock()
                    log(LogLevel.INFO, "Room created", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.RoomCreated(payload.roomCode, payload.userId)) }
                }
                
                MessageTypes.JOIN_REQUEST -> {
                    val payload = json.decodeFromJsonElement<JoinRequestPayload>(message.payload!!)
                    _pendingJoinRequests.value = _pendingJoinRequests.value + payload
                    log(LogLevel.INFO, "Join request received", "User: ${payload.username}")
                    if (_role.value == RoomRole.HOST) {
                        showJoinRequestNotification(payload)
                    }
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRequestReceived(payload.userId, payload.username)) }
                }
                
                MessageTypes.JOIN_APPROVED -> {
                    val payload = json.decodeFromJsonElement<JoinApprovedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = RoomRole.GUEST
                    sessionToken = payload.sessionToken
                    storedRoomCode = payload.roomCode
                    wasHost = false
                    sessionStartTime = System.currentTimeMillis()
                    
                    _roomState.value = payload.state
                    savePersistedSession()
                    acquireWakeLock()
                    log(LogLevel.INFO, "Joined room", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.JoinApproved(payload.roomCode, payload.userId, payload.state)) }
                }
                
                MessageTypes.JOIN_REJECTED -> {
                    val payload = json.decodeFromJsonElement<JoinRejectedPayload>(message.payload!!)
                    log(LogLevel.WARNING, "Join rejected", payload.reason)
                    scope.launch { _events.emit(ListenTogetherEvent.JoinRejected(payload.reason)) }
                }
                
                MessageTypes.USER_JOINED -> {
                    val payload = json.decodeFromJsonElement<UserJoinedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users + UserInfo(payload.userId, payload.username, false)
                    )
                    _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != payload.userId }
                    
                    joinRequestNotifications.remove(payload.userId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                    
                    log(LogLevel.INFO, "User joined", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserJoined(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_LEFT -> {
                    val payload = json.decodeFromJsonElement<UserLeftPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.filter { it.userId != payload.userId }
                    )
                    log(LogLevel.INFO, "User left", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserLeft(payload.userId, payload.username)) }
                }
                
                MessageTypes.HOST_CHANGED -> {
                    val payload = json.decodeFromJsonElement<HostChangedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        hostId = payload.newHostId,
                        users = _roomState.value!!.users.map { 
                            it.copy(isHost = it.userId == payload.newHostId)
                        }
                    )
                    if (payload.newHostId == _userId.value) {
                        _role.value = RoomRole.HOST
                    }
                    log(LogLevel.INFO, "Host changed", "New host: ${payload.newHostName}")
                    scope.launch { _events.emit(ListenTogetherEvent.HostChanged(payload.newHostId, payload.newHostName)) }
                }
                
                MessageTypes.KICKED -> {
                    val payload = json.decodeFromJsonElement<KickedPayload>(message.payload!!)
                    log(LogLevel.WARNING, "Kicked from room", payload.reason)
                    releaseWakeLock()
                    sessionToken = null
                    _roomState.value = null
                    _role.value = RoomRole.NONE
                    scope.launch { _events.emit(ListenTogetherEvent.Kicked(payload.reason)) }
                }
                
                MessageTypes.SYNC_PLAYBACK -> {
                    val payload = json.decodeFromJsonElement<PlaybackActionPayload>(message.payload!!)
                    log(LogLevel.DEBUG, "Playback sync", "Action: ${payload.action}")
                    
                    when (payload.action) {
                        PlaybackActions.PLAY -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = true,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.PAUSE -> {
                            _roomState.value = _roomState.value?.copy(
                                isPlaying = false,
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.SEEK -> {
                            _roomState.value = _roomState.value?.copy(
                                position = payload.position ?: _roomState.value!!.position
                            )
                        }
                        PlaybackActions.CHANGE_TRACK -> {
                            _roomState.value = _roomState.value?.copy(
                                currentTrack = payload.trackInfo,
                                isPlaying = false,
                                position = 0
                            )
                        }
                    }
                    
                    scope.launch { _events.emit(ListenTogetherEvent.PlaybackSync(payload)) }
                }
                
                MessageTypes.BUFFER_WAIT -> {
                    val payload = json.decodeFromJsonElement<BufferWaitPayload>(message.payload!!)
                    _bufferingUsers.value = payload.waitingFor
                    log(LogLevel.DEBUG, "Waiting for buffering", "Users: ${payload.waitingFor.size}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferWait(payload.trackId, payload.waitingFor)) }
                }
                
                MessageTypes.BUFFER_COMPLETE -> {
                    val payload = json.decodeFromJsonElement<BufferCompletePayload>(message.payload!!)
                    _bufferingUsers.value = emptyList()
                    log(LogLevel.INFO, "All users buffered", "Track: ${payload.trackId}")
                    scope.launch { _events.emit(ListenTogetherEvent.BufferComplete(payload.trackId)) }
                }
                
                MessageTypes.SYNC_STATE -> {
                    val payload = json.decodeFromJsonElement<SyncStatePayload>(message.payload!!)
                    log(LogLevel.INFO, "Sync state received", "Playing: ${payload.isPlaying}, Position: ${payload.position}")
                    scope.launch { _events.emit(ListenTogetherEvent.SyncStateReceived(payload)) }
                }
                
                MessageTypes.CHAT_MESSAGE -> {
                    val payload = json.decodeFromJsonElement<ChatMessagePayload>(message.payload!!)
                    log(LogLevel.DEBUG, "Chat message", "${payload.username}: ${payload.message}")
                    scope.launch { 
                        _events.emit(ListenTogetherEvent.ChatReceived(
                            payload.userId, 
                            payload.username, 
                            payload.message, 
                            payload.timestamp
                        ))
                    }
                }

                MessageTypes.SUGGESTION_RECEIVED -> {
                    val payload = json.decodeFromJsonElement<SuggestionReceivedPayload>(message.payload!!)
                    if (_role.value == RoomRole.HOST) {
                        _pendingSuggestions.value = _pendingSuggestions.value + payload
                        log(LogLevel.INFO, "Suggestion received", "${payload.fromUsername}: ${payload.trackInfo.title}")
                        showSuggestionNotification(payload)
                    }
                }

                MessageTypes.SUGGESTION_APPROVED -> {
                    val payload = json.decodeFromJsonElement<SuggestionApprovedPayload>(message.payload!!)
                    log(LogLevel.INFO, "Suggestion approved", payload.trackInfo.title)
                    
                    suggestionNotifications.remove(payload.suggestionId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                }

                MessageTypes.SUGGESTION_REJECTED -> {
                    val payload = json.decodeFromJsonElement<SuggestionRejectedPayload>(message.payload!!)
                    log(LogLevel.WARNING, "Suggestion rejected", payload.reason ?: "")
                    
                    suggestionNotifications.remove(payload.suggestionId)?.let { notifId ->
                        NotificationManagerCompat.from(context).cancel(notifId)
                    }
                }
                
                MessageTypes.ERROR -> {
                    val payload = json.decodeFromJsonElement<ErrorPayload>(message.payload!!)
                    log(LogLevel.ERROR, "Server error", "${payload.code}: ${payload.message}")
                    
                    when (payload.code) {
                        "session_not_found" -> {
                            if (storedRoomCode != null && storedUsername != null && !wasHost) {
                                log(LogLevel.WARNING, "Session expired on server", "Attempting automatic rejoin")
                                scope.launch {
                                    delay(500)
                                    joinRoom(storedRoomCode!!, storedUsername!!)
                                }
                            } else {
                                clearPersistedSession()
                                sessionToken = null
                            }
                        }
                    }
                    
                    scope.launch { _events.emit(ListenTogetherEvent.ServerError(payload.code, payload.message)) }
                }
                
                MessageTypes.PONG -> {
                    log(LogLevel.DEBUG, "Pong received")
                }
                
                MessageTypes.RECONNECTED -> {
                    val payload = json.decodeFromJsonElement<ReconnectedPayload>(message.payload!!)
                    _userId.value = payload.userId
                    _role.value = if (payload.isHost) RoomRole.HOST else RoomRole.GUEST
                    _roomState.value = payload.state
                    
                    wasHost = payload.isHost
                    sessionStartTime = System.currentTimeMillis()
                    savePersistedSession()
                    reconnectAttempts = 0
                    
                    acquireWakeLock()
                    log(LogLevel.INFO, "Successfully reconnected to room", "Code: ${payload.roomCode}")
                    scope.launch { _events.emit(ListenTogetherEvent.Reconnected(payload.roomCode, payload.userId, payload.state, payload.isHost)) }
                }
                
                MessageTypes.USER_RECONNECTED -> {
                    val payload = json.decodeFromJsonElement<UserReconnectedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = true) else user
                        }
                    )
                    log(LogLevel.INFO, "User reconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserReconnected(payload.userId, payload.username)) }
                }
                
                MessageTypes.USER_DISCONNECTED -> {
                    val payload = json.decodeFromJsonElement<UserDisconnectedPayload>(message.payload!!)
                    _roomState.value = _roomState.value?.copy(
                        users = _roomState.value!!.users.map { user ->
                            if (user.userId == payload.userId) user.copy(isConnected = false) else user
                        }
                    )
                    log(LogLevel.INFO, "User temporarily disconnected", payload.username)
                    scope.launch { _events.emit(ListenTogetherEvent.UserDisconnected(payload.userId, payload.username)) }
                }
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "Error parsing message", e.message)
        }
    }

    private inline fun <reified T> sendMessage(type: String, payload: T?) {
        val message = if (payload != null) {
            Message(type, json.encodeToJsonElement(payload))
        } else {
            Message(type, null)
        }
        
        val text = json.encodeToString(message)
        webSocket?.send(text)
    }
    
    private fun sendMessageNoPayload(type: String) {
        val message = Message(type, null)
        val text = json.encodeToString(message)
        webSocket?.send(text)
    }

    fun createRoom(username: String) {
        clearPersistedSession()
        sessionToken = null
        storedRoomCode = null
        wasHost = false
        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.CREATE_ROOM, CreateRoomPayload(username))
        } else {
            pendingAction = PendingAction.CreateRoom(username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        clearPersistedSession()
        sessionToken = null
        storedRoomCode = null
        wasHost = false
        storedUsername = username
        
        if (_connectionState.value == ConnectionState.CONNECTED) {
            sendMessage(MessageTypes.JOIN_ROOM, JoinRoomPayload(roomCode.uppercase(), username))
        } else {
            pendingAction = PendingAction.JoinRoom(roomCode, username)
            if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value == ConnectionState.ERROR) {
                connect()
            }
        }
    }

    fun leaveRoom() {
        sendMessageNoPayload(MessageTypes.LEAVE_ROOM)
        
        sessionToken = null
        storedRoomCode = null
        storedUsername = null
        pendingAction = null
        _roomState.value = null
        _role.value = RoomRole.NONE
        _userId.value = null
        _pendingJoinRequests.value = emptyList()
        _bufferingUsers.value = emptyList()
        
        clearPersistedSession()
        releaseWakeLock()
    }

    fun approveJoin(userId: String) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.APPROVE_JOIN, ApproveJoinPayload(userId))
            joinRequestNotifications.remove(userId)?.let { notifId ->
                NotificationManagerCompat.from(context).cancel(notifId)
            }
        }
    }

    fun rejectJoin(userId: String, reason: String? = null) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.REJECT_JOIN, RejectJoinPayload(userId, reason))
            _pendingJoinRequests.value = _pendingJoinRequests.value.filter { it.userId != userId }
            joinRequestNotifications.remove(userId)?.let { notifId ->
                NotificationManagerCompat.from(context).cancel(notifId)
            }
        }
    }

    fun kickUser(userId: String, reason: String? = null) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.KICK_USER, KickUserPayload(userId, reason))
        }
    }

    fun sendPlaybackAction(
        action: String, 
        trackId: String? = null, 
        position: Long? = null, 
        trackInfo: TrackInfo? = null, 
        insertNext: Boolean? = null, 
        queue: List<TrackInfo>? = null,
        queueTitle: String? = null
    ) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.PLAYBACK_ACTION, PlaybackActionPayload(action, trackId, position, trackInfo, insertNext, queue, queueTitle))
        }
    }

    fun sendBufferReady(trackId: String) {
        sendMessage(MessageTypes.BUFFER_READY, BufferReadyPayload(trackId))
    }

    fun sendChat(message: String) {
        if (_roomState.value != null) {
            sendMessage(MessageTypes.CHAT, ChatPayload(message))
        }
    }

    fun suggestTrack(trackInfo: TrackInfo) {
        if (isInRoom && _role.value != RoomRole.HOST) {
            sendMessage(MessageTypes.SUGGEST_TRACK, SuggestTrackPayload(trackInfo))
        }
    }

    fun approveSuggestion(suggestionId: String) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.APPROVE_SUGGESTION, ApproveSuggestionPayload(suggestionId))
            _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
            suggestionNotifications.remove(suggestionId)?.let { notifId ->
                NotificationManagerCompat.from(context).cancel(notifId)
            }
        }
    }

    fun rejectSuggestion(suggestionId: String, reason: String? = null) {
        if (_role.value == RoomRole.HOST) {
            sendMessage(MessageTypes.REJECT_SUGGESTION, RejectSuggestionPayload(suggestionId, reason))
            _pendingSuggestions.value = _pendingSuggestions.value.filter { it.suggestionId != suggestionId }
            suggestionNotifications.remove(suggestionId)?.let { notifId ->
                NotificationManagerCompat.from(context).cancel(notifId)
            }
        }
    }

    fun requestSync() {
        if (_roomState.value != null) {
            sendMessageNoPayload(MessageTypes.REQUEST_SYNC)
        }
    }

    val isInRoom: Boolean
        get() = _roomState.value != null

    val isHost: Boolean
        get() = _role.value == RoomRole.HOST
    
    fun forceReconnect() {
        log(LogLevel.INFO, "Forcing reconnection to server")
        reconnectAttempts = 0
        
        webSocket?.close(1000, "Forcing reconnection")
        webSocket = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        
        scope.launch {
            delay(500)
            connect()
        }
    }
    
    val hasPersistedSession: Boolean
        get() = sessionToken != null && storedRoomCode != null
    
    fun getPersistedRoomCode(): String? = storedRoomCode
    
    fun getSessionAge(): Long = if (sessionStartTime > 0) {
        System.currentTimeMillis() - sessionStartTime
    } else {
        0L
    }
}
