package com.aigrowth.os.feature.simple

import androidx.compose.runtime.Composable

@Composable
fun EnglishScreen(onOpenDrawer: () -> Unit) {
    SimpleListScreen("学英语", "english", onOpenDrawer)
}
