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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class StyleDescriptor(
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color
)

object CategoryStyleHelper {
    fun getCategoryStyle(categoryName: String?): StyleDescriptor {
        val name = categoryName?.lowercase() ?: ""
        return when {
            name.contains("drink") || name.contains("beverage") || name.contains("water") || name.contains("juice") ->
                StyleDescriptor(
                    icon = Icons.Default.LocalDrink,
                    color = Color(0xFF0284C7),
                    containerColor = Color(0xFFE0F2FE)
                )
            name.contains("food") || name.contains("snack") || name.contains("bread") || name.contains("biscuit") || name.contains("meal") ->
                StyleDescriptor(
                    icon = Icons.Default.Restaurant,
                    color = Color(0xFFEA580C),
                    containerColor = Color(0xFFFFEDD5)
                )
            name.contains("toiletr") || name.contains("soap") || name.contains("hygiene") || name.contains("wash") ->
                StyleDescriptor(
                    icon = Icons.Default.CleanHands,
                    color = Color(0xFF059669),
                    containerColor = Color(0xFFD1FAE5)
                )
            name.contains("house") || name.contains("clean") || name.contains("home") ->
                StyleDescriptor(
                    icon = Icons.Default.Home,
                    color = Color(0xFF7C3AED),
                    containerColor = Color(0xFFEDE9FE)
                )
            name.contains("electr") || name.contains("tech") || name.contains("phone") || name.contains("cable") ->
                StyleDescriptor(
                    icon = Icons.Default.Devices,
                    color = Color(0xFF4F46E5),
                    containerColor = Color(0xFFEEF2FF)
                )
            name.contains("station") || name.contains("book") || name.contains("pen") || name.contains("paper") ->
                StyleDescriptor(
                    icon = Icons.Default.Edit,
                    color = Color(0xFFDB2777),
                    containerColor = Color(0xFFFCE7F3)
                )
            else ->
                StyleDescriptor(
                    icon = Icons.Default.Category,
                    color = Color(0xFF0D9488),
                    containerColor = Color(0xFFCCFBF1)
                )
        }
    }

    fun getPaymentStyle(method: String?): StyleDescriptor {
        return when (method?.lowercase()) {
            "cash" -> StyleDescriptor(
                icon = Icons.Default.Payments,
                color = Color(0xFF059669),
                containerColor = Color(0xFFD1FAE5)
            )
            "mobile money", "momo" -> StyleDescriptor(
                icon = Icons.Default.PhoneAndroid,
                color = Color(0xFFD97706),
                containerColor = Color(0xFFFEF3C7)
            )
            "card" -> StyleDescriptor(
                icon = Icons.Default.CreditCard,
                color = Color(0xFF4F46E5),
                containerColor = Color(0xFFEEF2FF)
            )
            else -> StyleDescriptor(
                icon = Icons.Default.Wallet,
                color = Color(0xFF7C3AED),
                containerColor = Color(0xFFEDE9FE)
            )
        }
    }
}
