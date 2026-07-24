package com.gabow95k.keeply.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabow95k.keeply.BuildConfig
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.data.preferences.NotificationCadence
import com.gabow95k.keeply.data.preferences.NotificationTimeSlot
import com.gabow95k.keeply.databinding.FragmentSettingsBinding
import com.gabow95k.keeply.notifications.InventoryAlertScheduler
import com.gabow95k.keeply.notifications.NotificationScheduleEvaluator
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.presentation.privacy.PrivacyPolicyActivity
import com.gabow95k.keeply.util.PrettyToast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private lateinit var prefs: KeeplyPreferences
    private var suppressSwitchCallbacks = false
    private var suppressChipCallbacks = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableNotifications()
        } else {
            suppressSwitchCallbacks = true
            binding.switchNotifications.isChecked = false
            suppressSwitchCallbacks = false
            PrettyToast.error(
                binding.root,
                R.string.settings_notifications_permission_denied,
                long = true
            )
            updateNotificationOptionsEnabled(false)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = KeeplyPreferences.getInstance(requireContext())

        bindPreferenceState()
        setupListeners()
        observeProfile()
        bindPrivacyStatus()

        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    private fun bindPrivacyStatus() {
        binding.tvPrivacyStatus.text = if (prefs.hasAcceptedCurrentPrivacy()) {
            getString(
                R.string.settings_privacy_accepted,
                formatPrivacyDate(prefs.privacyAcceptedAt)
            )
        } else {
            getString(R.string.settings_privacy_pending)
        }
    }

    private fun formatPrivacyDate(millis: Long): String {
        if (millis <= 0L) return "—"
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))
    }

    private fun bindPreferenceState() {
        suppressSwitchCallbacks = true
        suppressChipCallbacks = true

        binding.switchNotifications.isChecked = prefs.notificationsEnabled
        binding.switchExpired.isChecked = prefs.notifyExpired
        binding.switchExpiring.isChecked = prefs.notifyExpiringSoon
        binding.switchOutOfStock.isChecked = prefs.notifyOutOfStock
        binding.switchLowStock.isChecked = prefs.notifyLowStock
        binding.switchShoppingPrompts.isChecked = prefs.notifyShoppingPrompts

        when (prefs.notificationCadence) {
            NotificationCadence.DAILY -> binding.chipCadenceDaily.isChecked = true
            NotificationCadence.EVERY_3_DAYS -> binding.chipCadenceEvery3.isChecked = true
            NotificationCadence.WEEKLY -> binding.chipCadenceWeekly.isChecked = true
        }

        when (prefs.notificationTimesPerDay) {
            1 -> binding.chipTimes1.isChecked = true
            2 -> binding.chipTimes2.isChecked = true
            else -> binding.chipTimes3.isChecked = true
        }

        bindSlotChips(prefs.notificationSlots)
        updateSlotsHint(prefs.notificationTimesPerDay)

        when (prefs.expiringSoonDays) {
            7 -> binding.chipDays7.isChecked = true
            15 -> binding.chipDays15.isChecked = true
            else -> binding.chipDays30.isChecked = true
        }

        suppressSwitchCallbacks = false
        suppressChipCallbacks = false
        updateNotificationOptionsEnabled(prefs.notificationsEnabled)
    }

    private fun setupListeners() {
        binding.cardProfile.setOnClickListener { openEditProfile() }
        binding.rowBackup.setOnClickListener { showComingSoon() }
        binding.rowCategories.setOnClickListener { showComingSoon() }
        binding.rowExport.setOnClickListener { showComingSoon() }
        binding.rowPrivacy.setOnClickListener {
            startActivity(PrivacyPolicyActivity.newViewIntent(requireContext()))
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            if (isChecked) {
                requestNotificationsPermissionOrEnable()
            } else {
                prefs.notificationsEnabled = false
                InventoryAlertScheduler.cancel(requireContext())
                updateNotificationOptionsEnabled(false)
            }
        }

        binding.switchExpired.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitchCallbacks) prefs.notifyExpired = isChecked
        }
        binding.switchExpiring.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitchCallbacks) prefs.notifyExpiringSoon = isChecked
        }
        binding.switchOutOfStock.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitchCallbacks) prefs.notifyOutOfStock = isChecked
        }
        binding.switchLowStock.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitchCallbacks) prefs.notifyLowStock = isChecked
        }
        binding.switchShoppingPrompts.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSwitchCallbacks) prefs.notifyShoppingPrompts = isChecked
        }

        binding.chipGroupCadence.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallbacks || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            prefs.notificationCadence = when {
                checkedIds.contains(R.id.chipCadenceEvery3) -> NotificationCadence.EVERY_3_DAYS
                checkedIds.contains(R.id.chipCadenceWeekly) -> NotificationCadence.WEEKLY
                else -> NotificationCadence.DAILY
            }
            rescheduleIfEnabled()
        }

        binding.chipGroupTimesPerDay.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallbacks || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val times = when {
                checkedIds.contains(R.id.chipTimes2) -> 2
                checkedIds.contains(R.id.chipTimes3) -> 3
                else -> 1
            }
            prefs.applyTimesPerDay(times)
            suppressChipCallbacks = true
            bindSlotChips(prefs.notificationSlots)
            suppressChipCallbacks = false
            updateSlotsHint(times)
            rescheduleIfEnabled()
        }

        binding.chipGroupSlots.setOnCheckedStateChangeListener { group, checkedIds ->
            if (suppressChipCallbacks) return@setOnCheckedStateChangeListener
            val max = prefs.notificationTimesPerDay
            if (checkedIds.size > max) {
                val newest = checkedIds.last()
                suppressChipCallbacks = true
                checkedIds.filter { it != newest }.forEach { id ->
                    group.findViewById<com.google.android.material.chip.Chip>(id)?.isChecked = false
                }
                suppressChipCallbacks = false
            }
            val selected = selectedSlotsFromChips()
            if (selected.size == max) {
                prefs.notificationSlots = selected
                rescheduleIfEnabled()
            } else if (selected.size < max && max == 3) {
                // keep waiting for full selection when not 3 defaults
            }
        }

        binding.chipGroupExpiringDays.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallbacks || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            prefs.expiringSoonDays = when {
                checkedIds.contains(R.id.chipDays7) -> 7
                checkedIds.contains(R.id.chipDays15) -> 15
                else -> 30
            }
        }
    }

    private fun bindSlotChips(slots: Set<NotificationTimeSlot>) {
        binding.chipSlotMorning.isChecked = NotificationTimeSlot.MORNING in slots
        binding.chipSlotAfternoon.isChecked = NotificationTimeSlot.AFTERNOON in slots
        binding.chipSlotEvening.isChecked = NotificationTimeSlot.EVENING in slots
        val single = prefs.notificationTimesPerDay == 1
        binding.chipGroupSlots.isSingleSelection = single
        binding.chipSlotMorning.isEnabled = prefs.notificationTimesPerDay != 3
        binding.chipSlotAfternoon.isEnabled = prefs.notificationTimesPerDay != 3
        binding.chipSlotEvening.isEnabled = prefs.notificationTimesPerDay != 3
    }

    private fun selectedSlotsFromChips(): Set<NotificationTimeSlot> {
        val selected = mutableSetOf<NotificationTimeSlot>()
        if (binding.chipSlotMorning.isChecked) selected += NotificationTimeSlot.MORNING
        if (binding.chipSlotAfternoon.isChecked) selected += NotificationTimeSlot.AFTERNOON
        if (binding.chipSlotEvening.isChecked) selected += NotificationTimeSlot.EVENING
        return selected
    }

    private fun updateSlotsHint(timesPerDay: Int) {
        binding.tvSlotsHint.setText(
            when (timesPerDay) {
                1 -> R.string.settings_slots_hint_1
                2 -> R.string.settings_slots_hint_2
                else -> R.string.settings_slots_hint_3
            }
        )
    }

    private fun observeProfile() {
        val dao = KeeplyDatabase.getInstance(requireContext()).userProfileDao()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.observeProfile().collect { profile ->
                    val name = profile?.name?.takeIf { it.isNotBlank() }
                    binding.tvProfileName.text = name ?: getString(R.string.settings_profile_empty)
                    binding.tvProfileInitial.text = name
                        ?.firstOrNull()
                        ?.uppercaseChar()
                        ?.toString()
                        ?: "?"
                    binding.tvProfileSubtitle.text = if (name == null) {
                        getString(R.string.settings_profile_complete)
                    } else {
                        getString(R.string.settings_profile_edit)
                    }
                }
            }
        }
    }

    private fun requestNotificationsPermissionOrEnable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            enableNotifications()
            return
        }
        val permission = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                    PackageManager.PERMISSION_GRANTED -> enableNotifications()

            else -> notificationPermissionLauncher.launch(permission)
        }
    }

    private fun enableNotifications() {
        prefs.notificationsEnabled = true
        NotificationScheduleEvaluator.ensureAnchor(prefs)
        InventoryAlertScheduler.schedule(requireContext())
        InventoryAlertScheduler.runOnceNow(requireContext())
        updateNotificationOptionsEnabled(true)
        PrettyToast.success(binding.root, R.string.settings_notifications_enabled)
    }

    private fun rescheduleIfEnabled() {
        if (!prefs.notificationsEnabled) return
        NotificationScheduleEvaluator.ensureAnchor(prefs)
        InventoryAlertScheduler.schedule(requireContext())
    }

    private fun updateNotificationOptionsEnabled(enabled: Boolean) {
        binding.notificationOptions.alpha = if (enabled) 1f else 0.45f
        binding.scheduleSection.alpha = if (enabled) 1f else 0.45f

        binding.switchExpired.isEnabled = enabled
        binding.switchExpiring.isEnabled = enabled
        binding.switchOutOfStock.isEnabled = enabled
        binding.switchLowStock.isEnabled = enabled
        binding.switchShoppingPrompts.isEnabled = enabled

        binding.chipCadenceDaily.isEnabled = enabled
        binding.chipCadenceEvery3.isEnabled = enabled
        binding.chipCadenceWeekly.isEnabled = enabled
        binding.chipTimes1.isEnabled = enabled
        binding.chipTimes2.isEnabled = enabled
        binding.chipTimes3.isEnabled = enabled

        val slotsEditable = enabled && prefs.notificationTimesPerDay != 3
        binding.chipSlotMorning.isEnabled = slotsEditable
        binding.chipSlotAfternoon.isEnabled = slotsEditable
        binding.chipSlotEvening.isEnabled = slotsEditable
    }

    private fun showComingSoon() {
        PrettyToast.success(binding.root, R.string.settings_coming_soon)
    }

    private fun openEditProfile() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, EditProfileFragment.newInstance())
            .addToBackStack(EditProfileFragment.TAG)
            .commit()
    }

    companion object {
        const val TAG = "SettingsFragment"

        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
