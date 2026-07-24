package com.gabow95k.keeply.presentation.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity
import com.gabow95k.keeply.databinding.FragmentShoppingListDetailBinding
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.util.PrettyToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ShoppingListDetailFragment : BaseFragment<FragmentShoppingListDetailBinding>() {

    private var listId: Long = 0L
    private var selectedInventoryItemId: Long? = null
    private var productSuggestions: List<ProductSuggestion> = emptyList()
    private var suggestionsAdapter: ArrayAdapter<String>? = null

    private val itemsAdapter = ShoppingItemsAdapter(
        onCheckedChange = { item, checked -> toggleItem(item.id, checked) },
        onDelete = { item -> deleteItem(item.id) }
    )

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentShoppingListDetailBinding =
        FragmentShoppingListDetailBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listId = arguments?.getLong(ARG_LIST_ID) ?: 0L
        binding.rvItems.adapter = itemsAdapter
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnDeleteList.setOnClickListener { confirmDeleteList() }
        binding.btnAddItem.setOnClickListener { addItem() }
        setupProductAutocomplete()
        binding.etNewItem.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItem()
                true
            } else {
                false
            }
        }
        observeDetail()
        observeInventorySuggestions()
    }

    private fun setupProductAutocomplete() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<String>()
        )
        suggestionsAdapter = adapter
        binding.etNewItem.setAdapter(adapter)
        binding.etNewItem.threshold = 1

        binding.etNewItem.setOnItemClickListener { parent, _, position, _ ->
            val label = parent.getItemAtPosition(position) as? String
                ?: return@setOnItemClickListener
            val suggestion = productSuggestions.firstOrNull { it.label == label }
                ?: return@setOnItemClickListener
            addItem(name = suggestion.name, inventoryItemId = suggestion.id)
        }

        binding.etNewItem.doAfterTextChanged { text ->
            val typed = text?.toString().orEmpty()
            val selected = productSuggestions.firstOrNull { it.id == selectedInventoryItemId }
            if (selected == null || !selected.name.equals(typed.trim(), ignoreCase = true)) {
                selectedInventoryItemId = null
            }
        }
    }

    private fun observeInventorySuggestions() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.inventoryItemDao().observeAll().collect { items ->
                    productSuggestions = items
                        .sortedBy { it.name.lowercase() }
                        .map { entity ->
                            val stock = formatQuantity(entity.quantity)
                            ProductSuggestion(
                                id = entity.id,
                                name = entity.name,
                                label = getString(
                                    R.string.shopping_suggestion_label,
                                    entity.name,
                                    stock
                                )
                            )
                        }
                    suggestionsAdapter?.let { adapter ->
                        adapter.clear()
                        adapter.addAll(productSuggestions.map { it.label })
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun observeDetail() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    db.shoppingListDao().observeById(listId),
                    db.shoppingListItemDao().observeByList(listId)
                ) { list, items ->
                    list to items
                }.collect { (list, items) ->
                    if (list == null) {
                        parentFragmentManager.popBackStack()
                        return@collect
                    }
                    binding.tvTitle.text = list.name
                    val ui = items.map {
                        ShoppingItemUi(
                            id = it.id,
                            name = it.name,
                            note = it.note,
                            isChecked = it.isChecked
                        )
                    }
                    itemsAdapter.submitList(ui)
                    binding.tvEmpty.isVisible = ui.isEmpty()
                    binding.rvItems.isVisible = ui.isNotEmpty()
                }
            }
        }
    }

    private fun addItem(
        name: String = binding.etNewItem.text?.toString()?.trim().orEmpty(),
        inventoryItemId: Long? = selectedInventoryItemId
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val db = KeeplyDatabase.getInstance(requireContext())
            val linkedId = inventoryItemId
                ?: productSuggestions
                    .firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
                    ?.id

            // Evitar duplicar el mismo producto ya presente en la lista
            val existing = db.shoppingListItemDao().getByList(listId)
            val alreadyInList = existing.any { item ->
                (linkedId != null && item.inventoryItemId == linkedId) ||
                        item.name.equals(trimmedName, ignoreCase = true)
            }
            if (alreadyInList) {
                PrettyToast.error(binding.root, R.string.shopping_item_already_added)
                selectedInventoryItemId = null
                binding.etNewItem.setText("")
                binding.etNewItem.dismissDropDown()
                return@launch
            }

            db.shoppingListItemDao().insert(
                ShoppingListItemEntity(
                    listId = listId,
                    name = trimmedName,
                    inventoryItemId = linkedId,
                    sortOrder = existing.size
                )
            )
            val list = db.shoppingListDao().getById(listId)
            if (list != null) {
                db.shoppingListDao().update(list.copy(updatedAt = System.currentTimeMillis()))
            }
            selectedInventoryItemId = null
            binding.etNewItem.setText("")
            binding.etNewItem.dismissDropDown()
        }
    }

    private fun toggleItem(itemId: Long, checked: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = KeeplyDatabase.getInstance(requireContext())
            val items = db.shoppingListItemDao().getByList(listId)
            val item = items.firstOrNull { it.id == itemId } ?: return@launch
            db.shoppingListItemDao().update(item.copy(isChecked = checked))
            val list = db.shoppingListDao().getById(listId)
            if (list != null) {
                db.shoppingListDao().update(list.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun deleteItem(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            KeeplyDatabase.getInstance(requireContext()).shoppingListItemDao().deleteById(itemId)
        }
    }

    private fun confirmDeleteList() {
        val name = binding.tvTitle.text?.toString().orEmpty()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.shopping_delete_list)
            .setMessage(getString(R.string.shopping_delete_list_message, name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.product_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    KeeplyDatabase.getInstance(requireContext()).shoppingListDao()
                        .deleteById(listId)
                    PrettyToast.success(binding.root, R.string.shopping_deleted)
                    parentFragmentManager.popBackStack()
                }
            }
            .show()
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }

    private data class ProductSuggestion(
        val id: Long,
        val name: String,
        val label: String
    )

    companion object {
        const val TAG = "ShoppingListDetailFragment"
        private const val ARG_LIST_ID = "arg_list_id"

        fun newInstance(listId: Long): ShoppingListDetailFragment {
            return ShoppingListDetailFragment().apply {
                arguments = Bundle().apply { putLong(ARG_LIST_ID, listId) }
            }
        }
    }
}
