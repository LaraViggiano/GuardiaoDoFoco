package com.example.guardiaodofoco


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var countDownTimer: CountDownTimer? = null

    // O BroadcastReceiver que "ouve" o desbloqueio da tela
    private val screenUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // A ação ACTION_USER_PRESENT é enviada quando o usuário desbloqueia o celular
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                // Abre a tela de interrupção
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
        // Registra o nosso "ouvinte" de desbloqueio
        registerReceiver(screenUnlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val minutes = intent?.getLongExtra("FOCUS_MINUTES", 1) ?: 1
        val timeInMillis = minutes * 60 * 1000

        startFocusSession(timeInMillis)

        // START_STICKY garante que o serviço tente se recriar se for encerrado
        return START_STICKY
    }

    private fun startFocusSession(timeInMillis: Long) {
        // Ativa o modo "Não Perturbe"
        setDndMode(true)

        // Inicia o timer
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Atualiza a notificação com o tempo restante
                val minutesLeft = (millisUntilFinished / 1000) / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeLeftFormatted = String.format("%02d:%02d", minutesLeft, secondsLeft)
                startForeground(NOTIFICATION_ID, createNotification(timeLeftFormatted))
            }

            override fun onFinish() {
                // O tempo acabou, encerra o modo foco
                stopFocusSession()
            }
        }.start()
    }

    private fun stopFocusSession() {
        // 1. Desativa o modo "Não Perturbe"
        setDndMode(false)
        // 2. Para o timer
        countDownTimer?.cancel()
        // 3. Encerra o serviço
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

    private fun createNotification(contentText: String): android.app.Notification {
        val channelId = "focus_service_channel"
        val channelName = "Guardião do Foco"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para reabrir o app se o usuário tocar na notificação
        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Modo Foco Ativado")
            .setContentText("Tempo restante: $contentText")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode) // Ícone padrão de modo silencioso
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Torna a notificação não-removível
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garante que o "ouvinte" seja desregistrado para evitar vazamento de memória
        unregisterReceiver(screenUnlockReceiver)
        stopFocusSession()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Não precisamos de binding neste caso
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}
