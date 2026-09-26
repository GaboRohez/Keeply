package com.gabow95k.keeply.util

object InputValidation {

    const val NAME_MAX_LENGTH = 80
    const val SHORT_LABEL_MAX_LENGTH = 40
    const val NOTES_MAX_LENGTH = 500
    const val EMAIL_MAX_LENGTH = 254
    const val PHONE_MAX_LENGTH = 20
    const val BARCODE_MAX_LENGTH = 14
    const val QUANTITY_MAX = 1_000_000_000.0

    private val forbiddenCharacters = Regex("[\\p{Cc}\\p{Cf}]")
    private val emailPattern = Regex(
        "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
        RegexOption.IGNORE_CASE
    )
    private val phonePattern = Regex("^[+0-9() .-]+$")
    private val barcodePattern = Regex("^[0-9]{8,14}$")
    private val bloodTypes = setOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    fun isValidRequiredText(value: String, maxLength: Int = NAME_MAX_LENGTH): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotEmpty() &&
                trimmed.length <= maxLength &&
                !forbiddenCharacters.containsMatchIn(trimmed)
    }

    fun isValidOptionalText(value: String?, maxLength: Int): Boolean {
        val trimmed = value?.trim().orEmpty()
        return trimmed.isEmpty() ||
                (trimmed.length <= maxLength && !forbiddenCharacters.containsMatchIn(trimmed))
    }

    fun parseQuantity(value: String, required: Boolean): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return if (required) null else 0.0
        val number = trimmed.toDoubleOrNull() ?: return null
        return number.takeIf { it.isFinite() && it >= 0.0 && it <= QUANTITY_MAX }
    }

    fun isValidAge(age: Int?): Boolean = age == null || age in 0..120

    fun normalizeBloodType(value: String): String = value.trim().uppercase()

    fun isValidBloodType(value: String): Boolean {
        val normalized = normalizeBloodType(value)
        return normalized.isEmpty() || normalized in bloodTypes
    }

    fun isValidPhone(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return true
        val digitCount = trimmed.count(Char::isDigit)
        return trimmed.length <= PHONE_MAX_LENGTH &&
                digitCount in 7..15 &&
                phonePattern.matches(trimmed)
    }

    fun isValidEmail(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isEmpty() ||
                (trimmed.length <= EMAIL_MAX_LENGTH && emailPattern.matches(trimmed))
    }

    fun isValidBarcode(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isEmpty() || barcodePattern.matches(trimmed)
    }
}
