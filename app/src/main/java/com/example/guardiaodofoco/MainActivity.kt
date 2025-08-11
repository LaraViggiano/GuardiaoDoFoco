package com.example.guardiaodofoco

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var minutesPicker: NumberPicker
    private lateinit var startButton: Button
    private lateinit var notificationManager: NotificationManager
    private val notificationPolicyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Após o usuário voltar da tela de permissões, verificamos novamente
        if (notificationManager.isNotificationPolicyAccessGranted) {
            startFocusService()
        } else {
            Toast.makeText(this, "Permissão necessária para controlar o modo foco.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        minutesPicker = findViewById(R.id.minutesPicker)
        startButton = findViewById(R.id.startButton)

        // Configura o NumberPicker
        setupNumberPicker()

        // Configura o clique do botão
        startButton.setOnClickListener {
            handleStartFocusClick()
        }
    }

    private fun setupNumberPicker() {
        minutesPicker.minValue = 5 // Mínimo de 5 minutos
        minutesPicker.maxValue = 120 // Máximo de 2 horas
        minutesPicker.value = 30 // Valor padrão
    }

    private fun handleStartFocusClick() {
        // Verifica se a permissão para o modo "Não Perturbe" foi concedida
        if (notificationManager.isNotificationPolicyAccessGranted) {
            startFocusService()
        } else {
            // Se não foi, pede ao usuário para concedê-la
            requestNotificationPolicyAccess()
        }
    }

    private fun requestNotificationPolicyAccess() {
        Toast.makeText(this, "Por favor, ative a permissão para o Guardião do Foco.", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        notificationPolicyLauncher.launch(intent)
    }

    private fun startFocusService() {
        val focusMinutes = minutesPicker.value.toLong()
        val serviceIntent = Intent(this, FocusService::class.java).apply {
            putExtra("FOCUS_MINUTES", focusMinutes)
        }
        // Inicia o serviço em primeiro plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        // Informa ao usuário que o modo foco começou
        Toast.makeText(this, "Modo Foco iniciado por $focusMinutes minutos.", Toast.LENGTH_SHORT).show()
    }
}
