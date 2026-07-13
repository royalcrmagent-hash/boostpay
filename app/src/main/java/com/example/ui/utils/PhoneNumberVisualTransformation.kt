package com.example.ui.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Mask: XXXXX XXXXXX
        val trimmed = if (text.text.length >= 11) text.text.substring(0, 11) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 4) out += " "
        }

        val phoneNumberOffsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 11) return offset + 1
                return 12
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 12) return offset - 1
                return 11
            }
        }

        return TransformedText(AnnotatedString(out), phoneNumberOffsetMapping)
    }
}
