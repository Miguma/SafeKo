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
import androidx.navigation.navDeepLink
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import com.example.safeko.ui.screens.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.preference.PreferenceManager
import android.graphics.Color as AndroidColor

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    private var shouldHideSystemUI = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Initialize Osmdroid Configuration - REMOVED
        
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        lifecycleScope.launch {
            delay(800)
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
            val auth = remember { Firebase.auth }
            val startDestination = if (auth.currentUser != null) "home" else "welcome"

            val slideSpec = tween<IntOffset>(220)
            val fadeSpec = tween<Float>(180)

            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { slideInHorizontally(slideSpec) { it / 5 } + fadeIn(fadeSpec) },
                exitTransition = { slideOutHorizontally(slideSpec) { -it / 5 } + fadeOut(fadeSpec) },
                popEnterTransition = { slideInHorizontally(slideSpec) { -it / 5 } + fadeIn(fadeSpec) },
                popExitTransition = { slideOutHorizontally(slideSpec) { it / 5 } + fadeOut(fadeSpec) }
            ) {
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
                    HomeScreen(navController)
                }
                composable("chat/{circleId}") { backStackEntry ->
                    val circleId = backStackEntry.arguments?.getString("circleId")
                    if (circleId != null) {
                        ChatScreen(
                            circleId = circleId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("profile") {
                    ProfileScreen(
                        auth = auth,
                        onBack = { navController.popBackStack() },
                        onSettings = { /* TODO: Settings */ },
                        onLogout = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo("welcome") { inclusive = false }
                            }
                        },
                        onScanClick = { /* TODO: Scan from profile if needed, or remove */ },
                        onPremium = { navController.navigate("premium") }
                    )
                }
                composable("premium") {
                    PremiumScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    "public_profile/{uid}",
                    deepLinks = listOf(navDeepLink { uriPattern = "https://safeko-3ca46.web.app/user/{uid}" })
                ) { backStackEntry ->
                    val uid = backStackEntry.arguments?.getString("uid")
                    if (uid != null) {
                        PublicProfileScreen(uid = uid, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && shouldHideSystemUI) {
            hideSystemUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (shouldHideSystemUI) {
            hideSystemUI()
            lifecycleScope.launch {
                delay(500)
                if (shouldHideSystemUI) {
                    hideSystemUI()
                }
            }
        }
    }

    fun setChatSystemBarsEnabled(enabled: Boolean) {
        shouldHideSystemUI = !enabled
        if (enabled) {
            showSystemUI()
        } else {
            hideSystemUI()
        }
    }

    private fun showSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
