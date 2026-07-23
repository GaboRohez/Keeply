package com.gabow95k.keeply.presentation.controller

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.ActivityControllerBinding
import com.gabow95k.keeply.presentation.alerts.AlertsFragment
import com.gabow95k.keeply.presentation.base.BaseActivity
import com.gabow95k.keeply.presentation.botiquin.BotiquinFragment
import com.gabow95k.keeply.presentation.home.HomeFragment
import com.gabow95k.keeply.presentation.settings.SettingsFragment

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

        if (savedInstanceState == null) {
            showTab(R.id.nav_home)
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Clear form back stack when changing tabs
            supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            showTab(item.itemId)
            true
        }
    }

    private fun showTab(itemId: Int) {
        val tag = when (itemId) {
            R.id.nav_home -> HomeFragment.TAG
            R.id.nav_botiquin -> BotiquinFragment.TAG
            R.id.nav_alerts -> AlertsFragment.TAG
            R.id.nav_settings -> SettingsFragment.TAG
            else -> return
        }

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        listOf(
            HomeFragment.TAG,
            BotiquinFragment.TAG,
            AlertsFragment.TAG,
            SettingsFragment.TAG
        ).forEach { existingTag ->
            fragmentManager.findFragmentByTag(existingTag)?.let { transaction.hide(it) }
        }

        val existing = fragmentManager.findFragmentByTag(tag)
        if (existing != null) {
            transaction.show(existing)
        } else {
            transaction.add(R.id.fragmentContainer, createFragment(itemId), tag)
        }
        transaction.commit()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_home -> HomeFragment.newInstance()
        R.id.nav_botiquin -> BotiquinFragment.newInstance()
        R.id.nav_alerts -> AlertsFragment.newInstance()
        R.id.nav_settings -> SettingsFragment.newInstance()
        else -> HomeFragment.newInstance()
    }
}
