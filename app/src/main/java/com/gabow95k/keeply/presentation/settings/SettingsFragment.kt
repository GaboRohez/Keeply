package com.gabow95k.keeply.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.FragmentPlaceholderBinding
import com.gabow95k.keeply.presentation.base.BaseFragment

class SettingsFragment : BaseFragment<FragmentPlaceholderBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlaceholderBinding = FragmentPlaceholderBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.setText(R.string.nav_settings)
    }

    companion object {
        const val TAG = "SettingsFragment"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
