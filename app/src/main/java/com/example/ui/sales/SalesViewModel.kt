package com.example.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.entity.SaleWithItems
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SalesUiState(
    val sales: List<SaleWithItems> = emptyList(),
    val filteredSales: List<SaleWithItems> = emptyList(),
    val searchQuery: String = "",
    val selectedPaymentMethod: String = "ALL",
    val selectedStatus: String = "ALL",
    val settings: AppSettings = AppSettings(),
    val message: String? = null,
    val error: String? = null
)

private data class SalesLocalState(
    val searchQuery: String = "",
    val selectedPaymentMethod: String = "ALL",
    val selectedStatus: String = "ALL",
    val message: String? = null,
    val error: String? = null
)

class SalesViewModel(private val repository: PosRepository) : ViewModel() {

    private val _localState = MutableStateFlow(SalesLocalState())

    val uiState: StateFlow<SalesUiState> = combine(
        repository.allSalesWithItemsFlow,
        repository.appSettingsFlow,
        _localState
    ) { allSales, settings, local ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val filtered = allSales.filter { saleWithItems ->
            val sale = saleWithItems.sale
            val matchesMethod = local.selectedPaymentMethod == "ALL" || sale.paymentMethod.equals(local.selectedPaymentMethod, ignoreCase = true)
            val matchesStatus = local.selectedStatus == "ALL" || sale.status.equals(local.selectedStatus, ignoreCase = true)

            val dateStr = dateFormat.format(Date(sale.createdAt))
            val matchesQuery = local.searchQuery.isBlank() ||
                    sale.receiptNumber.contains(local.searchQuery, ignoreCase = true) ||
                    dateStr.contains(local.searchQuery, ignoreCase = true) ||
                    saleWithItems.items.any { it.productName.contains(local.searchQuery, ignoreCase = true) }

            matchesMethod && matchesStatus && matchesQuery
        }

        SalesUiState(
            sales = allSales,
            filteredSales = filtered,
            searchQuery = local.searchQuery,
            selectedPaymentMethod = local.selectedPaymentMethod,
            selectedStatus = local.selectedStatus,
            settings = settings ?: AppSettings(),
            message = local.message,
            error = local.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SalesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _localState.value = _localState.value.copy(searchQuery = query)
    }

    fun onPaymentMethodFilterChanged(method: String) {
        _localState.value = _localState.value.copy(selectedPaymentMethod = method)
    }

    fun onStatusFilterChanged(status: String) {
        _localState.value = _localState.value.copy(selectedStatus = status)
    }

    fun voidSale(saleId: Long, reason: String) {
        viewModelScope.launch {
            val result = repository.voidSale(saleId, reason)
            result.onSuccess {
                _localState.value = _localState.value.copy(
                    message = "Sale ${it.sale.receiptNumber} successfully voided and inventory restored!"
                )
            }.onFailure { err ->
                _localState.value = _localState.value.copy(
                    error = err.message ?: "Failed to void sale."
                )
            }
        }
    }

    fun clearFeedback() {
        _localState.value = _localState.value.copy(message = null, error = null)
    }
}
