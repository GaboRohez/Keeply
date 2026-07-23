package com.gabow95k.keeply.presentation.botiquin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.ItemInventoryBinding

class InventoryItemsAdapter(
    private val onEditClick: (InventoryItemUi) -> Unit,
    private val onDeleteClick: (InventoryItemUi) -> Unit
) : ListAdapter<InventoryItemUi, InventoryItemsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryBinding.inflate(
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
        val binding: ItemInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItemUi) = with(binding) {
            cardForeground.translationX = 0f
            tvName.text = item.name
            tvCategory.text = root.context.getString(
                R.string.botiquin_category_format,
                item.categoryName
            )
            tvStock.text = item.stockLabel
            tvMeta.text = item.metaLabel
            tvMeta.isVisible = item.metaLabel.isNotBlank()

            val hasBarcode = !item.barcode.isNullOrBlank()
            ivBarcode.isVisible = true
            tvBarcode.text = if (hasBarcode) {
                item.barcode
            } else {
                root.context.getString(R.string.botiquin_no_barcode)
            }

            Glide.with(ivThumb)
                .load(item.photoPath)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(ivThumb)

            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<InventoryItemUi>() {
            override fun areItemsTheSame(
                oldItem: InventoryItemUi,
                newItem: InventoryItemUi
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: InventoryItemUi,
                newItem: InventoryItemUi
            ): Boolean = oldItem == newItem
        }
    }
}
