package com.niobium.finni

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

enum class BudgetCategory() {
    MANDATORY,
    OPTIONAL,
    SAVINGS
}

data class Amounts(
    val mandatory: Int = 0,
    val optional: Int = 0,
    val savings: Int = 0
) {
    val total: Int
        get() = mandatory + optional + savings

    operator fun get(category: BudgetCategory): Int = when (category) {
        BudgetCategory.MANDATORY -> mandatory
        BudgetCategory.OPTIONAL -> optional
        BudgetCategory.SAVINGS -> savings
    }
}

class BudgetActivity : AppCompatActivity() {

    private data class PlanRow(
        val minus: MaterialButton,
        val plus: MaterialButton,
        val amount: TextView
    )

    private data class FactRow(
        val numbers: TextView,
        val progress: ProgressBar,
        val status: TextView
    )

    private val categories = BudgetCategory.values()
    private val plan = IntArray(BudgetCategory.values().size)

    private lateinit var tvCoins: TextView
    private lateinit var tvPeriodTitle: TextView
    private lateinit var tvPeriodTimer: TextView
    private lateinit var containerPlanning: View
    private lateinit var containerActive: View
    private lateinit var tvInPlan: TextView
    private lateinit var tvRemainder: TextView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var btnFinish: MaterialButton

    private val planRows = mutableListOf<PlanRow>()
    private val factRows = mutableListOf<FactRow>()

    private val timerHandler = Handler(Looper.getMainLooper())

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (GameManager.isPlanConfirmed(this@BudgetActivity)) {
                updatePeriodTimer()
            }

