package com.example.giuaky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.giuaky.ui.theme.GIUAKYTheme

enum class Screen {
    Login, Register, Home, Create, Edit
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GIUAKYTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.Login) }
                    var selectedPostId by remember { mutableStateOf("") }

                    when (currentScreen) {
                        Screen.Login -> LoginScreen(
                            onLoginSuccess = { currentScreen = Screen.Home },
                            onNavigateToRegister = { currentScreen = Screen.Register }
                        )
                        Screen.Register -> RegisterScreen(
                            onRegisterSuccess = { currentScreen = Screen.Login },
                            onBackToLogin = { currentScreen = Screen.Login }
                        )
                        Screen.Home -> HomeScreen(
                            onNavigateToCreate = { currentScreen = Screen.Create },
                            onNavigateToEdit = { postId ->
                                selectedPostId = postId
                                currentScreen = Screen.Edit
                            }
                        )
                        Screen.Create -> CreateScreen(
                            onBackToHome = { currentScreen = Screen.Home }
                        )
                        Screen.Edit -> EditScreen(
                            postId = selectedPostId,
                            onBackToHome = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}
