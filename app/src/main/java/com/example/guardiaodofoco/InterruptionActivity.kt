package com.example.guardiaodofoco

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class InterruptionActivity : AppCompatActivity() {

    private lateinit var backToFocusButton: Button
    private lateinit var exitFocusButton: Button
    private lateinit var reasonRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interruption)

        // Desativa o botão "Voltar" usando a nova API OnBackPressedDispatcher
        // Isso força o usuário a tomar uma decisão na tela.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Deixar em branco intencionalmente para que o botão "Voltar" não faça nada.
            }
        })

        // Inicializa os componentes da UI
        backToFocusButton = findViewById(R.id.backToFocusButton)
        exitFocusButton = findViewById(R.id.exitFocusButton)
        reasonRadioGroup = findViewById(R.id.reasonRadioGroup)

        // Configura os cliques dos botões
        setupButtonClickListeners()
    }

    private fun setupButtonClickListeners() {
        // Botão "Voltar ao Foco": simplesmente fecha esta tela.
        // O serviço continua rodando e o usuário volta para a tela de bloqueio.
        backToFocusButton.setOnClickListener {
            finish()
        }

        // Botão "Sair do Foco": Ação mais complexa
        exitFocusButton.setOnClickListener {
            // Verifica se o usuário selecionou um motivo
            if (reasonRadioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Por favor, selecione um motivo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Para o serviço de foco
            val serviceIntent = Intent(this, FocusService::class.java)
            stopService(serviceIntent)

            // 2. Ativa o modo monocromático
            setMonochromaticMode(true)

            // 3. Fecha a tela de interrupção
            finish()

            // 4. Opcional: Leva o usuário para a tela inicial do celular
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }

    /**
     * Ativa ou desativa o modo monocromático do sistema.
     * IMPORTANTE: Esta função requer a permissão WRITE_SECURE_SETTINGS,
     * que deve ser concedida via ADB com o comando:
     * adb shell pm grant com.example.guardiaodofoco android.permission.WRITE_SECURE_SETTINGS
     */
    private fun setMonochromaticMode(enabled: Boolean) {
        try {
            // Ativa/desativa o modo de correção de cor
            Settings.Secure.putInt(
                contentResolver,
                "accessibility_display_daltonizer_enabled",
                if (enabled) 1 else 0
            )
            // Define o tipo de correção para Monocromia (valor "12")
            Settings.Secure.putString(
                contentResolver,
                "accessibility_display_daltonizer",
                if (enabled) "12" else "-1"
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Permissão WRITE_SECURE_SETTINGS não concedida. Ative via ADB.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}