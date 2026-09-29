package com.niobium.finni

import android.content.Context

enum class SpendResult {
    OK,
    PLAN_NOT_CONFIRMED,
    NOT_ENOUGH_COINS,
    INVALID_AMOUNT
}

enum class SavingsResult {
    OK,
    INVALID_AMOUNT,
    PLAN_NOT_CONFIRMED,
    NO_GOAL,
    GOAL_REACHED,
    EXCEEDS_REMAINING,
    NOT_ENOUGH_COINS,
    NOT_ENOUGH_SAVED
}

enum class FinishPeriodError {
    TOO_EARLY,
    NO_ACTIVITY
}

object GameManager {

    private const val PREFS_NAME = "finni_prefs"

    private const val KEY_BALANCE = "balance"
    private const val KEY_PET_GENDER = "pet_gender"
    private const val KEY_PET_NAME = "pet_name"
    private const val KEY_CHARACTER_CREATED = "character_created"
    private const val KEY_START_BONUS_SHOWN = "start_bonus_shown"

    private const val KEY_PERIOD = "period_number"
    private const val KEY_PERIOD_STARTED_AT = "period_started_at"
    private const val KEY_PLAN_CONFIRMED = "plan_confirmed"

    private const val KEY_SAVINGS_TOTAL = "savings_total"

    private const val KEY_GROWTH = "growth_points"
    private const val KEY_MOOD = "mood"
    private const val KEY_LAST_SUMMARY = "last_period_summary"

    private const val KEY_DEMO = "demo_mode"
    private const val KEY_SELECTED_GOAL = "selected_goal"

    const val START_COINS = 100

    private const val PERIOD_DURATION_MS =
        24L * 60L * 60L * 1000L

    private const val TEEN_GROWTH = 5
    private const val ADULT_GROWTH = 10

