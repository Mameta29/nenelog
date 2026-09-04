package app.nenelog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.nenelog.resources.*
import app.nenelog.ui.components.LinenIcon
import app.nenelog.ui.components.LinenIconType
import app.nenelog.ui.model.ManualRecordDraft
import app.nenelog.ui.model.RecordKind
import app.nenelog.ui.theme.LinenDimens
import app.nenelog.ui.theme.LocalLinenColors
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun RecordSheet(
    initialKind: RecordKind?,
    onDismiss: () -> Unit,
    onOpenNursingTimer: () -> Unit,
    onSave: (ManualRecordDraft) -> Boolean,
) {
    val colors = LocalLinenColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedKind by remember(initialKind) { mutableStateOf(initialKind) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.ink,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(
                showBack = selectedKind != null,
                onBack = { selectedKind = null },
                onDismiss = onDismiss,
            )
            if (selectedKind == null) {
                RecordKindChooser(
                    onSelected = { kind ->
                        if (kind == RecordKind.NURSING) {
                            onOpenNursingTimer()
                        } else {
                            selectedKind = kind
                        }
                    },
                )
            } else {
                RecordForm(
                    kind = requireNotNull(selectedKind),
                    onSave = onSave,
                )
            }
        }
    }
}

@Composable
private fun SheetHeader(
    showBack: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalLinenColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.sageStrong),
            ) {
                Text(stringResource(Res.string.back))
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (showBack) Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.sizeIn(minHeight = LinenDimens.touchTarget),
            colors = ButtonDefaults.textButtonColors(contentColor = colors.inkMuted),
        ) {
            Text(stringResource(Res.string.close))
        }
    }
    HorizontalDivider(color = colors.divider)
}

