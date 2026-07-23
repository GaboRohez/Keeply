package com.gabow95k.keeply.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.UserProfileEntity
import com.gabow95k.keeply.databinding.FragmentEditProfileBinding
import com.gabow95k.keeply.presentation.base.BaseFragment
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
        if (name.isBlank()) {
            binding.etName.error = getString(R.string.profile_error_name)
            return
        }

        val ageText = binding.etAge.text?.toString()?.trim().orEmpty()
        val age = ageText.toIntOrNull()
        if (ageText.isNotEmpty() && age == null) {
            binding.etAge.error = getString(R.string.profile_error_age)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            KeeplyDatabase.getInstance(requireContext()).userProfileDao().upsert(
                UserProfileEntity(
                    id = UserProfileEntity.SINGLE_PROFILE_ID,
                    name = name,
                    age = age,
                    bloodType = binding.etBloodType.text?.toString()?.trim()?.ifBlank { null },
                    phone = binding.etPhone.text?.toString()?.trim()?.ifBlank { null },
                    email = binding.etEmail.text?.toString()?.trim()?.ifBlank { null },
                    notes = binding.etNotes.text?.toString()?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    companion object {
        const val TAG = "EditProfileFragment"

        fun newInstance(): EditProfileFragment = EditProfileFragment()
    }
}