    val STAGES = listOf(
        "Малыш",
        "Подросток",
        "Взрослый"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun isCharacterCreated(context: Context): Boolean =
        prefs(context).getBoolean(
            KEY_CHARACTER_CREATED,
            false
        )

    fun getPetGender(context: Context): String =
        prefs(context).getString(
            KEY_PET_GENDER,
            "boy"
        ) ?: "boy"

    fun getPetName(context: Context): String =
        prefs(context).getString(
            KEY_PET_NAME,
            ""
        ) ?: ""

    fun saveCharacter(
        context: Context,
        isBoy: Boolean,
        name: String
    ) {
        val p = prefs(context)

        val alreadyCreated =
            p.getBoolean(
                KEY_CHARACTER_CREATED,
                false
            )

        p.edit().apply {
            putString(
                KEY_PET_GENDER,
                if (isBoy) "boy" else "girl"
            )
            putString(KEY_PET_NAME, name)
            putBoolean(KEY_CHARACTER_CREATED, true)

            if (!alreadyCreated) {
                putInt(KEY_BALANCE, START_COINS)
            }
        }.apply()
    }

    fun consumeStartBonusFlag(context: Context): Boolean {
        val p = prefs(context)

        val created =
            p.getBoolean(
                KEY_CHARACTER_CREATED,
                false
            )

        val shown =
            p.getBoolean(
                KEY_START_BONUS_SHOWN,
                false
            )

        if (!created || shown) {
            return false
        }

        p.edit()
            .putBoolean(
                KEY_START_BONUS_SHOWN,
                true
            )
            .apply()

        return true
    }

    fun getBalance(context: Context): Int =
        prefs(context).getInt(
            KEY_BALANCE,
            0
        )

    fun setBalance(
        context: Context,
        value: Int
    ) {
        prefs(context)
            .edit()
            .putInt(KEY_BALANCE, maxOf(0, value))
            .apply()
    }

    fun addCoins(
        context: Context,
        amount: Int
    ): Int {
        if (amount <= 0) {
            return getBalance(context)
        }

        val newBalance =
            getBalance(context) + amount

        setBalance(context, newBalance)

        return newBalance
    }

    fun canSpend(
        context: Context,
        amount: Int
    ): SpendResult {
        return when {
            amount <= 0 ->
                SpendResult.INVALID_AMOUNT

            !isPlanConfirmed(context) ->
                SpendResult.PLAN_NOT_CONFIRMED

            getBalance(context) < amount ->
                SpendResult.NOT_ENOUGH_COINS

            else ->
                SpendResult.OK
        }
    }

    fun spendCoins(
        context: Context,
        amount: Int,
        category: BudgetCategory
    ): SpendResult {
        val result = canSpend(context, amount)

        if (result != SpendResult.OK) {
            return result
        }

        val p = prefs(context)
        val key = factKey(category)

        val balance =
            p.getInt(KEY_BALANCE, 0)

        val fact =
            p.getInt(key, 0)

        p.edit()
            .putInt(
                KEY_BALANCE,
                balance - amount
            )
            .putInt(
                key,
                fact + amount
            )
            .apply()

        return SpendResult.OK
    }

    fun getSavings(context: Context): Int =
        prefs(context).getInt(
            KEY_SAVINGS_TOTAL,
            0
        )

    fun getSelectedGoalId(context: Context): String =
        prefs(context).getString(
            KEY_SELECTED_GOAL,
            ""
        ) ?: ""

    fun getSelectedGoal(
        context: Context
    ): SavingsGoal? =
        GoalCatalog.find(
            getSelectedGoalId(context)
        )

    fun selectGoal(
        context: Context,
        goalId: String
    ) {
        if (GoalCatalog.find(goalId) == null) {
            return
        }

        prefs(context)
            .edit()
            .putString(KEY_SELECTED_GOAL, goalId)
            .apply()
    }

    fun goalSavedKey(goalId: String) =
        "goal_saved_$goalId"

    fun getGoalSaved(
        context: Context,
        goalId: String
    ): Int =
        prefs(context).getInt(
            goalSavedKey(goalId),
            0
        )

    fun canDeposit(
        context: Context,
        goal: SavingsGoal,
        amount: Int
    ): SavingsResult {
        val saved =
            getGoalSaved(
                context,
                goal.id
            )

        return when {
            amount <= 0 ->
                SavingsResult.INVALID_AMOUNT

            !isPlanConfirmed(context) ->
                SavingsResult.PLAN_NOT_CONFIRMED

            saved >= goal.cost ->
                SavingsResult.GOAL_REACHED

            getBalance(context) < amount ->
                SavingsResult.NOT_ENOUGH_COINS

            amount > goal.cost - saved ->
                SavingsResult.EXCEEDS_REMAINING

            else ->
                SavingsResult.OK
        }
    }

    fun depositToGoal(
        context: Context,
        goalId: String,
        amount: Int
    ): SavingsResult {
        val goal =
            GoalCatalog.find(goalId)
                ?: return SavingsResult.NO_GOAL

        val result =
            canDeposit(
                context,
                goal,
                amount
            )

        if (result != SavingsResult.OK) {
            return result
        }

        val p = prefs(context)

        val savingsFactKey =
            factKey(BudgetCategory.SAVINGS)

        val balance =
            p.getInt(KEY_BALANCE, 0)

        val savings =
            p.getInt(KEY_SAVINGS_TOTAL, 0)

        val goalSaved =
            p.getInt(
                goalSavedKey(goal.id),
                0
            )

        val fact =
            p.getInt(savingsFactKey, 0)

        p.edit()
            .putInt(
                KEY_BALANCE,
                balance - amount
            )
            .putInt(
                KEY_SAVINGS_TOTAL,
                savings + amount
            )
            .putInt(
                goalSavedKey(goal.id),
                goalSaved + amount
            )
            .putInt(
                savingsFactKey,
                fact + amount
            )
            .apply()

        return SavingsResult.OK
    }

    fun withdrawFromGoal(
        context: Context,
        goalId: String,
        amount: Int
    ): SavingsResult {
        val goal =
            GoalCatalog.find(goalId)
                ?: return SavingsResult.NO_GOAL

        if (amount <= 0) {
            return SavingsResult.INVALID_AMOUNT
        }

        val p = prefs(context)

        val saved =
            p.getInt(
                goalSavedKey(goal.id),
                0
            )

        if (saved < amount) {
            return SavingsResult.NOT_ENOUGH_SAVED
        }

        val savingsFactKey =
            factKey(BudgetCategory.SAVINGS)

        p.edit()
            .putInt(
                KEY_BALANCE,
                p.getInt(KEY_BALANCE, 0) + amount
            )
            .putInt(
                KEY_SAVINGS_TOTAL,
                maxOf(
                    0,
                    p.getInt(KEY_SAVINGS_TOTAL, 0) - amount
                )
            )
            .putInt(
                goalSavedKey(goal.id),
                saved - amount
            )
            .putInt(
                savingsFactKey,
                maxOf(
                    0,
                    p.getInt(savingsFactKey, 0) - amount
                )
            )
            .apply()

        return SavingsResult.OK
    }

    fun getPeriod(context: Context): Int =
        prefs(context).getInt(
            KEY_PERIOD,
            1
        )

    fun isPlanConfirmed(context: Context): Boolean =
        prefs(context).getBoolean(
            KEY_PLAN_CONFIRMED,
            false
        )

    fun savePlan(
        context: Context,
        amounts: Amounts
    ) {
        prefs(context).edit().apply {
            BudgetCategory.values().forEach { category ->
                putInt(
                    planKey(category),
                    amounts[category]
                )
            }
        }.apply()
    }

    fun confirmPlan(context: Context): Boolean {
        val plan =
            readAmounts(
                context,
                ::planKey
            )

        val valid =
            plan.total > 0 &&
                    plan.total <= getBalance(context) &&
                    plan.mandatory > 0 &&
                    plan.savings > 0

        if (!valid) {
            return false
        }

        prefs(context)
            .edit()
            .putBoolean(
                KEY_PLAN_CONFIRMED,
                true
            )
            .putLong(
                KEY_PERIOD_STARTED_AT,
                System.currentTimeMillis()
            )
            .apply()

        return true
    }

    fun canFinishPeriod(context: Context): Boolean {
        if (!isPlanConfirmed(context)) {
            return false
        }

        val actual =
            readAmounts(
                context,
                ::factKey
            )

        if (actual.total <= 0) {
            return false
        }

        if (isDemoMode(context)) {
            return true
        }

        val started =
            prefs(context).getLong(
                KEY_PERIOD_STARTED_AT,
                0L
            )

        if (started <= 0L) {
            return false
        }

        return System.currentTimeMillis() - started >=
                PERIOD_DURATION_MS
    }

    fun getRemainingPeriodTime(context: Context): Long {
        if (!isPlanConfirmed(context)) {
            return 0L
        }

        if (isDemoMode(context)) {
            return 0L
        }

        val started =
            prefs(context).getLong(
                KEY_PERIOD_STARTED_AT,
                0L
            )

        if (started <= 0L) {
            return 0L
        }

        val elapsed =
            System.currentTimeMillis() - started

        return (
                PERIOD_DURATION_MS - elapsed
                ).coerceAtLeast(0L)
    }

    fun getFinishPeriodError(
        context: Context
    ): FinishPeriodError? {
        if (!isPlanConfirmed(context)) {
            return FinishPeriodError.NO_ACTIVITY
        }

        val actual =
            readAmounts(
                context,
                ::factKey
            )

        if (actual.total <= 0) {
            return FinishPeriodError.NO_ACTIVITY
        }

        if (!isDemoMode(context)) {
            val started =
                prefs(context).getLong(
                    KEY_PERIOD_STARTED_AT,
                    0L
                )

            if (
                started <= 0L ||
                System.currentTimeMillis() - started <
                PERIOD_DURATION_MS
            ) {
                return FinishPeriodError.TOO_EARLY
            }
        }

        return null
    }

    fun calculateGrowthGain(points: Int): Int =
        if (points >= 2) 1 else 0

    fun getGrowth(context: Context): Int =
        prefs(context).getInt(
            KEY_GROWTH,
            0
        )

    fun stageIndex(growth: Int): Int =
        when {
            growth >= ADULT_GROWTH -> 2
            growth >= TEEN_GROWTH -> 1
            else -> 0
        }

    fun getStageName(context: Context): String =
        STAGES[
            stageIndex(
                getGrowth(context)
            )
        ]

    fun finishPeriod(
        context: Context,
        newGrowth: Int,
        mood: Int,
        summary: String
    ) {
        val p = prefs(context)

        val nextPeriod =
            p.getInt(
                KEY_PERIOD,
                1
            ) + 1

        p.edit().apply {
            putInt(
                KEY_GROWTH,
                newGrowth
            )

            putInt(
                KEY_MOOD,
                mood
            )

            putInt(
                KEY_PERIOD,
                nextPeriod
            )

            putBoolean(
                KEY_PLAN_CONFIRMED,
                false
            )

            putLong(
                KEY_PERIOD_STARTED_AT,
                0L
            )

            BudgetCategory.values().forEach { category ->
                putInt(
                    planKey(category),
                    0
                )

                putInt(
                    factKey(category),
                    0
                )
            }

            putString(
                KEY_LAST_SUMMARY,
                summary
            )
        }.apply()
    }

    fun getMood(context: Context): Int =
        prefs(context).getInt(
            KEY_MOOD,
            70
        )

    fun getLastSummary(context: Context): String =
        prefs(context).getString(
            KEY_LAST_SUMMARY,
            ""
        ) ?: ""

    fun planKey(category: BudgetCategory): String =
        "plan_${category.name}"

    fun factKey(category: BudgetCategory): String =
        "fact_${category.name}"

    fun readAmounts(
        context: Context,
        key: (BudgetCategory) -> String
    ): Amounts {
        val p = prefs(context)

        return Amounts(
            mandatory = p.getInt(
                key(BudgetCategory.MANDATORY),
                0
            ),
            optional = p.getInt(
                key(BudgetCategory.OPTIONAL),
                0
            ),
            savings = p.getInt(
                key(BudgetCategory.SAVINGS),
                0
            )
        )
    }

    fun isDemoMode(context: Context): Boolean =
        prefs(context).getBoolean(
            KEY_DEMO,
            false
        )

    fun setDemoMode(
        context: Context,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(
                KEY_DEMO,
                value
            )
            .apply()
    }

    fun writeRawInt(
        context: Context,
        key: String,
        value: Int
    ) {
        prefs(context)
            .edit()
            .putInt(key, value)
            .apply()
    }

    fun writeRawString(
        context: Context,
        key: String,
        value: String
    ) {
        prefs(context)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun writeRawBoolean(
        context: Context,
        key: String,
        value: Boolean
    ) {
        prefs(context)
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    fun commitEdit(
        context: Context,
        block: android.content.SharedPreferences.Editor.() -> Unit
    ) {
        prefs(context)
            .edit()
            .apply(block)
            .apply()
    }

    fun resetAll(context: Context) {
        prefs(context)
            .edit()
            .clear()
            .apply()
    }

    fun resetProgress(context: Context) {
        val gender = getPetGender(context)
        val name = getPetName(context)
        val demo = isDemoMode(context)

        prefs(context).edit().apply {
            clear()

            putString(
                KEY_PET_GENDER,
                gender
            )

            putString(
                KEY_PET_NAME,
                name
            )

            putBoolean(
                KEY_CHARACTER_CREATED,
                true
            )

            putBoolean(
                KEY_DEMO,
                demo
            )

            putInt(
                KEY_BALANCE,
                START_COINS
            )
        }.apply()
    }
}