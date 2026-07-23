package com.gabow95k.keeply.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gabow95k.keeply.R
import com.gabow95k.keeply.databinding.ItemHomeRecentBinding
import com.gabow95k.keeply.presentation.botiquin.InventoryItemUi

class HomeRecentAdapter :
    ListAdapter<InventoryItemUi, HomeRecentAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeRecentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemHomeRecentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItemUi) = with(binding) {
            tvName.text = item.name
            tvCategory.text = root.context.getString(
                R.string.botiquin_category_format,
                item.categoryName
            )
            tvStock.text = item.stockLabel
            tvMeta.text = item.metaLabel
            tvMeta.isVisible = item.metaLabel.isNotBlank()
            tvBarcode.text = item.barcode?.takeIf { it.isNotBlank() }
                ?: root.context.getString(R.string.botiquin_no_barcode)

            Glide.with(ivThumb)
                .load(item.photoPath)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(ivThumb)
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
