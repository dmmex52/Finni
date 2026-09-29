package com.niobium.finni

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        if (GameManager.isCharacterCreated(this)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_splash)

        val btnStart = findViewById<MaterialButton>(R.id.btn_start)
        btnStart.setOnClickListener {
            val intent = Intent(this, CharacterActivity::class.java)
            startActivity(intent)
        }
    }
}