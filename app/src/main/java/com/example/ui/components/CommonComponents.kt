package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Dimens
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.radiusMd
import com.example.ui.theme.radiusPill
import com.example.ui.theme.radiusSm

/* ------------------------------------------------------------------ *
 *  App bar
 * ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinguaTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

/* ------------------------------------------------------------------ *
 *  Containers
 * ------------------------------------------------------------------ */

/** Standard flat bordered container used for every content block. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.cardPadding,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.radiusMd),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = Dimens.minTapTarget)
    val shapeFinal = shape
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = base,
            shape = shapeFinal,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(Dimens.hairline, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    } else {
        Card(
            modifier = base,
            shape = shapeFinal,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(Dimens.hairline, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    }
}

/** Low-emphasis inset block, e.g. an example sentence inside a card. */
@Composable
fun AppInset(
    modifier: Modifier = Modifier,
    padding: Dp = Dimens.space10,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.radiusSm),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            color = color,
            contentColor = contentColor,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            modifier = base,
            color = color,
            contentColor = contentColor,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    }
}

/* ------------------------------------------------------------------ *
 *  Text
 * ------------------------------------------------------------------ */

/** Small section heading with an optional trailing text action. */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(Dimens.space8))
            TextButton(
                onClick = onActionClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Dimens.space8,
                    vertical = Dimens.space2
                ),
                modifier = Modifier.heightIn(min = Dimens.minTapTarget)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

/** Uppercase micro-label used above groups of controls. */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.hairline)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/* ------------------------------------------------------------------ *
 *  Badges & chips
 * ------------------------------------------------------------------ */

@Composable
fun CefrBadge(level: String, modifier: Modifier = Modifier) {
    val normalized = level.trim().uppercase()
    val (bgColor, textColor) = when (normalized) {
        "A1", "A2" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "B1", "B2" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "C1" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "C2" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        contentColor = textColor,
        shape = RoundedCornerShape(Dimens.radiusXs),
        modifier = modifier
    ) {
        Text(
            text = normalized,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

/** Generic pill for counts, tags and statuses. */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null
) {
    val base = modifier
        .defaultMinSize(minHeight = 28.dp)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = base,
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(Dimens.radiusPill)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Dimens.space10, vertical = Dimens.space6)
            )
        }
    } else {
        Surface(
            modifier = base,
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(Dimens.radiusPill)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Dimens.space10, vertical = Dimens.space6)
            )
        }
    }
}

/** Filter / toggle chip with a selected state. */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Dimens.minTapTarget),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(Dimens.radiusSm),
        border = BorderStroke(
            Dimens.hairline,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/* ------------------------------------------------------------------ *
 *  Buttons & icon controls
 * ------------------------------------------------------------------ */

@Composable
fun AudioSpeakerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.audioButton,
    contentDescription: String = "Play pronunciation"
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(size)) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** Rounded icon container used at the top of tiles and list rows. */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconTileMd,
    iconSize: Dp = Dimens.iconMd,
    shape: RoundedCornerShape = RoundedCornerShape(Dimens.radiusSm)
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/* ------------------------------------------------------------------ *
 *  Stats
 * ------------------------------------------------------------------ */

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color = PrimaryBlue,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        padding = Dimens.space10
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = icon, tint = color, size = Dimens.iconTileSm, iconSize = Dimens.iconSm)
            Spacer(modifier = Modifier.width(Dimens.space8))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ *
 *  Empty state
 * ------------------------------------------------------------------ */

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        IconTile(
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            size = Dimens.space24 * 2,
            iconSize = Dimens.iconLg,
            shape = RoundedCornerShape(radiusMd)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Dimens.space4))
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
