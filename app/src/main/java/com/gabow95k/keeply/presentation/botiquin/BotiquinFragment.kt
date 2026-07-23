package com.gabow95k.keeply.presentation.botiquin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.mapper.toDomain
import com.gabow95k.keeply.databinding.FragmentBotiquinBinding
import com.gabow95k.keeply.domain.model.Category
import com.gabow95k.keeply.presentation.base.BaseFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BotiquinFragment : BaseFragment<FragmentBotiquinBinding>() {

    private lateinit var itemsAdapter: InventoryItemsAdapter
    private var swipeCallback: InventorySwipeCallback? = null
    private var allItems: List<InventoryItemUi> = emptyList()
    private var categories: List<Category> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategoryId: Long? = null

    private var consumeSessionItemId: Long? = null
    private var consumeSessionCount: Int = 0
    private val commitConsumeRunnable = Runnable { commitConsumeSession() }

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
        binding.btnFilter.setOnClickListener { showCategoryFilterDialog() }
        binding.tvFilterChip.setOnClickListener {
            selectedCategoryId = null
            applyFilters()
        }
        binding.etSearch.doAfterTextChanged { text ->
            searchQuery = text?.toString().orEmpty()
            applyFilters()
        }
        setupSwipe()
        observeItems()
    }

    override fun onDestroyView() {
        binding.rvItems.removeCallbacks(commitConsumeRunnable)
        commitConsumeSession()
        super.onDestroyView()
    }

    private fun setupSwipe() {
        val density = resources.displayMetrics.density
        val revealWidth = density * 120f
        val consumePeek = density * 88f
        val callback = InventorySwipeCallback(
            revealWidthPx = revealWidth,
            consumePeekWidthPx = consumePeek,
            getForeground = { holder ->
                (holder as InventoryItemsAdapter.ViewHolder).binding.cardForeground
            },
            onOpenChanged = { },
            onConsumeCycle = { position ->
                val item =
                    itemsAdapter.currentList.getOrNull(position) ?: return@InventorySwipeCallback
                registerConsumeCycle(item)
            }
        )
        swipeCallback = callback
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvItems)
    }

    private fun registerConsumeCycle(item: InventoryItemUi) {
        if (consumeSessionItemId != null && consumeSessionItemId != item.id) {
            commitConsumeSession()
        }

        val maxAvailable = when {
            item.quantity <= 0.0 -> 0
            item.quantity < 1.0 -> 1
            else -> kotlin.math.floor(item.quantity).toInt()
        }

        if (maxAvailable <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.inventory_consume_empty, item.name),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (consumeSessionCount >= maxAvailable) {
            Toast.makeText(
                requireContext(),
                getString(R.string.inventory_consume_limit, maxAvailable, item.name),
                Toast.LENGTH_SHORT
            ).show()
            scheduleConsumeCommit()
            return
        }

        consumeSessionItemId = item.id
        consumeSessionCount += 1
        itemsAdapter.setPendingConsumeCount(item.id, consumeSessionCount)
        scheduleConsumeCommit()
    }

    private fun scheduleConsumeCommit() {
        binding.rvItems.removeCallbacks(commitConsumeRunnable)
        binding.rvItems.postDelayed(commitConsumeRunnable, CONSUME_COMMIT_DELAY_MS)
    }

    private fun commitConsumeSession() {
        val itemId = consumeSessionItemId ?: return
        val count = consumeSessionCount
        consumeSessionItemId = null
        consumeSessionCount = 0
        binding.rvItems.removeCallbacks(commitConsumeRunnable)
        itemsAdapter.clearPendingConsumeCount(itemId)

        if (count <= 0) return

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = KeeplyDatabase.getInstance(requireContext()).inventoryItemDao()
            val entity = dao.getById(itemId) ?: return@launch
            if (entity.quantity <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.inventory_consume_empty, entity.name),
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val newQuantity = (entity.quantity - count.toDouble()).coerceAtLeast(0.0)
            dao.update(
                entity.copy(
                    quantity = newQuantity,
                    updatedAt = System.currentTimeMillis()
                )
            )
            Toast.makeText(
                requireContext(),
                getString(
                    R.string.inventory_consume_done,
                    count,
                    getString(R.string.botiquin_stock_in_stock, formatQuantity(newQuantity))
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeItems() {
        val db = KeeplyDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    db.inventoryItemDao().observeAll(),
                    db.categoryDao().observeAll()
                ) { items, categoryEntities ->
                    categories = categoryEntities.map { it.toDomain() }
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
                            categoryId = item.categoryId,
                            categoryName = categoryNames[item.categoryId].orEmpty(),
                            quantity = item.quantity,
                            stockLabel = getString(R.string.botiquin_stock_in_stock, quantityLabel),
                            barcode = item.barcode,
                            metaLabel = meta,
                            photoPath = item.photoPath
                        )
                    }
                }.collect { uiItems ->
                    allItems = uiItems
                    applyFilters()
                }
            }
        }
    }

    private fun applyFilters() {
        val query = searchQuery.trim()
        val filtered = allItems.filter { item ->
            val matchesCategory =
                selectedCategoryId == null || item.categoryId == selectedCategoryId
            val matchesQuery = query.isEmpty() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true) ||
                    item.barcode.orEmpty().contains(query, ignoreCase = true) ||
                    item.metaLabel.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        itemsAdapter.submitList(filtered)

        val hasFilters = query.isNotEmpty() || selectedCategoryId != null
        binding.tvEmpty.isVisible = filtered.isEmpty()
        binding.rvItems.isVisible = filtered.isNotEmpty()
        binding.tvEmpty.setText(
            if (hasFilters && allItems.isNotEmpty()) {
                R.string.inventory_empty_filtered
            } else {
                R.string.inventory_empty
            }
        )
        updateFilterUi()
    }

    private fun updateFilterUi() {
        val categoryId = selectedCategoryId
        val categoryName = categories.firstOrNull { it.id == categoryId }?.name
        binding.tvFilterChip.isVisible = categoryName != null
        if (categoryName != null) {
            binding.tvFilterChip.text = getString(R.string.inventory_filter_active, categoryName)
        }

        val tint = ContextCompat.getColor(
            requireContext(),
            if (categoryId != null) R.color.keeply_primary else R.color.keeply_icon_primary
        )
        binding.ivFilter.setColorFilter(tint)
    }

    private fun showCategoryFilterDialog() {
        val labels = mutableListOf(getString(R.string.inventory_filter_all))
        labels.addAll(categories.map { it.name })

        val checkedIndex = selectedCategoryId
            ?.let { id -> categories.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?.plus(1)
            ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.inventory_filter_title)
            .setSingleChoiceItems(labels.toTypedArray(), checkedIndex) { dialog, which ->
                selectedCategoryId = if (which == 0) {
                    null
                } else {
                    categories.getOrNull(which - 1)?.id
                }
                applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        private const val CONSUME_COMMIT_DELAY_MS = 900L

        fun newInstance(): BotiquinFragment = BotiquinFragment()
    }
}
