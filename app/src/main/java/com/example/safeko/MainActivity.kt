package com.example.safeko

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safeko.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.preference.PreferenceManager
import android.graphics.Color as AndroidColor

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Initialize Osmdroid Configuration - REMOVED
        
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        lifecycleScope.launch {
            delay(3500)
            keepSplash = false
        }
        
        // Ensure the theme is set correctly before onCreate finishes
        setTheme(R.style.Theme_SafeKo)
        super.onCreate(savedInstanceState)
        
        // Manual Edge-to-Edge and Immersive Mode configuration
        // We avoid enableEdgeToEdge() as it might conflict with permanent immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        
        // Hide system bars (immersive mode)
        hideSystemUI()

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "welcome") {
                composable("welcome") {
                    WelcomeScreen(onGetStartedClick = { navController.navigate("onboarding") })
                }
                composable("onboarding") {
                    OnboardingScreen1(onNextClick = { navController.navigate("onboarding2") })
                }
                composable("onboarding2") {
                    OnboardingScreen2(
                        onNextClick = { navController.navigate("onboarding3") },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("onboarding3") {
                    OnboardingScreen3(
                        onNextClick = { navController.navigate("login") },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = { navController.navigate("home") },
                        onFacebookClick = { 
                            Toast.makeText(this@MainActivity, "Facebook Login Clicked", Toast.LENGTH_SHORT).show()
                        },
                        onSupportClick = { /* TODO: Implement Support */ }
                    )
                }
                composable("home") {
                    HomeScreen()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Hide immediately
        hideSystemUI()
        // And retry after a short delay to catch any system UI reappearance
        lifecycleScope.launch {
            delay(500)
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
