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

    private lateinit var hoursPicker: NumberPicker
    private lateinit var minutesPicker: NumberPicker
    private lateinit var startButton: Button
    private lateinit var notificationManager: NotificationManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    private val notificationPolicyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { handleStartFocusClick() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)

        hoursPicker = findViewById(R.id.hoursPicker)
        minutesPicker = findViewById(R.id.minutesPicker)
        startButton = findViewById(R.id.startButton)

        setupNumberPickers()

        startButton.setOnClickListener {
            handleStartFocusClick()
        }
    }

    private fun setupNumberPickers() {
        hoursPicker.minValue = 0
        hoursPicker.maxValue = 5
        hoursPicker.value = 0

        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        minutesPicker.value = 25
    }

    private fun handleStartFocusClick() {
        when {
            !notificationManager.isNotificationPolicyAccessGranted -> requestNotificationPolicyAccess()
            !Settings.canDrawOverlays(this) -> requestOverlayPermission()
            !devicePolicyManager.isAdminActive(adminComponentName) -> requestDeviceAdminAccess()
            else -> startFocusService()
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

    private fun requestDeviceAdminAccess() {
        Toast.makeText(this, "Por fim, ative o Guardião como administrador.", Toast.LENGTH_LONG).show()
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Esta permissão é necessária para bloquear a tela durante o modo foco.")
        }
        deviceAdminLauncher.launch(intent)
    }


    private fun startFocusService() {
        val totalMinutes = (hoursPicker.value * 60) + minutesPicker.value
        if (totalMinutes == 0) {
            Toast.makeText(this, "Por favor, selecione um tempo maior que zero.", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, FocusService::class.java).apply {
            action = FocusService.ACTION_START_FOCUS
            putExtra("FOCUS_MINUTES", totalMinutes.toLong())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Modo Foco iniciado por $totalMinutes minutos.", Toast.LENGTH_SHORT).show()
        lockScreen()
    }

    private fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            devicePolicyManager.lockNow()
        }
    }
}
