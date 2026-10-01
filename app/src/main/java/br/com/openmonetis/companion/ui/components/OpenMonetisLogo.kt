package br.com.openmonetis.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.openmonetis.companion.R

@Composable
fun OpenMonetisLogo(
    modifier: Modifier = Modifier,
    markHeight: Dp = 32.dp,
    wordmarkWidth: Dp = 116.dp,
    showWordmark: Boolean = true
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.openmonetis_mark),
            contentDescription = if (showWordmark) null else stringResource(R.string.app_name),
            modifier = Modifier.width(markHeight * (188f / 195f)).height(markHeight)
        )
        if (showWordmark) Image(
            painter = painterResource(R.drawable.openmonetis_wordmark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.width(wordmarkWidth).height(wordmarkWidth * (943f / 6015f)),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
    }
}
