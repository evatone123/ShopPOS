package com.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.data.entity.SaleWithItems
import com.example.data.repository.CartItem
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val discountPesewas: Long = 0L,
    val settings: AppSettings = AppSettings(),
    val isCheckingOut: Boolean = false,
    val completedSale: SaleWithItems? = null,
    val barcodeNotFoundDialog: String? = null,
    val error: String? = null
) {
    val subtotalPesewas: Long
        get() = cartItems.sumOf { it.subtotalPesewas }

    val totalPesewas: Long
        get() = (subtotalPesewas - discountPesewas).coerceAtLeast(0L)

    val totalItemCount: Int
        get() = cartItems.sumOf { it.quantity }
}

private data class PosLocalState(
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val cartItems: List<CartItem> = emptyList(),
    val discountPesewas: Long = 0L,
    val isCheckingOut: Boolean = false,
    val completedSale: SaleWithItems? = null,
    val barcodeNotFoundDialog: String? = null,
    val errorMessage: String? = null
)

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val _localState = MutableStateFlow(PosLocalState())

    val uiState: StateFlow<PosUiState> = combine(
        repository.activeProductsFlow,
        repository.allCategoriesFlow,
        repository.appSettingsFlow,
        _localState
    ) { activeProds, cats, settings, local ->
        val filtered = activeProds.filter { prod ->
            val matchesCategory = local.selectedCategoryId == null || prod.categoryId == local.selectedCategoryId
            val matchesQuery = local.searchQuery.isBlank() ||
                    prod.name.contains(local.searchQuery, ignoreCase = true) ||
                    prod.sku.contains(local.searchQuery, ignoreCase = true) ||
                    (prod.barcode != null && prod.barcode.contains(local.searchQuery, ignoreCase = true))
            matchesCategory && matchesQuery
        }

        PosUiState(
            products = activeProds,
            filteredProducts = filtered,
            categories = cats,
            selectedCategoryId = local.selectedCategoryId,
            searchQuery = local.searchQuery,
            cartItems = local.cartItems,
            discountPesewas = local.discountPesewas,
            settings = settings ?: AppSettings(),
            isCheckingOut = local.isCheckingOut,
            completedSale = local.completedSale,
            barcodeNotFoundDialog = local.barcodeNotFoundDialog,
            error = local.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _localState.value = _localState.value.copy(searchQuery = query)
    }

    fun onCategorySelected(categoryId: Long?) {
        val newCat = if (_localState.value.selectedCategoryId == categoryId) null else categoryId
        _localState.value = _localState.value.copy(selectedCategoryId = newCat)
    }

    fun addToCart(product: Product) {
        if (product.stockQuantity <= 0) {
            _localState.value = _localState.value.copy(errorMessage = "Cannot add '${product.name}'. Out of stock.")
            return
        }

        val currentList = _localState.value.cartItems.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index >= 0) {
            val existing = currentList[index]
            if (existing.quantity + 1 > product.stockQuantity) {
                _localState.value = _localState.value.copy(errorMessage = "Cannot add more. Available stock is only ${product.stockQuantity}.")
                return
            }
            currentList[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            currentList.add(CartItem(product = product, quantity = 1))
        }
        _localState.value = _localState.value.copy(cartItems = currentList)
    }

    fun increaseQuantity(productId: Long) {
        val currentList = _localState.value.cartItems.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity + 1 > item.product.stockQuantity) {
                _localState.value = _localState.value.copy(errorMessage = "Cannot exceed available stock of ${item.product.stockQuantity}.")
                return
            }
            currentList[index] = item.copy(quantity = item.quantity + 1)
            _localState.value = _localState.value.copy(cartItems = currentList)
        }
    }

    fun decreaseQuantity(productId: Long) {
        val currentList = _localState.value.cartItems.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _localState.value = _localState.value.copy(cartItems = currentList)
        }
    }

    fun removeFromCart(productId: Long) {
        val updated = _localState.value.cartItems.filterNot { it.product.id == productId }
        _localState.value = _localState.value.copy(cartItems = updated)
    }

    fun clearCart() {
        _localState.value = _localState.value.copy(cartItems = emptyList(), discountPesewas = 0L)
    }

    fun applyDiscountPesewas(amount: Long) {
        _localState.value = _localState.value.copy(discountPesewas = amount.coerceAtLeast(0L))
    }

    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            val foundProduct = repository.findProductByBarcode(barcode)
                ?: repository.findProductBySku(barcode)

            if (foundProduct != null) {
                addToCart(foundProduct)
            } else {
                _localState.value = _localState.value.copy(barcodeNotFoundDialog = barcode)
            }
        }
    }

    fun dismissBarcodeNotFound() {
        _localState.value = _localState.value.copy(barcodeNotFoundDialog = null)
    }

    fun startCheckout() {
        if (_localState.value.cartItems.isEmpty()) {
            _localState.value = _localState.value.copy(errorMessage = "Cart is empty")
            return
        }
        _localState.value = _localState.value.copy(isCheckingOut = true)
    }

    fun dismissCheckout() {
        _localState.value = _localState.value.copy(isCheckingOut = false)
    }

    fun completeSale(
        paymentMethod: String,
        amountReceivedPesewas: Long,
        paymentReference: String? = null
    ) {
        val currentState = uiState.value
        val total = currentState.totalPesewas
        val change = (amountReceivedPesewas - total).coerceAtLeast(0L)

        viewModelScope.launch {
            val result = repository.completeSale(
                cartItems = currentState.cartItems,
                subtotalPesewas = currentState.subtotalPesewas,
                discountPesewas = currentState.discountPesewas,
                totalPesewas = total,
                paymentMethod = paymentMethod,
                amountReceivedPesewas = amountReceivedPesewas,
                changeAmountPesewas = change,
                cashierName = "Cashier",
                paymentReference = paymentReference
            )

            result.onSuccess { saleWithItems ->
                _localState.value = _localState.value.copy(
                    cartItems = emptyList(),
                    discountPesewas = 0L,
                    isCheckingOut = false,
                    completedSale = saleWithItems
                )
            }.onFailure { err ->
                _localState.value = _localState.value.copy(
                    errorMessage = err.message ?: "Failed to complete sale"
                )
            }
        }
    }

    fun dismissReceipt() {
        _localState.value = _localState.value.copy(completedSale = null)
    }

    fun clearErrorMessage() {
        _localState.value = _localState.value.copy(errorMessage = null)
    }
}
