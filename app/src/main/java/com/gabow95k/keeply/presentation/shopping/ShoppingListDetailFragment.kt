package com.gabow95k.keeply.presentation.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.ShoppingListItemEntity
import com.gabow95k.keeply.databinding.FragmentShoppingListDetailBinding
import com.gabow95k.keeply.presentation.base.BaseFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ShoppingListDetailFragment : BaseFragment<FragmentShoppingListDetailBinding>() {

    private var listId: Long = 0L
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
        binding.etNewItem.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItem()
                true
            } else {
                false
            }
        }
        observeDetail()
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

    private fun addItem() {
        val name = binding.etNewItem.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val db = KeeplyDatabase.getInstance(requireContext())
            val existing = db.shoppingListItemDao().getByList(listId)
            db.shoppingListItemDao().insert(
                ShoppingListItemEntity(
                    listId = listId,
                    name = name,
                    sortOrder = existing.size
                )
            )
            val list = db.shoppingListDao().getById(listId)
            if (list != null) {
                db.shoppingListDao().update(list.copy(updatedAt = System.currentTimeMillis()))
            }
            binding.etNewItem.text = null
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
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.shopping_delete_list)
            .setMessage(getString(R.string.shopping_delete_list_message, name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.product_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    KeeplyDatabase.getInstance(requireContext()).shoppingListDao()
                        .deleteById(listId)
                    Toast.makeText(requireContext(), R.string.shopping_deleted, Toast.LENGTH_SHORT)
                        .show()
                    parentFragmentManager.popBackStack()
                }
            }
            .show()
    }

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
