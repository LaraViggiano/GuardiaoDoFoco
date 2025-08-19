package com.example.guardiaodofoco

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
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
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    // ... (launchers existentes) ...
    private val notificationPolicyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }

    // NOVO LAUNCHER para a permissão de administrador
    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)

        minutesPicker = findViewById(R.id.minutesPicker)
        startButton = findViewById(R.id.startButton)

        setupNumberPicker()

        startButton.setOnClickListener {
            handleStartFocusClick()
        }
    }

    private fun setupNumberPicker() {
        minutesPicker.minValue = 1 // Mínimo de 1 minuto para testes
        minutesPicker.maxValue = 120
        minutesPicker.value = 25
    }

    private fun handleStartFocusClick() {
        // Cadeia de verificação de permissões
        when {
            !notificationManager.isNotificationPolicyAccessGranted -> requestNotificationPolicyAccess()
            !Settings.canDrawOverlays(this) -> requestOverlayPermission()
            !devicePolicyManager.isAdminActive(adminComponentName) -> requestDeviceAdminAccess() // NOVA VERIFICAÇÃO
            else -> startFocusService()
        }
    }

    // ... (métodos de pedido de permissão existentes) ...
    private fun requestNotificationPolicyAccess() {
        // ... (código existente) ...
    }
    private fun requestOverlayPermission() {
        // ... (código existente) ...
    }

    // NOVO MÉTODO para pedir a permissão de administrador
    private fun requestDeviceAdminAccess() {
        Toast.makeText(this, "Por fim, ative o Guardião como administrador.", Toast.LENGTH_LONG).show()
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Esta permissão é necessária para bloquear a tela durante o modo foco.")
        }
        deviceAdminLauncher.launch(intent)
    }


    private fun startFocusService() {
        val focusMinutes = minutesPicker.value.toLong()
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

        // BLOQUEIA A TELA
        lockScreen()
    }

    // NOVO MÉTODO para bloquear a tela
    private fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            devicePolicyManager.lockNow()
        }
    }
}
