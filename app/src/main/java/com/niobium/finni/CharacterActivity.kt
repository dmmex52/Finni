package com.niobium.finni

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class CharacterActivity : AppCompatActivity() {

    private lateinit var flBoyCircle: FrameLayout
    private lateinit var flGirlCircle: FrameLayout
    private lateinit var llBoy: LinearLayout
    private lateinit var llGirl: LinearLayout
    private lateinit var etName: TextInputEditText
    private lateinit var btnDone: MaterialButton

    private var isBoySelected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_character)

        flBoyCircle = findViewById(R.id.fl_boy_circle)
        flGirlCircle = findViewById(R.id.fl_girl_circle)
        llBoy = findViewById(R.id.ll_boy)
        llGirl = findViewById(R.id.ll_girl)
        etName = findViewById(R.id.et_name)
        btnDone = findViewById(R.id.btn_done)

        setupGenderSelection()
        selectGender(true)
        setupNameValidation()

        findViewById<MaterialButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        btnDone.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            GameManager.saveCharacter(this, isBoySelected, name)

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupGenderSelection() {
        llBoy.setOnClickListener { selectGender(true) }
        llGirl.setOnClickListener { selectGender(false) }
    }

    private fun selectGender(isBoy: Boolean) {
        isBoySelected = isBoy

        flBoyCircle.background = ContextCompat.getDrawable(
            this,
            if (isBoy) R.drawable.circle_selected_blue
            else R.drawable.circle_unselected_blue
        )

        flGirlCircle.background = ContextCompat.getDrawable(
            this,
            if (!isBoy) R.drawable.circle_selected_pink
            else R.drawable.circle_unselected_pink
        )
    }

    private fun setupNameValidation() {
        updateDoneButtonState(etName.text?.toString().orEmpty())

        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateDoneButtonState(s?.toString().orEmpty())
            }
        })
    }

    private fun updateDoneButtonState(name: String) {
        val isValid = name.trim().isNotEmpty()
        btnDone.isEnabled = isValid

        val backgroundColor = ContextCompat.getColor(
            this,
            if (isValid) R.color.blue_button else R.color.gray_button
        )

        btnDone.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }
}