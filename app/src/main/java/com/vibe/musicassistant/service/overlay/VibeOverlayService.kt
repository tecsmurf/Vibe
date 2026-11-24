package com.vibe.musicassistant.service.overlay

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vibe.musicassistant.MainActivity
import com.vibe.musicassistant.R
import com.vibe.musicassistant.VibeApplication
import com.vibe.musicassistant.domain.model.OrbVisibility
import com.vibe.musicassistant.domain.model.VibePermission
import com.vibe.musicassistant.domain.model.VibeSettings
import com.vibe.musicassistant.domain.model.VoiceCommand
import com.vibe.musicassistant.domain.model.VoiceStatus
import com.vibe.musicassistant.presentation.theme.VibeTheme
import com.vibe.musicassistant.service.VibeRuntime
import com.vibe.musicassistant.service.media.VibeMusicAppDetector
import com.vibe.musicassistant.service.voice.VibeCommandParser
import com.vibe.musicassistant.service.voice.VibeVoiceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Foreground service that owns the floating orb and the feedback/quick-control bubble,
 * both drawn with Compose inside [WindowManager] overlay windows. It starts only after
 * the user activates VIBE and stops on "Stop VIBE". The microphone is used only between
 * an orb tap and the end of that one command.
 */
class VibeOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    private val app get() = application as VibeApplication
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private lateinit var voice: VibeVoiceController
    private lateinit var detector: VibeMusicAppDetector

    private var orbView: ComposeView? = null
    private var bubbleView: ComposeView? = null
    private lateinit var orbParams: WindowManager.LayoutParams

    private val panelOpen = MutableStateFlow(false)
    private val hiddenByUser = MutableStateFlow(false)
    private var settings = VibeSettings()
    private var resetJob: Job? = null
    private var panelJob: Job? = null
    private var snapAnimator: ValueAnimator? = null

    override fun onCreate() {
        savedStateController.performRestore(null)
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        voice = VibeVoiceController(this)
        detector = VibeMusicAppDetector(this, app.mediaRepository)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                lifecycleScope.launch {
                    app.preferences.setAssistantEnabled(false)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_SHOW_ORB -> hiddenByUser.value = false
            ACTION_RESET_POSITION -> orbView?.let { placeOrbAtDefault(); persistPosition() }
        }

        if (!Settings.canDrawOverlays(this) || !goForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (orbView == null) start()
        return START_STICKY
    }

    private fun start() {
        VibeRuntime.serviceRunning.value = true
        app.mediaRepository.startObserving()
        lifecycleScope.launch {
            settings = app.preferences.settings.first()
            if (!settings.assistantEnabled) {
                stopSelf()
                return@launch
            }
            addBubble()
            addOrb()
            observe()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observe() {
        lifecycleScope.launch {
            app.preferences.settings.collect { s ->
                settings = s
                if (!s.assistantEnabled) stopSelf()
            }
        }
        // Orb visibility: user's "hide", plus "only with music" mode.
        lifecycleScope.launch {
            val visibilityMode = app.preferences.settings
                .map { it.orbVisibility }
                .distinctUntilChanged()
                .flatMapLatest { mode ->
                    if (mode == OrbVisibility.ALWAYS) flowOf(true)
                    else detector.musicContext { app.permissions.isGranted(VibePermission.USAGE_ACCESS) }
                }
            combine(visibilityMode, hiddenByUser, panelOpen) { allowed, hidden, panel ->
                (allowed || panel) && !hidden
            }.collect { show ->
                orbView?.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
        // Return to idle a moment after a result is shown.
        lifecycleScope.launch {
            VibeRuntime.voiceStatus.collectLatest { status ->
                if (status is VoiceStatus.Success || status is VoiceStatus.Error || status is VoiceStatus.Unconfirmed) {
                    delay(if (status is VoiceStatus.Success) 2_200 else 3_500)
                    VibeRuntime.voiceStatus.value = VoiceStatus.Idle
                }
            }
        }
    }

    // region Foreground notification

    private fun goForeground(): Boolean {
        val notification = buildNotification()
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val withMic = when {
            Build.VERSION.SDK_INT >= 34 -> ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                (if (micGranted) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
            Build.VERSION.SDK_INT >= 30 && micGranted -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else -> 0
        }
        val withoutMic = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, withMic)
            true
        } catch (e: Exception) {
            // Android may refuse the microphone type when started from the background
            // (for example after the system restarts the service). Keep the orb anyway.
            runCatching { ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, withoutMic) }.isSuccess
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "VIBE assistant", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shown while the VIBE orb is active"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, VibeOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val show = PendingIntent.getService(
            this, 2, Intent(this, VibeOverlayService::class.java).setAction(ACTION_SHOW_ORB),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vibe)
            .setContentTitle("VIBE is on")
            .setContentText("Tap the orb and speak to control your music.")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, "Show orb", show)
            .addAction(0, "Stop VIBE", stop)
            .build()
    }

    // endregion

    // region Windows

    private fun overlayParams(width: Int, height: Int, gravity: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { this.gravity = gravity }

    private fun composeView(content: @androidx.compose.runtime.Composable () -> Unit) =
        ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VibeOverlayService)
            setViewTreeSavedStateRegistryOwner(this@VibeOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { VibeTheme { content() } }
        }

    private fun orbSizePx(): Int = (OVERLAY_ORB_DP * OVERLAY_ORB_CANVAS_SCALE * resources.displayMetrics.density).roundToInt()

    private fun addOrb() {
        val size = orbSizePx()
        orbParams = overlayParams(size, size, Gravity.TOP or Gravity.START)
        if (settings.orbX >= 0 && settings.orbY >= 0) {
            orbParams.x = settings.orbX
            orbParams.y = settings.orbY
            clampOrb()
        } else {
            placeOrbAtDefault()
        }
        val view = composeView {
            val status by VibeRuntime.voiceStatus.collectAsStateWithLifecycle()
            OverlayOrb(status)
        }
        view.contentDescription = "VIBE orb. Tap to speak, long press for controls."
        attachTouchHandling(view)
        orbView = view
        windowManager.addView(view, orbParams)
    }

    private fun addBubble() {
        val params = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        ).apply { y = (110 * resources.displayMetrics.density).roundToInt() }
        val view = composeView {
            val status by VibeRuntime.voiceStatus.collectAsStateWithLifecycle()
            val panel by panelOpen.collectAsStateWithLifecycle()
            val media by app.mediaRepository.current.collectAsStateWithLifecycle()
            OverlayBubble(
                status = status,
                panelOpen = panel,
                media = media,
                onPrevious = { quickCommand(VoiceCommand.PREVIOUS_TRACK) },
                onPlayPause = { quickToggle() },
                onNext = { quickCommand(VoiceCommand.NEXT_TRACK) },
                onHideOrb = {
                    panelOpen.value = false
                    hiddenByUser.value = true
                    VibeRuntime.voiceStatus.value =
                        VoiceStatus.Success("Orb hidden. Use \"Show orb\" in the VIBE notification.")
                },
                onClosePanel = { panelOpen.value = false },
            )
        }
        bubbleView = view
        windowManager.addView(view, params)
    }

    private fun screenBounds(): Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

    private fun placeOrbAtDefault() {
        val screen = screenBounds()
        orbParams.x = screen.width() - orbSizePx()
        orbParams.y = (screen.height() * 0.42f).roundToInt()
        orbView?.let { windowManager.updateViewLayout(it, orbParams) }
    }

    private fun clampOrb() {
        val screen = screenBounds()
        val size = orbSizePx()
        orbParams.x = orbParams.x.coerceIn(0, (screen.width() - size).coerceAtLeast(0))
        orbParams.y = orbParams.y.coerceIn(0, (screen.height() - size).coerceAtLeast(0))
    }

    private fun persistPosition() {
        val x = orbParams.x
        val y = orbParams.y
        lifecycleScope.launch { app.preferences.setOrbPosition(x, y) }
    }

    /** Snaps the orb to the nearest side so it doesn't sit over app controls. */
    private fun snapToEdge() {
        val view = orbView ?: return
        val screen = screenBounds()
        val size = orbSizePx()
        val targetX = if (orbParams.x + size / 2 < screen.width() / 2) 0 else screen.width() - size
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(orbParams.x, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                orbParams.x = it.animatedValue as Int
                if (view.isAttachedToWindow) windowManager.updateViewLayout(view, orbParams)
            }
            doOnEndCompat { persistPosition() }
            start()
        }
    }

    private inline fun ValueAnimator.doOnEndCompat(crossinline action: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = action()
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachTouchHandling(view: View) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            openPanel()
        }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = orbParams.x
                    startY = orbParams.y
                    dragging = false
                    longPressed = false
                    handler.postDelayed(longPress, longPressTimeout)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && !longPressed && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        orbParams.x = startX + dx.roundToInt()
                        orbParams.y = startY + dy.roundToInt()
                        clampOrb()
                        windowManager.updateViewLayout(v, orbParams)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    when {
                        dragging -> snapToEdge()
                        !longPressed -> {
                            v.performClick()
                            onOrbTap()
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    if (dragging) snapToEdge()
                }
            }
            true
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        orbView?.let {
            clampOrb()
            windowManager.updateViewLayout(it, orbParams)
            snapToEdge()
        }
    }

    // endregion

    // region Interaction

    private fun openPanel() {
        voice.cancel()
        VibeRuntime.voiceStatus.value = VoiceStatus.Idle
        app.mediaRepository.refresh()
        panelOpen.value = true
        schedulePanelClose()
    }

    private fun schedulePanelClose() {
        panelJob?.cancel()
        panelJob = lifecycleScope.launch {
            delay(8_000)
            panelOpen.value = false
        }
    }

    private fun onOrbTap() {
        panelOpen.value = false
        if (voice.isActive) {
            voice.cancel()
            VibeRuntime.voiceStatus.value = VoiceStatus.Idle
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            VibeRuntime.voiceStatus.value = VoiceStatus.Error("Allow microphone access in VIBE first.")
            openApp()
            return
        }
        VibeRuntime.voiceStatus.value = VoiceStatus.Listening()
        voice.start(settings.voiceLanguage.tag, object : VibeVoiceController.Listener {
            override fun onListening() {
                VibeRuntime.voiceStatus.value = VoiceStatus.Listening()
            }

            override fun onPartial(text: String) {
                VibeRuntime.voiceStatus.value = VoiceStatus.Listening(text)
            }

            override fun onProcessing() {
                val heard = (VibeRuntime.voiceStatus.value as? VoiceStatus.Listening)?.partialText.orEmpty()
                VibeRuntime.voiceStatus.value = VoiceStatus.Processing(heard.ifBlank { "…" })
            }

            override fun onResults(alternatives: List<String>) {
                val match = VibeCommandParser.parseBest(alternatives)
                if (match == null) {
                    VibeRuntime.voiceStatus.value = VoiceStatus.Error(
                        "Didn't understand \"${alternatives.first()}\". Try \"next song\" or \"pause\".",
                    )
                    return
                }
                VibeRuntime.voiceStatus.value = VoiceStatus.Processing(match.heard)
                lifecycleScope.launch {
                    VibeRuntime.voiceStatus.value = app.commandExecutor.execute(match.command)
                }
            }

            override fun onError(message: String) {
                VibeRuntime.voiceStatus.value = VoiceStatus.Error(message)
            }
        })
    }

    private fun quickCommand(command: VoiceCommand) {
        schedulePanelClose()
        lifecycleScope.launch {
            val result = app.commandExecutor.execute(command)
            if (result !is VoiceStatus.Success) {
                panelOpen.value = false
                VibeRuntime.voiceStatus.value = result
            }
        }
    }

    private fun quickToggle() {
        schedulePanelClose()
        lifecycleScope.launch {
            val result = app.commandExecutor.togglePlayPause()
            if (result !is VoiceStatus.Success) {
                panelOpen.value = false
                VibeRuntime.voiceStatus.value = result
            }
        }
    }

    private fun openApp() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    // endregion

    override fun onDestroy() {
        voice.cancel()
        snapAnimator?.cancel()
        handler.removeCallbacksAndMessages(null)
        orbView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        orbView = null
        bubbleView = null
        app.mediaRepository.stopObserving()
        VibeRuntime.voiceStatus.value = VoiceStatus.Idle
        VibeRuntime.serviceRunning.value = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "vibe_assistant"
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.vibe.musicassistant.STOP"
        const val ACTION_SHOW_ORB = "com.vibe.musicassistant.SHOW_ORB"
        const val ACTION_RESET_POSITION = "com.vibe.musicassistant.RESET_POSITION"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, VibeOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VibeOverlayService::class.java))
        }

        fun resetPosition(context: Context) {
            if (VibeRuntime.serviceRunning.value) {
                context.startService(
                    Intent(context, VibeOverlayService::class.java).setAction(ACTION_RESET_POSITION),
                )
            }
        }
    }
}
