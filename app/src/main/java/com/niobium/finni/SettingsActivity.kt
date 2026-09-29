package com.niobium.finni

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.random.Random

class SettingsActivity : AppCompatActivity() {

    private lateinit var adultContainer: LinearLayout
    private lateinit var tvAdultProgress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_settings)

        adultContainer = findViewById(R.id.adult_container)
        tvAdultProgress = findViewById(R.id.tv_adult_progress)

        findViewById<View>(R.id.btn_back4).setOnClickListener {
            finish()
        }

        setupSwitches()

        findViewById<View>(R.id.btn_help).setOnClickListener {
            showInfo("Как играть", getString(R.string.help_text))
        }

        findViewById<View>(R.id.btn_glossary).setOnClickListener {
            showInfo("Справочник терминов", getString(R.string.glossary_text))
        }

        findViewById<View>(R.id.btn_adult).setOnClickListener {
            if (adultContainer.visibility == View.VISIBLE) {
                adultContainer.visibility = View.GONE
            } else {
                askArithmeticBarrier()
            }
        }

        findViewById<View>(R.id.btn_reset_progress).setOnClickListener {
            confirmResetProgress()
        }

        findViewById<View>(R.id.btn_reset).setOnClickListener {
            confirmDelete()
        }
    }

    private fun setupSwitches() {
        val demo = findViewById<SwitchMaterial>(R.id.switch_demo)

        demo.isChecked = GameManager.isDemoMode(this)

        demo.setOnCheckedChangeListener { _, on ->
            GameManager.setDemoMode(this, on)
        }
    }

    private fun showInfo(title: String, message: String) {
        CustomDialog(this)
            .title(title)
            .message(message)
            .button("Понятно") {}
            .show()
    }

    private fun askArithmeticBarrier() {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)

        CustomDialog(this)
            .title("Раздел для взрослых")
            .message("Сколько будет $a × $b?")
            .input(
                hint = "Ответ",
                inputType = android.text.InputType.TYPE_CLASS_NUMBER,
                maxLength = 3,
                imeAction = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            )
            .inputButton("Открыть") { value ->
                if (value.trim().toIntOrNull() == a * b) {
                    showAdultSection()
                } else {
                    Popup.show(this, "Неверно. Попробуйте ещё раз")
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun showAdultSection() {
        val progress = buildString {
            append("Игровой период: ${GameManager.getPeriod(this@SettingsActivity)}\n")
            append("Стадия питомца: ${GameManager.getStageName(this@SettingsActivity)}\n")
            append("Монет на балансе: ${GameManager.getBalance(this@SettingsActivity)}\n")
            append("В накоплениях: ${GameManager.getSavings(this@SettingsActivity)}")

            val summary = GameManager.getLastSummary(this@SettingsActivity)

            if (summary.isNotBlank()) {
                append("\n\nИтоги прошлого периода:\n$summary")
            }
        }

        tvAdultProgress.text = progress
        adultContainer.visibility = View.VISIBLE
    }

    private fun confirmResetProgress() {
        CustomDialog(this)
            .title("Сбросить профиль?")
            .message("Монеты, накопления, покупки и прогресс начнутся заново. Питомец и его имя останутся.")
            .button("Сбросить") {
                GameManager.resetProgress(this)
                restartApp()
            }
            .button("Отмена") {}
            .show()
    }

    private fun confirmDelete() {
        CustomDialog(this)
            .title("Удалить профиль?")
            .message("Питомец, монеты, накопления и прогресс будут удалены. Это нельзя отменить.")
            .button("Удалить") {
                GameManager.resetAll(this)
                restartApp()
            }
            .button("Отмена") {}
            .show()
    }

    private fun restartApp() {
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            ?: Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )

        startActivity(intent)
        finishAffinity()
    }
}