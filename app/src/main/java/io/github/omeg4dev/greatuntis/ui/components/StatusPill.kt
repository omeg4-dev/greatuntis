package io.github.omeg4dev.greatuntis.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.omeg4dev.greatuntis.ui.theme.StatusColor

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    colors: StatusColor,
    modifier: Modifier = Modifier,
) = StatusPill(
    text = text,
    containerColor = colors.container,
    contentColor = colors.onContainer,
    modifier = modifier,
)
