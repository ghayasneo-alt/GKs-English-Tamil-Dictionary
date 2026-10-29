package com.gk.englishtamildictionary

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.clickable { onClick() }
