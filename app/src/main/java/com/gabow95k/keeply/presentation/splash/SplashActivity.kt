package com.gabow95k.keeply.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gabow95k.keeply.MainActivity
import com.gabow95k.keeply.databinding.ActivitySplashBinding
import com.gabow95k.keeply.presentation.base.BaseActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val splashHandler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        splashHandler.postDelayed(navigateRunnable, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        splashHandler.removeCallbacks(navigateRunnable)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 2_000L
    }
}
