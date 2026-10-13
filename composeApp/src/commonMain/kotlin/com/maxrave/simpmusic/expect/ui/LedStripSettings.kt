package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

/** Rows of the "LED strip" settings group. Android drives a BLE strip; Desktop has none. */
@Composable
expect fun ColumnScope.LedStripSettingItems()
