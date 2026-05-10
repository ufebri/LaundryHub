package com.raylabs.laundryhub.ui.common.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.filter { it.isDigit() }
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val number = originalText.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        
        val locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
        val formatter = NumberFormat.getInstance(locale)
        val formattedText = "Rp " + formatter.format(number)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isEmpty()) return 0
                val safeOffset = offset.coerceIn(0, originalText.length)
                val substring = originalText.take(safeOffset)
                if (substring.isEmpty()) return 3
                val formattedSubstring = formatter.format(substring.toLongOrNull() ?: 0L)
                return 3 + formattedSubstring.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (originalText.isEmpty()) return 0
                if (offset <= 3) return 0 // If cursor is in "Rp "
                
                val digitsAndDots = formattedText.substring(3, offset.coerceIn(3, formattedText.length))
                return digitsAndDots.count { it.isDigit() }.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
