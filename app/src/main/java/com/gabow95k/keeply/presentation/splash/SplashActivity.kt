package com.gabow95k.keeply.presentation.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.databinding.ActivitySplashBinding
import com.gabow95k.keeply.presentation.base.BaseActivity
import com.gabow95k.keeply.presentation.controller.ControllerActivity
import com.gabow95k.keeply.presentation.privacy.PrivacyPolicyActivity

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val splashHandler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable { navigateNext() }

    override fun inflateBinding(inflater: LayoutInflater): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe ir antes de super.onCreate: reemplaza el splash del sistema (icono launcher)
        // por el de Keeply y hace el handoff sin “doble splash”.
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        splashHandler.postDelayed(navigateRunnable, SPLASH_DELAY_MS)
    }

    private fun navigateNext() {
        val prefs = KeeplyPreferences.getInstance(this)
        val next = if (prefs.hasAcceptedCurrentPrivacy()) {
            Intent(this, ControllerActivity::class.java)
        } else {
            PrivacyPolicyActivity.newGateIntent(this)
        }
        startActivity(next)
        finish()
    }

    override fun onDestroy() {
        splashHandler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 2_000L
    }
}
