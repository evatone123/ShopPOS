package com.example.ui.dashboard

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
import java.util.Calendar

data class DashboardUiState(
    val todayRevenuePesewas: Long = 0L,
    val todayTransactionsCount: Int = 0,
    val todayItemsSoldCount: Int = 0,
    val totalActiveProductsCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val recentSales: List<SaleWithItems> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isDemoLoading: Boolean = false,
    val message: String? = null
)

private data class TodaySalesMetrics(
    val revenue: Long,
    val transactions: Int,
    val itemsSold: Int
)

private data class InventoryMetrics(
    val totalProducts: Int,
    val lowStock: Int,
    val outOfStock: Int
)

private data class DashboardLocalState(
    val isDemoLoading: Boolean = false,
    val message: String? = null
)

class DashboardViewModel(private val repository: PosRepository) : ViewModel() {

    private val localState = MutableStateFlow(DashboardLocalState())

    private fun getTodayTimestamps(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private val todayRange = getTodayTimestamps()
    private val startToday = todayRange.first
    private val endToday = todayRange.second

    private val todaySalesFlow = combine(
        repository.getTotalRevenueFlow(startToday, endToday),
        repository.getSalesCountFlow(startToday, endToday),
        repository.getItemsSoldCountFlow(startToday, endToday)
    ) { revenue, transactions, itemsSold ->
        TodaySalesMetrics(revenue, transactions, itemsSold)
    }

    private val inventoryMetricsFlow = combine(
        repository.totalActiveProductsCountFlow,
        repository.lowStockCountFlow,
        repository.outOfStockCountFlow
    ) { totalProds, lowStock, outOfStock ->
        InventoryMetrics(totalProds, lowStock, outOfStock)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        todaySalesFlow,
        inventoryMetricsFlow,
        repository.recentSalesFlow,
        repository.appSettingsFlow,
        localState
    ) { today, inv, recentSales, settings, local ->
        DashboardUiState(
            todayRevenuePesewas = today.revenue,
            todayTransactionsCount = today.transactions,
            todayItemsSoldCount = today.itemsSold,
            totalActiveProductsCount = inv.totalProducts,
            lowStockCount = inv.lowStock,
            outOfStockCount = inv.outOfStock,
            recentSales = recentSales,
            settings = settings ?: AppSettings(),
            isDemoLoading = local.isDemoLoading,
            message = local.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun loadDemoData() {
        viewModelScope.launch {
            localState.value = localState.value.copy(isDemoLoading = true)
            try {
                repository.loadDemoData()
                localState.value = DashboardLocalState(isDemoLoading = false, message = "Demo store data loaded successfully!")
            } catch (e: Exception) {
                localState.value = DashboardLocalState(isDemoLoading = false, message = "Error loading demo data: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        localState.value = localState.value.copy(message = null)
    }
}
