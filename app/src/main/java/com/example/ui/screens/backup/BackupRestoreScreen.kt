package com.example.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupPayload
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackableDivider
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.ClassicRed
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Export selection. "All app data" is a superset: picking it selects and
    // locks the other sections.
    var exportInventory by remember { mutableStateOf(true) }
    var exportDiet by remember { mutableStateOf(true) }
    var exportWeight by remember { mutableStateOf(false) }
    var exportAll by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.finishExport(context.contentResolver, uri)
        } else {
            viewModel.cancelExport()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.prepareImport(context.contentResolver, uri)
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }

    val anythingSelected = exportAll || exportInventory || exportDiet || exportWeight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .testTag("backup_restore_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Export your data as a JSON file, or restore from a backup you saved earlier.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            if (isBusy) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ClassicRed,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Working…",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Export",
                    supportingText = "Choose what to include in the file.",
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ExportOptionRow(
                        label = "Inventory",
                        description = "Every food you have saved",
                        checked = exportInventory,
                        enabled = !exportAll,
                        modifier = Modifier.testTag("backup_export_inventory"),
                        onCheckedChange = { exportInventory = it }
                    )
                    ExportOptionRow(
                        label = "Diet history",
                        description = "Meal logs and water intake",
                        checked = exportDiet,
                        enabled = !exportAll,
                        modifier = Modifier.testTag("backup_export_diet"),
                        onCheckedChange = { exportDiet = it }
                    )
                    ExportOptionRow(
                        label = "Weight history",
                        description = "All recorded weigh-ins",
                        checked = exportWeight,
                        enabled = !exportAll,
                        modifier = Modifier.testTag("backup_export_weight"),
                        onCheckedChange = { exportWeight = it }
                    )
                    ExportOptionRow(
                        label = "All app data",
                        description = "Everything above, plus profile, goals and settings",
                        checked = exportAll,
                        enabled = true,
                        modifier = Modifier.testTag("backup_export_all"),
                        onCheckedChange = { checked ->
                            exportAll = checked
                            if (checked) {
                                exportInventory = true
                                exportDiet = true
                                exportWeight = true
                            }
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.prepareExport(
                            includeInventory = exportInventory,
                            includeDiet = exportDiet,
                            includeWeightHistory = exportWeight,
                            includeAllAppData = exportAll
                        ) { fileName ->
                            createDocumentLauncher.launch(fileName)
                        }
                    },
                    enabled = anythingSelected && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 4.dp)
                        .testTag("backup_export_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClassicRed,
                        contentColor = Color.White,
                        disabledContainerColor = SurfaceCardHigh,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export backup", style = MaterialTheme.typography.titleSmall)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "All app data includes your saved API keys and personal details. Keep the file somewhere private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber.copy(alpha = 0.9f)
                    )
                }
                TrackableDivider()
            }

            item {
                SectionHeader(
                    title = "Import",
                    supportingText = "Restore inventory, logs, weight history or everything.",
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_import_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isBusy) {
                                openDocumentLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "application/octet-stream",
                                        "text/plain"
                                    )
                                )
                            }
                            .padding(vertical = 12.dp)
                            .testTag("backup_import_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = ClassicRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Choose a backup file",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "You will see what it contains before restoring.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Browse",
                            style = MaterialTheme.typography.labelMedium,
                            color = ClassicRed
                        )
                    }
                    TrackableDivider()
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.55f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Backups stay on your device unless you share them.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.7f)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message?.isError == true) Color(0xFF3A1618) else SurfaceCardHigh
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(if (message?.isError == true) ErrorRed else SurfaceBorder)
                )
            ) {
                Text(
                    text = data.visuals.message,
                    color = if (message?.isError == true) ErrorRed else TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }

    pendingImport?.let { payload ->
        ImportSelectionDialog(
            payload = payload,
            onConfirm = { selection -> viewModel.confirmImport(selection) },
            onDismiss = viewModel::cancelImport
        )
    }
}

/** One export option: a checkbox with a label and a quiet explanation. */
@Composable
private fun ExportOptionRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = ClassicRed,
                uncheckedColor = SurfaceBorder,
                disabledCheckedColor = SurfaceBorder,
                disabledUncheckedColor = SurfaceBorder.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) TextPrimary else TextMuted
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

/**
 * After a backup file is chosen, the user sees exactly how much data it holds
 * and picks the parts they want to restore.
 */
@Composable
private fun ImportSelectionDialog(
    payload: BackupPayload,
    onConfirm: (RestoreSelection) -> Unit,
    onDismiss: () -> Unit
) {
    var inventory by remember(payload) { mutableStateOf(payload.foods != null) }
    var diet by remember(payload) { mutableStateOf(payload.foodLogs != null || payload.waterLogs != null) }
    var weight by remember(payload) { mutableStateOf(payload.weightLogs != null) }
    var profile by remember(payload) { mutableStateOf(payload.goals != null) }
    var settings by remember(payload) { mutableStateOf(payload.settings != null) }

    val foodCount = payload.foods?.size ?: 0
    val mealCount = payload.foodLogs?.size ?: 0
    val waterCount = payload.waterLogs?.size ?: 0
    val weightCount = payload.weightLogs?.size ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Restore backup",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "This file contains:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (payload.foods != null) {
                    ImportOptionRow(
                        label = "Inventory",
                        detail = "$foodCount food${if (foodCount == 1) "" else "s"}",
                        checked = inventory,
                        onCheckedChange = { inventory = it }
                    )
                }
                if (payload.foodLogs != null || payload.waterLogs != null) {
                    ImportOptionRow(
                        label = "Diet history",
                        detail = "$mealCount meal${if (mealCount == 1) "" else "s"} · $waterCount water entr${if (waterCount == 1) "y" else "ies"}",
                        checked = diet,
                        onCheckedChange = { diet = it }
                    )
                }
                if (payload.weightLogs != null) {
                    ImportOptionRow(
                        label = "Weight history",
                        detail = "$weightCount entr${if (weightCount == 1) "y" else "ies"}",
                        checked = weight,
                        onCheckedChange = { weight = it }
                    )
                }
                if (payload.goals != null) {
                    ImportOptionRow(
                        label = "Profile & goals",
                        detail = "Personal details and targets",
                        checked = profile,
                        onCheckedChange = { profile = it }
                    )
                }
                if (payload.settings != null) {
                    ImportOptionRow(
                        label = "App settings",
                        detail = "Includes saved API keys",
                        checked = settings,
                        onCheckedChange = { settings = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Selected data will replace what the app already has. This can't be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        RestoreSelection(
                            inventory = inventory,
                            diet = diet,
                            weightHistory = weight,
                            profile = profile,
                            settings = settings
                        )
                    )
                },
                enabled = inventory || diet || weight || profile || settings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClassicRed,
                    contentColor = Color.White,
                    disabledContainerColor = SurfaceCardHigh,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Import selected", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun ImportOptionRow(
    label: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ClassicRed,
                uncheckedColor = SurfaceBorder
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
