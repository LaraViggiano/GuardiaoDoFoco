package com.example.guardiaodofoco

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var minutesPicker: NumberPicker
    private lateinit var startButton: Button
    private lateinit var notificationManager: NotificationManager

    // Launcher para a permissão de "Não Perturbe"
    private val notificationPolicyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Após voltar, verifica de novo e tenta iniciar a próxima verificação
        handleStartFocusClick()
    }

    // Launcher para a permissão de "Desenhar sobre outros"
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Após voltar, verifica de novo e tenta iniciar o serviço
        handleStartFocusClick()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        minutesPicker = findViewById(R.id.minutesPicker)
        startButton = findViewById(R.id.startButton)

        setupNumberPicker()

        startButton.setOnClickListener {
            handleStartFocusClick()
        }
    }

    private fun setupNumberPicker() {
        minutesPicker.minValue = 5
        minutesPicker.maxValue = 120
        minutesPicker.value = 25 // Valor padrão
    }

    private fun handleStartFocusClick() {
        // Cadeia de verificação de permissões
        when {
            // 1. Verifica a permissão "Não Perturbe"
            !notificationManager.isNotificationPolicyAccessGranted -> {
                requestNotificationPolicyAccess()
            }
            // 2. Verifica a permissão "Desenhar sobre outros"
            !Settings.canDrawOverlays(this) -> {
                requestOverlayPermission()
            }
            // 3. Se todas as permissões estiverem OK, inicia o serviço
            else -> {
                startFocusService()
            }
        }
    }

    private fun requestNotificationPolicyAccess() {
        Toast.makeText(this, "Por favor, ative a permissão para o Guardião do Foco.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        notificationPolicyLauncher.launch(intent)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "Agora, ative a permissão para sobrepor outros apps.", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startFocusService() {
        val focusMinutes = minutesPicker.value.toLong()

        // Envia o comando correto para o serviço
        val serviceIntent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_START_FOCUS
            putExtra("FOCUS_MINUTES", focusMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Modo Foco iniciado por $focusMinutes minutos.", Toast.LENGTH_SHORT).show()
        finish() // Fecha a activity para o utilizador não ficar nela
    }
}
