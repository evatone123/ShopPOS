package com.example.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.BestSellerDto
import com.example.data.entity.AppSettings
import com.example.data.entity.Product
import com.example.data.entity.SaleWithItems
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class PaymentBreakdown(
    val method: String,
    val totalPesewas: Long,
    val count: Int,
    val percentage: Float
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.TODAY,
    val totalSalesPesewas: Long = 0L,
    val transactionsCount: Int = 0,
    val itemsSoldCount: Int = 0,
    val averageTransactionPesewas: Long = 0L,
    val paymentBreakdowns: List<PaymentBreakdown> = emptyList(),
    val bestSellers: List<BestSellerDto> = emptyList(),
    val products: List<Product> = emptyList(),
    val totalInventoryStockValueCostPesewas: Long = 0L,
    val totalInventoryStockValueRetailPesewas: Long = 0L,
    val settings: AppSettings = AppSettings()
)

class ReportsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _period = MutableStateFlow(ReportPeriod.TODAY)

    val uiState: StateFlow<ReportsUiState> = combine(
        _period,
        _period.flatMapLatest { p ->
            val (start, end) = getRangeForPeriod(p)
            repository.getSalesWithItemsBetweenFlow(start, end)
        },
        _period.flatMapLatest { p ->
            val (start, end) = getRangeForPeriod(p)
            repository.getBestSellersFlow(start, end, 10)
        },
        repository.activeProductsFlow,
        repository.appSettingsFlow
    ) { period, salesWithItems, bestSellers, activeProducts, settings ->
        val completedSales = salesWithItems.filter { it.sale.status == "COMPLETED" }

        val totalSales = completedSales.sumOf { it.sale.totalPesewas }
        val transCount = completedSales.size
        val itemsCount = completedSales.sumOf { swi -> swi.items.sumOf { it.quantity } }
        val avgTrans = if (transCount > 0) totalSales / transCount else 0L

        // Payment method breakdown
        val paymentGroups = completedSales.groupBy { it.sale.paymentMethod }
        val breakdowns = paymentGroups.map { (method, sales) ->
            val methodTotal = sales.sumOf { it.sale.totalPesewas }
            val pct = if (totalSales > 0) (methodTotal.toFloat() / totalSales) * 100f else 0f
            PaymentBreakdown(
                method = method,
                totalPesewas = methodTotal,
                count = sales.size,
                percentage = pct
            )
        }.sortedByDescending { it.totalPesewas }

        // Inventory values
        val costVal = activeProducts.sumOf { it.costPricePesewas * it.stockQuantity }
        val retailVal = activeProducts.sumOf { it.sellingPricePesewas * it.stockQuantity }

        ReportsUiState(
            period = period,
            totalSalesPesewas = totalSales,
            transactionsCount = transCount,
            itemsSoldCount = itemsCount,
            averageTransactionPesewas = avgTrans,
            paymentBreakdowns = breakdowns,
            bestSellers = bestSellers,
            products = activeProducts,
            totalInventoryStockValueCostPesewas = costVal,
            totalInventoryStockValueRetailPesewas = retailVal,
            settings = settings ?: AppSettings()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun onPeriodSelected(period: ReportPeriod) {
        _period.value = period
    }

    companion object {
        fun getRangeForPeriod(period: ReportPeriod): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            return when (period) {
                ReportPeriod.TODAY -> {
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
                    Pair(start, end)
                }
                ReportPeriod.YESTERDAY -> {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
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
                    Pair(start, end)
                }
                ReportPeriod.THIS_WEEK -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_WEEK, 6)
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                ReportPeriod.THIS_MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis
                    Pair(start, end)
                }
                ReportPeriod.ALL_TIME -> {
                    Pair(0L, Long.MAX_VALUE)
                }
            }
        }
    }
}
