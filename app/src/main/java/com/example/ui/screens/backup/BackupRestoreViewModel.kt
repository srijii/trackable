package com.example.ui.screens.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupJson
import com.example.data.backup.BackupPayload
import com.example.data.backup.BackupScope
import com.example.data.repository.NutritionRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BackupMessage(
    val text: String,
    val isError: Boolean = false
)

/** Which sections of a backup the user chose to restore. */
data class RestoreSelection(
    val inventory: Boolean = true,
    val diet: Boolean = true,
    val weightHistory: Boolean = true,
    val profile: Boolean = true,
    val settings: Boolean = true
)

class BackupRestoreViewModel(
    private val repository: NutritionRepository
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<BackupMessage?>(null)
    val message: StateFlow<BackupMessage?> = _message.asStateFlow()

    /** Non-null while waiting for the user to confirm replacing their data. */
    private val _pendingImport = MutableStateFlow<BackupPayload?>(null)
    val pendingImport: StateFlow<BackupPayload?> = _pendingImport.asStateFlow()

    private var pendingExportJson: String? = null

    /**
     * Builds the backup file contents on a background thread, then hands the
     * suggested file name back so the UI can open the system file picker.
     */
    fun prepareExport(
        includeInventory: Boolean,
        includeDiet: Boolean,
        includeWeightHistory: Boolean,
        includeAllAppData: Boolean,
        onReady: (fileName: String) -> Unit
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val payload = repository.buildBackupPayload(
                    includeInventory = includeInventory,
                    includeDiet = includeDiet,
                    includeWeightHistory = includeWeightHistory,
                    includeAllAppData = includeAllAppData
                )
                val json = withContext(Dispatchers.Default) { BackupJson.encode(payload) }
                pendingExportJson = json
                onReady(fileNameFor(payload.scope))
            } catch (t: Throwable) {
                _message.value = BackupMessage(
                    t.message ?: "Couldn't create the backup file.",
                    isError = true
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    /** Writes the prepared backup to the file the user chose. */
    fun finishExport(resolver: ContentResolver, uri: Uri) {
        val json = pendingExportJson
        pendingExportJson = null
        if (json == null) {
            _message.value = BackupMessage("The backup was cancelled.", isError = true)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Couldn't open the selected file for writing.")
                }
                _message.value = BackupMessage("Backup saved successfully.")
            } catch (t: Throwable) {
                _message.value = BackupMessage(
                    t.message ?: "Couldn't save the backup file.",
                    isError = true
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelExport() {
        pendingExportJson = null
    }

    /**
     * Reads and validates a chosen backup file. The user then picks which
     * sections to restore before anything is applied.
     */
    fun prepareImport(resolver: ContentResolver, uri: Uri) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("Couldn't read the selected file.")
                }
                val payload = withContext(Dispatchers.Default) { BackupJson.decode(text) }
                _pendingImport.value = payload
            } catch (t: Throwable) {
                val friendly = when (t) {
                    is org.json.JSONException -> "That file isn't valid JSON, so it can't be used as a backup."
                    is IllegalArgumentException -> t.message ?: "That file isn't a valid Trackable backup."
                    else -> t.message ?: "Couldn't import this backup file."
                }
                _message.value = BackupMessage(friendly, isError = true)
            } finally {
                _isBusy.value = false
            }
        }
    }

    /** Applies the selected sections of the backup the user confirmed. */
    fun confirmImport(selection: RestoreSelection) {
        val payload = _pendingImport.value ?: return
        _pendingImport.value = null

        val filtered = payload.copy(
            foods = payload.foods?.takeIf { selection.inventory },
            foodLogs = payload.foodLogs?.takeIf { selection.diet },
            waterLogs = payload.waterLogs?.takeIf { selection.diet },
            weightLogs = payload.weightLogs?.takeIf { selection.weightHistory },
            goals = payload.goals?.takeIf { selection.profile },
            settings = payload.settings?.takeIf { selection.settings }
        )
        if (filtered.isEmpty()) {
            _message.value = BackupMessage("Nothing selected to restore.", isError = true)
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            try {
                repository.restoreBackupPayload(filtered)
                _message.value = BackupMessage(restoredMessage(filtered))
            } catch (t: Throwable) {
                _message.value = BackupMessage(
                    t.message ?: "Couldn't restore this backup.",
                    isError = true
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun restoredMessage(payload: BackupPayload): String {
        val parts = buildList {
            if (payload.foods != null) add("inventory")
            if (payload.foodLogs != null || payload.waterLogs != null) add("diet")
            if (payload.weightLogs != null) add("weight history")
            if (payload.goals != null) add("profile")
            if (payload.settings != null) add("settings")
        }
        return if (parts.isEmpty()) "Backup restored." else "Restored ${parts.joinToString(", ")}."
    }

    private fun fileNameFor(scope: BackupScope): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "trackable_${scope.fileNamePart}_$stamp.json"
    }
}
