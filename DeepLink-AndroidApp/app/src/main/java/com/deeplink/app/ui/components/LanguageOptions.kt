package com.deeplink.app.ui.components

data class LanguageOption(val code: String, val label: String)

val supportedLanguages = listOf(
    LanguageOption("en", "English (en)"),
    LanguageOption("ru", "Russian (ru)"),
    LanguageOption("tj", "Tajik (tj)")
)

fun languageLabel(code: String?): String =
    supportedLanguages.find { it.code == code }?.label ?: code ?: "English"

fun normalizeLanguageCode(code: String?): String =
    supportedLanguages.find { it.code == code }?.code ?: "en"
