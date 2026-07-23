package com.gabow95k.keeply.presentation.botiquin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.mapper.toDomain
import com.gabow95k.keeply.databinding.FragmentBotiquinBinding
import com.gabow95k.keeply.presentation.base.BaseFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BotiquinFragment : BaseFragment<FragmentBotiquinBinding>() {

    private lateinit var itemsAdapter: InventoryItemsAdapter
    private var swipeCallback: InventorySwipeCallback? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBotiquinBinding = FragmentBotiquinBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemsAdapter = InventoryItemsAdapter(
            onEditClick = { item -> openEditProductForm(item.id) },
            onDeleteClick = { item -> confirmDelete(item) }
        )
        binding.rvItems.adapter = itemsAdapter
        binding.fabAdd.setOnClickListener { openAddProductForm() }
        setupSwipe()
        observeItems()
    }

    private fun setupSwipe() {
        val revealWidth = resources.displayMetrics.density * 120f
        val callback = InventorySwipeCallback(
            revealWidthPx = revealWidth,
            getForeground = { holder ->
                (holder as InventoryItemsAdapter.ViewHolder).binding.cardForeground
            },
            onOpenChanged = { }
        )
        swipeCallback = callback
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvItems)
    }

    private fun observeItems() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    db.inventoryItemDao().observeAll(),
                    db.categoryDao().observeAll()
                ) { items, categories ->
                    val categoryNames = categories.associate { it.id to it.name }
                    items.map { entity ->
                        val item = entity.toDomain()
                        val quantityLabel = formatQuantity(item.quantity)
                        val meta = item.unit?.takeIf { it.isNotBlank() }
                            ?: item.formType?.takeIf { it.isNotBlank() }
                            ?: ""
                        InventoryItemUi(
                            id = item.id,
                            name = item.name,
                            categoryName = categoryNames[item.categoryId].orEmpty(),
                            stockLabel = getString(R.string.botiquin_stock_in_stock, quantityLabel),
                            barcode = item.barcode,
                            metaLabel = meta,
                            photoPath = item.photoPath
                        )
                    }
                }.collect { uiItems ->
                    itemsAdapter.submitList(uiItems)
                    binding.tvEmpty.isVisible = uiItems.isEmpty()
                    binding.rvItems.isVisible = uiItems.isNotEmpty()
                }
            }
        }
    }

    private fun openAddProductForm() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddInventoryItemFragment.newInstance())
            .addToBackStack(AddInventoryItemFragment.TAG)
            .commit()
    }

    private fun openEditProductForm(itemId: Long) {
        swipeCallback?.closeOpenItem(binding.rvItems)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddInventoryItemFragment.newInstance(itemId))
            .addToBackStack(AddInventoryItemFragment.TAG)
            .commit()
    }

    private fun confirmDelete(item: InventoryItemUi) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.product_delete_title)
            .setMessage(getString(R.string.product_delete_message, item.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.product_delete) { _, _ -> deleteItem(item.id) }
            .show()
    }

    private fun deleteItem(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            KeeplyDatabase.getInstance(requireContext()).inventoryItemDao().deleteById(itemId)
            swipeCallback?.closeOpenItem(binding.rvItems)
            Toast.makeText(requireContext(), R.string.product_deleted, Toast.LENGTH_SHORT).show()
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
        const val TAG = "BotiquinFragment"

        fun newInstance(): BotiquinFragment = BotiquinFragment()
    }
}
