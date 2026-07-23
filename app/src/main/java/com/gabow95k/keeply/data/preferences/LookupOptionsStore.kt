package com.gabow95k.keeply.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.gabow95k.keeply.R

enum class LookupOptionType {
    FORM_TYPE,
    UNIT,
    LOCATION
}

class LookupOptionsStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOptions(type: LookupOptionType): List<String> {
        val defaults = defaultOptions(type)
        val custom = customOptions(type)
        return (defaults + custom)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }

    fun addCustom(type: LookupOptionType, value: String): Boolean {
        val cleaned = value.trim()
        if (cleaned.isEmpty()) return false
        val existing = getOptions(type)
        if (existing.any { it.equals(cleaned, ignoreCase = true) }) return false
        val updated = customOptions(type) + cleaned
        prefs.edit { putStringSet(keyFor(type), updated.toSet()) }
        return true
    }

    fun ensurePresent(type: LookupOptionType, value: String?) {
        val cleaned = value?.trim().orEmpty()
        if (cleaned.isEmpty()) return
        if (getOptions(type).none { it.equals(cleaned, ignoreCase = true) }) {
            addCustom(type, cleaned)
        }
    }

    private fun customOptions(type: LookupOptionType): List<String> {
        return prefs.getStringSet(keyFor(type), emptySet()).orEmpty().toList().sorted()
    }

    private fun defaultOptions(type: LookupOptionType): List<String> {
        val arrayRes = when (type) {
            LookupOptionType.FORM_TYPE -> R.array.product_form_types
            LookupOptionType.UNIT -> R.array.product_units
            LookupOptionType.LOCATION -> R.array.product_locations
        }
        return context.resources.getStringArray(arrayRes).toList()
    }

    private fun keyFor(type: LookupOptionType): String = "lookup_${type.name.lowercase()}"

    companion object {
        private const val PREFS_NAME = "keeply_lookup_options"

        @Volatile
        private var instance: LookupOptionsStore? = null

        fun getInstance(context: Context): LookupOptionsStore {
            return instance ?: synchronized(this) {
                instance ?: LookupOptionsStore(context).also { instance = it }
            }
        }
    }
}
