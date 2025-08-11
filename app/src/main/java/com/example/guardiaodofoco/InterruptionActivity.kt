package com.example.guardiaodofoco

import android.content.Intent
import android.os.Bundle
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

        // Desativa o botão "Voltar" usando a nova API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Deixar em branco para que o botão "Voltar" não faça nada
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
        backToFocusButton.setOnClickListener {
            finish()
        }

        // Botão "Sair do Foco":
        exitFocusButton.setOnClickListener {
            if (reasonRadioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Por favor, selecione um motivo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Envia o comando para o serviço mostrar a camada cinzenta
            val overlayIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_SHOW_OVERLAY
            }
            startService(overlayIntent)

            // 2. Envia o comando para o serviço parar a contagem e o modo "Não Perturbe"
            val stopIntent = Intent(this, FocusService::class.java).apply {
                action = FocusService.ACTION_STOP_FOCUS
            }
            startService(stopIntent)

            // 3. Fecha a tela de interrupção
            finish()

            // 4. Leva o utilizador para a tela inicial do telemóvel
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }
}
