package com.gabow95k.keeply.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gabow95k.keeply.R
import com.gabow95k.keeply.data.local.db.KeeplyDatabase
import com.gabow95k.keeply.data.local.mapper.toDomain
import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import com.gabow95k.keeply.databinding.FragmentHomeBinding
import com.gabow95k.keeply.databinding.ItemHomeAlertCardBinding
import com.gabow95k.keeply.databinding.ItemHomeInsightBinding
import com.gabow95k.keeply.databinding.ItemHomeStatBinding
import com.gabow95k.keeply.insights.InsightCard
import com.gabow95k.keeply.insights.InsightKind
import com.gabow95k.keeply.insights.MonthlyInsights
import com.gabow95k.keeply.insights.MonthlyInsightsEvaluator
import com.gabow95k.keeply.presentation.base.BaseFragment
import com.gabow95k.keeply.presentation.botiquin.AddInventoryItemFragment
import com.gabow95k.keeply.presentation.botiquin.InventoryItemUi
import com.gabow95k.keeply.presentation.controller.ControllerActivity
import com.gabow95k.keeply.prompts.SoftPrompt
import com.gabow95k.keeply.prompts.SoftPromptEvaluator
import com.gabow95k.keeply.prompts.SoftPromptType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val recentAdapter = HomeRecentAdapter()
    private var allRecentItems: List<InventoryItemUi> = emptyList()
    private var expiredProductNames: List<String> = emptyList()
    private var lowStockProductLines: List<String> = emptyList()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter

        binding.tvViewAll.setOnClickListener {
            (activity as? ControllerActivity)?.navigateToTab(R.id.nav_botiquin)
        }
        binding.btnAddProduct.setOnClickListener { openAddProduct() }
        binding.tvProfileHint.setOnClickListener {
            (activity as? ControllerActivity)?.navigateToTab(R.id.nav_settings)
        }
        binding.etSearch.doAfterTextChanged { query ->
            filterRecent(query?.toString().orEmpty())
        }

        binding.cardExpiredAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_expired_title,
                emptyMessageRes = R.string.home_alert_empty_expired,
                lines = expiredProductNames
            )
        }
        binding.cardLowStockAlert.root.setOnClickListener {
            showProductsAlert(
                titleRes = R.string.home_alert_dialog_low_stock_title,
                emptyMessageRes = R.string.home_alert_empty_low_stock,
                lines = lowStockProductLines
            )
        }

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
                    db.userProfileDao().observeProfile(),
                    db.inventoryItemDao().observeAll(),
                    db.categoryDao().observeAll(),
                    db.inventoryItemDao().observeRecent(RECENT_LIMIT),
                    db.stockChangeEventDao().observeSince(monthStart)
                ) { profile, allItems, categories, recent, monthEvents ->
                    val expiredItems = allItems.filter { entity ->
                        val date = entity.expirationDate ?: return@filter false
                        date < now
                    }
                    val lowStockItems = allItems.filter { entity ->
                        val min = entity.minQuantity ?: return@filter false
                        entity.quantity > 0.0 && entity.quantity <= min
                    }
                    val outOfStockCount = allItems.count { it.quantity <= 0.0 }
                    val insights = MonthlyInsightsEvaluator.evaluate(monthEvents, allItems)

                    HomeUiState(
                        userName = profile?.name?.takeIf { it.isNotBlank() },
                        totalCount = allItems.size,
                        expiredCount = expiredItems.size,
                        expiringCount = allItems.count {
                            val date = it.expirationDate ?: return@count false
                            date in now..expiringLimit
                        },
                        lowStockCount = lowStockItems.size,
                        outOfStockCount = outOfStockCount,
                        expiredNames = expiredItems.map { it.name },
                        lowStockLines = lowStockItems.map { entity ->
                            getString(
                                R.string.home_alert_item_low_stock,
                                entity.name,
                                formatQuantity(entity.quantity)
                            )
                        },
                        insights = insights,
                        recentItems = recent.map { entity ->
                            val item = entity.toDomain()
                            val categoryName = categories
                                .firstOrNull { it.id == item.categoryId }
                                ?.name
                                .orEmpty()
                            InventoryItemUi(
                                id = item.id,
                                name = item.name,
                                categoryId = item.categoryId,
                                categoryName = categoryName,
                                quantity = item.quantity,
                                stockLabel = getString(
                                    R.string.botiquin_stock_in_stock,
                                    formatQuantity(item.quantity)
                                ),
                                barcode = item.barcode,
                                metaLabel = item.unit?.takeIf { it.isNotBlank() }
                                    ?: item.formType?.takeIf { it.isNotBlank() }
                                    ?: "",
                                photoPath = item.photoPath
                            )
                        }
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
        lowStockProductLines = state.lowStockLines

        bindAlertCard(
            binding.cardExpiredAlert,
            title = getString(R.string.home_alert_expired_title),
            count = state.expiredCount
        )
        bindAlertCard(
            binding.cardLowStockAlert,
            title = getString(R.string.home_alert_low_stock_title),
            count = state.lowStockCount
        )

        allRecentItems = state.recentItems
        filterRecent(binding.etSearch.text?.toString().orEmpty())
        bindInsights(state.insights)
        bindSoftPrompt(state)
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

        card.btnInsightsShop.isVisible = insights.showShoppingCta
        card.btnInsightsShop.setOnClickListener {
            (activity as? ControllerActivity)?.navigateToShoppingAutoGenerate()
        }
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
                    body = getString(R.string.prompt_low_stock_body, count),
                    primaryLabel = getString(R.string.prompt_low_stock_action)
                )

                SoftPromptType.FEATURE_TIP -> SoftPrompt(
                    type = type,
                    title = "Tip Keeply",
                    body = "",
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
        count: Int
    ) {
        cardBinding.tvAlertTitle.text = title
        cardBinding.tvAlertCount.text = getString(R.string.home_alert_count, count)
        cardBinding.tvAlertBadge.text = count.toString()
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

        AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setMessage(message)
            .setPositiveButton(R.string.home_alert_accept, null)
            .show()
    }

    private fun bindStat(statBinding: ItemHomeStatBinding, value: String, label: String) {
        statBinding.tvStatValue.text = value
        statBinding.tvStatLabel.text = label
    }

    private fun filterRecent(query: String) {
        val filtered = if (query.isBlank()) {
            allRecentItems
        } else {
            allRecentItems.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.categoryName.contains(query, ignoreCase = true) ||
                        it.barcode?.contains(query, ignoreCase = true) == true
            }
        }
        recentAdapter.submitList(filtered)
        binding.tvRecentEmpty.isVisible = filtered.isEmpty()
        binding.rvRecent.isVisible = filtered.isNotEmpty()
    }

    private fun openAddProduct() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AddInventoryItemFragment.newInstance())
            .addToBackStack(AddInventoryItemFragment.TAG)
            .commit()
    }

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1.0 == 0.0) {
            quantity.toInt().toString()
        } else {
            quantity.toString()
        }
    }

    private data class HomeUiState(
        val userName: String?,
        val totalCount: Int,
        val expiredCount: Int,
        val expiringCount: Int,
        val lowStockCount: Int,
        val outOfStockCount: Int,
        val expiredNames: List<String>,
        val lowStockLines: List<String>,
        val insights: MonthlyInsights,
        val recentItems: List<InventoryItemUi>
    )

    companion object {
        const val TAG = "HomeFragment"
        private const val RECENT_LIMIT = 5
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
