package app.nenelog.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.nenelog.domain.Event
import app.nenelog.resources.*
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.platform.formatLocalDateHeading
import app.nenelog.ui.platform.formatLocalTime
import app.nenelog.ui.presentation.detailLabel
import app.nenelog.ui.presentation.iconType
import app.nenelog.ui.presentation.sourceLabel
import app.nenelog.ui.presentation.typeLabel
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun EventDetailDialog(
    event: Event,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalLinenColors.current
    var confirmingDelete by remember(event.id) { mutableStateOf(false) }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            textContentColor = colors.inkMuted,
            icon = { LinenIcon(LinenIconType.TRASH, colors.terracottaStrong, size = 28.dp) },
            title = {
                Text(
                    text = stringResource(Res.string.delete_record_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = { Text(stringResource(Res.string.delete_record_body)) },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.terracottaStrong),
                ) {
                    Text(stringResource(Res.string.delete_record))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingDelete = false },
                    modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.inkMuted),
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
        return
    }
    val title = event.typeLabel()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        icon = { LinenIcon(event.iconType(), colors.sageStrong, size = 28.dp) },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = event.detailLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                )
                Text(
                    text = "${formatLocalDateHeading(event.occurredAt.epochMillis)} · " +
                        formatLocalTime(event.occurredAt.epochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LinenIcon(LinenIconType.CHECK, colors.sageStrong, size = 16.dp)
                    Text(
                        text = event.sourceLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirmingDelete = true },
                modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.terracottaStrong),
            ) {
                LinenIcon(LinenIconType.TRASH, colors.terracottaStrong, size = 18.dp)
                Text(stringResource(Res.string.delete_record), modifier = Modifier.padding(start = 6.dp))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.inkMuted),
            ) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}