@Composable
private fun RecordKindChooser(onSelected: (RecordKind) -> Unit) {
    val entries = listOf(
        RecordChoice(RecordKind.NURSING, Res.string.event_nursing, LinenIconType.NURSING),
        RecordChoice(RecordKind.BOTTLE, Res.string.event_bottle, LinenIconType.BOTTLE),
        RecordChoice(RecordKind.DIAPER, Res.string.event_diaper, LinenIconType.DIAPER),
        RecordChoice(RecordKind.SLEEP, Res.string.event_sleep, LinenIconType.SLEEP),
        RecordChoice(RecordKind.PUMPING, Res.string.event_pumping, LinenIconType.PUMPING),
        RecordChoice(RecordKind.TEMPERATURE, Res.string.event_temperature, LinenIconType.TEMPERATURE),
        RecordChoice(RecordKind.MEDICINE, Res.string.event_medicine, LinenIconType.MEDICINE),
        RecordChoice(RecordKind.BATH, Res.string.event_bath, LinenIconType.BATH),
        RecordChoice(RecordKind.MEMO, Res.string.event_memo, LinenIconType.MEMO),
        RecordChoice(RecordKind.GROWTH, Res.string.event_growth, LinenIconType.GROWTH),
    )
    Column(modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal, vertical = 20.dp)) {
        Text(
            text = stringResource(Res.string.add_record),
            style = MaterialTheme.typography.headlineMedium,
            color = LocalLinenColors.current.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.choose_record_type),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalLinenColors.current.inkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
        )
        entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { choice ->
                    RecordKindButton(choice, onClick = { onSelected(choice.kind) }, Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class RecordChoice(
    val kind: RecordKind,
    val label: StringResource,
    val icon: LinenIconType,
)

@Composable
private fun RecordKindButton(
    choice: RecordChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLinenColors.current
    val label = stringResource(choice.label)
    Surface(
        modifier = modifier
            .sizeIn(minHeight = 84.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceQuiet,
        border = BorderStroke(1.dp, colors.divider),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            LinenIcon(choice.icon, colors.sageStrong, size = 22.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.ink,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RecordForm(
    kind: RecordKind,
    onSave: (ManualRecordDraft) -> Boolean,
) {
    val title = recordKindLabel(kind)
    Column(modifier = Modifier.padding(horizontal = LinenDimens.screenHorizontal, vertical = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = LocalLinenColors.current.ink,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.record_details),
            style = MaterialTheme.typography.bodyLarge,
            color = LocalLinenColors.current.inkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
        )
        when (kind) {
            RecordKind.BOTTLE -> BottleForm(onSave)
            RecordKind.PUMPING -> PumpingForm(onSave)
            RecordKind.DIAPER -> DiaperForm(onSave)
            RecordKind.SLEEP -> SleepForm(onSave)
            RecordKind.TEMPERATURE -> TemperatureForm(onSave)
            RecordKind.MEDICINE -> MedicineForm(onSave)
            RecordKind.BATH -> BathForm(onSave)
            RecordKind.MEMO -> MemoForm(onSave)
            RecordKind.GROWTH -> GrowthForm(onSave)
            RecordKind.NURSING -> Unit
        }
    }
}

@Composable
private fun BottleForm(onSave: (ManualRecordDraft) -> Boolean) {
    var amount by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("formula") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    NumberField(amount, { amount = it; error = null }, Res.string.amount_ml_label, error)
    ChoiceLabel(Res.string.bottle_kind_label)
    ChoiceChips(
        choices = listOf(
            "formula" to Res.string.bottle_formula,
            "breast_milk" to Res.string.bottle_breast_milk,
            "mixed" to Res.string.bottle_mixed,
        ),
        selected = kind,
        onSelected = { kind = it },
    )
    SaveButton {
        val parsed = amount.toIntOrNull()
        if (parsed == null || parsed !in 1..1_000) {
            error = Res.string.invalid_bottle_amount
            false
        } else {
            onSave(ManualRecordDraft.Bottle(parsed, kind))
        }
    }
}

@Composable
private fun PumpingForm(onSave: (ManualRecordDraft) -> Boolean) {
    var side by remember { mutableStateOf("left") }
    var amount by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    ChoiceLabel(Res.string.pumping_side_label)
    ChoiceChips(
        choices = listOf("left" to Res.string.left, "right" to Res.string.right),
        selected = side,
        onSelected = { side = it },
    )
    NumberField(amount, { amount = it; error = null }, Res.string.pumping_amount_label, error)
    NumberField(duration, { duration = it; error = null }, Res.string.pumping_duration_label, null)
    SaveButton {
        val parsedAmount = amount.toIntOrNull()
        val parsedDuration = duration.toIntOrNull()
        if ((amount.isNotBlank() && parsedAmount == null) ||
            (duration.isNotBlank() && parsedDuration == null) ||
            (parsedAmount == null && parsedDuration == null) ||
            (parsedAmount != null && parsedAmount !in 1..1_000) ||
            (parsedDuration != null && parsedDuration !in 1..720)
        ) {
            error = Res.string.invalid_pumping
            false
        } else {
            onSave(ManualRecordDraft.Pumping(side, parsedAmount, parsedDuration))
        }
    }
}

@Composable
private fun DiaperForm(onSave: (ManualRecordDraft) -> Boolean) {
    var contents by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<StringResource?>(null) }
    ChoiceLabel(Res.string.diaper_contents_label)
    ChoiceChips(
        choices = listOf(
            "wet" to Res.string.diaper_wet,
            "dirty" to Res.string.diaper_dirty,
            "both" to Res.string.diaper_both,
        ),
        selected = contents,
        onSelected = { contents = it; error = null },
    )
    if (error != null) FieldError(requireNotNull(error))
    ChoiceLabel(Res.string.diaper_amount_label)
    ChoiceChips(
        choices = listOf(
            "s" to Res.string.amount_small,
            "m" to Res.string.amount_medium,
            "l" to Res.string.amount_large,
        ),
        selected = amount,
        onSelected = { amount = if (amount == it) null else it },
    )
    SaveButton {
        when (contents) {
            "wet" -> onSave(ManualRecordDraft.Diaper(pee = true, poop = false, amount))
            "dirty" -> onSave(ManualRecordDraft.Diaper(pee = false, poop = true, amount))
            "both" -> onSave(ManualRecordDraft.Diaper(pee = true, poop = true, amount))
            else -> {
                error = Res.string.invalid_diaper
                false
            }
        }
    }
}

@Composable
private fun SleepForm(onSave: (ManualRecordDraft) -> Boolean) {
    var duration by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    NumberField(duration, { duration = it; error = null }, Res.string.sleep_duration_label, error)
    SaveButton {
        val parsed = duration.toIntOrNull()
        if (parsed == null || parsed !in 1..1_440) {
            error = Res.string.invalid_sleep_duration
            false
        } else {
            onSave(ManualRecordDraft.Sleep(parsed))
        }
    }
}

@Composable
private fun TemperatureForm(onSave: (ManualRecordDraft) -> Boolean) {
    var temperature by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    NumberField(
        value = temperature,
        onValueChange = { temperature = it; error = null },
        label = Res.string.temperature_label,
        error = error,
        decimal = true,
    )
    SaveButton {
        val parsed = temperature.toDoubleOrNull()
        if (parsed == null || parsed !in 30.0..45.0) {
            error = Res.string.invalid_temperature
            false
        } else {
            onSave(ManualRecordDraft.Temperature(parsed))
        }
    }
}

@Composable
private fun MedicineForm(onSave: (ManualRecordDraft) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    TextEntryField(name, { name = it; error = null }, Res.string.medicine_name_label, error)
    TextEntryField(note, { note = it; error = null }, Res.string.medicine_note_label, null)
    SaveButton {
        if (name.isBlank() && note.isBlank()) {
            error = Res.string.field_required
            false
        } else {
            onSave(ManualRecordDraft.Medicine(name.ifBlank { null }, note.ifBlank { null }))
        }
    }
}

@Composable
private fun BathForm(onSave: (ManualRecordDraft) -> Boolean) {
    Text(
        text = stringResource(Res.string.record_bath_prompt),
        style = MaterialTheme.typography.bodyLarge,
        color = LocalLinenColors.current.ink,
    )
    SaveButton { onSave(ManualRecordDraft.Bath) }
}

@Composable
private fun MemoForm(onSave: (ManualRecordDraft) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.take(2_000); error = null },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.memo_label)) },
        placeholder = { Text(stringResource(Res.string.memo_placeholder)) },
        minLines = 3,
        isError = error != null,
        supportingText = error?.let { resource -> ({ FieldError(resource) }) },
    )
    SaveButton {
        if (text.isBlank()) {
            error = Res.string.field_required
            false
        } else {
            onSave(ManualRecordDraft.Memo(text.trim()))
        }
    }
}

