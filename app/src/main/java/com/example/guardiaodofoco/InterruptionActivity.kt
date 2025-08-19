package com.example.guardiaodofoco

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class InterruptionActivity : AppCompatActivity() {

    private lateinit var backToFocusButton: Button
    private lateinit var exitFocusButton: Button

    // Lista para guardar todos os botões de motivo
    private lateinit var reasonButtons: List<Button>
    private var selectedReasonButton: Button? = null

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

        // Inicializa os componentes
        backToFocusButton = findViewById(R.id.backToFocusButton)
        exitFocusButton = findViewById(R.id.exitFocusButton)

        // Adiciona os botões à lista com os novos IDs
        reasonButtons = listOf(
            findViewById(R.id.reasonMessages),
            findViewById(R.id.reasonUrgent),
            findViewById(R.id.reasonTask),
            findViewById(R.id.reasonHabit),
            findViewById(R.id.reasonNotificationCuriosity)
        )

        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        backToFocusButton.setOnClickListener {
            lockScreen()
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 200)
        }

        // Configura o clique para cada botão de motivo
        reasonButtons.forEach { button ->
            button.setOnClickListener {
                selectedReasonButton = button
                updateReasonButtonsUI(clickedButton = button)
            }
        }

        exitFocusButton.setOnClickListener {
            if (selectedReasonButton == null) {
                Toast.makeText(this, "Por favor, selecione um motivo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // A lógica permanece a mesma: a camada cinzenta só não aparece se o botão "Urgente" for selecionado
            val shouldShowOverlay = selectedReasonButton?.id != R.id.reasonUrgent

            val intent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_INTERRUPT_FOCUS
                putExtra("SHOW_OVERLAY", shouldShowOverlay)
            }
            startService(intent)

            finish()

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }

    private fun updateReasonButtonsUI(clickedButton: Button) {
        reasonButtons.forEach { button ->
            button.isSelected = (button == clickedButton)
        }
    }

    private fun lockScreen() {
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            devicePolicyManager.lockNow()
        }
    }
}
