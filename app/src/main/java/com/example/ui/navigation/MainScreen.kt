package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.PosRepository
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.inventory.InventoryScreen
import com.example.ui.inventory.InventoryViewModel
import com.example.ui.pos.PosScreen
import com.example.ui.pos.PosViewModel
import com.example.ui.products.ProductsScreen
import com.example.ui.products.ProductsViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportsViewModel
import com.example.ui.sales.SalesScreen
import com.example.ui.sales.SalesViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel

enum class PosNavDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    POS("POS", Icons.Default.PointOfSale),
    PRODUCTS("Products", Icons.Default.Inventory2),
    INVENTORY("Inventory", Icons.Default.Inventory),
    SALES("Sales", Icons.Default.ReceiptLong),
    REPORTS("Reports", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: PosRepository,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(PosNavDestination.DASHBOARD) }
    var prefilledProductBarcode by remember { mutableStateOf<String?>(null) }

    // Instantiate ViewModels
    val dashboardViewModel: DashboardViewModel = viewModel { DashboardViewModel(repository) }
    val posViewModel: PosViewModel = viewModel { PosViewModel(repository) }
    val productsViewModel: ProductsViewModel = viewModel { ProductsViewModel(repository) }
    val inventoryViewModel: InventoryViewModel = viewModel { InventoryViewModel(repository) }
    val salesViewModel: SalesViewModel = viewModel { SalesViewModel(repository) }
    val reportsViewModel: ReportsViewModel = viewModel { ReportsViewModel(repository) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(repository) }

    val posUiState by posViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // Handle system back navigation to return to Dashboard
    BackHandler(enabled = currentDestination != PosNavDestination.DASHBOARD) {
        currentDestination = PosNavDestination.DASHBOARD
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // Tablet Layout with Navigation Rail on Left
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ShopPOS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    PosNavDestination.values().forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                prefilledProductBarcode = null
                                currentDestination = destination
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color(0xFF4F46E5),
                                selectedTextColor = Color(0xFF4F46E5),
                                indicatorColor = Color(0xFFEEF2FF),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                if (destination == PosNavDestination.POS && posUiState.cartItems.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = Color(0xFFEF4444),
                                                contentColor = Color.White
                                            ) { Text("${posUiState.totalItemCount}", fontWeight = FontWeight.Bold) }
                                        }
                                    ) {
                                        Icon(destination.icon, contentDescription = destination.label)
                                    }
                                } else {
                                    Icon(destination.icon, contentDescription = destination.label)
                                }
                            },
                            label = { Text(destination.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (settingsUiState.settings.pinLockEnabled) {
                        IconButton(onClick = onLockApp, modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock POS")
                        }
                    }
                }

                // Main Content Area
                Box(modifier = Modifier.weight(1f)) {
                    ScreenContent(
                        destination = currentDestination,
                        prefilledBarcode = prefilledProductBarcode,
                        dashboardViewModel = dashboardViewModel,
                        posViewModel = posViewModel,
                        productsViewModel = productsViewModel,
                        inventoryViewModel = inventoryViewModel,
                        salesViewModel = salesViewModel,
                        reportsViewModel = reportsViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigate = { dest ->
                            prefilledProductBarcode = null
                            currentDestination = dest
                        },
                        onNavigateToAddProductWithBarcode = { barcode ->
                            prefilledProductBarcode = barcode
                            currentDestination = PosNavDestination.PRODUCTS
                        }
                    )
                }
            }
        } else {
            // Phone Layout with Top App Bar and Bottom Navigation Bar
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFF4F46E5), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = when (currentDestination) {
                                        PosNavDestination.DASHBOARD -> settingsUiState.settings.shopName
                                        else -> currentDestination.label
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = CircleShape
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF10B981), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { currentDestination = PosNavDestination.REPORTS }) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Reports",
                                    tint = if (currentDestination == PosNavDestination.REPORTS) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { currentDestination = PosNavDestination.SETTINGS }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = if (currentDestination == PosNavDestination.SETTINGS) Color(0xFF4F46E5) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (settingsUiState.settings.pinLockEnabled) {
                                IconButton(onClick = onLockApp) {
                                    Icon(Icons.Default.Lock, contentDescription = "Lock POS", tint = Color(0xFFD97706))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        tonalElevation = 8.dp
                    ) {
                        val bottomDestinations = listOf(
                            PosNavDestination.DASHBOARD,
                            PosNavDestination.POS,
                            PosNavDestination.PRODUCTS,
                            PosNavDestination.INVENTORY,
                            PosNavDestination.SALES
                        )

                        bottomDestinations.forEach { destination ->
                            val isSelected = currentDestination == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    prefilledProductBarcode = null
                                    currentDestination = destination
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF4F46E5),
                                    selectedTextColor = Color(0xFF4F46E5),
                                    indicatorColor = Color(0xFFEEF2FF),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    if (destination == PosNavDestination.POS && posUiState.cartItems.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = Color(0xFFEF4444),
                                                    contentColor = Color.White
                                                ) { Text("${posUiState.totalItemCount}", fontWeight = FontWeight.Bold) }
                                            }
                                        ) {
                                            Icon(destination.icon, contentDescription = destination.label)
                                        }
                                    } else {
                                        Icon(destination.icon, contentDescription = destination.label)
                                    }
                                },
                                label = { Text(destination.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenContent(
                        destination = currentDestination,
                        prefilledBarcode = prefilledProductBarcode,
                        dashboardViewModel = dashboardViewModel,
                        posViewModel = posViewModel,
                        productsViewModel = productsViewModel,
                        inventoryViewModel = inventoryViewModel,
                        salesViewModel = salesViewModel,
                        reportsViewModel = reportsViewModel,
                        settingsViewModel = settingsViewModel,
                        onNavigate = { dest ->
                            prefilledProductBarcode = null
                            currentDestination = dest
                        },
                        onNavigateToAddProductWithBarcode = { barcode ->
                            prefilledProductBarcode = barcode
                            currentDestination = PosNavDestination.PRODUCTS
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    destination: PosNavDestination,
    prefilledBarcode: String?,
    dashboardViewModel: DashboardViewModel,
    posViewModel: PosViewModel,
    productsViewModel: ProductsViewModel,
    inventoryViewModel: InventoryViewModel,
    salesViewModel: SalesViewModel,
    reportsViewModel: ReportsViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigate: (PosNavDestination) -> Unit,
    onNavigateToAddProductWithBarcode: (String) -> Unit
) {
    when (destination) {
        PosNavDestination.DASHBOARD -> DashboardScreen(
            viewModel = dashboardViewModel,
            onNavigateToPos = { onNavigate(PosNavDestination.POS) },
            onNavigateToAddProduct = { onNavigate(PosNavDestination.PRODUCTS) },
            onNavigateToInventory = { onNavigate(PosNavDestination.INVENTORY) },
            onNavigateToSales = { onNavigate(PosNavDestination.SALES) }
        )
        PosNavDestination.POS -> PosScreen(
            viewModel = posViewModel,
            onNavigateToAddProductWithBarcode = onNavigateToAddProductWithBarcode
        )
        PosNavDestination.PRODUCTS -> ProductsScreen(
            viewModel = productsViewModel,
            prefilledBarcode = prefilledBarcode
        )
        PosNavDestination.INVENTORY -> InventoryScreen(
            viewModel = inventoryViewModel
        )
        PosNavDestination.SALES -> SalesScreen(
            viewModel = salesViewModel
        )
        PosNavDestination.REPORTS -> ReportsScreen(
            viewModel = reportsViewModel
        )
        PosNavDestination.SETTINGS -> SettingsScreen(
            viewModel = settingsViewModel
        )
    }
}
