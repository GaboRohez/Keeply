package com.gabow95k.keeply.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.ViewPrettyToastBinding

/**
 * Toast estilo Cap-go pretty-toast:
 * pill tipo island arriba, icono + título, stroke de acento.
 */
object PrettyToast {

    enum class Style {
        SUCCESS,
        ERROR
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentToast: View? = null
    private var dismissRunnable: Runnable? = null

    fun success(
        anchor: View,
        title: CharSequence,
        message: CharSequence? = null,
        long: Boolean = false
    ) {
        show(anchor, title, message, Style.SUCCESS, long)
    }

    fun success(anchor: View, @StringRes titleRes: Int, long: Boolean = false) {
        show(anchor, anchor.context.getString(titleRes), null, Style.SUCCESS, long)
    }

    fun error(
        anchor: View,
        title: CharSequence,
        message: CharSequence? = null,
        long: Boolean = false
    ) {
        show(anchor, title, message, Style.ERROR, long)
    }

    fun error(anchor: View, @StringRes titleRes: Int, long: Boolean = false) {
        show(anchor, anchor.context.getString(titleRes), null, Style.ERROR, long)
    }

    fun dismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
        val toast = currentToast ?: return
        currentToast = null
        toast.animate()
            .translationY(-40f)
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                (toast.parent as? ViewGroup)?.removeView(toast)
            }
            .start()
    }

    private fun show(
        anchor: View,
        title: CharSequence,
        message: CharSequence?,
        style: Style,
        long: Boolean
    ) {
        val activity = anchor.context.findActivity() ?: return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val context = activity

        // Reemplaza el toast actual (force, como Cap-go)
        dismissRunnable?.let { handler.removeCallbacks(it) }
        currentToast?.let { old ->
            (old.parent as? ViewGroup)?.removeView(old)
        }
        currentToast = null

        val binding = ViewPrettyToastBinding.inflate(LayoutInflater.from(context), content, false)
        binding.tvToastTitle.text = title
        val hasMessage = !message.isNullOrBlank()
        binding.tvToastMessage.isVisible = hasMessage
        if (hasMessage) {
            binding.tvToastMessage.text = message
        }

        when (style) {
            Style.SUCCESS -> {
                binding.toastContent.setBackgroundResource(R.drawable.bg_toast_success)
                binding.iconContainer.setBackgroundResource(R.drawable.bg_toast_icon_success)
                binding.ivToastIcon.setImageResource(R.drawable.ic_toast_success)
                binding.tvToastTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.keeply_toast_success_text)
                )
            }

            Style.ERROR -> {
                binding.toastContent.setBackgroundResource(R.drawable.bg_toast_error)
                binding.iconContainer.setBackgroundResource(R.drawable.bg_toast_icon_error)
                binding.ivToastIcon.setImageResource(R.drawable.ic_toast_error)
                binding.tvToastTitle.setTextColor(
                    ContextCompat.getColor(context, R.color.keeply_toast_error_text)
                )
            }
        }

        val topInset = ViewCompat.getRootWindowInsets(content)
            ?.getInsets(WindowInsetsCompat.Type.statusBars())
            ?.top
            ?: 0
        val density = context.resources.displayMetrics.density
        val topMargin = topInset + (12 * density).toInt()
        val sideMargin = (20 * density).toInt()

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setMargins(sideMargin, topMargin, sideMargin, 0)
        }

        val toastView = binding.root
        content.addView(toastView, lp)
        currentToast = toastView

        toastView.translationY = -48f
        toastView.alpha = 0f
        toastView.scaleX = 0.92f
        toastView.scaleY = 0.92f
        toastView.animate()
            .translationY(0f)
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(240)
            .start()

        val delay = if (long) 3500L else 2200L
        val runnable = Runnable { dismiss() }
        dismissRunnable = runnable
        handler.postDelayed(runnable, delay)
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
