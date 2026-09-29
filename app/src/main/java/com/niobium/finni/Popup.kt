package com.niobium.finni

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

object Popup {

    private const val POPUP_TAG = "app_popup"
    private const val ANIM_DURATION = 250L
    private const val SLIDE_OFFSET = -40f

    fun show(activity: Activity, text: String, showTimeMs: Long = 1800L, onEnd: (() -> Unit)? = null) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        root.findViewWithTag<View>(POPUP_TAG)?.let {
            it.animate().cancel()
            root.removeView(it)
        }

        val popup = LayoutInflater.from(activity).inflate(R.layout.view_popup, root, false)
        popup.tag = POPUP_TAG
        popup.findViewById<TextView>(R.id.tv_popup_text).text = text

        val topMargin = (48 * activity.resources.displayMetrics.density).toInt()
        popup.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            this.topMargin = topMargin
        }

        popup.alpha = 0f
        popup.translationY = SLIDE_OFFSET
        root.addView(popup)

        popup.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(0)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                popup.animate()
                    .alpha(0f)
                    .translationY(SLIDE_OFFSET)
                    .setStartDelay(showTimeMs)
                    .setDuration(ANIM_DURATION)
                    .withEndAction {
                        root.removeView(popup)
                        onEnd?.invoke()
                    }
                    .start()
            }
            .start()
    }
}