package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.entity.StockStatus
import com.example.ui.theme.CategoryStyleHelper
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun StockStatusBadge(
    status: StockStatus,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val (bgColor, textColor, borderColor, icon, label) = when (status) {
        StockStatus.IN_STOCK -> Tuple5(
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFECFDF5),
            if (isDark) Color(0xFF34D399) else Color(0xFF047857),
            if (isDark) Color(0xFF059669).copy(alpha = 0.6f) else Color(0xFFA7F3D0),
            Icons.Default.CheckCircle,
            "IN STOCK"
        )
        StockStatus.LOW_STOCK -> Tuple5(
            if (isDark) Color(0xFF78350F).copy(alpha = 0.55f) else Color(0xFFFFFBEB),
            if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
            if (isDark) Color(0xFFD97706).copy(alpha = 0.6f) else Color(0xFFFDE68A),
            Icons.Default.WarningAmber,
            "LOW STOCK"
        )
        StockStatus.OUT_OF_STOCK -> Tuple5(
            if (isDark) Color(0xFF881337).copy(alpha = 0.55f) else Color(0xFFFFF1F2),
            if (isDark) Color(0xFFFB7185) else Color(0xFFBE123C),
            if (isDark) Color(0xFFE11D48).copy(alpha = 0.6f) else Color(0xFFFECDD3),
            Icons.Default.ErrorOutline,
            "OUT OF STOCK"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)

@Composable
fun CategoryPillBadge(
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val style = CategoryStyleHelper.getCategoryStyle(categoryName, isDark)
    Surface(
        color = style.containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = categoryName ?: "General",
                color = style.color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    gradientBackground: Brush? = null
) {
    val isDark = LocalIsDarkTheme.current
    val effectiveContainerColor = when {
        gradientBackground != null -> Color.Transparent
        isDark -> MaterialTheme.colorScheme.surfaceVariant
        containerColor != null -> containerColor
        else -> MaterialTheme.colorScheme.surface
    }

    val cardModifier = if (gradientBackground != null) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(gradientBackground)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = effectiveContainerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        border = if (isDark && gradientBackground == null) {
            BorderStroke(1.dp, iconTint.copy(alpha = 0.25f))
        } else if (gradientBackground == null) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (gradientBackground != null) Color.White.copy(alpha = 0.9f)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (gradientBackground != null) Color.White.copy(alpha = 0.2f)
                            else iconTint.copy(alpha = if (isDark) 0.22f else 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (gradientBackground != null) Color.White else iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (gradientBackground != null) Color.White else MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (gradientBackground != null) Color.White.copy(alpha = 0.8f)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(actionText, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            } else {
                trailingAction?.invoke()
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}