            timerHandler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_budget)

        initViews()
        initRows()
        initListeners()
    }

    override fun onResume() {
        super.onResume()
        render()
        timerHandler.post(timerRunnable)
    }

    override fun onPause() {
        timerHandler.removeCallbacks(timerRunnable)
        super.onPause()
    }

    private fun initViews() {
        tvCoins = findViewById(R.id.tv_coins)
        tvPeriodTitle = findViewById(R.id.tv_period_title)
        tvPeriodTimer = findViewById(R.id.tv_period_timer)
        containerPlanning = findViewById(R.id.container_planning)
        containerActive = findViewById(R.id.container_active)
        tvInPlan = findViewById(R.id.tv_in_plan)
        tvRemainder = findViewById(R.id.tv_remainder)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnFinish = findViewById(R.id.btn_finish)
    }

    private fun initRows() {
        val minus = intArrayOf(
            R.id.btn_minus_1,
            R.id.btn_minus_2,
            R.id.btn_minus_3
        )

        val plus = intArrayOf(
            R.id.btn_plus_1,
            R.id.btn_plus_2,
            R.id.btn_plus_3
        )

        val amounts = intArrayOf(
            R.id.tv_amount_1,
            R.id.tv_amount_2,
            R.id.tv_amount_3
        )

        val factNumbers = intArrayOf(
            R.id.tv_fact_numbers_1,
            R.id.tv_fact_numbers_2,
            R.id.tv_fact_numbers_3
        )

        val factProgress = intArrayOf(
            R.id.progress_fact_1,
            R.id.progress_fact_2,
            R.id.progress_fact_3
        )

        val factStatus = intArrayOf(
            R.id.tv_fact_status_1,
            R.id.tv_fact_status_2,
            R.id.tv_fact_status_3
        )

        categories.forEachIndexed { index, category ->
            planRows += PlanRow(
                minus = findViewById(minus[index]),
                plus = findViewById(plus[index]),
                amount = findViewById(amounts[index])
            )

            factRows += FactRow(
                numbers = findViewById(factNumbers[index]),
                progress = findViewById(factProgress[index]),
                status = findViewById(factStatus[index])
            )
        }
    }

    private fun initListeners() {
        findViewById<View>(R.id.btn_back3).setOnClickListener {
            finish()
        }

        planRows.forEachIndexed { index, row ->
            row.minus.setOnClickListener {
                changePlan(index, -STEP)
            }

            row.plus.setOnClickListener {
                changePlan(index, STEP)
            }
        }

        btnConfirm.setOnClickListener {
            askConfirmPlan()
        }

        btnFinish.setOnClickListener {
            askFinishPeriod()
        }
    }

    private fun render() {
        tvCoins.text = "${GameManager.getBalance(this)} монет"
        tvPeriodTitle.text = "Период ${GameManager.getPeriod(this)}"

        val confirmed = GameManager.isPlanConfirmed(this)

        containerPlanning.visibility =
            if (confirmed) View.GONE else View.VISIBLE

        containerActive.visibility =
            if (confirmed) View.VISIBLE else View.GONE

        if (confirmed) {
            tvPeriodTimer.visibility = View.VISIBLE
            btnFinish.visibility = View.VISIBLE

            renderFact()
            updatePeriodTimer()
        } else {
            tvPeriodTimer.visibility = View.GONE
            btnFinish.visibility = View.GONE

            loadPlan()
            renderPlan()
        }
    }

    private fun updatePeriodTimer() {
        if (!GameManager.isPlanConfirmed(this)) {
            tvPeriodTimer.visibility = View.GONE
            btnFinish.visibility = View.GONE
            return
        }

        tvPeriodTimer.visibility = View.VISIBLE
        btnFinish.visibility = View.VISIBLE

        if (GameManager.isDemoMode(this)) {
            tvPeriodTimer.text = "Период можно завершить"
            setButtonEnabled(
                btnFinish,
                GameManager.canFinishPeriod(this)
            )
            return
        }

        val remaining = GameManager.getRemainingPeriodTime(this)

        if (remaining <= 0L) {
            tvPeriodTimer.text = "Период можно завершить"
        } else {
            val totalMinutes = remaining / 60_000L
            val days = totalMinutes / (60L * 24L)
            val hours = (totalMinutes % (60L * 24L)) / 60L
            val minutes = totalMinutes % 60L

            tvPeriodTimer.text = when {
                days > 0L ->
                    "До завершения периода: $days д. $hours ч."

                hours > 0L ->
                    "До завершения периода: $hours ч. $minutes мин."

                else ->
                    "До завершения периода: $minutes мин."
            }
        }

        setButtonEnabled(
            btnFinish,
            GameManager.canFinishPeriod(this)
        )
    }

    private fun loadPlan() {
        val saved = GameManager.readAmounts(
            this,
            GameManager::planKey
        )

        categories.forEachIndexed { index, category ->
            plan[index] = saved[category]
        }

        if (plan.sum() > available()) {
            plan.fill(0)
            GameManager.savePlan(this, currentPlan())
        }
    }

    private fun available(): Int =
        GameManager.getBalance(this)

    private fun remainder(): Int =
        available() - plan.sum()

    private fun currentPlan(): Amounts =
        Amounts(
            mandatory = plan[BudgetCategory.MANDATORY.ordinal],
            optional = plan[BudgetCategory.OPTIONAL.ordinal],
            savings = plan[BudgetCategory.SAVINGS.ordinal]
        )

    private fun changePlan(index: Int, delta: Int) {
        val newValue = plan[index] + delta

        if (newValue < 0) return

        if (delta > 0 && delta > remainder()) return

        plan[index] = newValue
        GameManager.savePlan(this, currentPlan())

        renderPlan()
    }

    private fun canConfirm(): Boolean =
        plan[BudgetCategory.MANDATORY.ordinal] > 0 &&
                plan[BudgetCategory.SAVINGS.ordinal] > 0 &&
                plan.sum() <= available()

    private fun renderPlan() {
        val rest = remainder()

        tvInPlan.text = "В плане: ${plan.sum()}"
        tvRemainder.text = "Осталось: $rest"

        planRows.forEachIndexed { index, row ->
            row.amount.text = plan[index].toString()

            setButtonEnabled(
                row.minus,
                plan[index] > 0
            )

            setButtonEnabled(
                row.plus,
                rest > 0
            )
        }

        setButtonEnabled(btnConfirm, canConfirm())
    }

    private fun askConfirmPlan() {
        if (!canConfirm()) {
            Popup.show(
                this,
                "Отложи монеты на обязательное и в копилку"
            )
            return
        }

        CustomDialog(this)
            .title("Всё верно?")
            .message("После старта план поменять нельзя.")
            .button("Да") {
                confirmPlan()
            }
            .button("Нет") {}
            .show()
    }

    private fun confirmPlan() {
        if (!GameManager.confirmPlan(this)) {
            Popup.show(
                this,
                "План не подходит. Проверь монеты"
            )
            return
        }

        Popup.show(this, "Твой план готов!")
        render()
    }

    private fun renderFact() {
        val planned = GameManager.readAmounts(
            this,
            GameManager::planKey
        )

        val actual = GameManager.readAmounts(
            this,
            GameManager::factKey
        )

        categories.forEachIndexed { index, category ->
            val row = factRows[index]

            val planAmount = planned[category]
            val factAmount = actual[category]

            row.numbers.text =
                "Надо: $planAmount   Сейчас: $factAmount"

            val max = maxOf(planAmount, 1)

            row.progress.max = max
            row.progress.progress = minOf(factAmount, max)

            row.status.text =
                statusText(category, planAmount, factAmount)
        }

        updatePeriodTimer()
    }

    private fun askFinishPeriod() {
        when (GameManager.getFinishPeriodError(this)) {
            FinishPeriodError.TOO_EARLY -> {
                Popup.show(
                    this,
                    "Период ещё идёт. Вернись позже!"
                )
                return
            }

            FinishPeriodError.NO_ACTIVITY -> {
                Popup.show(
                    this,
                    "Сначала потрать или отложи монеты"
                )
                return
            }

            null -> Unit
        }

        CustomDialog(this)
            .title("Заканчиваем период?")
            .message(
                "Посмотрим, как ты справился — " +
                        "и начнём новый!"
            )
            .button("Да") {
                finishPeriod()
            }
            .button("Нет") {}
            .show()
    }

    private fun finishPeriod() {
        val planned = GameManager.readAmounts(
            this,
            GameManager::planKey
        )

        val actual = GameManager.readAmounts(
            this,
            GameManager::factKey
        )

        val mandatoryOk =
            planned.mandatory > 0 &&
                    actual.mandatory >= planned.mandatory

        val optionalOk =
            actual.optional <= planned.optional

        val savingsOk =
            planned.savings > 0 &&
                    actual.savings >= planned.savings

        val results = listOf(
            mandatoryOk,
            optionalOk,
            savingsOk
        )

        val lines = listOf(
            if (mandatoryOk) {
                "Обязательные: план выполнен"
            } else {
                "Обязательные: план не выполнен.\n" +
                        "В следующий раз выдели монеты на еду и уход."
            },

            if (optionalOk) {
                "Необязательные: план выполнен"
            } else {
                "Необязательные: план превышен.\n" +
                        "В следующий раз отложи желаемую покупку."
            },

            if (savingsOk) {
                "Копилка: план выполнен"
            } else {
                "Копилка: отложено меньше плана.\n" +
                        "В следующий раз переведи монеты в копилку."
            }
        )

        val points = results.count { it }

        val oldGrowth = GameManager.getGrowth(this)
        val growthGain = GameManager.calculateGrowthGain(points)
        val newGrowth = oldGrowth + growthGain

        val mood = 40 + points * 20
        val oldStage = GameManager.stageIndex(oldGrowth)
        val newStage = GameManager.stageIndex(newGrowth)

        val period = GameManager.getPeriod(this)

        GameManager.finishPeriod(
            context = this,
            newGrowth = newGrowth,
            mood = mood,
            summary = lines.joinToString("\n")
        )

        val stageUp = newStage > oldStage
        val stageName = GameManager.STAGES[newStage]

        val text = buildString {
            append(lines.joinToString("\n\n"))
            append("\n\n")
            append("Настроение питомца: $mood из 100.")
            append("\nВыполнено пунктов: $points из 3.")

            if (growthGain > 0) {
                append("\nРост: +$growthGain.")
            } else {
                append("\nРост: пока без изменений.")
            }

            append("\nСтадия: $stageName")

            if (stageUp) {
                append("\n\nПитомец вырос!")
            }
        }

        CustomDialog(this)
            .title("Итоги периода $period")
            .message(text)
            .button("К новому плану") {
                render()
            }
            .show()
    }

    private fun statusText(
        category: BudgetCategory,
        planned: Int,
        actual: Int
    ): String {
        return when (category) {
            BudgetCategory.MANDATORY,
            BudgetCategory.SAVINGS -> {
                when {
                    planned > 0 && actual >= planned ->
                        "План выполнен"

                    planned > 0 ->
                        "План не выполнен"

                    else ->
                        "Не запланировано"
                }
            }

            BudgetCategory.OPTIONAL -> {
                if (actual > planned) {
                    "План превышен"
                } else {
                    "План выполнен"
                }
            }
        }
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