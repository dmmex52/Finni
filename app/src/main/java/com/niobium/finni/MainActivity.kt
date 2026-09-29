package com.niobium.finni

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var navItems: List<LinearLayout>
    private lateinit var ivPet: ImageView
    private lateinit var tvCoins: TextView
    private lateinit var tvPetName: TextView
    private lateinit var tvPetLevel: TextView
    private lateinit var tvGoalSubtitle: TextView
    private lateinit var progressGoal: ProgressBar
    private lateinit var progressMood: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        ivPet = findViewById(R.id.iv_pet)
        tvCoins = findViewById(R.id.tv_coins)
        tvPetName = findViewById(R.id.tv_pet_name)
        tvPetLevel = findViewById(R.id.tv_pet_level)
        tvGoalSubtitle = findViewById(R.id.tv_goal_subtitle)
        progressGoal = findViewById(R.id.progress_goal)
        progressMood = findViewById(R.id.progress_mood)

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        selectNavigationItem(0)
        updatePetView()
        updatePetName()
        updatePetLevel()
        updateCoinsView()
        updateGoalView()
        updateMoodView()

        if (GameManager.consumeStartBonusFlag(this)) {
            Popup.show(this, "+${GameManager.START_COINS} монет — стартовый бюджет")
        }
    }

    private fun updatePetView() {
        val gender = GameManager.getPetGender(this)
        val drawableRes = if (gender == "boy") R.drawable.dolphin_boy else R.drawable.dolphin_girl
        ivPet.setImageResource(drawableRes)
    }

    private fun updatePetName() {
        val name = GameManager.getPetName(this)
        tvPetName.text = name.ifBlank { "Питомец" }
    }

    private fun updatePetLevel() {
        tvPetLevel.text = GameManager.getStageName(this)
    }

    private fun updateCoinsView() {
        val balance = GameManager.getBalance(this)
        tvCoins.text = "$balance монет"
    }

    private fun updateMoodView() {
        progressMood.max = 100
        progressMood.progress = GameManager.getMood(this)
    }

    private fun updateGoalView() {
        val goal = GameManager.getSelectedGoal(this)

        if (goal == null) {
            tvGoalSubtitle.text = "Нет цели"
            progressGoal.max = 100
            progressGoal.progress = 0
            return
        }

        val saved = GameManager.getGoalSaved(this, goal.id)
        tvGoalSubtitle.text = "${goal.title}: $saved/${goal.cost}"
        progressGoal.max = goal.cost
        progressGoal.progress = minOf(saved, goal.cost)
    }

    private fun setupNavigation() {
        navItems = listOf(
            findViewById(R.id.nav_home),
            findViewById(R.id.nav_budget),
            findViewById(R.id.nav_shop),
            findViewById(R.id.nav_savings),
            findViewById(R.id.nav_tasks)
        )

        navItems.forEachIndexed { index, item ->
            item.setOnClickListener {
                selectNavigationItem(index)
                handleNavigationClick(index)
            }
        }

        findViewById<View>(R.id.card_goal).setOnClickListener {
            startActivity(Intent(this, SavingsActivity::class.java))
        }

        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun handleNavigationClick(index: Int) {
        when (index) {
            1 -> startActivity(Intent(this, BudgetActivity::class.java))
            2 -> startActivity(Intent(this, ShopActivity::class.java))
            3 -> startActivity(Intent(this, SavingsActivity::class.java))
        }
    }

    private fun selectNavigationItem(selectedIndex: Int) {
        navItems.forEachIndexed { index, item ->
            val icon = item.getChildAt(0) as ImageView
            val title = item.getChildAt(1) as TextView

            val colorRes = if (index == selectedIndex) R.color.blue_button else R.color.nav_gray
            icon.imageTintList = ContextCompat.getColorStateList(this, colorRes)
            title.setTextColor(ContextCompat.getColor(this, colorRes))
        }
    }
}