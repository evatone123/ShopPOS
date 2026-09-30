package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.PosRepository
import com.example.ui.auth.PinLockScreen
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = PosRepository(database)

        setContent {
            MyApplicationTheme {
                ShopPosApp(repository = repository)
            }
        }
    }
}

@Composable
fun ShopPosApp(repository: PosRepository) {
    val settings by repository.appSettingsFlow.collectAsStateWithLifecycle(initialValue = null)
    var isUnlocked by remember { mutableStateOf(false) }

    val pinRequired = settings?.pinLockEnabled == true
    val currentPin = settings?.securityPin ?: "1234"
    val shopName = settings?.shopName ?: "ShopPOS"

    Box(modifier = Modifier.fillMaxSize()) {
        if (pinRequired && !isUnlocked) {
            PinLockScreen(
                correctPin = currentPin,
                shopName = shopName,
                onUnlockSuccess = { isUnlocked = true }
            )
        } else {
            MainScreen(
                repository = repository,
                onLockApp = { isUnlocked = false }
            )
        }
    }
}
