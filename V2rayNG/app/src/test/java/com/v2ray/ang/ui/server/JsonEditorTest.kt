package com.v2ray.ang.ui.server

import androidx.compose.ui.graphics.Color
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonEditorTest {
    @Test
    fun formattingPreservesLargeIntegersAndNestedConfigurationValues() {
        val source = """{"id":9223372036854775807,"small":0.0000001,"nested":[true,null,"a\\b\"c"]}"""
        val formatted = requireNotNull(formatJsonForEditor(source).formatted)
        assertTrue(formatted.contains('\n'))
        assertEquals(JsonParser.parseString(source), JsonParser.parseString(formatted))
        assertTrue(formatted.contains("9223372036854775807"))
    }

    @Test
    fun invalidAndBlankJsonDoNotReplaceTheDraft() {
        for (source in listOf("", " ", "{\"key\":}")) {
            val result = formatJsonForEditor(source)
            assertEquals(source, result.source)
            assertNull(result.formatted)
        }
        val error = formatJsonForEditor("{\n\"key\": } ")
        assertNotNull(error.line)
        assertNotNull(error.column)
    }

    @Test
    fun highlightingDoesNotChangeTextOrTreatNumbersInsideStringsAsLiterals() {
        val source = """{"key":"123 true", "escaped":"a\"b", "n":12, "ok":false}"""
        val highlighted = highlightJson(source, Color.Red, Color.Green, Color.Blue)
        assertEquals(source, highlighted.text)
        val token = highlighted.spanStyles.single { source.substring(it.start, it.end) == "\"123 true\"" }
        assertEquals(Color.Green, token.item.color)
        assertFalse(highlighted.spanStyles.any { it.start > token.start && it.end < token.end })
        assertEquals(Color.Red, highlighted.spanStyles.first().item.color)
    }
}
