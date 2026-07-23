package com.gabow95k.keeply.prompts

import com.gabow95k.keeply.data.preferences.KeeplyPreferences
import java.util.Calendar

enum class SoftPromptType {
    END_OF_MONTH_SHOPPING,
    LOW_STOCK_SHOPPING,
    FEATURE_TIP
}

data class SoftPrompt(
    val type: SoftPromptType,
    val title: String,
    val body: String,
    val primaryLabel: String
)

object SoftPromptEvaluator {

    private val tips = listOf(
        "Puedes deslizar una card a la derecha varias veces para restar stock rápido.",
        "Toma una foto al crear un producto: Keeply intenta prellenar nombre, marca y caducidad.",
        "Activa notificaciones en Ajustes para enterarte de caducados y stock bajo.",
        "Usa el filtro de categorías en Inventario para encontrar productos más rápido."
    )

    fun evaluate(
        prefs: KeeplyPreferences,
        lowStockCount: Int,
        outOfStockCount: Int,
        titleFor: (SoftPromptType, Int) -> SoftPrompt
    ): SoftPrompt? {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val monthKey = calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
        val dayKey = monthKey * 100 + day

        if (day >= 25 && prefs.lastMonthEndPromptMonth != monthKey) {
            return titleFor(SoftPromptType.END_OF_MONTH_SHOPPING, 0)
        }

        val needsRestock = lowStockCount + outOfStockCount
        if (needsRestock > 0 && prefs.lastLowStockPromptDay != dayKey) {
            return titleFor(SoftPromptType.LOW_STOCK_SHOPPING, needsRestock)
        }

        if (prefs.lastTipPromptDay != dayKey) {
            val tipIndex = prefs.nextTipIndex % tips.size
            return SoftPrompt(
                type = SoftPromptType.FEATURE_TIP,
                title = "Tip Keeply",
                body = tips[tipIndex],
                primaryLabel = "Entendido"
            )
        }

        return null
    }

    fun markShown(prefs: KeeplyPreferences, type: SoftPromptType) {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val monthKey = calendar.get(Calendar.YEAR) * 100 + (calendar.get(Calendar.MONTH) + 1)
        val dayKey = monthKey * 100 + day

        when (type) {
            SoftPromptType.END_OF_MONTH_SHOPPING -> prefs.lastMonthEndPromptMonth = monthKey
            SoftPromptType.LOW_STOCK_SHOPPING -> prefs.lastLowStockPromptDay = dayKey
            SoftPromptType.FEATURE_TIP -> {
                prefs.lastTipPromptDay = dayKey
                prefs.nextTipIndex = prefs.nextTipIndex + 1
            }
        }
    }
}
