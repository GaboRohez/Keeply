package com.gabow95k.keeply.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.entity.InventoryItemEntity
import com.gabow95k.keeply.data.local.entity.StockChangeEventEntity
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.databinding.FragmentHomeBinding
import com.gabow95k.keeply.databinding.ItemHomeAlertCardBinding
import com.gabow95k.keeply.databinding.ItemHomeInsightBinding
import com.gabow95k.keeply.databinding.ItemHomeStatBinding
import com.gabow95k.keeply.databinding.ItemHomeUsageBarBinding
import com.gabow95k.keeply.insights.InsightCard
import com.gabow95k.keeply.insights.InsightKind
import com.gabow95k.keeply.insights.MonthlyInsights
import com.gabow95k.keeply.insights.MonthlyInsightsEvaluator
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.presentation.controller.ControllerActivity
import com.gabow95k.keeply.prompts.SoftPrompt
import com.gabow95k.keeply.prompts.SoftPromptEvaluator
import com.gabow95k.keeply.prompts.SoftPromptType
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private var expiredProductNames: List<String> = emptyList()
    private var expiringProductLines: List<String> = emptyList()
    private var lowStockProductLines: List<String> = emptyList()
    private var outOfStockProductNames: List<String> = emptyList()
    private var insightsExpanded = false

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvProfileHint.setOnClickListener {
            (activity as? ControllerActivity)?.navigateToTab(R.id.nav_settings)
        }
        binding.monthlyInsights.btnInsightsMore.setOnClickListener {
            insightsExpanded = !insightsExpanded
            applyInsightsExpandedUi()
        }

        binding.cardExpiredAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_expired_title,
                emptyMessageRes = R.string.home_alert_empty_expired,
                lines = expiredProductNames
            )
        }
        binding.cardExpiringAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_expiring_title,
                emptyMessageRes = R.string.home_alert_empty_expiring,
                lines = expiringProductLines
            )
        }
        binding.cardLowStockAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_low_stock_title,
                emptyMessageRes = R.string.home_alert_empty_low_stock,
                lines = lowStockProductLines
            )
        }
        binding.cardOutOfStockAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_out_of_stock_title,
                emptyMessageRes = R.string.home_alert_empty_out_of_stock,
                lines = outOfStockProductNames
            )
        }

        applyInsightsExpandedUi()
        observeHomeData()
    }

    private fun observeHomeData() {
        val db = KeeplyDatabase.getInstance(requireContext())
        val now = System.currentTimeMillis()
        val monthStart = MonthlyInsightsEvaluator.monthStartMillis(now)
        val expiringDays = KeeplyPreferences.getInstance(requireContext())
            .expiringSoonDays
            .toLong()
        val expiringLimit = now + TimeUnit.DAYS.toMillis(expiringDays)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    combine(
                        db.userProfileDao().observeProfile(),
                        db.inventoryItemDao().observeAll(),
                        db.stockChangeEventDao().observeSince(monthStart)
                    ) { profile, allItems, monthEvents ->
                        HomeRaw(
                            profileName = profile?.name?.takeIf { it.isNotBlank() },
                            allItems = allItems,
                            monthEvents = monthEvents
                        )
                    },
                    db.stockChangeEventDao().observeLatest()
                ) { raw, latestEvents ->
                    val latestEvent = latestEvents.firstOrNull()
                    val expiredItems = raw.allItems.filter { entity ->
                        val date = entity.expirationDate ?: return@filter false
                        date < now
                    }
                    val expiringItems = raw.allItems.filter { entity ->
                        val date = entity.expirationDate ?: return@filter false
                        date in now..expiringLimit
                    }
                    val lowStockItems = raw.allItems.filter { entity ->
                        val min = entity.minQuantity ?: return@filter false
                        entity.quantity > 0.0 && entity.quantity <= min
                    }
                    val outOfStockItems = raw.allItems.filter { it.quantity <= 0.0 }
                    val insights = MonthlyInsightsEvaluator.evaluate(raw.monthEvents, raw.allItems)
                    val nextExpiry = raw.allItems
                        .mapNotNull { item ->
                            val date = item.expirationDate ?: return@mapNotNull null
                            if (date < startOfDay(now)) null else item to date
                        }
                        .minByOrNull { it.second }
                        ?.first

                    HomeUiState(
                        userName = raw.profileName,
                        totalCount = raw.allItems.size,
                        expiredCount = expiredItems.size,
                        expiringCount = expiringItems.size,
                        lowStockCount = lowStockItems.size,
                        outOfStockCount = outOfStockItems.size,
                        expiredNames = expiredItems.map { it.name },
                        expiringLines = expiringItems.map { it.name },
                        lowStockLines = lowStockItems.map { entity ->
                            getString(
                                R.string.home_alert_item_low_stock,
                                entity.name,
                                formatQuantity(entity.quantity)
                            )
                        },
                        outOfStockNames = outOfStockItems.map { it.name },
                        nextExpiry = nextExpiry?.let { buildNextExpiry(it, now) },
                        lastActivity = latestEvent?.let { buildLastActivity(it, now) },
                        insights = insights
                    )
                }.collect { state ->
                    bindState(state)
                }
            }
        }
    }

    private fun bindState(state: HomeUiState) {
        binding.tvGreeting.text = if (state.userName != null) {
            getString(R.string.home_greeting, state.userName)
        } else {
            getString(R.string.home_greeting_default)
        }
        binding.tvProfileHint.isVisible = state.userName == null

        bindStat(
            binding.statTotal,
            state.totalCount.toString(),
            getString(R.string.home_stat_total)
        )
        bindStat(
            binding.statExpiring,
            state.expiringCount.toString(),
            getString(R.string.home_stat_expiring)
        )
        bindStat(
            binding.statLowStock,
            state.lowStockCount.toString(),
            getString(R.string.home_stat_low_stock)
        )

        expiredProductNames = state.expiredNames.map { name ->
            getString(R.string.home_alert_item_expired, name)
        }
        expiringProductLines = state.expiringLines.map { name ->
            getString(R.string.home_alert_item_expiring, name)
        }
        lowStockProductLines = state.lowStockLines
        outOfStockProductNames = state.outOfStockNames.map { name ->
            getString(R.string.home_alert_item_out_of_stock, name)
        }

        bindAlertCard(
            binding.cardExpiredAlert,
            title = getString(R.string.home_alert_expired_title),
            count = state.expiredCount,
            tone = AlertTone.EXPIRED
        )
        bindAlertCard(
            binding.cardExpiringAlert,
            title = getString(R.string.home_alert_expiring_title),
            count = state.expiringCount,
            tone = AlertTone.EXPIRING
        )
        bindAlertCard(
            binding.cardLowStockAlert,
            title = getString(R.string.home_alert_low_stock_title),
            count = state.lowStockCount,
            tone = AlertTone.LOW_STOCK
        )
        bindAlertCard(
            binding.cardOutOfStockAlert,
            title = getString(R.string.home_alert_out_of_stock_title),
            count = state.outOfStockCount,
            tone = AlertTone.OUT_OF_STOCK
        )

        bindNextExpiry(state.nextExpiry)
        bindLastActivity(state.lastActivity)
        bindInsights(state.insights)
        bindSoftPrompt(state)
    }

    private fun bindNextExpiry(spotlight: SpotlightUi?) {
        val card = binding.cardNextExpiry
        card.tvSpotlightEyebrow.text = getString(R.string.home_next_expiry_eyebrow)
        if (spotlight == null) {
            applySpotlightSurface(card.root, urgent = false)
            card.tvSpotlightEyebrow.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.keeply_primary)
            )
            card.tvSpotlightTitle.text = getString(R.string.home_next_expiry_empty_title)
            card.tvSpotlightSubtitle.text = getString(R.string.home_next_expiry_empty_body)
            card.tvSpotlightBadge.isVisible = false
            return
        }
        applySpotlightSurface(card.root, urgent = spotlight.daysLeft <= 3)
        card.tvSpotlightTitle.text = spotlight.title
        card.tvSpotlightSubtitle.text = spotlight.subtitle
        card.tvSpotlightBadge.isVisible = true
        card.tvSpotlightBadge.text = spotlight.badge
        card.tvSpotlightEyebrow.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (spotlight.daysLeft <= 3) R.color.keeply_on_warning_container
                else R.color.keeply_primary
            )
        )
    }

    private fun bindLastActivity(spotlight: SpotlightUi?) {
        val card = binding.cardLastActivity
        applySpotlightSurface(card.root, urgent = false)
        card.tvSpotlightEyebrow.text = getString(R.string.home_last_activity_eyebrow)
        card.tvSpotlightEyebrow.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.keeply_primary)
        )
        if (spotlight == null) {
            card.tvSpotlightTitle.text = getString(R.string.home_last_activity_empty_title)
            card.tvSpotlightSubtitle.text = getString(R.string.home_last_activity_empty_body)
            card.tvSpotlightBadge.isVisible = false
            return
        }
        card.tvSpotlightTitle.text = spotlight.title
        card.tvSpotlightSubtitle.text = spotlight.subtitle
        card.tvSpotlightBadge.isVisible = true
        card.tvSpotlightBadge.text = spotlight.badge
    }

    private fun applySpotlightSurface(card: MaterialCardView, urgent: Boolean) {
        val colorRes = if (urgent) {
            R.color.keeply_warning_container
        } else {
            R.color.keeply_surface
        }
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun buildNextExpiry(item: InventoryItemEntity, now: Long): SpotlightUi {
        val expiration = item.expirationDate ?: return SpotlightUi(
            title = item.name,
            subtitle = "",
            badge = "",
            daysLeft = 0
        )
        val daysLeft = daysUntil(now, expiration).coerceAtLeast(0)
        val subtitle = when (daysLeft) {
            0 -> getString(R.string.home_next_expiry_today)
            1 -> getString(R.string.home_next_expiry_tomorrow)
            else -> resources.getQuantityString(
                R.plurals.home_next_expiry_in_days,
                daysLeft,
                daysLeft
            )
        }
        val badge = when (daysLeft) {
            0 -> getString(R.string.home_next_expiry_badge_today)
            else -> getString(R.string.home_next_expiry_badge_days, daysLeft)
        }
        return SpotlightUi(
            title = item.name,
            subtitle = subtitle,
            badge = badge,
            daysLeft = daysLeft
        )
    }

    private fun buildLastActivity(event: StockChangeEventEntity, now: Long): SpotlightUi {
        val amount = formatQuantity(event.delta)
        val subtitle = when (event.changeType) {
            StockChangeEventEntity.TYPE_CONSUME -> getString(
                R.string.home_last_activity_consume,
                amount
            )

            StockChangeEventEntity.TYPE_ADJUST_DOWN -> getString(
                R.string.home_last_activity_adjust_down
            )

            StockChangeEventEntity.TYPE_ADJUST_UP -> getString(
                R.string.home_last_activity_adjust_up
            )

            StockChangeEventEntity.TYPE_ADD -> getString(R.string.home_last_activity_add)
            else -> ""
        }
        val daysAgo = daysUntil(event.createdAt, now).coerceAtLeast(0)
        val badge = when (daysAgo) {
            0 -> getString(R.string.home_last_activity_time_today)
            1 -> getString(R.string.home_last_activity_time_yesterday)
            else -> getString(R.string.home_last_activity_time_days, daysAgo)
        }
        return SpotlightUi(
            title = event.productName,
            subtitle = subtitle,
            badge = badge,
            daysLeft = Int.MAX_VALUE
        )
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun daysUntil(fromMillis: Long, toMillis: Long): Int {
        val from = startOfDay(fromMillis)
        val to = startOfDay(toMillis)
        return ceil((to - from).toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()
    }

    private fun bindInsights(insights: MonthlyInsights) {
        val card = binding.monthlyInsights
        val container = card.insightsContainer
        container.removeAllViews()

        insights.cards.forEach { insight ->
            val row = ItemHomeInsightBinding.inflate(
                layoutInflater,
                container,
                false
            )
            val (title, body) = insightCopy(insight)
            row.tvInsightTitle.text = title
            row.tvInsightBody.text = body
            container.addView(row.root)
        }

        val stats = insights.stats
        card.tvStatConsumed.text = formatQuantity(stats.totalConsumed)
        card.tvStatMovements.text = stats.movementCount.toString()
        card.tvStatProducts.text = stats.productsTouched.toString()

        val barsContainer = card.usageBarsContainer
        barsContainer.removeAllViews()
        val hasTop = stats.topProducts.isNotEmpty()
        card.tvTopUsedEmpty.isVisible = !hasTop
        barsContainer.isVisible = hasTop
        stats.topProducts.forEach { bar ->
            val row = ItemHomeUsageBarBinding.inflate(layoutInflater, barsContainer, false)
            row.tvBarLabel.text = bar.productName
            row.tvBarValue.text = formatQuantity(bar.amount)
            row.progressBar.progress = bar.progressPercent
            barsContainer.addView(row.root)
        }

        card.segmentActivity.setShares(
            consume = stats.consumeShare,
            adjust = stats.adjustShare,
            add = stats.addShare
        )

        card.btnInsightsShop.isVisible = insights.showShoppingCta
        card.btnInsightsShop.setOnClickListener {
            (activity as? ControllerActivity)?.navigateToShoppingAutoGenerate()
        }
    }

    private fun applyInsightsExpandedUi() {
        val card = binding.monthlyInsights
        card.insightsExpanded.isVisible = insightsExpanded
        card.btnInsightsMore.text = getString(
            if (insightsExpanded) R.string.home_insights_see_less
            else R.string.home_insights_see_more
        )
        card.btnInsightsMore.setIconResource(
            if (insightsExpanded) R.drawable.ic_expand_less
            else R.drawable.ic_expand_more
        )
    }

    private fun insightCopy(insight: InsightCard): Pair<String, String> {
        val name = insight.productName.orEmpty()
        return when (insight.kind) {
            InsightKind.MOST_USED -> getString(R.string.home_insights_most_used_title, name) to
                    getString(
                        R.string.home_insights_most_used_body,
                        formatQuantity(insight.amount ?: 0.0)
                    )

            InsightKind.RAN_OUT -> getString(R.string.home_insights_ran_out_title, name) to
                    getString(R.string.home_insights_ran_out_body)

            InsightKind.BUY_MORE -> getString(R.string.home_insights_buy_more_title, name) to
                    getString(R.string.home_insights_buy_more_body)

            InsightKind.EMPTY_TRACKING -> getString(R.string.home_insights_empty_title) to
                    getString(R.string.home_insights_empty_body)
        }
    }

    private fun bindSoftPrompt(state: HomeUiState) {
        val prefs = KeeplyPreferences.getInstance(requireContext())
        val prompt = SoftPromptEvaluator.evaluate(
            prefs = prefs,
            lowStockCount = state.lowStockCount,
            outOfStockCount = state.outOfStockCount
        ) { type, count ->
            when (type) {
                SoftPromptType.END_OF_MONTH_SHOPPING -> SoftPrompt(
                    type = type,
                    title = getString(R.string.prompt_end_month_title),
                    body = getString(R.string.prompt_end_month_body),
                    primaryLabel = getString(R.string.prompt_end_month_action)
                )

                SoftPromptType.LOW_STOCK_SHOPPING -> SoftPrompt(
                    type = type,
                    title = getString(R.string.prompt_low_stock_title),
                    body = resources.getQuantityString(
                        R.plurals.prompt_low_stock_body,
                        count,
                        count
                    ),
                    primaryLabel = getString(R.string.prompt_low_stock_action)
                )

                SoftPromptType.FEATURE_TIP -> SoftPrompt(
                    type = type,
                    title = getString(R.string.prompt_tip_title),
                    body = resources.getStringArray(R.array.feature_tips)[count],
                    primaryLabel = getString(R.string.prompt_tip_action)
                )
            }
        }

        val card = binding.softPrompt
        if (prompt == null) {
            card.root.isVisible = false
            return
        }

        card.root.isVisible = true
        card.tvPromptTitle.text = prompt.title
        card.tvPromptBody.text = prompt.body
        card.btnPromptAction.text = prompt.primaryLabel
        card.btnPromptDismiss.setOnClickListener {
            SoftPromptEvaluator.markShown(prefs, prompt.type)
            card.root.isVisible = false
        }
        card.btnPromptAction.setOnClickListener {
            SoftPromptEvaluator.markShown(prefs, prompt.type)
            card.root.isVisible = false
            when (prompt.type) {
                SoftPromptType.END_OF_MONTH_SHOPPING,
                SoftPromptType.LOW_STOCK_SHOPPING -> {
                    (activity as? ControllerActivity)?.navigateToShoppingAutoGenerate()
                }

                SoftPromptType.FEATURE_TIP -> Unit
            }
        }
    }

    private fun bindAlertCard(
        cardBinding: ItemHomeAlertCardBinding,
        title: String,
        count: Int,
        tone: AlertTone
    ) {
        val active = count > 0
        val backgroundRes = if (active) tone.containerColor else R.color.keeply_surface
        val titleColorRes = if (active) tone.onContainerColor else R.color.keeply_text_primary
        val countColorRes = if (active) tone.onContainerColor else R.color.keeply_text_secondary
        val badgeBgRes = if (active) tone.badgeColor else R.color.keeply_surface_variant

        cardBinding.root.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), backgroundRes)
        )
        cardBinding.tvAlertTitle.text = title
        cardBinding.tvAlertTitle.setTextColor(
            ContextCompat.getColor(requireContext(), titleColorRes)
        )
        cardBinding.tvAlertCount.text = resources.getQuantityString(
            R.plurals.home_alert_count,
            count,
            count
        )
        cardBinding.tvAlertCount.setTextColor(
            ContextCompat.getColor(requireContext(), countColorRes)
        )
        cardBinding.tvAlertBadge.text = count.toString()
        cardBinding.tvAlertBadge.setTextColor(
            ContextCompat.getColor(requireContext(), titleColorRes)
        )
        val badgeBg =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_item_thumb)?.mutate()
        badgeBg?.setTint(ContextCompat.getColor(requireContext(), badgeBgRes))
        cardBinding.tvAlertBadge.background = badgeBg
    }

    private enum class AlertTone(
        val containerColor: Int,
        val onContainerColor: Int,
        val badgeColor: Int
    ) {
        EXPIRED(
            containerColor = R.color.keeply_error_container,
            onContainerColor = R.color.keeply_on_error_container,
            badgeColor = R.color.white
        ),
        EXPIRING(
            containerColor = R.color.keeply_warning_container,
            onContainerColor = R.color.keeply_on_warning_container,
            badgeColor = R.color.white
        ),
        LOW_STOCK(
            containerColor = R.color.keeply_primary_container,
            onContainerColor = R.color.keeply_on_primary_container,
            badgeColor = R.color.white
        ),
        OUT_OF_STOCK(
            containerColor = R.color.keeply_error_container,
            onContainerColor = R.color.keeply_on_error_container,
            badgeColor = R.color.white
        )
    }

    private fun showProductsAlert(
        titleRes: Int,
        emptyMessageRes: Int,
        lines: List<String>
    ) {
        val message = if (lines.isEmpty()) {
            getString(emptyMessageRes)
        } else {
            lines.joinToString(separator = "\n")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setMessage(message)
            .setPositiveButton(R.string.home_alert_accept, null)
            .show()
    }

    private fun bindStat(statBinding: ItemHomeStatBinding, value: String, label: String) {
        statBinding.tvStatValue.text = value
        statBinding.tvStatLabel.text = label
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }

    private data class HomeRaw(
        val profileName: String?,
        val allItems: List<InventoryItemEntity>,
        val monthEvents: List<StockChangeEventEntity>
    )

    private data class SpotlightUi(
        val title: String,
        val subtitle: String,
        val badge: String,
        val daysLeft: Int
    )

    private data class HomeUiState(
        val userName: String?,
        val totalCount: Int,
        val expiredCount: Int,
        val expiringCount: Int,
        val lowStockCount: Int,
        val outOfStockCount: Int,
        val expiredNames: List<String>,
        val expiringLines: List<String>,
        val lowStockLines: List<String>,
        val outOfStockNames: List<String>,
        val nextExpiry: SpotlightUi?,
        val lastActivity: SpotlightUi?,
        val insights: MonthlyInsights
    )

    companion object {
        const val TAG = "HomeFragment"
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
