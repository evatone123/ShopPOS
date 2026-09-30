package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.repository.PosRepository
import com.example.util.BackupHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SettingsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _isExporting = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.appSettingsFlow,
        _isExporting,
        _isImporting,
        _message,
        _error
    ) { settings, exporting, importing, msg, err ->
        SettingsUiState(
            settings = settings ?: AppSettings(),
            isExporting = exporting,
            isImporting = importing,
            message = msg,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            try {
                repository.updateAppSettings(settings)
                _message.value = "Settings saved successfully!"
            } catch (e: Exception) {
                _error.value = "Failed to update settings: ${e.message}"
            }
        }
    }

    fun exportBackup(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val json = BackupHelper.exportToJson(repository)
                val result = BackupHelper.writeToUri(context, destinationUri, json)
                result.onSuccess {
                    _message.value = "Backup successfully exported!"
                }.onFailure { err ->
                    _error.value = "Failed to write backup file: ${err.message}"
                }
            } catch (e: Exception) {
                _error.value = "Export error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importBackup(context: Context, sourceUri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val readResult = BackupHelper.readFromUri(context, sourceUri)
                readResult.onSuccess { jsonStr ->
                    val backupData = BackupHelper.parseJsonBackup(jsonStr)
                    repository.restoreData(
                        categories = backupData.categories,
                        products = backupData.products,
                        sales = backupData.sales,
                        saleItems = backupData.saleItems,
                        movements = backupData.movements,
                        payments = backupData.payments,
                        settings = backupData.settings,
                        replaceExisting = replaceExisting
                    )
                    _message.value = "Backup successfully restored (${backupData.products.size} products, ${backupData.sales.size} sales)!"
                }.onFailure { err ->
                    _error.value = "Failed to read backup file: ${err.message}"
                }
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            try {
                repository.loadDemoData()
                _message.value = "Demo Ghanaian store items loaded!"
            } catch (e: Exception) {
                _error.value = "Error loading demo data: ${e.message}"
            }
        }
    }

    fun clearFeedback() {
        _message.value = null
        _error.value = null
    }
}
