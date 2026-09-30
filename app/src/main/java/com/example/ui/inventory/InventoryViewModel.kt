package com.example.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Product
import com.example.data.entity.StockStatus
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InventoryFilter(val label: String) {
    ALL("All Products"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    IN_STOCK("In Stock")
}

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val movements: List<InventoryMovement> = emptyList(),
    val currentFilter: InventoryFilter = InventoryFilter.ALL,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val settings: AppSettings = AppSettings(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val message: String? = null,
    val error: String? = null
)

private data class InventoryLocalState(
    val currentFilter: InventoryFilter = InventoryFilter.ALL,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val message: String? = null,
    val error: String? = null
)

class InventoryViewModel(private val repository: PosRepository) : ViewModel() {

    private val _localState = MutableStateFlow(InventoryLocalState())

    private val stockCountsFlow = combine(
        repository.lowStockCountFlow,
        repository.outOfStockCountFlow
    ) { low, out ->
        Pair(low, out)
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.activeProductsFlow,
        repository.allInventoryMovementsFlow,
        repository.appSettingsFlow,
        stockCountsFlow,
        _localState
    ) { products, movements, settings, counts, local ->
        val filtered = products.filter { prod ->
            val status = prod.getStockStatus()
            val matchesFilter = when (local.currentFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> status == StockStatus.LOW_STOCK
                InventoryFilter.OUT_OF_STOCK -> status == StockStatus.OUT_OF_STOCK
                InventoryFilter.IN_STOCK -> status == StockStatus.IN_STOCK
            }
            val matchesQuery = local.searchQuery.isBlank() ||
                    prod.name.contains(local.searchQuery, ignoreCase = true) ||
                    prod.sku.contains(local.searchQuery, ignoreCase = true) ||
                    (prod.barcode != null && prod.barcode.contains(local.searchQuery, ignoreCase = true))

            matchesFilter && matchesQuery
        }

        InventoryUiState(
            products = products,
            filteredProducts = filtered,
            movements = movements,
            currentFilter = local.currentFilter,
            searchQuery = local.searchQuery,
            selectedTab = local.selectedTab,
            settings = settings ?: AppSettings(),
            lowStockCount = counts.first,
            outOfStockCount = counts.second,
            message = local.message,
            error = local.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _localState.value = _localState.value.copy(searchQuery = query)
    }

    fun onFilterChanged(filter: InventoryFilter) {
        _localState.value = _localState.value.copy(currentFilter = filter)
    }

    fun onTabChanged(tab: Int) {
        _localState.value = _localState.value.copy(selectedTab = tab)
    }

    fun adjustStock(productId: Long, type: String, quantity: Int, reason: String) {
        viewModelScope.launch {
            val result = repository.adjustStock(productId, type, quantity, reason)
            result.onSuccess { newStock ->
                _localState.value = _localState.value.copy(message = "Stock updated successfully! New stock level: $newStock")
            }.onFailure { err ->
                _localState.value = _localState.value.copy(error = err.message ?: "Failed to adjust stock.")
            }
        }
    }

    fun clearFeedback() {
        _localState.value = _localState.value.copy(message = null, error = null)
    }
}
