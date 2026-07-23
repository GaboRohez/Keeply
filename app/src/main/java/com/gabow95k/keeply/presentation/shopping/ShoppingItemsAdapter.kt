package com.gabow95k.keeply.presentation.shopping

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gabow95k.keeply.databinding.ItemShoppingListItemBinding

class ShoppingItemsAdapter(
    private val onCheckedChange: (ShoppingItemUi, Boolean) -> Unit,
    private val onDelete: (ShoppingItemUi) -> Unit
) : ListAdapter<ShoppingItemUi, ShoppingItemsAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingListItemBinding.inflate(
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
        private val binding: ItemShoppingListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShoppingItemUi) = with(binding) {
            checkItem.setOnCheckedChangeListener(null)
            checkItem.isChecked = item.isChecked
            tvItemName.text = item.name
            tvItemName.paintFlags = if (item.isChecked) {
                tvItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            tvItemNote.isVisible = !item.note.isNullOrBlank()
            tvItemNote.text = item.note.orEmpty()
            checkItem.setOnCheckedChangeListener { _, checked ->
                onCheckedChange(item, checked)
            }
            btnDeleteItem.setOnClickListener { onDelete(item) }
        }
    }

    private companion object {
        val Diff = object : DiffUtil.ItemCallback<ShoppingItemUi>() {
            override fun areItemsTheSame(oldItem: ShoppingItemUi, newItem: ShoppingItemUi) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ShoppingItemUi, newItem: ShoppingItemUi) =
                oldItem == newItem
        }
    }
}
