package com.example.guardiaodofoco // Substitua pelo seu pacote

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
    private var countDownTimer: CountDownTimer? = null

    // --- Lógica da Camada Cinzenta (Overlay) ---
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    // O BroadcastReceiver que "ouve" o desbloqueio do ecrã
    private val screenUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                val interruptionIntent = Intent(context, InterruptionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(interruptionIntent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        registerReceiver(screenUnlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Agora o serviço pode receber diferentes ações
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                val minutes = intent.getLongExtra("FOCUS_MINUTES", 1)
                startFocusSession(minutes * 60 * 1000)
            }
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            ACTION_STOP_FOCUS -> stopFocusSession()
        }
        return START_STICKY
    }

    private fun startFocusSession(timeInMillis: Long) {
        setDndMode(true)
        hideOverlay() // Garante que a camada não está visível no início

        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = (millisUntilFinished / 1000) / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeLeftFormatted = String.format("%02d:%02d", minutesLeft, secondsLeft)
                startForeground(NOTIFICATION_ID, createNotification(timeLeftFormatted))
            }

            override fun onFinish() {
                stopFocusSession()
            }
        }.start()
    }

    private fun stopFocusSession() {
        setDndMode(false)
        hideOverlay()
        countDownTimer?.cancel()
        stopForeground(true)
        stopSelf()
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

    // --- NOVOS MÉTODOS PARA A CAMADA CINZENTA ---

    private fun showOverlay() {
        if (overlayView == null) {
            overlayView = View(this)
            // Cor cinzenta com 70% de transparência
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
            windowManager.removeView(it)
            overlayView = null
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
        unregisterReceiver(screenUnlockReceiver)
        stopFocusSession()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
        // Ações para controlar o serviço
        const val ACTION_START_FOCUS = "ACTION_START_FOCUS"
        const val ACTION_STOP_FOCUS = "ACTION_STOP_FOCUS"
        const val ACTION_SHOW_OVERLAY = "ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "ACTION_HIDE_OVERLAY"
    }
}
