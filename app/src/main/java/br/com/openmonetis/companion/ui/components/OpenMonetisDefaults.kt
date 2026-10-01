package br.com.openmonetis.companion.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

object OpenMonetisDefaults {
    @Composable
    fun textFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        focusedLabelColor = MaterialTheme.colorScheme.secondary,
        cursorColor = MaterialTheme.colorScheme.secondary
    )
}
