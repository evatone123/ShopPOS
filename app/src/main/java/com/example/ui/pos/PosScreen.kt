package com.example.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Product
import com.example.data.entity.StockStatus
import com.example.data.repository.CartItem
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CategoryPillBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ReceiptDialog
import com.example.ui.theme.CategoryStyleHelper
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateToAddProductWithBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCameraScanner by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // On phones, show floating quick checkout / cart bar if cart has items
                if (!isTablet && uiState.cartItems.isNotEmpty()) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .clickable { showMobileCartSheet = true }
                                    .padding(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.totalItemCount} items in Cart",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatPesewas(
                                        uiState.totalPesewas,
                                        uiState.settings.currency
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showMobileCartSheet = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Cart")
                                }

                                Button(
                                    onClick = { viewModel.startCheckout() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.testTag("pos_mobile_checkout_btn")
                                ) {
                                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            if (isTablet) {
                // Dual-Pane Tablet Layout: Left 60% Products Grid, Right 40% Cart & Checkout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Left: Product Catalog
                    Box(modifier = Modifier.weight(1.4f)) {
                        ProductCatalogSection(
                            uiState = uiState,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onBarcodeScanned = viewModel::onBarcodeScanned,
                            onCategorySelect = viewModel::onCategorySelected,
                            onProductClick = viewModel::addToCart,
                            onOpenScanner = { showCameraScanner = true },
                            columns = 3
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Right: Cart & Quick Actions
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        CartContent(
                            uiState = uiState,
                            onIncrease = viewModel::increaseQuantity,
                            onDecrease = viewModel::decreaseQuantity,
                            onRemove = viewModel::removeFromCart,
                            onClear = viewModel::clearCart,
                            onOpenDiscount = { showDiscountDialog = true },
                            onCheckout = viewModel::startCheckout
                        )
                    }
                }
            } else {
                // Phone Single-Pane Layout
                ProductCatalogSection(
                    uiState = uiState,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onBarcodeScanned = viewModel::onBarcodeScanned,
                    onCategorySelect = viewModel::onCategorySelected,
                    onProductClick = viewModel::addToCart,
                    onOpenScanner = { showCameraScanner = true },
                    columns = 2,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    // Mobile Bottom Sheet for Cart
    if (showMobileCartSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMobileCartSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                CartContent(
                    uiState = uiState,
                    onIncrease = viewModel::increaseQuantity,
                    onDecrease = viewModel::decreaseQuantity,
                    onRemove = viewModel::removeFromCart,
                    onClear = viewModel::clearCart,
                    onOpenDiscount = { showDiscountDialog = true },
                    onCheckout = {
                        showMobileCartSheet = false
                        viewModel.startCheckout()
                    }
                )
            }
        }
    }

    // Barcode Scanner Camera Dialog
    if (showCameraScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                showCameraScanner = false
                viewModel.onBarcodeScanned(barcode)
            },
            onDismiss = { showCameraScanner = false }
        )
    }

    // Barcode Not Found Dialog
    uiState.barcodeNotFoundDialog?.let { missingBarcode ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissBarcodeNotFound() },
            title = { Text("Product Not Found") },
            text = {
                Text("No product found matching barcode '$missingBarcode'. Would you like to create a new product with this barcode?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissBarcodeNotFound()
                        onNavigateToAddProductWithBarcode(missingBarcode)
                    }
                ) {
                    Text("Add New Product")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissBarcodeNotFound() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Discount Input Dialog
    if (showDiscountDialog) {
        var discountInput by remember {
            mutableStateOf(CurrencyFormatter.pesewasToEditableString(uiState.discountPesewas))
        }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Discount") },
            text = {
                Column {
                    Text("Enter discount amount in ${uiState.settings.currency}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pesewas = CurrencyFormatter.toPesewas(discountInput)
                        viewModel.applyDiscountPesewas(pesewas)
                        showDiscountDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDiscountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Checkout Modal
    if (uiState.isCheckingOut) {
        CheckoutDialog(
            subtotalPesewas = uiState.subtotalPesewas,
            discountPesewas = uiState.discountPesewas,
            totalPesewas = uiState.totalPesewas,
            settings = uiState.settings,
            onCompleteSale = { method, amount, ref ->
                viewModel.completeSale(method, amount, ref)
            },
            onDismiss = { viewModel.dismissCheckout() }
        )
    }

    // Receipt Modal after successful sale
    uiState.completedSale?.let { saleWithItems ->
        ReceiptDialog(
            saleWithItems = saleWithItems,
            settings = uiState.settings,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
private fun ProductCatalogSection(
    uiState: PosUiState,
    onQueryChange: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onProductClick: (Product) -> Unit,
    onOpenScanner: () -> Unit,
    columns: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search & Barcode Scan Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search name, SKU, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (uiState.searchQuery.isNotBlank()) {
                        onBarcodeScanned(uiState.searchQuery.trim())
                    }
                }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Barcode camera button
            Surface(
                onClick = onOpenScanner,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips with Colorful Icons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = uiState.selectedCategoryId == null
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All Products", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            items(uiState.categories) { category ->
                val isSelected = uiState.selectedCategoryId == category.id
                val style = CategoryStyleHelper.getCategoryStyle(category.name)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category.id) },
                    label = { Text(category.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else style.color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Products Grid
        if (uiState.filteredProducts.isEmpty()) {
            EmptyStateView(
                title = "No Products Found",
                message = if (uiState.searchQuery.isNotBlank()) "No products match '${uiState.searchQuery}'" else "No active products available",
                icon = Icons.Default.Search,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.filteredProducts, key = { it.id }) { product ->
                    val category = uiState.categories.find { it.id == product.categoryId }
                    PosProductCard(
                        product = product,
                        categoryName = category?.name,
                        currency = uiState.settings.currency,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun PosProductCard(
    product: Product,
    categoryName: String?,
    currency: String,
    onClick: () -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = product.stockQuantity in 1..product.minimumStock
    val style = CategoryStyleHelper.getCategoryStyle(categoryName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp)
            .clickable(enabled = !isOutOfStock, onClick = onClick)
            .testTag("pos_product_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp),
        border = when {
            isLowStock -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
            isOutOfStock -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryPillBadge(categoryName = categoryName)

                    // Stock dot indicator
                    val (dotColor, _) = when {
                        isOutOfStock -> Color(0xFFE11D48) to "Out"
                        isLowStock -> Color(0xFFD97706) to "Low"
                        else -> Color(0xFF10B981) to "In"
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.sku.isNotBlank()) {
                    Text(
                        text = product.sku,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                Text(
                    text = CurrencyFormatter.formatPesewas(product.sellingPricePesewas, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF047857)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stockText = when {
                        isOutOfStock -> "Out of Stock"
                        else -> "Stock: ${product.stockQuantity}"
                    }
                    val stockColor = when {
                        isOutOfStock -> Color(0xFFE11D48)
                        isLowStock -> Color(0xFFD97706)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.labelSmall,
                        color = stockColor,
                        fontWeight = if (isOutOfStock || isLowStock) FontWeight.Bold else FontWeight.Medium
                    )

                    if (!isOutOfStock) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartContent(
    uiState: PosUiState,
    onIncrease: (Long) -> Unit,
    onDecrease: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onClear: () -> Unit,
    onOpenDiscount: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Cart (${uiState.totalItemCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.cartItems.isNotEmpty()) {
                TextButton(onClick = onClear) {
                    Text("Clear Cart", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.cartItems.isEmpty()) {
            EmptyStateView(
                title = "Cart is Empty",
                message = "Tap any product in the catalog or scan a barcode to add it to the cart.",
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.cartItems, key = { it.product.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.quantity} × ${CurrencyFormatter.formatPesewas(item.product.sellingPricePesewas, uiState.settings.currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatPesewas(item.subtotalPesewas, uiState.settings.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Stepper (- and +)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = { onDecrease(item.product.id) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Decrease",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                Surface(
                                    onClick = { onIncrease(item.product.id) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onRemove(item.product.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Totals & Discount Button
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        CurrencyFormatter.formatPesewas(uiState.subtotalPesewas, uiState.settings.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenDiscount() }
                    ) {
                        Icon(
                            Icons.Default.Discount,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Discount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val discountText = if (uiState.discountPesewas > 0)
                        "-${CurrencyFormatter.formatPesewas(uiState.discountPesewas, uiState.settings.currency)}"
                    else "GHS 0.00"
                    Text(
                        text = discountText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.discountPesewas > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.formatPesewas(uiState.totalPesewas, uiState.settings.currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("pos_checkout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHECKOUT (${CurrencyFormatter.formatPesewas(uiState.totalPesewas, uiState.settings.currency)})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
