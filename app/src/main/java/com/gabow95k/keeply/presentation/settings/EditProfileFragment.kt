package com.gabow95k.keeply.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import com.gabow95k.keeply.databinding.FragmentEditProfileBinding
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.util.PrettyToast
import com.gabow95k.keeply.util.InputValidation
import kotlinx.coroutines.launch

class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentEditProfileBinding = FragmentEditProfileBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { saveProfile() }

        viewLifecycleOwner.lifecycleScope.launch {
            val profile = KeeplyDatabase.getInstance(requireContext())
                .userProfileDao()
                .getProfile()
            if (profile != null) {
                binding.etName.setText(profile.name)
                binding.etAge.setText(profile.age?.toString().orEmpty())
                binding.etBloodType.setText(profile.bloodType.orEmpty())
                binding.etPhone.setText(profile.phone.orEmpty())
                binding.etEmail.setText(profile.email.orEmpty())
                binding.etNotes.setText(profile.notes.orEmpty())
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        if (!InputValidation.isValidRequiredText(name)) {
            binding.etName.error = getString(R.string.profile_error_name)
            return
        }
        binding.etName.error = null

        val ageText = binding.etAge.text?.toString()?.trim().orEmpty()
        val age = ageText.toIntOrNull()
        if ((ageText.isNotEmpty() && age == null) || !InputValidation.isValidAge(age)) {
            binding.etAge.error = getString(R.string.profile_error_age)
            return
        }
        binding.etAge.error = null

        val bloodType = binding.etBloodType.text?.toString().orEmpty()
        if (!InputValidation.isValidBloodType(bloodType)) {
            binding.etBloodType.error = getString(R.string.profile_error_blood_type)
            return
        }
        binding.etBloodType.error = null

        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        if (!InputValidation.isValidPhone(phone)) {
            binding.etPhone.error = getString(R.string.profile_error_phone)
            return
        }
        binding.etPhone.error = null

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        if (!InputValidation.isValidEmail(email)) {
            binding.etEmail.error = getString(R.string.profile_error_email)
            return
        }
        binding.etEmail.error = null

        val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
        if (!InputValidation.isValidOptionalText(notes, InputValidation.NOTES_MAX_LENGTH)) {
            binding.etNotes.error = getString(R.string.input_error_too_long)
            return
        }
        binding.etNotes.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            KeeplyDatabase.getInstance(requireContext()).userProfileDao().upsert(
                UserProfileEntity(
                    id = UserProfileEntity.SINGLE_PROFILE_ID,
                    name = name,
                    age = age,
                    bloodType = InputValidation.normalizeBloodType(bloodType).ifBlank { null },
                    phone = phone.ifBlank { null },
                    email = email.ifBlank { null },
                    notes = notes.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
            PrettyToast.success(binding.root, R.string.profile_saved)
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        const val TAG = "EditProfileFragment"

        fun newInstance(): EditProfileFragment = EditProfileFragment()
    }
}
