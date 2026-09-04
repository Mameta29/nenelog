package app.nenelog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nenelog.resources.*
import app.nenelog.ui.model.AppDestination
import app.nenelog.ui.model.RecordKind
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = LinenDimens.touchTarget, minHeight = LinenDimens.touchTarget)
                    .clickable(role = Role.Button, onClick = onAction),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.sageStrong,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: LinenIconType,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.sageSoft,
            contentColor = colors.sageStrong,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                LinenIcon(icon, colors.sageStrong, size = 28.dp)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
fun DataErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinenIcon(LinenIconType.ERROR, colors.terracottaStrong, size = 40.dp)
        Text(
            text = stringResource(Res.string.error_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(Res.string.error_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 24.dp).height(LinenDimens.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.sageStrong,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(stringResource(Res.string.retry))
        }
    }
}

@Composable
fun LinenBottomNavigation(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val navigationLabelStyle = if (largeText) {
        MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, lineHeight = 12.sp)
    } else {
        MaterialTheme.typography.labelMedium
    }
    val items = listOf(
        Triple(AppDestination.HOME, Res.string.nav_home, LinenIconType.HOME),
        Triple(AppDestination.JOURNAL, Res.string.nav_journal, LinenIconType.JOURNAL),
        Triple(AppDestination.SUMMARY, Res.string.nav_summary, LinenIconType.SUMMARY),
        Triple(AppDestination.SETTINGS, Res.string.nav_settings, LinenIconType.SETTINGS),
    )
    Surface(
        color = colors.canvas,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(color = colors.divider)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEach { (destination, labelResource, icon) ->
                    val label = stringResource(labelResource)
                    val selected = selectedDestination == destination
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(minHeight = 60.dp)
                            .clickable(role = Role.Tab) { onDestinationSelected(destination) }
                            .semantics {
                                this.selected = selected
                                role = Role.Tab
                                contentDescription = label
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        LinenIcon(
                            icon,
                            tint = if (selected) colors.sageStrong else colors.inkMuted,
                            size = 22.dp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = navigationLabelStyle,
                            color = if (selected) colors.sageStrong else colors.inkMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickRecordBar(
    onRecordSelected: (RecordKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val largeText = LocalDensity.current.fontScale >= 1.5f
    val items = listOf(
        Triple(RecordKind.NURSING, Res.string.event_nursing, LinenIconType.NURSING),
        Triple(RecordKind.BOTTLE, Res.string.event_bottle, LinenIconType.BOTTLE),
        Triple(RecordKind.DIAPER, Res.string.event_diaper, LinenIconType.DIAPER),
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEach { (kind, labelResource, icon) ->
                val label = stringResource(labelResource)
                QuickRecordItem(
                    label = label,
                    icon = icon,
                    tint = colors.sageStrong,
                    largeText = largeText,
                    onClick = { onRecordSelected(kind) },
                    modifier = Modifier
                        .weight(1f),
                )
            }
            val moreLabel = stringResource(Res.string.record_more)
            QuickRecordItem(
                label = moreLabel,
                icon = LinenIconType.PLUS,
                tint = colors.terracottaStrong,
                largeText = largeText,
                onClick = { onRecordSelected(null) },
                modifier = Modifier
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun QuickRecordItem(
    label: String,
    icon: LinenIconType,
    tint: Color,
    largeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val itemModifier = modifier
        .sizeIn(minHeight = LinenDimens.touchTarget)
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { contentDescription = label }
    if (largeText) {
        Column(
            modifier = itemModifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LinenIcon(icon, tint, size = 20.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    } else {
        Row(
            modifier = itemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            LinenIcon(icon, tint, size = 20.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.ink,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
    }
}

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = LocalLinenColors.current.divider)
}

@Composable
fun OutlinedLinenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(LinenDimens.buttonHeight),
        border = BorderStroke(1.dp, colors.divider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink),
    ) {
        Text(text)
    }
}
