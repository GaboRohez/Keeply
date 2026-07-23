package com.gabow95k.keeply.presentation.shopping

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Swipe left to reveal edit/delete; swipe right to close again.
 */
class ShoppingListSwipeCallback(
    private val revealWidthPx: Float,
    private val getForeground: (RecyclerView.ViewHolder) -> View,
    private val onOpenChanged: (openPosition: Int?) -> Unit = {}
) : ItemTouchHelper.Callback() {

    private var openPosition: Int? = null
    private var currentDx = 0f

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int = makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 2f

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 10f

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) return
        if (!isCurrentlyActive) return

        val foreground = getForeground(viewHolder)
        val position = viewHolder.bindingAdapterPosition
        val startTranslation =
            if (openPosition == position) -revealWidthPx else 0f

        if (openPosition != null && openPosition != position) {
            closeOpenItem(recyclerView)
        }

        // Solo permitir abrir a la izquierda; a la derecha solo para cerrar.
        val translation = (startTranslation + dX).coerceIn(-revealWidthPx, 0f)
        currentDx = translation
        foreground.translationX = translation
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val foreground = getForeground(viewHolder)
        val position = viewHolder.bindingAdapterPosition
        val shouldOpen = currentDx <= -revealWidthPx / 2f
        val target = if (shouldOpen) -revealWidthPx else 0f

        foreground.animate()
            .translationX(target)
            .setDuration(160)
            .start()

        openPosition = if (shouldOpen) {
            position
        } else if (openPosition == position) {
            null
        } else {
            openPosition
        }
        onOpenChanged(openPosition)
        currentDx = target
    }

    fun closeOpenItem(recyclerView: RecyclerView) {
        val position = openPosition ?: return
        val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: run {
            openPosition = null
            onOpenChanged(null)
            return
        }
        getForeground(holder).animate()
            .translationX(0f)
            .setDuration(160)
            .start()
        openPosition = null
        onOpenChanged(null)
    }
}
