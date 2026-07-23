package com.gabow95k.keeply.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class ProductLabelAnalyzer(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()

    suspend fun analyze(
        imageUri: Uri,
        knownFormTypes: List<String>,
        knownUnits: List<String>
    ): ProductLabelHints {
        val image = InputImage.fromFilePath(context, imageUri)
        val textTask = textRecognizer.process(image)
        val barcodeTask = barcodeScanner.process(image)

        val textResult = runCatching { textTask.await() }.getOrNull()
        val barcodeResult = runCatching { barcodeTask.await() }.getOrNull().orEmpty()

        val fullText = textResult?.text.orEmpty()
        val lines = textResult?.textBlocks
            ?.flatMap { block -> block.lines.map { it.text.trim() } }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val barcode = barcodeResult
            .asSequence()
            .mapNotNull { it.rawValue?.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: extractBarcodeFromText(fullText)

        val expiration = extractExpiration(fullText)
        val formType = matchKnownOption(fullText, knownFormTypes)
        val unit = matchKnownOption(fullText, knownUnits)
        val quantity = extractQuantity(fullText, unit)
        val name = extractName(lines)
        val brand = extractBrand(lines, name)

        return ProductLabelHints(
            name = name,
            brand = brand,
            barcode = barcode,
            formType = formType,
            unit = unit,
            quantity = quantity,
            expirationMillis = expiration
        )
    }

    fun close() {
        textRecognizer.close()
        barcodeScanner.close()
    }

    private fun extractBarcodeFromText(text: String): String? {
        val matcher = BARCODE_PATTERN.matcher(text.replace(" ", ""))
        return if (matcher.find()) matcher.group() else null
    }

    private fun extractExpiration(text: String): Long? {
        val normalized = text.replace('\n', ' ')
        EXPIRATION_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(normalized)
            if (matcher.find()) {
                val raw = matcher.group(1) ?: return@forEach
                parseDate(raw)?.let { return it }
            }
        }
        DATE_ONLY_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(normalized)
            while (matcher.find()) {
                val raw = matcher.group() ?: continue
                parseDate(raw)?.let { return it }
            }
        }
        return null
    }

    private fun parseDate(raw: String): Long? {
        val cleaned = raw.trim().replace('.', '/').replace('-', '/')
        DATE_FORMATS.forEach { format ->
            runCatching {
                val parsed = format.parse(cleaned) ?: return@runCatching null
                val cal = Calendar.getInstance().apply { time = parsed }
                if (format.toPattern().equals("MM/yyyy", ignoreCase = true)) {
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                }
                return cal.timeInMillis
            }
        }
        return null
    }

    private fun matchKnownOption(text: String, options: List<String>): String? {
        val lower = text.lowercase(Locale.getDefault())
        return options
            .sortedByDescending { it.length }
            .firstOrNull { option ->
                val needle = option.lowercase(Locale.getDefault())
                needle.length >= 2 && Regex("""\b${Regex.escape(needle)}\b""").containsMatchIn(lower)
            }
    }

    private fun extractQuantity(text: String, unit: String?): String? {
        if (unit != null) {
            val pattern = Pattern.compile(
                """(?i)(\d+(?:[.,]\d+)?)\s*${Pattern.quote(unit)}\b"""
            )
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.replace(',', '.')
            }
        }
        val generic = Pattern.compile(
            """(?i)(\d+(?:[.,]\d+)?)\s*(mg|g|ml|l|pzas?|tabletas?|cápsulas?|capsulas?)"""
        )
        val matcher = generic.matcher(text)
        return if (matcher.find()) matcher.group(1)?.replace(',', '.') else null
    }

    private fun extractName(lines: List<String>): String? {
        return lines
            .asSequence()
            .map { it.trim() }
            .filter { line ->
                line.length in 3..60 &&
                        line.any { it.isLetter() } &&
                        !NOISE_LINE.matcher(line).find() &&
                        !DATE_ONLY_PATTERNS.any { it.matcher(line).matches() } &&
                        !BARCODE_PATTERN.matcher(line.replace(" ", "")).matches()
            }
            .sortedByDescending { scoreNameCandidate(it) }
            .firstOrNull()
            ?.replace(Regex("""\s+"""), " ")
    }

    private fun extractBrand(lines: List<String>, name: String?): String? {
        val candidates = lines
            .asSequence()
            .map { it.trim() }
            .filter { line ->
                line.length in 2..40 &&
                        line.any { it.isLetter() } &&
                        !line.equals(name, ignoreCase = true) &&
                        !NOISE_LINE.matcher(line).find()
            }
            .toList()

        candidates.firstOrNull { it.contains('®') || it.contains('™') }
            ?.let { return it.replace("®", "").replace("™", "").trim() }

        return candidates
            .filter { it.length <= 28 && it.split(' ').size <= 4 }
            .maxByOrNull { scoreNameCandidate(it) }
            ?.takeIf { scoreNameCandidate(it) >= 4 }
    }

    private fun scoreNameCandidate(line: String): Int {
        var score = line.count { it.isLetter() }
        if (line.any { it.isLowerCase() }) score += 4
        if (line.split(' ').size in 1..5) score += 3
        if (line == line.uppercase(Locale.getDefault()) && line.length > 18) score -= 6
        if (NOISE_LINE.matcher(line).find()) score -= 20
        return score
    }

    companion object {
        private val BARCODE_PATTERN: Pattern =
            Pattern.compile("""\b\d{8}(?:\d{4,6})?\b""")

        private val NOISE_LINE: Pattern = Pattern.compile(
            """(?i)\b(indicaciones|composici[oó]n|modo de empleo|advertencias|conservaci[oó]n|""" +
                    """lote|batch|fabricante|distribuidor|registro sanitario|cont\.?\s*neto|""" +
                    """hecha en|made in|keep out|uso oral|vía oral|via oral)\b"""
        )

        private val EXPIRATION_PATTERNS = listOf(
            Pattern.compile(
                """(?i)(?:cad\.?|caducidad|vence|venc\.?|exp\.?|expiry|best before|usar antes de|fecha de caducidad)\s*[:\-]?\s*(\d{1,2}[\/.\-]\d{1,2}[\/.\-]\d{2,4}|\d{1,2}[\/.\-]\d{4}|\d{4}[\/.\-]\d{1,2}[\/.\-]\d{1,2})"""
            )
        )

        private val DATE_ONLY_PATTERNS = listOf(
            Pattern.compile("""\b\d{1,2}[\/.\-]\d{1,2}[\/.\-]\d{2,4}\b"""),
            Pattern.compile("""\b\d{1,2}[\/.\-]\d{4}\b"""),
            Pattern.compile("""\b\d{4}[\/.\-]\d{1,2}[\/.\-]\d{1,2}\b""")
        )

        private val DATE_FORMATS = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
            SimpleDateFormat("MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("M/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        ).onEach { it.isLenient = false }
    }
}
