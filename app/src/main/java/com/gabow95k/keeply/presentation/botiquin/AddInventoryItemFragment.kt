package com.gabow95k.keeply.presentation.botiquin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.mapper.toDomain
import com.gabow95k.keeply.databinding.FragmentAddInventoryItemBinding
import com.gabow95k.keeply.domain.model.Category
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddInventoryItemFragment : BaseFragment<FragmentAddInventoryItemBinding>() {

    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long? = null
    private var selectedExpirationDate: Long? = null
    private var editingItemId: Long? = null
    private var existingCreatedAt: Long = System.currentTimeMillis()
    private var existingPhotoPath: String? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddInventoryItemBinding =
        FragmentAddInventoryItemBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editingItemId = arguments?.getLong(ARG_ITEM_ID)?.takeIf { it > 0L }

        if (editingItemId != null) {
            binding.tvTitle.setText(R.string.product_form_title_edit)
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.etExpiration.setOnClickListener { showExpirationPicker() }
        binding.btnSave.setOnClickListener { saveProduct() }
        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories.getOrNull(position)?.id
            binding.tilCategory.error = null
        }

        loadCategoriesAndMaybeItem()
    }

    private fun loadCategoriesAndMaybeItem() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            categories = db.categoryDao().observeAll().first().map { it.toDomain() }
            val names = categories.map { it.name }
            binding.actCategory.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            )

            val itemId = editingItemId
            if (itemId != null) {
                val entity = db.inventoryItemDao().getById(itemId)
                if (entity != null) {
                    bindItem(entity)
                }
            } else if (categories.isNotEmpty()) {
                selectedCategoryId = categories.first().id
                binding.actCategory.setText(categories.first().name, false)
            }
        }
    }

    private fun bindItem(entity: InventoryItemEntity) {
        existingCreatedAt = entity.createdAt
        existingPhotoPath = entity.photoPath
        selectedCategoryId = entity.categoryId
        selectedExpirationDate = entity.expirationDate

        binding.etName.setText(entity.name)
        binding.etBrand.setText(entity.brand.orEmpty())
        binding.etFormType.setText(entity.formType.orEmpty())
        binding.etUnit.setText(entity.unit.orEmpty())
        binding.etQuantity.setText(formatQuantity(entity.quantity))
        binding.etMinQuantity.setText(entity.minQuantity?.let { formatQuantity(it) }.orEmpty())
        binding.etBarcode.setText(entity.barcode.orEmpty())
        binding.etLocation.setText(entity.location.orEmpty())
        binding.etNotes.setText(entity.notes.orEmpty())
        binding.etExpiration.setText(
            entity.expirationDate?.let { dateFormat.format(Date(it)) }.orEmpty()
        )

        val category = categories.firstOrNull { it.id == entity.categoryId }
        if (category != null) {
            binding.actCategory.setText(category.name, false)
        }
    }

    private fun showExpirationPicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.product_field_expiration)
            .setSelection(selectedExpirationDate ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedExpirationDate = utcMillis
            binding.etExpiration.setText(dateFormat.format(Date(utcMillis)))
        }
        picker.show(parentFragmentManager, "expiration_picker")
    }

    private fun saveProduct() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val quantityText = binding.etQuantity.text?.toString()?.trim().orEmpty()
        val categoryId = selectedCategoryId

        var hasError = false
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.product_error_name)
            hasError = true
        } else {
            binding.tilName.error = null
        }

        if (categoryId == null) {
            binding.tilCategory.error = getString(R.string.product_error_category)
            hasError = true
        } else {
            binding.tilCategory.error = null
        }

        val quantity = quantityText.toDoubleOrNull()
        if (quantity == null || quantity < 0) {
            binding.tilQuantity.error = getString(R.string.product_error_quantity)
            hasError = true
        } else {
            binding.tilQuantity.error = null
        }

        if (hasError) return

        val safeCategoryId = categoryId ?: return
        val safeQuantity = quantity ?: return

        val now = System.currentTimeMillis()
        val entity = InventoryItemEntity(
            id = editingItemId ?: 0L,
            categoryId = safeCategoryId,
            name = name,
            brand = binding.etBrand.text?.toString()?.trim()?.ifEmpty { null },
            formType = binding.etFormType.text?.toString()?.trim()?.ifEmpty { null },
            unit = binding.etUnit.text?.toString()?.trim()?.ifEmpty { null },
            quantity = safeQuantity,
            minQuantity = binding.etMinQuantity.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toDoubleOrNull(),
            expirationDate = selectedExpirationDate,
            barcode = binding.etBarcode.text?.toString()?.trim()?.ifEmpty { null },
            photoPath = existingPhotoPath,
            location = binding.etLocation.text?.toString()?.trim()?.ifEmpty { null },
            notes = binding.etNotes.text?.toString()?.trim()?.ifEmpty { null },
            createdAt = if (editingItemId != null) existingCreatedAt else now,
            updatedAt = now
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = KeeplyDatabase.getInstance(requireContext()).inventoryItemDao()
            if (editingItemId != null) {
                dao.update(entity)
                Toast.makeText(requireContext(), R.string.product_updated, Toast.LENGTH_SHORT)
                    .show()
            } else {
                dao.insert(entity)
                Toast.makeText(requireContext(), R.string.product_saved, Toast.LENGTH_SHORT).show()
            }
            parentFragmentManager.popBackStack()
        }
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }

    companion object {
        const val TAG = "AddInventoryItemFragment"
        private const val ARG_ITEM_ID = "arg_item_id"

        fun newInstance(itemId: Long? = null): AddInventoryItemFragment {
            return AddInventoryItemFragment().apply {
                if (itemId != null) {
                    arguments = Bundle().apply { putLong(ARG_ITEM_ID, itemId) }
                }
            }
        }
    }
}
