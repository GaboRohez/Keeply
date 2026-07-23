package com.gabow95k.keeply.presentation.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.FragmentPlaceholderBinding
import com.gabow95k.keeply.presentation.base.BaseFragment

class AlertsFragment : BaseFragment<FragmentPlaceholderBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentPlaceholderBinding = FragmentPlaceholderBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.setText(R.string.nav_alerts)
    }

    companion object {
        const val TAG = "AlertsFragment"

        fun newInstance(): AlertsFragment = AlertsFragment()
    }
}
