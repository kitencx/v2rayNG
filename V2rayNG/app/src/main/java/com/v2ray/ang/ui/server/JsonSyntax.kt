package com.v2ray.ang.ui.server

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

private val jsonTokens = Regex("\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\b(?:true|false|null)\\b")

internal fun highlightJson(source: String, key: Color, string: Color, literal: Color): AnnotatedString =
    buildAnnotatedString {
        append(source)
        jsonTokens.findAll(source).forEach { token ->
            val value = token.value
            val color = if (value.startsWith('"')) {
                var next = token.range.last + 1
                while (next < source.length && source[next].isWhitespace()) next++
                if (source.getOrNull(next) == ':') key else string
            } else literal
            addStyle(SpanStyle(color = color), token.range.first, token.range.last + 1)
        }
    }
