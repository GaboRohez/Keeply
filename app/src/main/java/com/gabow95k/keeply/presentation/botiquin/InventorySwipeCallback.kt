package com.gabow95k.keeply.presentation.botiquin

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * Left swipe: reveals edit/delete and can stay open.
 *
 * Right swipe cycles (drag right → return left → repeat): each cycle emits [onConsumeCycle].
 * The host accumulates ticks and commits when the user stops.
 */
class InventorySwipeCallback(
    private val revealWidthPx: Float,
    private val consumePeekWidthPx: Float,
    private val getForeground: (RecyclerView.ViewHolder) -> View,
    private val onOpenChanged: (openPosition: Int?) -> Unit,
    private val onConsumeCycle: (adapterPosition: Int) -> Unit
) : ItemTouchHelper.Callback() {

    private var openPosition: Int? = null
    private var currentDx = 0f
    private var activeHolderPosition: Int? = null
    private var gestureStartedClosed = true
    private var canArmConsumeTick = true
    private var cyclesThisGesture = 0

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

        val foreground = getForeground(viewHolder)
        val position = viewHolder.bindingAdapterPosition
        val startTranslation =
            if (openPosition == position) -revealWidthPx else 0f

        if (!isCurrentlyActive) return

        if (activeHolderPosition != position) {
            activeHolderPosition = position
            gestureStartedClosed = openPosition != position
            canArmConsumeTick = true
            cyclesThisGesture = 0
        }
        if (openPosition != null && openPosition != position) {
            closeOpenItem(recyclerView)
        }

        val translation = (startTranslation + dX).coerceIn(-revealWidthPx, consumePeekWidthPx)
        currentDx = translation
        foreground.translationX = translation

        if (!gestureStartedClosed || position == RecyclerView.NO_POSITION) return

        val tickThreshold = consumePeekWidthPx * TICK_RATIO
        val resetThreshold = consumePeekWidthPx * RESET_RATIO

        if (canArmConsumeTick && translation >= tickThreshold) {
            canArmConsumeTick = false
            cyclesThisGesture++
            onConsumeCycle(position)
            recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        } else if (!canArmConsumeTick && translation <= resetThreshold) {
            canArmConsumeTick = true
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val foreground = getForeground(viewHolder)
        val position = viewHolder.bindingAdapterPosition
        val dx = currentDx

        if (gestureStartedClosed &&
            position != RecyclerView.NO_POSITION &&
            cyclesThisGesture == 0 &&
            dx >= consumePeekWidthPx * TICK_RATIO
        ) {
            onConsumeCycle(position)
            recyclerView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        }

        val shouldOpenLeft = dx <= -revealWidthPx / 2f && cyclesThisGesture == 0 && dx < 0f
        val target = if (shouldOpenLeft) -revealWidthPx else 0f

        foreground.animate()
            .translationX(target)
            .setDuration(140)
            .start()

        openPosition = if (shouldOpenLeft) {
            position
        } else if (openPosition == position) {
            null
        } else {
            openPosition
        }
        onOpenChanged(openPosition)
        currentDx = target
        activeHolderPosition = null
        gestureStartedClosed = true
        canArmConsumeTick = true
        cyclesThisGesture = 0
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

    companion object {
        private const val TICK_RATIO = 0.42f
        private const val RESET_RATIO = 0.14f
    }
}
