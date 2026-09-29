package com.niobium.finni

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CustomDialog(context: Context) {

    private val dialog = Dialog(context)

    private val root = LayoutInflater.from(context)
        .inflate(R.layout.dialog_custom, null)

    private val tvTitle =
        root.findViewById<TextView>(R.id.dialog_title)

    private val tvMessage =
        root.findViewById<TextView>(R.id.dialog_message)

    private val buttonsContainer =
        root.findViewById<LinearLayout>(R.id.dialog_buttons)

    private val inputLayout =
        root.findViewById<TextInputLayout>(R.id.dialog_input_layout)

    private val input =
        root.findViewById<TextInputEditText>(R.id.dialog_input)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(root)
        dialog.setCancelable(false)
    }

    fun title(text: String) = apply {
        tvTitle.text = text
    }

    fun message(text: String) = apply {
        tvMessage.text = text
    }

    fun input(
        hint: String = "",
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        maxLength: Int = 100,
        imeAction: Int = EditorInfo.IME_ACTION_DONE
    ) = apply {
        inputLayout.visibility = View.VISIBLE
        input.hint = hint
        input.inputType = inputType
        input.imeOptions = imeAction
        input.filters = arrayOf(
            InputFilter.LengthFilter(maxLength)
        )
    }

    fun button(
        label: String,
        action: () -> Unit
    ) = apply {
        addButton(label) {
            dialog.dismiss()
            action()
        }
    }

    fun inputButton(
        label: String,
        action: (String) -> Unit
    ) = apply {
        addButton(label) {
            val value = input.text?.toString().orEmpty()
            dialog.dismiss()
            action(value)
        }
    }

    private fun addButton(
        label: String,
        action: () -> Unit
    ) {
        val context = root.context

        val btn = MaterialButton(context).apply {
            text = label
            textSize = 15f
            setTextColor(context.getColor(R.color.white))
            backgroundTintList =
                context.getColorStateList(R.color.blue_button)
            rippleColor =
                context.getColorStateList(R.color.blue_button_ripple)
            cornerRadius =
                (16 * resources.displayMetrics.density).toInt()

            setOnClickListener {
                action()
            }
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart =
                (8 * context.resources.displayMetrics.density).toInt()
        }

        buttonsContainer.addView(btn, lp)
    }

    fun show() {
        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    fun dismiss() {
        dialog.dismiss()
    }
}