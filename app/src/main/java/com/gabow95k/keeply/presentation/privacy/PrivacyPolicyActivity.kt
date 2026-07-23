package com.gabow95k.keeply.presentation.privacy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.databinding.ActivityPrivacyPolicyBinding
import com.gabow95k.keeply.presentation.base.BaseActivity
import com.gabow95k.keeply.presentation.controller.ControllerActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrivacyPolicyActivity : BaseActivity<ActivityPrivacyPolicyBinding>() {

    private lateinit var prefs: KeeplyPreferences
    private var viewOnly: Boolean = false

    override fun inflateBinding(inflater: LayoutInflater): ActivityPrivacyPolicyBinding =
        ActivityPrivacyPolicyBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = KeeplyPreferences.getInstance(this)
        viewOnly = intent.getBooleanExtra(EXTRA_VIEW_ONLY, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindMode()
        binding.btnAccept.setOnClickListener { acceptAndContinue() }
        binding.btnDecline.setOnClickListener { declineAndExit() }
        binding.btnClose.setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewOnly) {
                        finish()
                    } else {
                        declineAndExit()
                    }
                }
            }
        )
    }

    private fun bindMode() {
        binding.gateActions.isVisible = !viewOnly
        binding.btnClose.isVisible = viewOnly

        if (viewOnly && prefs.hasAcceptedCurrentPrivacy()) {
            binding.tvAcceptedStatus.isVisible = true
            binding.tvAcceptedStatus.text = getString(
                R.string.privacy_accepted_on,
                formatAcceptedDate(prefs.privacyAcceptedAt)
            )
        } else {
            binding.tvAcceptedStatus.isVisible = false
        }
    }

    private fun acceptAndContinue() {
        prefs.acceptCurrentPrivacy()
        startActivity(Intent(this, ControllerActivity::class.java))
        finish()
    }

    private fun declineAndExit() {
        finishAffinity()
    }

    private fun formatAcceptedDate(millis: Long): String {
        if (millis <= 0L) return "—"
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))
    }

    companion object {
        private const val EXTRA_VIEW_ONLY = "extra_view_only"

        fun newGateIntent(context: Context): Intent =
            Intent(context, PrivacyPolicyActivity::class.java)

        fun newViewIntent(context: Context): Intent =
            Intent(context, PrivacyPolicyActivity::class.java).putExtra(EXTRA_VIEW_ONLY, true)
    }
}
