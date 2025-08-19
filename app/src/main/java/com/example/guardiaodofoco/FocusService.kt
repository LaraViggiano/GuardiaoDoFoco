package com.example.guardiaodofoco

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var focusTimer: CountDownTimer? = null
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    private enum class FocusState {
        IDLE,
        FOCUSSING,
        INTERRUPTED
    }
    private var currentState = FocusState.IDLE

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    // Se estivermos em modo de foco, mostra a tela de interrupção
                    if (currentState == FocusState.FOCUSSING) {
                        launchInterruptionActivity()
                    }
                }
                // Utilizador bloqueou o ecrã
                Intent.ACTION_SCREEN_OFF -> {
                    // Se o utilizador tinha saído do foco (com overlay), ao bloquear o ecrã,
                    // a penalidade é removida e voltamos ao modo de foco normal.
                    if (currentState == FocusState.INTERRUPTED) {
                        hideOverlay()
                        setDndMode(true) // Reativa o "Não Perturbe"
                        currentState = FocusState.FOCUSSING
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                val minutes = intent.getLongExtra("FOCUS_MINUTES", 1)
                startFocusSession(minutes * 60 * 1000)
            }
            ACTION_INTERRUPT_FOCUS -> {
                val showOverlay = intent.getBooleanExtra("SHOW_OVERLAY", false)
                handleInterruption(showOverlay)
            }
        }
        return START_STICKY
    }

    private fun startFocusSession(durationInMillis: Long) {
        currentState = FocusState.FOCUSSING
        setDndMode(true)
        hideOverlay()

        // ESTA LINHA GARANTE QUE QUALQUER TIMER ANTERIOR SEJA CANCELADO ANTES DE INICIAR UM NOVO
        focusTimer?.cancel()
        focusTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                stopFocusSessionCleanly()
            }
        }.start()
    }

    private fun handleInterruption(shouldShowOverlay: Boolean) {
        // O utilizador saiu do foco. O timer continua, mas o DND é desativado.
        setDndMode(false)

        if (shouldShowOverlay) {
            // Motivo fútil: aplica a penalidade do overlay
            currentState = FocusState.INTERRUPTED
            showOverlay()
        } else {
            // Motivo urgente: termina a sessão de foco completamente
            stopFocusSessionCleanly()
        }
    }

    // Chamado quando o tempo acaba ou o utilizador sai por motivo urgente
    private fun stopFocusSessionCleanly() {
        currentState = FocusState.IDLE
        setDndMode(false)
        hideOverlay()
        focusTimer?.cancel()
        stopForeground(true)
        stopSelf()
    }

    private fun launchInterruptionActivity() {
        val intent = Intent(this, InterruptionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun updateNotification(millisUntilFinished: Long) {
        val minutesLeft = (millisUntilFinished / 1000) / 60
        val secondsLeft = (millisUntilFinished / 1000) % 60
        val timeLeftFormatted = String.format("%02d:%02d", minutesLeft, secondsLeft)
        startForeground(NOTIFICATION_ID, createNotification(timeLeftFormatted))
    }

    private fun showOverlay() {
        if (overlayView == null) {
            overlayView = View(this)
            overlayView?.setBackgroundColor(Color.parseColor("#B3808080"))

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            windowManager.addView(overlayView, params)
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            overlayView = null
        }
    }

    private fun setDndMode(enabled: Boolean) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (enabled) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    private fun createNotification(contentText: String): android.app.Notification {
        val channelId = "focus_service_channel"
        val channelName = "Guardião do Foco"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Modo Foco Ativado")
            .setContentText("Tempo restante: $contentText")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        stopFocusSessionCleanly()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START_FOCUS = "ACTION_START_FOCUS"
        const val ACTION_INTERRUPT_FOCUS = "ACTION_INTERRUPT_FOCUS"
    }
}
