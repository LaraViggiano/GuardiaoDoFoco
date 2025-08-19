package com.example.guardiaodofoco

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class InterruptionActivity : AppCompatActivity() {

    private lateinit var backToFocusButton: Button
    private lateinit var exitFocusButton: Button
    private lateinit var reasonRadioGroup: RadioGroup

    // Variáveis para o bloqueio de ecrã
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interruption)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Não faz nada */ }
        })

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, FocusDeviceAdminReceiver::class.java)

        backToFocusButton = findViewById(R.id.backToFocusButton)
        exitFocusButton = findViewById(R.id.exitFocusButton)
        reasonRadioGroup = findViewById(R.id.reasonRadioGroup)

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        backToFocusButton.setOnClickListener {
            // Primeiro, bloqueia o ecrã
            lockScreen()

            // DEPOIS, espera um curto período antes de fechar a activity
            // Isto dá tempo ao sistema para processar o bloqueio.
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 200) // 200 milissegundos de atraso
        }

        exitFocusButton.setOnClickListener {
            if (reasonRadioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Por favor, selecione um motivo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val overlayIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_SHOW_OVERLAY
            }
            startService(overlayIntent)

            val stopIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_FOCUS
            }
            startService(stopIntent)

            finish()

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }

    private fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            devicePolicyManager.lockNow()
        }
    }
}
