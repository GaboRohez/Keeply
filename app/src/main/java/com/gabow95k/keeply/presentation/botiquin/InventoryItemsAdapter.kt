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

    private val pendingConsumeCounts = mutableMapOf<Long, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), pendingConsumeCounts[getItem(position).id] ?: 0)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_CONSUME_COUNT)) {
            val item = getItem(position)
            holder.bindConsumeCount(pendingConsumeCounts[item.id] ?: 0)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun setPendingConsumeCount(itemId: Long, count: Int) {
        if (count <= 0) {
            pendingConsumeCounts.remove(itemId)
        } else {
            pendingConsumeCounts[itemId] = count
        }
        val index = currentList.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            notifyItemChanged(index, PAYLOAD_CONSUME_COUNT)
        }
    }

    fun clearPendingConsumeCount(itemId: Long) {
        if (pendingConsumeCounts.remove(itemId) != null) {
            val index = currentList.indexOfFirst { it.id == itemId }
            if (index >= 0) {
                notifyItemChanged(index, PAYLOAD_CONSUME_COUNT)
            }
        }
    }

    inner class ViewHolder(
        val binding: ItemInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItemUi, consumeCount: Int) = with(binding) {
            cardForeground.translationX = 0f
            tvName.text = item.name
            tvCategory.text = root.context.getString(
                R.string.botiquin_category_format,
                item.categoryName
            )
            tvStock.text = item.stockLabel
            bindConsumeCount(consumeCount)

            val hasBarcode = !item.barcode.isNullOrBlank()
            ivBarcode.isVisible = true
            tvBarcode.text = if (hasBarcode) {
                item.barcode
            } else {
                root.context.getString(R.string.botiquin_no_barcode)
            }

            Glide.with(ivThumb)
                .load(item.photoPath)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .centerCrop()
                .into(ivThumb)

            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        fun bindConsumeCount(count: Int) = with(binding) {
            val shown = count.coerceAtLeast(1)
            tvConsumeBadge.text = root.context.getString(
                R.string.inventory_consume_badge_format,
                shown
            )
        }
    }

    private companion object {
        const val PAYLOAD_CONSUME_COUNT = "consume_count"

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
