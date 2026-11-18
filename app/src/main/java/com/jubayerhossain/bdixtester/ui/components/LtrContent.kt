package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Forces left-to-right directional layout for technical values such as URLs,
 * IPv4/host strings, ports, latency measurements, Mbps rates, and SHA-256 digests.
 */
@Composable
fun LtrContent(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    content()
  }
}
