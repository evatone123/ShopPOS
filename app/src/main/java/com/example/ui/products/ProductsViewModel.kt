package com.example.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    PRICE_LOW_HIGH("Price (Low-High)"),
    PRICE_HIGH_LOW("Price (High-Low)"),
    STOCK_LOW_HIGH("Stock (Lowest first)"),
    STOCK_HIGH_LOW("Stock (Highest first)")
}

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val sortOption: ProductSortOption = ProductSortOption.NAME_ASC,
    val showInactive: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isCategoryDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class ProductsLocalState(
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val sortOption: ProductSortOption = ProductSortOption.NAME_ASC,
    val showInactive: Boolean = false,
    val isCategoryDialogOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class ProductsViewModel(private val repository: PosRepository) : ViewModel() {

    private val _localState = MutableStateFlow(ProductsLocalState())

    val uiState: StateFlow<ProductsUiState> = combine(
        repository.allProductsFlow,
        repository.allCategoriesFlow,
        repository.appSettingsFlow,
        _localState
    ) { allProds, cats, settings, local ->
        var list = if (local.showInactive) allProds else allProds.filter { it.active }

        if (local.selectedCategoryId != null) {
            list = list.filter { it.categoryId == local.selectedCategoryId }
        }

        if (local.searchQuery.isNotBlank()) {
            list = list.filter { prod ->
                prod.name.contains(local.searchQuery, ignoreCase = true) ||
                        prod.sku.contains(local.searchQuery, ignoreCase = true) ||
                        (prod.barcode != null && prod.barcode.contains(local.searchQuery, ignoreCase = true))
            }
        }

        val sortedList = when (local.sortOption) {
            ProductSortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            ProductSortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            ProductSortOption.PRICE_LOW_HIGH -> list.sortedBy { it.sellingPricePesewas }
            ProductSortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.sellingPricePesewas }
            ProductSortOption.STOCK_LOW_HIGH -> list.sortedBy { it.stockQuantity }
            ProductSortOption.STOCK_HIGH_LOW -> list.sortedByDescending { it.stockQuantity }
        }

        ProductsUiState(
            products = allProds,
            filteredProducts = sortedList,
            categories = cats,
            selectedCategoryId = local.selectedCategoryId,
            searchQuery = local.searchQuery,
            sortOption = local.sortOption,
            showInactive = local.showInactive,
            settings = settings ?: AppSettings(),
            isCategoryDialogOpen = local.isCategoryDialogOpen,
            message = local.message,
            error = local.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _localState.value = _localState.value.copy(searchQuery = query)
    }

    fun onCategorySelected(categoryId: Long?) {
        val newCat = if (_localState.value.selectedCategoryId == categoryId) null else categoryId
        _localState.value = _localState.value.copy(selectedCategoryId = newCat)
    }

    fun onSortOptionChanged(option: ProductSortOption) {
        _localState.value = _localState.value.copy(sortOption = option)
    }

    fun toggleShowInactive() {
        _localState.value = _localState.value.copy(showInactive = !_localState.value.showInactive)
    }

    fun openCategoryDialog() {
        _localState.value = _localState.value.copy(isCategoryDialogOpen = true)
    }

    fun closeCategoryDialog() {
        _localState.value = _localState.value.copy(isCategoryDialogOpen = false)
    }

    fun saveProduct(product: Product, initialStockReason: String? = null) {
        viewModelScope.launch {
            try {
                repository.saveProduct(product, initialStockReason)
                _localState.value = _localState.value.copy(message = "Product '${product.name}' saved successfully!")
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(error = "Failed to save product: ${e.message}")
            }
        }
    }

    fun toggleProductActive(product: Product) {
        viewModelScope.launch {
            try {
                repository.updateProduct(product.copy(active = !product.active, updatedAt = System.currentTimeMillis()))
                val statusStr = if (!product.active) "activated" else "deactivated"
                _localState.value = _localState.value.copy(message = "Product '${product.name}' $statusStr.")
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(error = "Failed to update product: ${e.message}")
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.deleteProduct(product)
            result.onSuccess {
                _localState.value = _localState.value.copy(message = "Product '${product.name}' deleted.")
            }.onFailure { err ->
                _localState.value = _localState.value.copy(error = err.message ?: "Failed to delete product.")
            }
        }
    }

    fun addCategory(name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.saveCategory(Category(name = name.trim(), description = description?.trim()))
                _localState.value = _localState.value.copy(message = "Category '$name' created.")
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(error = "Failed to add category: ${e.message}")
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.updateCategory(category)
                _localState.value = _localState.value.copy(message = "Category updated.")
            } catch (e: Exception) {
                _localState.value = _localState.value.copy(error = "Failed to update category: ${e.message}")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val result = repository.deleteCategory(category)
            result.onSuccess {
                _localState.value = _localState.value.copy(message = "Category deleted.")
            }.onFailure { err ->
                _localState.value = _localState.value.copy(error = err.message ?: "Failed to delete category.")
            }
        }
    }

    fun clearFeedback() {
        _localState.value = _localState.value.copy(message = null, error = null)
    }
}
