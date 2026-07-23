package com.gabow95k.keeply.presentation.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.ActivityControllerBinding
import com.gabow95k.keeply.presentation.base.BaseActivity

class ControllerActivity : BaseActivity<ActivityControllerBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityControllerBinding =
        ActivityControllerBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.controllerRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val message = when (item.itemId) {
                R.id.nav_home -> getString(R.string.nav_home)
                R.id.nav_botiquin -> getString(R.string.nav_botiquin)
                R.id.nav_alerts -> getString(R.string.nav_alerts)
                R.id.nav_settings -> getString(R.string.nav_settings)
                else -> return@setOnItemSelectedListener false
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            true
        }
    }
}
