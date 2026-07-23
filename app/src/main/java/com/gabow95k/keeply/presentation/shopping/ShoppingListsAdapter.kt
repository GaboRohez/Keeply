package com.gabow95k.keeply.presentation.shopping

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gabow95k.keeply.databinding.ItemShoppingListBinding

class ShoppingListsAdapter(
    private val onClick: (ShoppingListUi) -> Unit,
    private val onEditClick: (ShoppingListUi) -> Unit,
    private val onDeleteClick: (ShoppingListUi) -> Unit
) : ListAdapter<ShoppingListUi, ShoppingListsAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        val binding: ItemShoppingListBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShoppingListUi) = with(binding) {
            cardForeground.translationX = 0f
            tvListName.text = item.name
            tvListMeta.text = item.meta
            cardForeground.setOnClickListener { onClick(item) }
            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    private companion object {
        val Diff = object : DiffUtil.ItemCallback<ShoppingListUi>() {
            override fun areItemsTheSame(oldItem: ShoppingListUi, newItem: ShoppingListUi) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ShoppingListUi, newItem: ShoppingListUi) =
                oldItem == newItem
        }
    }
}
