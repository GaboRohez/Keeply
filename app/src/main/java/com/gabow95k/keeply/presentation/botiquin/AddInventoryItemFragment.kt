package com.gabow95k.keeply.presentation.botiquin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.StockChangeLogger
import com.gabow95k.keeply.data.local.db.CategoryDefaults
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.CategoryEntity
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.mapper.toDomain
import com.gabow95k.keeply.data.preferences.LookupOptionType
import com.gabow95k.keeply.data.preferences.LookupOptionsStore
import com.gabow95k.keeply.databinding.DialogAddCategoryBinding
import com.gabow95k.keeply.databinding.DialogAddOptionBinding
import com.gabow95k.keeply.databinding.FragmentAddInventoryItemBinding
import com.gabow95k.keeply.domain.model.Category
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.scanner.ProductLabelAnalyzer
import com.gabow95k.keeply.scanner.ProductLabelHints
import com.gabow95k.keeply.util.ProductPhotoStore
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddInventoryItemFragment : BaseFragment<FragmentAddInventoryItemBinding>() {

    private lateinit var lookupStore: LookupOptionsStore
    private var labelAnalyzer: ProductLabelAnalyzer? = null
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long? = null
    private var selectedExpirationDate: Long? = null
    private var editingItemId: Long? = null
    private var existingCreatedAt: Long = System.currentTimeMillis()
    private var existingQuantity: Double = 0.0
    private var existingPhotoPath: String? = null
    private var addOtherLabel: String = ""
    private var pendingPhotoFile: File? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.product_photo_camera_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingPhotoFile
        if (!success || file == null || !file.exists()) {
            file?.delete()
            pendingPhotoFile = null
            Toast.makeText(
                requireContext(),
                R.string.product_photo_capture_failed,
                Toast.LENGTH_SHORT
            ).show()
            return@registerForActivityResult
        }

        val previous = existingPhotoPath
        existingPhotoPath = file.absolutePath
        if (previous != null && previous != existingPhotoPath) {
            ProductPhotoStore.deleteIfOwned(requireContext(), previous)
        }
        pendingPhotoFile = null
        bindPhotoPreview(existingPhotoPath)
        analyzeCapturedPhoto(file)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddInventoryItemBinding =
        FragmentAddInventoryItemBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lookupStore = LookupOptionsStore.getInstance(requireContext())
        labelAnalyzer = ProductLabelAnalyzer(requireContext().applicationContext)
        addOtherLabel = getString(R.string.product_option_add_other)
        editingItemId = arguments?.getLong(ARG_ITEM_ID)?.takeIf { it > 0L }

        if (editingItemId != null) {
            binding.tvTitle.setText(R.string.product_form_title_edit)
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.etExpiration.setOnClickListener { showExpirationPicker() }
        binding.btnSave.setOnClickListener { saveProduct() }
        binding.tilBarcode.setEndIconOnClickListener { startBarcodeScan() }
        binding.btnTakePhoto.setOnClickListener { requestCameraAndCapture() }
        binding.btnRemovePhoto.setOnClickListener { removePhoto() }

        setupLookupDropdown(
            view = binding.actFormType,
            type = LookupOptionType.FORM_TYPE,
            dialogTitleRes = R.string.dialog_form_type_title
        )
        setupLookupDropdown(
            view = binding.actUnit,
            type = LookupOptionType.UNIT,
            dialogTitleRes = R.string.dialog_unit_title
        )
        setupLookupDropdown(
            view = binding.actLocation,
            type = LookupOptionType.LOCATION,
            dialogTitleRes = R.string.dialog_location_title
        )
        setupCategoryDropdown()

        loadCategoriesAndMaybeItem()
        warmUpBarcodeScanner()
    }

    override fun onDestroyView() {
        labelAnalyzer?.close()
        labelAnalyzer = null
        super.onDestroyView()
    }

    private fun requestCameraAndCapture() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) ==
                    PackageManager.PERMISSION_GRANTED -> launchCamera()

            else -> cameraPermissionLauncher.launch(permission)
        }
    }

    private fun launchCamera() {
        val file = ProductPhotoStore.createPhotoFile(requireContext())
        pendingPhotoFile = file
        val uri = ProductPhotoStore.uriFor(requireContext(), file)
        takePictureLauncher.launch(uri)
    }

    private fun removePhoto() {
        ProductPhotoStore.deleteIfOwned(requireContext(), existingPhotoPath)
        existingPhotoPath = null
        bindPhotoPreview(null)
    }

    private fun bindPhotoPreview(path: String?) {
        val hasPhoto = !path.isNullOrBlank()
        binding.btnRemovePhoto.isVisible = hasPhoto
        binding.btnTakePhoto.setText(
            if (hasPhoto) R.string.product_change_photo else R.string.product_take_photo
        )
        if (hasPhoto) {
            Glide.with(binding.ivProductPhoto)
                .load(path)
                .centerCrop()
                .placeholder(R.drawable.ic_product_placeholder)
                .into(binding.ivProductPhoto)
        } else {
            binding.ivProductPhoto.setImageResource(R.drawable.ic_product_placeholder)
        }
    }

    private fun analyzeCapturedPhoto(file: File) {
        val analyzer = labelAnalyzer ?: return
        binding.progressPhotoAnalyze.isVisible = true
        binding.tvPhotoHint.setText(R.string.product_photo_analyzing)
        binding.btnTakePhoto.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val hints = withContext(Dispatchers.IO) {
                runCatching {
                    analyzer.analyze(
                        imageUri = Uri.fromFile(file),
                        knownFormTypes = lookupStore.getOptions(LookupOptionType.FORM_TYPE),
                        knownUnits = lookupStore.getOptions(LookupOptionType.UNIT)
                    )
                }.getOrElse { ProductLabelHints() }
            }

            if (!isAdded) return@launch
            binding.progressPhotoAnalyze.isVisible = false
            binding.btnTakePhoto.isEnabled = true
            binding.tvPhotoHint.setText(R.string.product_photo_hint)

            val filled = applyLabelHints(hints)
            Toast.makeText(
                requireContext(),
                if (filled) R.string.product_photo_filled else R.string.product_photo_no_data,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun applyLabelHints(hints: ProductLabelHints): Boolean {
        var filled = false

        hints.name?.takeIf { it.isNotBlank() }?.let {
            binding.etName.setText(it)
            filled = true
        }
        hints.brand?.takeIf { it.isNotBlank() }?.let {
            binding.etBrand.setText(it)
            filled = true
        }
        hints.barcode?.takeIf { it.isNotBlank() }?.let {
            binding.etBarcode.setText(it)
            filled = true
        }
        hints.quantity?.takeIf { it.isNotBlank() }?.let {
            binding.etQuantity.setText(it)
            filled = true
        }
        hints.expirationMillis?.let { millis ->
            selectedExpirationDate = millis
            binding.etExpiration.setText(dateFormat.format(Date(millis)))
            filled = true
        }
        hints.formType?.let { value ->
            lookupStore.ensurePresent(LookupOptionType.FORM_TYPE, value)
            refreshLookupAdapter(binding.actFormType, LookupOptionType.FORM_TYPE)
            binding.actFormType.setText(value, false)
            binding.actFormType.tag = value
            filled = true
        }
        hints.unit?.let { value ->
            lookupStore.ensurePresent(LookupOptionType.UNIT, value)
            refreshLookupAdapter(binding.actUnit, LookupOptionType.UNIT)
            binding.actUnit.setText(value, false)
            binding.actUnit.tag = value
            filled = true
        }
        return filled
    }

    private fun setupLookupDropdown(
        view: AutoCompleteTextView,
        type: LookupOptionType,
        dialogTitleRes: Int
    ) {
        refreshLookupAdapter(view, type)
        view.setOnItemClickListener { _, _, position, _ ->
            val options = lookupStore.getOptions(type)
            if (position >= options.size) {
                val previous = view.tag as? String
                view.setText(previous.orEmpty(), false)
                showAddOptionDialog(type, dialogTitleRes, view)
            } else {
                val selected = options[position]
                view.tag = selected
                view.setText(selected, false)
            }
        }
        view.setOnClickListener { view.showDropDown() }
        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) view.showDropDown()
        }
    }

    private fun refreshLookupAdapter(view: AutoCompleteTextView, type: LookupOptionType) {
        val items = lookupStore.getOptions(type) + addOtherLabel
        view.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        )
    }

    private fun setupCategoryDropdown() {
        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            if (position >= categories.size) {
                val previousName = categories.firstOrNull { it.id == selectedCategoryId }?.name
                binding.actCategory.setText(previousName.orEmpty(), false)
                showAddCategoryDialog()
            } else {
                selectedCategoryId = categories[position].id
                binding.actCategory.setText(categories[position].name, false)
                binding.tilCategory.error = null
            }
        }
        binding.actCategory.setOnClickListener { binding.actCategory.showDropDown() }
        binding.actCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.actCategory.showDropDown()
        }
    }

    private fun refreshCategoryAdapter(selectId: Long? = selectedCategoryId) {
        val names = categories.map { it.name } + addOtherLabel
        binding.actCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
        )
        val selected = categories.firstOrNull { it.id == selectId }
        if (selected != null) {
            selectedCategoryId = selected.id
            binding.actCategory.setText(selected.name, false)
        }
    }

    private fun loadCategoriesAndMaybeItem() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            categories = db.categoryDao().observeAll().first().map { it.toDomain() }
            refreshCategoryAdapter()

            refreshLookupAdapter(binding.actFormType, LookupOptionType.FORM_TYPE)
            refreshLookupAdapter(binding.actUnit, LookupOptionType.UNIT)
            refreshLookupAdapter(binding.actLocation, LookupOptionType.LOCATION)

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
        existingQuantity = entity.quantity
        existingPhotoPath = entity.photoPath
        selectedCategoryId = entity.categoryId
        selectedExpirationDate = entity.expirationDate

        binding.etName.setText(entity.name)
        binding.etBrand.setText(entity.brand.orEmpty())
        binding.etQuantity.setText(formatQuantity(entity.quantity))
        binding.etMinQuantity.setText(entity.minQuantity?.let { formatQuantity(it) }.orEmpty())
        binding.etBarcode.setText(entity.barcode.orEmpty())
        binding.etNotes.setText(entity.notes.orEmpty())
        binding.etExpiration.setText(
            entity.expirationDate?.let { dateFormat.format(Date(it)) }.orEmpty()
        )
        bindPhotoPreview(entity.photoPath)

        lookupStore.ensurePresent(LookupOptionType.FORM_TYPE, entity.formType)
        lookupStore.ensurePresent(LookupOptionType.UNIT, entity.unit)
        lookupStore.ensurePresent(LookupOptionType.LOCATION, entity.location)
        refreshLookupAdapter(binding.actFormType, LookupOptionType.FORM_TYPE)
        refreshLookupAdapter(binding.actUnit, LookupOptionType.UNIT)
        refreshLookupAdapter(binding.actLocation, LookupOptionType.LOCATION)

        binding.actFormType.setText(entity.formType.orEmpty(), false)
        binding.actFormType.tag = entity.formType
        binding.actUnit.setText(entity.unit.orEmpty(), false)
        binding.actUnit.tag = entity.unit
        binding.actLocation.setText(entity.location.orEmpty(), false)
        binding.actLocation.tag = entity.location

        val category = categories.firstOrNull { it.id == entity.categoryId }
        if (category != null) {
            binding.actCategory.setText(category.name, false)
        }
    }

    private fun showAddOptionDialog(
        type: LookupOptionType,
        titleRes: Int,
        target: AutoCompleteTextView
    ) {
        val dialogBinding = DialogAddOptionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_option_create, null)
            .setNegativeButton(R.string.dialog_option_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = dialogBinding.etOption.text?.toString()?.trim().orEmpty()
                if (value.isEmpty()) {
                    dialogBinding.tilOption.error = getString(R.string.dialog_option_error_empty)
                    return@setOnClickListener
                }
                val added = lookupStore.addCustom(type, value)
                if (!added && lookupStore.getOptions(type)
                        .none { it.equals(value, ignoreCase = true) }
                ) {
                    dialogBinding.tilOption.error = getString(R.string.dialog_option_error_exists)
                    return@setOnClickListener
                }
                val resolved = lookupStore.getOptions(type)
                    .firstOrNull { it.equals(value, ignoreCase = true) }
                    ?: value
                refreshLookupAdapter(target, type)
                target.setText(resolved, false)
                target.tag = resolved
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_category_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.dialog_option_create, null)
            .setNegativeButton(R.string.dialog_option_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etCategoryName.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) {
                    dialogBinding.tilCategoryName.error =
                        getString(R.string.dialog_option_error_empty)
                    return@setOnClickListener
                }
                if (categories.any { it.name.equals(name, ignoreCase = true) }) {
                    dialogBinding.tilCategoryName.error =
                        getString(R.string.dialog_option_error_exists)
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val dao = KeeplyDatabase.getInstance(requireContext()).categoryDao()
                    val newId = dao.insert(
                        CategoryEntity(
                            name = name,
                            iconKey = CategoryDefaults.ICON_OTHER,
                            colorHex = "#5C5C63",
                            sortOrder = categories.size,
                            isDefault = false
                        )
                    )
                    categories = dao.getAll().map { it.toDomain() }
                    refreshCategoryAdapter(selectId = newId)
                    Toast.makeText(
                        requireContext(),
                        R.string.dialog_category_created,
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun warmUpBarcodeScanner() {
        val scanner = GmsBarcodeScanning.getClient(requireContext())
        val request = ModuleInstallRequest.newBuilder().addApi(scanner).build()
        ModuleInstall.getClient(requireContext()).installModules(request)
            .addOnFailureListener { /* optional module; scan still attempts later */ }
    }

    private fun startBarcodeScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX
            )
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue?.trim().orEmpty()
                if (value.isNotEmpty()) {
                    binding.etBarcode.setText(value)
                    binding.etBarcode.setSelection(value.length)
                }
            }
            .addOnCanceledListener {
                Toast.makeText(
                    requireContext(),
                    R.string.product_barcode_scan_canceled,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    R.string.product_barcode_scan_failed,
                    Toast.LENGTH_SHORT
                ).show()
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
            formType = binding.actFormType.text?.toString()?.trim()
                ?.takeUnless { it.isEmpty() || it == addOtherLabel },
            unit = binding.actUnit.text?.toString()?.trim()
                ?.takeUnless { it.isEmpty() || it == addOtherLabel },
            quantity = safeQuantity,
            minQuantity = binding.etMinQuantity.text?.toString()?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.toDoubleOrNull(),
            expirationDate = selectedExpirationDate,
            barcode = binding.etBarcode.text?.toString()?.trim()?.ifEmpty { null },
            photoPath = existingPhotoPath,
            location = binding.actLocation.text?.toString()?.trim()
                ?.takeUnless { it.isEmpty() || it == addOtherLabel },
            notes = binding.etNotes.text?.toString()?.trim()?.ifEmpty { null },
            createdAt = if (editingItemId != null) existingCreatedAt else now,
            updatedAt = now
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val db = KeeplyDatabase.getInstance(requireContext())
            val dao = db.inventoryItemDao()
            if (editingItemId != null) {
                dao.update(entity)
                StockChangeLogger.logQuantityEdit(
                    dao = db.stockChangeEventDao(),
                    itemId = entity.id,
                    productName = entity.name,
                    quantityBefore = existingQuantity,
                    quantityAfter = safeQuantity
                )
                Toast.makeText(requireContext(), R.string.product_updated, Toast.LENGTH_SHORT)
                    .show()
            } else {
                val newId = dao.insert(entity)
                StockChangeLogger.logAdd(
                    dao = db.stockChangeEventDao(),
                    itemId = newId,
                    productName = entity.name,
                    quantity = safeQuantity
                )
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
