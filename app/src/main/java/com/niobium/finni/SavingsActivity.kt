package com.niobium.finni

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton

class SavingsActivity : AppCompatActivity() {

    private lateinit var tvCoins: TextView
    private lateinit var containerGoals: LinearLayout
    private lateinit var containerSelected: View
    private lateinit var tvGoalName: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var tvGoalLeft: TextView
    private lateinit var progressGoal: ProgressBar
    private lateinit var tvAmount: TextView
    private lateinit var btnMinus: MaterialButton
    private lateinit var btnPlus: MaterialButton
    private lateinit var btnDeposit: MaterialButton
    private lateinit var btnWithdraw: MaterialButton

    private val goalButtons = mutableMapOf<String, MaterialButton>()
    private var amount = STEP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_savings)

        tvCoins = findViewById(R.id.tv_coins)
        containerGoals = findViewById(R.id.container_goals)
        containerSelected = findViewById(R.id.container_selected_goal)
        tvGoalName = findViewById(R.id.tv_goal_name)
        tvGoalProgress = findViewById(R.id.tv_goal_progress)
        tvGoalLeft = findViewById(R.id.tv_goal_left)
        progressGoal = findViewById(R.id.progress_goal_detail)
        tvAmount = findViewById(R.id.tv_savings_amount)
        btnMinus = findViewById(R.id.btn_savings_minus)
        btnPlus = findViewById(R.id.btn_savings_plus)
        btnDeposit = findViewById(R.id.btn_deposit)
        btnWithdraw = findViewById(R.id.btn_withdraw)

        findViewById<View>(R.id.btn_back_savings).setOnClickListener {
            finish()
        }

        buildGoalButtons()

        btnMinus.setOnClickListener {
            amount = maxOf(STEP, amount - STEP)
            renderAmount()
        }

        btnPlus.setOnClickListener {
            amount += STEP
            renderAmount()
        }

        btnDeposit.setOnClickListener {
            deposit()
        }

        btnWithdraw.setOnClickListener {
            askWithdraw()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun buildGoalButtons() {
        val font = ResourcesCompat.getFont(
            this,
            R.font.nunitobold
        )

        GoalCatalog.goals.forEach { goal ->
            val button = MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }

                minimumHeight = dp(60)
                typeface = font
                textSize = 16f
                isAllCaps = false
                cornerRadius = dp(20)
                strokeWidth = dp(2)

                strokeColor = ContextCompat.getColorStateList(
                    context,
                    R.color.blue_button
                )

                setOnClickListener {
                    GameManager.selectGoal(
                        this@SavingsActivity,
                        goal.id
                    )

                    amount = STEP
                    render()
                }
            }

            goalButtons[goal.id] = button
            containerGoals.addView(button)
        }
    }

    private fun render() {
        tvCoins.text = "${GameManager.getBalance(this)} монет"

        val selected = GameManager.getSelectedGoal(this)

        GoalCatalog.goals.forEach { goal ->
            val button = goalButtons[goal.id]
                ?: return@forEach

            val saved = GameManager.getGoalSaved(
                this,
                goal.id
            )

            val isSelected = goal.id == selected?.id
            val isDone = saved >= goal.cost

            button.text = when {
                isDone ->
                    "Выполнено: ${goal.title}  ·  $saved/${goal.cost}"

                isSelected ->
                    "${goal.title}  ·  $saved/${goal.cost}"

                else ->
                    "${goal.title}  ·  $saved/${goal.cost}"
            }

            button.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) {
                        R.color.white
                    } else {
                        R.color.dark_blue
                    }
                )
            )

            button.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (isSelected) {
                            R.color.blue_button
                        } else {
                            R.color.white
                        }
                    )
                )
        }

        if (selected == null) {
            containerSelected.visibility = View.GONE
            return
        }

        containerSelected.visibility = View.VISIBLE

        val saved = GameManager.getGoalSaved(
            this,
            selected.id
        )

        val left = maxOf(
            0,
            selected.cost - saved
        )

        tvGoalName.text = selected.title
        tvGoalProgress.text = "$saved из ${selected.cost}"

        tvGoalLeft.text = if (left == 0) {
            "Готово!"
        } else {
            "Осталось $left"
        }

        progressGoal.max = selected.cost
        progressGoal.progress = minOf(
            saved,
            selected.cost
        )

        renderAmount()
    }

    private fun renderAmount() {
        val selected = GameManager.getSelectedGoal(this)

        val saved = selected?.let {
            GameManager.getGoalSaved(
                this,
                it.id
            )
        } ?: 0

        val limit = maxOf(
            STEP,
            maxOf(
                GameManager.getBalance(this),
                saved
            )
        )

        amount = amount.coerceIn(
            STEP,
            limit
        )

        tvAmount.text = amount.toString()

        setButtonEnabled(
            btnMinus,
            amount - STEP >= STEP
        )

        setButtonEnabled(
            btnPlus,
            amount + STEP <= limit
        )
    }

    private fun deposit() {
        val goal = GameManager.getSelectedGoal(this)
            ?: return

        when (
            GameManager.depositToGoal(
                this,
                goal.id,
                amount
            )
        ) {
            SavingsResult.OK -> {
                val saved = GameManager.getGoalSaved(
                    this,
                    goal.id
                )

                if (saved >= goal.cost) {
                    Popup.show(
                        this,
                        "Цель достигнута!"
                    )
                } else {
                    Popup.show(
                        this,
                        "+$amount в копилку!"
                    )
                }
            }

            SavingsResult.PLAN_NOT_CONFIRMED ->
                Popup.show(
                    this,
                    "Сначала открой «Бюджет»"
                )

            SavingsResult.NOT_ENOUGH_COINS ->
                Popup.show(
                    this,
                    "Не хватает монет"
                )

            SavingsResult.EXCEEDS_REMAINING -> {
                val left = goal.cost -
                        GameManager.getGoalSaved(
                            this,
                            goal.id
                        )

                Popup.show(
                    this,
                    "До цели осталось $left"
                )
            }

            SavingsResult.GOAL_REACHED ->
                Popup.show(
                    this,
                    "Цель уже достигнута"
                )

            else ->
                Popup.show(
                    this,
                    "Выбери сумму"
                )
        }

        render()
    }

    private fun askWithdraw() {
        val goal = GameManager.getSelectedGoal(this)
            ?: return

        val saved = GameManager.getGoalSaved(
            this,
            goal.id
        )

        if (saved <= 0) {
            Popup.show(
                this,
                "Копилка пустая"
            )
            return
        }

        if (amount > saved) {
            Popup.show(
                this,
                "В копилке только $saved"
            )
            return
        }

        CustomDialog(this)
            .title("Забрать монеты?")
            .message("$amount вернется на баланс")
            .button("Да") {
                withdraw(goal)
            }
            .button("Нет") {}
            .show()
    }

    private fun withdraw(goal: SavingsGoal) {
        when (
            GameManager.withdrawFromGoal(
                this,
                goal.id,
                amount
            )
        ) {
            SavingsResult.OK ->
                Popup.show(
                    this,
                    "-$amount из копилки"
                )

            SavingsResult.NOT_ENOUGH_SAVED ->
                Popup.show(
                    this,
                    "В копилке меньше"
                )

            else ->
                Popup.show(
                    this,
                    "Не получилось забрать"
                )
        }

        render()
    }

    private fun setButtonEnabled(
        button: MaterialButton,
        enabled: Boolean
    ) {
        button.isEnabled = enabled

        button.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (enabled) {
                    R.color.blue_button
                } else {
                    R.color.gray_button
                }
            )
    }

    companion object {
        private const val STEP = 5
    }
}

data class SavingsGoal(
    val id: String,
    val title: String,
    val cost: Int
)

object GoalCatalog {

    val goals = listOf(
        SavingsGoal(
            "book",
            "Книга",
            30
        ),
        SavingsGoal(
            "paints",
            "Краски",
            50
        ),
        SavingsGoal(
            "scooter",
            "Самокат",
            80
        )
    )

    fun find(id: String): SavingsGoal? =
        goals.firstOrNull {
            it.id == id
        }
}