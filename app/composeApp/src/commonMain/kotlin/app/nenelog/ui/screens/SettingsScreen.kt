package app.nenelog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.nenelog.resources.*
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.components.ScreenHeader
import app.nenelog.ui.components.SectionHeader
import app.nenelog.ui.components.VoiceStatusPanel
import app.nenelog.ui.model.VoiceUiStatus
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import app.nenelog.ui.theme.ThemePreference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    selectedTheme: ThemePreference,
    voiceStatus: VoiceUiStatus,
    voiceControlAvailable: Boolean,
    reducedMotion: Boolean,
    onThemeSelected: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            ScreenHeader(
                title = stringResource(Res.string.settings_title),
                subtitle = stringResource(Res.string.settings_subtitle),
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 24.dp,
                    bottom = 24.dp,
                ),
            )
        }
        item {
            SectionHeader(
                title = stringResource(Res.string.appearance),
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal, vertical = 6.dp),
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal)) {
                ThemeOptionRow(
                    preference = ThemePreference.AUTO,
                    label = Res.string.theme_auto,
                    description = Res.string.theme_auto_description,
                    selected = selectedTheme == ThemePreference.AUTO,
                    onClick = { onThemeSelected(ThemePreference.AUTO) },
                )
                HorizontalDivider(color = LocalLinenColors.current.divider)
                ThemeOptionRow(
                    preference = ThemePreference.LIGHT,
                    label = Res.string.theme_light,
                    selected = selectedTheme == ThemePreference.LIGHT,
                    onClick = { onThemeSelected(ThemePreference.LIGHT) },
                )
                HorizontalDivider(color = LocalLinenColors.current.divider)
                ThemeOptionRow(
                    preference = ThemePreference.DARK,
                    label = Res.string.theme_dark,
                    selected = selectedTheme == ThemePreference.DARK,
                    onClick = { onThemeSelected(ThemePreference.DARK) },
                )
                HorizontalDivider(color = LocalLinenColors.current.divider)
                ThemeOptionRow(
                    preference = ThemePreference.NIGHT,
                    label = Res.string.theme_night,
                    selected = selectedTheme == ThemePreference.NIGHT,
                    onClick = { onThemeSelected(ThemePreference.NIGHT) },
                )
            }
        }
        item {
            SectionHeader(
                title = stringResource(Res.string.voice_controls),
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 30.dp,
                    bottom = 12.dp,
                ),
            )
        }
        item {
            VoiceStatusPanel(
                status = voiceStatus,
                available = voiceControlAvailable,
                reducedMotion = reducedMotion,
                modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal),
            )
            Text(
                text = stringResource(Res.string.voice_controls_description),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalLinenColors.current.inkMuted,
                modifier = Modifier.padding(
                    start = LinenDimens.screenHorizontal,
                    end = LinenDimens.screenHorizontal,
                    top = 10.dp,
                ),
            )
        }
        item {
            InfoSection(
                title = stringResource(Res.string.accessibility),
                body = stringResource(Res.string.accessibility_description),
                icon = LinenIconType.SUMMARY,
                modifier = Modifier.padding(top = 30.dp),
            )
        }
        item {
            InfoSection(
                title = stringResource(Res.string.privacy),
                body = stringResource(Res.string.privacy_statement) + " " +
                    stringResource(Res.string.local_first_statement),
                icon = LinenIconType.CHECK,
            )
        }
        item {
            InfoSection(
                title = stringResource(Res.string.language),
                body = stringResource(Res.string.language_description),
                icon = LinenIconType.JOURNAL,
            )
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun ThemeOptionRow(
    preference: ThemePreference,
    label: StringResource,
    selected: Boolean,
    onClick: () -> Unit,
    description: StringResource? = null,
) {
    val colors = LocalLinenColors.current
    val labelText = stringResource(label)
    val descriptionText = description?.let { stringResource(it) }
    val selectedText = if (selected) stringResource(Res.string.theme_selected) else ""
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 64.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.RadioButton
                contentDescription = listOf(labelText, descriptionText, selectedText)
                    .filterNotNull()
                    .filter(String::isNotEmpty)
                    .joinToString(". ")
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when (preference) {
            ThemePreference.AUTO -> LinenIconType.SETTINGS
            ThemePreference.LIGHT -> LinenIconType.HOME
            ThemePreference.DARK, ThemePreference.NIGHT -> LinenIconType.SLEEP
        }
        LinenIcon(
            icon,
            tint = if (selected) colors.sageStrong else colors.inkMuted,
            size = 22.dp,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
            )
            if (descriptionText != null) {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.sageStrong,
                unselectedColor = colors.inkMuted,
            ),
        )
    }
}

@Composable
private fun InfoSection(
    title: String,
    body: String,
    icon: LinenIconType,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    Column(modifier = modifier.padding(horizontal = LinenDimens.screenHorizontal)) {
        HorizontalDivider(color = colors.divider)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LinenIcon(icon, colors.sageStrong, size = 22.dp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
