package com.suvojeet.suvmusic.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.ui.components.SeekbarStyle
import com.suvojeet.suvmusic.ui.views.DynamicSeekbarView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service that displays a Floating Player overlay when music is playing
 * and the app is in the background. Draggable to any position on screen.
 */
@AndroidEntryPoint
class DynamicIslandService : Service() {
    
    @Inject
    lateinit var musicPlayer: MusicPlayer
    
    @Inject
    lateinit var sessionManager: SessionManager
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isExpanded = false
    private var isSeeking = false
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserverJob: Job? = null
    
    companion object {
        private const val CHANNEL_ID = "floating_player_channel"
        private const val NOTIFICATION_ID = 2001
        
        fun hasOverlayPermission(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }
        
        fun start(context: Context) {
            if (!hasOverlayPermission(context)) return
            val intent = Intent(context, DynamicIslandService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, DynamicIslandService::class.java))
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        observePlayerState()
        observeSeekbarStyle()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        removeOverlay()
    }

    private fun observeSeekbarStyle() {
        serviceScope.launch {
            sessionManager.seekbarStyleFlow.collectLatest { styleName ->
                val style = try {
                    SeekbarStyle.valueOf(styleName)
                } catch (e: Exception) {
                    SeekbarStyle.WAVEFORM
                }
                overlayView?.findViewById<DynamicSeekbarView>(R.id.dynamicSeekbar)?.style = style
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating player controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Player Active")
            .setContentText("Drag to move anywhere on screen")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun observePlayerState() {
        stateObserverJob = serviceScope.launch {
            musicPlayer.playerState.collectLatest { state ->
                if (state.currentSong != null && state.isPlaying) {
                    showOverlay(state)
                } else if (state.currentSong == null) {
                    removeOverlay()
                }
                // Update overlay content if visible
                updateOverlayContent(state)
            }
        }
    }
    
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showOverlay(state: PlayerState) {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return
        
        // Wrap context with theme to allow attribute resolution
        val themeContext = android.view.ContextThemeWrapper(this, R.style.Theme_SuvMusic)
        val inflater = LayoutInflater.from(themeContext)
        overlayView = inflater.inflate(R.layout.dynamic_island_layout, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0  // Will be centered initially
            y = 100 // Offset from top
        }
        
        setupOverlayInteractions()
        updateOverlayContent(state)
        
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayInteractions() {
        val view = overlayView ?: return
        
        // Custom Seekbar Logic
        view.findViewById<DynamicSeekbarView>(R.id.dynamicSeekbar)?.apply {
            onSeekListener = { progress ->
                isSeeking = true
                val duration = musicPlayer.playerState.value.duration
                if (duration > 0) {
                    val newPosition = (progress * duration).toLong()
                    musicPlayer.seekTo(newPosition)
                }
                isSeeking = false
            }
        }
        
        // Expanded controls - these still use click listeners
        view.findViewById<ImageButton>(R.id.btnPlayPause)?.setOnClickListener {
            musicPlayer.togglePlayPause()
        }
        
        view.findViewById<ImageButton>(R.id.btnNext)?.setOnClickListener {
            musicPlayer.seekToNext()
        }
        
        view.findViewById<ImageButton>(R.id.btnPrev)?.setOnClickListener {
            musicPlayer.seekToPrevious()
        }
        
        // Close button
        view.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
            removeOverlay()
        }
        
        // Drag state variables
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val touchSlop = 15 // pixels to consider a drag vs click
        
        // Helper function to setup drag on a container
        fun setupDragOnView(containerView: View, onClickAction: () -> Unit) {
            containerView.setOnTouchListener { _, event ->
                val params = view.layoutParams as? WindowManager.LayoutParams 
                    ?: return@setOnTouchListener false
                
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        
                        // Only start dragging after moving beyond touch slop
                        if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                            isDragging = true
                        }
                        
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(view, params)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // It was a click, not a drag
                            onClickAction()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        
        // Setup drag + click for collapsed container (click = expand)
        view.findViewById<View>(R.id.collapsedContainer)?.let { collapsed ->
            setupDragOnView(collapsed) { toggleExpanded() }
        }
        
        // Setup drag + click for expanded container (click = open app)
        view.findViewById<View>(R.id.expandedContainer)?.let { expanded ->
            setupDragOnView(expanded) { openApp() }
        }
    }
    
    private fun toggleExpanded() {
        isExpanded = !isExpanded
        val view = overlayView ?: return
        
        view.findViewById<View>(R.id.collapsedContainer)?.visibility = 
            if (isExpanded) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.expandedContainer)?.visibility = 
            if (isExpanded) View.VISIBLE else View.GONE
    }
    
    private fun updateOverlayContent(state: PlayerState) {
        val view = overlayView ?: return
        val song = state.currentSong ?: return
        
        // Update song info
        view.findViewById<TextView>(R.id.tvTitle)?.text = song.title
        view.findViewById<TextView>(R.id.tvArtist)?.text = song.artist
        view.findViewById<TextView>(R.id.tvTitleExpanded)?.text = song.title
        view.findViewById<TextView>(R.id.tvArtistExpanded)?.text = song.artist
        
        // Update Seekbar
        if (!isSeeking && state.duration > 0) {
            val progress = state.currentPosition.toFloat() / state.duration
            view.findViewById<DynamicSeekbarView>(R.id.dynamicSeekbar)?.progress = progress
        }
        
        // Update play/pause button
        val playPauseBtn = view.findViewById<ImageButton>(R.id.btnPlayPause)
        playPauseBtn?.setImageResource(
            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        
        // Load album art
        val artworkView = view.findViewById<ImageView>(R.id.ivArtwork)
        val artworkExpandedView = view.findViewById<ImageView>(R.id.ivArtworkExpanded)
        
        song.thumbnailUrl?.let { url ->
            val imageLoader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .target { drawable ->
                    artworkView?.setImageDrawable(drawable)
                    artworkExpandedView?.setImageDrawable(drawable)
                }
                .build()
            imageLoader.enqueue(request)
        }
    }
    
    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
        isExpanded = false
    }
    
    private fun openApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        removeOverlay()
    }
}