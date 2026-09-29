package com.niobium.finni

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class ShopActivity : AppCompatActivity() {

    private data class ShopItem(
        val viewId: Int,
        val title: String,
        val price: Int,
        val effectName: String,
        val effectValue: Int,
        val category: BudgetCategory
    ) {
        val effectText: String
            get() = "$effectName +$effectValue"

        val categoryLabel: String
            get() = when (category) {
                BudgetCategory.MANDATORY -> "Обязательное"
                BudgetCategory.OPTIONAL -> "Желаемое"
                BudgetCategory.SAVINGS -> "Копилка"
            }
    }

    private lateinit var btnNeed: MaterialButton
    private lateinit var btnWant: MaterialButton
    private lateinit var llNeedItems: LinearLayout
    private lateinit var llWantItems: LinearLayout
    private lateinit var tvCoins: TextView

    private val needItems = listOf(
        ShopItem(R.id.item_food, "Еда", 20, "Сытость", 20, BudgetCategory.MANDATORY),
        ShopItem(R.id.item_care, "Уход", 30, "Настроение", 5, BudgetCategory.MANDATORY),
        ShopItem(R.id.item_water, "Чистка воды", 40, "Настроение", 5, BudgetCategory.MANDATORY)
    ).sortedBy { it.price }

    private val wantItems = listOf(
        ShopItem(R.id.item_fish, "Игрушечная рыбка", 50, "Настроение", 10, BudgetCategory.OPTIONAL),
        ShopItem(R.id.item_shell, "Ракушка", 70, "Настроение", 10, BudgetCategory.OPTIONAL),
        ShopItem(R.id.item_ball, "Мяч", 100, "Настроение", 10, BudgetCategory.OPTIONAL),
        ShopItem(R.id.item_balloons, "Воздушные шарики", 130, "Настроение", 10, BudgetCategory.OPTIONAL),
        ShopItem(R.id.item_coral, "Коралловое украшение", 180, "Настроение", 10, BudgetCategory.OPTIONAL)
    ).sortedBy { it.price }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_shop)

        findViewById<MaterialButton>(R.id.btn_back2)?.setOnClickListener {
            finish()
        }

        btnNeed = findViewById(R.id.btn_need)
        btnWant = findViewById(R.id.btn_want)
        llNeedItems = findViewById(R.id.ll_need_items)
        llWantItems = findViewById(R.id.ll_want_items)
        tvCoins = findViewById(R.id.tv_coins)

        selectTab(isNeed = true)

        btnNeed.setOnClickListener { selectTab(isNeed = true) }
        btnWant.setOnClickListener { selectTab(isNeed = false) }

        (needItems + wantItems).forEach { item -> bindItem(item) }
    }

    override fun onResume() {
        super.onResume()
        updateCoinsText()
    }

    private fun bindItem(item: ShopItem) {
        findViewById<LinearLayout>(item.viewId)?.setOnClickListener {
            onItemClicked(item)
        }
    }

    private fun selectTab(isNeed: Boolean) {
        llNeedItems.visibility = if (isNeed) View.VISIBLE else View.GONE
        llWantItems.visibility = if (isNeed) View.GONE else View.VISIBLE

        if (isNeed) {
            setButtonActive(btnNeed)
            setButtonInactive(btnWant)
        } else {
            setButtonActive(btnWant)
            setButtonInactive(btnNeed)
        }
    }

    private fun setButtonActive(button: MaterialButton) {
        button.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.blue_button)
        button.setTextColor(getColor(android.R.color.white))
        button.rippleColor =
            ContextCompat.getColorStateList(this, R.color.blue_button_ripple)
    }

    private fun setButtonInactive(button: MaterialButton) {
        button.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.gray_button)
        button.setTextColor(getColor(R.color.dark_blue))
        button.rippleColor =
            ContextCompat.getColorStateList(this, R.color.gray_button_ripple)
    }

    private fun onItemClicked(item: ShopItem) {
        when (GameManager.canSpend(this, item.price)) {
            SpendResult.OK -> showPurchaseConfirmation(item)
            SpendResult.PLAN_NOT_CONFIRMED -> showPlanRequired()
            SpendResult.NOT_ENOUGH_COINS -> showNotEnoughCoins(item)
            SpendResult.INVALID_AMOUNT -> Unit
        }
    }

    private fun showPlanRequired() {
        CustomDialog(this)
            .title("Сначала план")
            .message("Составь план бюджета — тогда начнётся период и можно будет покупать.")
            .button("К плану") {
                startActivity(Intent(this, BudgetActivity::class.java))
            }
            .button("Позже") {}
            .show()
    }

    private fun showNotEnoughCoins(item: ShopItem) {
        val balance = GameManager.getBalance(this)
        Popup.show(
            this,
            "Не хватает ${item.price - balance} монет. " +
                    "Можно выполнить задание или выбрать подешевле"
        )
    }

    private fun showPurchaseConfirmation(item: ShopItem) {
        CustomDialog(this)
            .title("Покупка")
            .message(
                "Купить «${item.title}» за ${item.price} монет?\n\n" +
                        "Тип: ${item.categoryLabel}\n" +
                        "Для питомца: ${item.effectText}"
            )
            .button("Да") { purchase(item) }
            .button("Нет") {}
            .show()
    }

    private fun purchase(item: ShopItem) {
        when (GameManager.spendCoins(this, item.price, item.category)) {
            SpendResult.OK -> {
                updateCoinsText()
                Popup.show(this, "- ${item.price} монет") {
                    Popup.show(this, "${item.effectName} +")
                }
            }

            SpendResult.PLAN_NOT_CONFIRMED -> showPlanRequired()
            SpendResult.NOT_ENOUGH_COINS -> showNotEnoughCoins(item)
            SpendResult.INVALID_AMOUNT -> Unit
        }
    }

    private fun updateCoinsText() {
        tvCoins.text = "${GameManager.getBalance(this)} монет"
    }
}