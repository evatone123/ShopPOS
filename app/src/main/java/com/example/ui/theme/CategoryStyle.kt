package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class StyleDescriptor(
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color
)

object CategoryStyleHelper {
    fun getCategoryStyle(categoryName: String?, isDark: Boolean = false): StyleDescriptor {
        val name = categoryName?.lowercase() ?: ""
        return when {
            name.contains("drink") || name.contains("beverage") || name.contains("water") || name.contains("juice") ->
                StyleDescriptor(
                    icon = Icons.Default.LocalDrink,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    containerColor = if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.55f) else Color(0xFFE0F2FE)
                )
            name.contains("food") || name.contains("snack") || name.contains("bread") || name.contains("biscuit") || name.contains("meal") ->
                StyleDescriptor(
                    icon = Icons.Default.Restaurant,
                    color = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                    containerColor = if (isDark) Color(0xFF7C2D12).copy(alpha = 0.55f) else Color(0xFFFFEDD5)
                )
            name.contains("toiletr") || name.contains("soap") || name.contains("hygiene") || name.contains("wash") ->
                StyleDescriptor(
                    icon = Icons.Default.CleanHands,
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                    containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFD1FAE5)
                )
            name.contains("house") || name.contains("clean") || name.contains("home") ->
                StyleDescriptor(
                    icon = Icons.Default.Home,
                    color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                    containerColor = if (isDark) Color(0xFF4C1D95).copy(alpha = 0.55f) else Color(0xFFEDE9FE)
                )
            name.contains("electr") || name.contains("tech") || name.contains("phone") || name.contains("cable") ->
                StyleDescriptor(
                    icon = Icons.Default.Devices,
                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    containerColor = if (isDark) Color(0xFF312E81).copy(alpha = 0.55f) else Color(0xFFEEF2FF)
                )
            name.contains("station") || name.contains("book") || name.contains("pen") || name.contains("paper") ->
                StyleDescriptor(
                    icon = Icons.Default.Edit,
                    color = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                    containerColor = if (isDark) Color(0xFF831843).copy(alpha = 0.55f) else Color(0xFFFCE7F3)
                )
            else ->
                StyleDescriptor(
                    icon = Icons.Default.Category,
                    color = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                    containerColor = if (isDark) Color(0xFF134E4A).copy(alpha = 0.55f) else Color(0xFFCCFBF1)
                )
        }
    }

    fun getPaymentStyle(method: String?, isDark: Boolean = false): StyleDescriptor {
        return when (method?.lowercase()) {
            "cash" -> StyleDescriptor(
                icon = Icons.Default.Payments,
                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFD1FAE5)
            )
            "mobile money", "momo" -> StyleDescriptor(
                icon = Icons.Default.PhoneAndroid,
                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.55f) else Color(0xFFFEF3C7)
            )
            "card" -> StyleDescriptor(
                icon = Icons.Default.CreditCard,
                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                containerColor = if (isDark) Color(0xFF312E81).copy(alpha = 0.55f) else Color(0xFFEEF2FF)
            )
            else -> StyleDescriptor(
                icon = Icons.Default.Wallet,
                color = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                containerColor = if (isDark) Color(0xFF4C1D95).copy(alpha = 0.55f) else Color(0xFFEDE9FE)
            )
        }
    }
}
