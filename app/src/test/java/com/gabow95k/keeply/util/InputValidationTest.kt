package com.gabow95k.keeply.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {

    @Test
    fun requiredText_acceptsUnicodeAccentsDigitsAndPunctuation() {
        assertTrue(InputValidation.isValidRequiredText("Café 100% — niño #2 🧴"))
    }

    @Test
    fun requiredText_rejectsBlankControlCharactersAndExcessLength() {
        assertFalse(InputValidation.isValidRequiredText("   "))
        assertFalse(InputValidation.isValidRequiredText("Nombre\ninyectado"))
        assertFalse(
            InputValidation.isValidRequiredText("a".repeat(InputValidation.NAME_MAX_LENGTH + 1))
        )
    }

    @Test
    fun quantity_acceptsBoundsAndRejectsNegativeNonFiniteOrMalformedValues() {
        assertEquals(0.0, InputValidation.parseQuantity("0", required = true)!!, 0.0)
        assertEquals(12.5, InputValidation.parseQuantity("12.5", required = true)!!, 0.0)
        assertNull(InputValidation.parseQuantity("-1", required = true))
        assertNull(InputValidation.parseQuantity("NaN", required = true))
        assertNull(InputValidation.parseQuantity("Infinity", required = true))
        assertNull(InputValidation.parseQuantity("1,5", required = true))
        assertNull(InputValidation.parseQuantity("", required = true))
    }

    @Test
    fun barcode_acceptsSupportedGtinLengthsAndDigitsOnly() {
        assertTrue(InputValidation.isValidBarcode(""))
        assertTrue(InputValidation.isValidBarcode("7501234567890"))
        assertTrue(InputValidation.isValidBarcode("12345678"))
        assertFalse(InputValidation.isValidBarcode("1234567"))
        assertFalse(InputValidation.isValidBarcode("ABC123456789"))
        assertFalse(InputValidation.isValidBarcode("1234-5678"))
    }

    @Test
    fun profileFields_validateExpectedFormats() {
        assertTrue(InputValidation.isValidAge(0))
        assertTrue(InputValidation.isValidAge(120))
        assertFalse(InputValidation.isValidAge(121))
        assertTrue(InputValidation.isValidBloodType(" ab+ "))
        assertFalse(InputValidation.isValidBloodType("C+"))
        assertTrue(InputValidation.isValidPhone("+52 (55) 1234-5678"))
        assertFalse(InputValidation.isValidPhone("call-me"))
        assertTrue(InputValidation.isValidEmail("persona+keeply@example.com"))
        assertFalse(InputValidation.isValidEmail("persona@localhost"))
    }
}
