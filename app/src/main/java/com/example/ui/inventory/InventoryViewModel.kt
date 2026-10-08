package com.example.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Product
import com.example.data.entity.StockStatus
import com.example.data.repository.PosRepository
import com.example.data.repository.StockReceivingItem
import com.example.data.repository.StockReceivingResult
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
    val categories: List<Category> = emptyList(),
    val movements: List<InventoryMovement> = emptyList(),
    val currentFilter: InventoryFilter = InventoryFilter.ALL,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val settings: AppSettings = AppSettings(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    // Receiving session states
    val receivingManifest: List<StockReceivingItem> = emptyList(),
    val receivingReference: String = "",
    val receivingSupplier: String = "",
    val receivingNotes: String = "",
    val isReceivingInProgress: Boolean = false,
    val lastReceivingResult: StockReceivingResult? = null,
    val isBulkPickerOpen: Boolean = false,
    val isScannerOpen: Boolean = false,
    val isNewProductDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class InventoryLocalState(
    val currentFilter: InventoryFilter = InventoryFilter.ALL,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val receivingManifest: List<StockReceivingItem> = emptyList(),
    val receivingReference: String = "",
    val receivingSupplier: String = "",
    val receivingNotes: String = "",
    val isReceivingInProgress: Boolean = false,
    val lastReceivingResult: StockReceivingResult? = null,
    val isBulkPickerOpen: Boolean = false,
    val isScannerOpen: Boolean = false,
    val isNewProductDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class InventoryViewModel(private val repository: PosRepository) : ViewModel() {

    private val _localState = MutableStateFlow(InventoryLocalState())

    init {
        generateNewReceivingReference()
    }

    private val stockCountsFlow = combine(
        repository.lowStockCountFlow,
        repository.outOfStockCountFlow
    ) { low, out ->
        Pair(low, out)
    }

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.allProductsFlow,
        repository.allCategoriesFlow,
        repository.allInventoryMovementsFlow,
        repository.appSettingsFlow,
        combine(stockCountsFlow, _localState) { counts, local -> Pair(counts, local) }
    ) { products, categories, movements, settings, (counts, local) ->
        val activeProducts = products.filter { it.active }
        val filtered = activeProducts.filter { prod ->
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
            products = activeProducts,
            filteredProducts = filtered,
            categories = categories,
            movements = movements,
            currentFilter = local.currentFilter,
            searchQuery = local.searchQuery,
            selectedTab = local.selectedTab,
            settings = settings ?: AppSettings(),
            lowStockCount = counts.first,
            outOfStockCount = counts.second,
            receivingManifest = local.receivingManifest,
            receivingReference = local.receivingReference,
            receivingSupplier = local.receivingSupplier,
            receivingNotes = local.receivingNotes,
            isReceivingInProgress = local.isReceivingInProgress,
            lastReceivingResult = local.lastReceivingResult,
            isBulkPickerOpen = local.isBulkPickerOpen,
            isScannerOpen = local.isScannerOpen,
            isNewProductDialogOpen = local.isNewProductDialogOpen,
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

    // --- Stock Adjustments (Single product) ---
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

    // --- Bulk Stock Receiving Workflow ---
    fun generateNewReceivingReference() {
        viewModelScope.launch {
            val ref = repository.generateNextReceivingReference()
            _localState.value = _localState.value.copy(receivingReference = ref)
        }
    }

    fun onReceivingReferenceChanged(ref: String) {
        _localState.value = _localState.value.copy(receivingReference = ref)
    }

    fun onReceivingSupplierChanged(supplier: String) {
        _localState.value = _localState.value.copy(receivingSupplier = supplier)
    }

    fun onReceivingNotesChanged(notes: String) {
        _localState.value = _localState.value.copy(receivingNotes = notes)
    }

    fun openBulkPicker() {
        _localState.value = _localState.value.copy(isBulkPickerOpen = true)
    }

    fun closeBulkPicker() {
        _localState.value = _localState.value.copy(isBulkPickerOpen = false)
    }

    fun openScanner() {
        _localState.value = _localState.value.copy(isScannerOpen = true)
    }

    fun closeScanner() {
        _localState.value = _localState.value.copy(isScannerOpen = false)
    }

    fun openNewProductDialog() {
        _localState.value = _localState.value.copy(isNewProductDialogOpen = true)
    }

    fun closeNewProductDialog() {
        _localState.value = _localState.value.copy(isNewProductDialogOpen = false)
    }

    fun addProductsToReceivingManifest(products: List<Product>, defaultQuantity: Int = 10) {
        val current = _localState.value.receivingManifest.toMutableList()
        var addedCount = 0

        for (product in products) {
            val existingIndex = current.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val existing = current[existingIndex]
                current[existingIndex] = existing.copy(quantityToAdd = existing.quantityToAdd + defaultQuantity)
            } else {
                current.add(
                    StockReceivingItem(
                        product = product,
                        quantityToAdd = defaultQuantity,
                        unitCostPesewas = product.costPricePesewas,
                        updateCatalogCost = false,
                        previousStock = product.stockQuantity
                    )
                )
                addedCount++
            }
        }

        _localState.value = _localState.value.copy(
            receivingManifest = current,
            isBulkPickerOpen = false,
            message = "Added ${products.size} product(s) to receiving manifest."
        )
    }

    fun quickAddLowStockToManifest(defaultQty: Int = 10) {
        val currentProducts = uiState.value.products
        val lowStockProducts = currentProducts.filter {
            it.getStockStatus() == StockStatus.LOW_STOCK || it.getStockStatus() == StockStatus.OUT_OF_STOCK
        }

        if (lowStockProducts.isEmpty()) {
            _localState.value = _localState.value.copy(message = "All products are currently well-stocked!")
            return
        }

        addProductsToReceivingManifest(lowStockProducts, defaultQty)
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val product = repository.findProductByBarcode(barcode)
            if (product != null) {
                val current = _localState.value.receivingManifest.toMutableList()
                val existingIndex = current.indexOfFirst { it.product.id == product.id }
                if (existingIndex >= 0) {
                    val existing = current[existingIndex]
                    current[existingIndex] = existing.copy(quantityToAdd = existing.quantityToAdd + 1)
                } else {
                    current.add(
                        StockReceivingItem(
                            product = product,
                            quantityToAdd = 1,
                            unitCostPesewas = product.costPricePesewas,
                            updateCatalogCost = false,
                            previousStock = product.stockQuantity
                        )
                    )
                }
                _localState.value = _localState.value.copy(
                    receivingManifest = current,
                    isScannerOpen = false,
                    message = "Added '${product.name}' to receiving manifest."
                )
            } else {
                _localState.value = _localState.value.copy(
                    isScannerOpen = false,
                    error = "No product found with barcode: $barcode"
                )
            }
        }
    }

    fun updateReceivingItemQuantity(productId: Long, quantity: Int) {
        val current = _localState.value.receivingManifest.map {
            if (it.product.id == productId) it.copy(quantityToAdd = quantity.coerceAtLeast(1)) else it
        }
        _localState.value = _localState.value.copy(receivingManifest = current)
    }

    fun updateReceivingItemCost(productId: Long, costPricePesewas: Long) {
        val current = _localState.value.receivingManifest.map {
            if (it.product.id == productId) it.copy(unitCostPesewas = costPricePesewas.coerceAtLeast(0L)) else it
        }
        _localState.value = _localState.value.copy(receivingManifest = current)
    }

    fun toggleUpdateCatalogCost(productId: Long) {
        val current = _localState.value.receivingManifest.map {
            if (it.product.id == productId) it.copy(updateCatalogCost = !it.updateCatalogCost) else it
        }
        _localState.value = _localState.value.copy(receivingManifest = current)
    }

    fun removeReceivingItem(productId: Long) {
        val current = _localState.value.receivingManifest.filterNot { it.product.id == productId }
        _localState.value = _localState.value.copy(receivingManifest = current)
    }

    fun setAllReceivingQuantities(quantity: Int) {
        val current = _localState.value.receivingManifest.map {
            it.copy(quantityToAdd = quantity.coerceAtLeast(1))
        }
        _localState.value = _localState.value.copy(
            receivingManifest = current,
            message = "All quantities updated to $quantity."
        )
    }

    fun clearReceivingManifest() {
        _localState.value = _localState.value.copy(
            receivingManifest = emptyList(),
            message = "Receiving manifest cleared."
        )
    }

    fun saveNewProductAndAddToReceiving(product: Product, quantityToAdd: Int = 10) {
        viewModelScope.launch {
            try {
                val newId = repository.saveProduct(product.copy(stockQuantity = 0), "Initial created during stock receiving")
                val savedProduct = repository.getProductById(newId) ?: product.copy(id = newId)

                val current = _localState.value.receivingManifest.toMutableList()
                current.add(
                    StockReceivingItem(
                        product = savedProduct,
                        quantityToAdd = quantityToAdd,
                        unitCostPesewas = savedProduct.costPricePesewas,
                        updateCatalogCost = false,
                        previousStock = 0
                    )
                )

                _localState.value = _localState.value.copy(
                    receivingManifest = current,
                    isNewProductDialogOpen = false,
                    message = "Created '${product.name}' and added to receiving manifest."
                )
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(
                    error = "Failed to create new product: ${e.message}"
                )
            }
        }
    }

    fun confirmAndReceiveStock() {
        val currentState = _localState.value
        val manifest = currentState.receivingManifest
        if (manifest.isEmpty()) {
            _localState.value = currentState.copy(error = "Receiving manifest is empty.")
            return
        }

        viewModelScope.launch {
            _localState.value = _localState.value.copy(isReceivingInProgress = true)
            val result = repository.receiveStockBatch(
                items = manifest,
                referenceNumber = if (currentState.receivingReference.isNotBlank()) currentState.receivingReference else repository.generateNextReceivingReference(),
                supplierName = currentState.receivingSupplier,
                notes = currentState.receivingNotes
            )

            result.onSuccess { receivingResult ->
                val nextRef = repository.generateNextReceivingReference()
                _localState.value = _localState.value.copy(
                    isReceivingInProgress = false,
                    receivingManifest = emptyList(),
                    receivingReference = nextRef,
                    receivingSupplier = "",
                    receivingNotes = "",
                    lastReceivingResult = receivingResult,
                    message = "Successfully received ${receivingResult.totalUnitsReceived} units across ${receivingResult.items.size} product(s)!"
                )
            }.onFailure { err ->
                _localState.value = _localState.value.copy(
                    isReceivingInProgress = false,
                    error = err.message ?: "Failed to receive stock batch."
                )
            }
        }
    }

    fun dismissReceivingSummary() {
        _localState.value = _localState.value.copy(lastReceivingResult = null)
    }

    fun clearFeedback() {
        _localState.value = _localState.value.copy(message = null, error = null)
    }
}
