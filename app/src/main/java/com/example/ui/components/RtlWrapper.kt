package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * The app chrome is English, so the whole app runs LTR.
 * Kept explicit so individual blocks can opt out when they render
 * Persian learning content (meanings, examples, AI feedback).
 */
@Composable
fun EnglishLtrLayout(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
    }
}

/** Wraps Persian *content* (not UI chrome) so it renders right-to-left. */
@Composable
fun PersianContentRtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        content()
    }
}
