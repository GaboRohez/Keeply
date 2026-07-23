package com.gabow95k.keeply.presentation.controller

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.databinding.ActivityControllerBinding
import com.gabow95k.keeply.presentation.base.BaseActivity
import com.gabow95k.keeply.presentation.botiquin.BotiquinFragment
import com.gabow95k.keeply.presentation.home.HomeFragment
import com.gabow95k.keeply.presentation.privacy.PrivacyPolicyActivity
import com.gabow95k.keeply.presentation.settings.SettingsFragment
import com.gabow95k.keeply.presentation.shopping.ShoppingListsFragment

class ControllerActivity : BaseActivity<ActivityControllerBinding>() {

    private var openShoppingAutoOnce: Boolean = false

    override fun inflateBinding(inflater: LayoutInflater): ActivityControllerBinding =
        ActivityControllerBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!KeeplyPreferences.getInstance(this).hasAcceptedCurrentPrivacy()) {
            startActivity(PrivacyPolicyActivity.newGateIntent(this))
            finish()
            return
        }

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

    fun navigateToTab(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    fun navigateToShoppingAutoGenerate() {
        openShoppingAutoOnce = true
        if (binding.bottomNavigation.selectedItemId == R.id.nav_shopping) {
            supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            showTab(R.id.nav_shopping)
        } else {
            binding.bottomNavigation.selectedItemId = R.id.nav_shopping
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
            R.id.nav_shopping -> ShoppingListsFragment.TAG
            R.id.nav_settings -> SettingsFragment.TAG
            else -> return
        }

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        listOf(
            HomeFragment.TAG,
            BotiquinFragment.TAG,
            ShoppingListsFragment.TAG,
            SettingsFragment.TAG
        ).forEach { existingTag ->
            fragmentManager.findFragmentByTag(existingTag)?.let { transaction.hide(it) }
        }

        val existing = fragmentManager.findFragmentByTag(tag)
        if (itemId == R.id.nav_shopping && openShoppingAutoOnce) {
            existing?.let { transaction.remove(it) }
            transaction.add(
                R.id.fragmentContainer,
                ShoppingListsFragment.newInstance(openAutoDialog = true),
                tag
            )
            openShoppingAutoOnce = false
        } else if (existing != null) {
            transaction.show(existing)
        } else {
            transaction.add(R.id.fragmentContainer, createFragment(itemId), tag)
        }
        transaction.commit()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_home -> HomeFragment.newInstance()
        R.id.nav_botiquin -> BotiquinFragment.newInstance()
        R.id.nav_shopping -> ShoppingListsFragment.newInstance(openShoppingAutoOnce).also {
            openShoppingAutoOnce = false
        }

        R.id.nav_settings -> SettingsFragment.newInstance()
        else -> HomeFragment.newInstance()
    }
}