@Composable
private fun GrowthForm(onSave: (ManualRecordDraft) -> Boolean) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    NumberField(weight, { weight = it; error = null }, Res.string.growth_weight_label, error)
    NumberField(height, { height = it; error = null }, Res.string.growth_height_label, null, decimal = true)
    SaveButton {
        val parsedWeight = weight.toIntOrNull()
        val parsedHeightMm = height.toDoubleOrNull()?.times(10)?.roundToInt()
        if ((weight.isNotBlank() && parsedWeight == null) ||
            (height.isNotBlank() && parsedHeightMm == null) ||
            (parsedWeight == null && parsedHeightMm == null) ||
            (parsedWeight != null && parsedWeight !in 100..100_000) ||
            (parsedHeightMm != null && parsedHeightMm !in 100..2_500)
        ) {
            error = Res.string.invalid_growth
            false
        } else {
            onSave(ManualRecordDraft.Growth(parsedWeight, parsedHeightMm))
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    error: StringResource?,
    decimal: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            onValueChange(next.filter { it.isDigit() || (decimal && it == '.') })
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { resource -> ({ FieldError(resource) }) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

@Composable
private fun TextEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    error: StringResource?,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { resource -> ({ FieldError(resource) }) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

@Composable
private fun ChoiceLabel(resource: StringResource) {
    Text(
        text = stringResource(resource),
        style = MaterialTheme.typography.labelLarge,
        color = LocalLinenColors.current.ink,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun ChoiceChips(
    choices: List<Pair<String, StringResource>>,
    selected: String?,
    onSelected: (String) -> Unit,
) {
    val colors = LocalLinenColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { (value, labelResource) ->
            val label = stringResource(labelResource)
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = LinenDimens.touchTarget),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.surface,
                    labelColor = colors.ink,
                    selectedContainerColor = colors.sageSoft,
                    selectedLabelColor = colors.ink,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == value,
                    borderColor = colors.divider,
                    selectedBorderColor = colors.sageStrong,
                ),
            )
        }
    }
}

@Composable
private fun SaveButton(onClick: () -> Boolean) {
    val colors = LocalLinenColors.current
    val focusManager = LocalFocusManager.current
    Button(
        onClick = {
            focusManager.clearFocus()
            onClick()
        },
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(LinenDimens.buttonHeight),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.sageStrong,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(stringResource(Res.string.save_record), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FieldError(resource: StringResource) {
    Text(
        text = stringResource(resource),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalLinenColors.current.terracottaStrong,
    )
}

@Composable
private fun recordKindLabel(kind: RecordKind): String = stringResource(
    when (kind) {
        RecordKind.NURSING -> Res.string.event_nursing
        RecordKind.BOTTLE -> Res.string.event_bottle
        RecordKind.PUMPING -> Res.string.event_pumping
        RecordKind.DIAPER -> Res.string.event_diaper
        RecordKind.SLEEP -> Res.string.event_sleep
        RecordKind.TEMPERATURE -> Res.string.event_temperature
        RecordKind.MEDICINE -> Res.string.event_medicine
        RecordKind.BATH -> Res.string.event_bath
        RecordKind.MEMO -> Res.string.event_memo
        RecordKind.GROWTH -> Res.string.event_growth
    },
)
