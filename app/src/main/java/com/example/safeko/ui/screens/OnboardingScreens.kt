package com.example.safeko.ui.screens

import androidx.compose.runtime.Composable
import com.example.safeko.R
import com.example.safeko.ui.components.OnboardingLayout

@Composable
fun OnboardingScreen1(onNextClick: () -> Unit) {
    OnboardingLayout(
        imageRes = R.drawable.start1,
        title = "Get guided directions in real time\nto reach the emergency location\nfaster and provide immediate\nhelp.",
        pageIndicator = "1/3",
        currentDotIndex = 0,
        onNextClick = onNextClick,
        onBackClick = null
    )
}

@Composable
fun OnboardingScreen2(onNextClick: () -> Unit, onBackClick: () -> Unit) {
    OnboardingLayout(
        imageRes = R.drawable.start2,
        title = "Report incidents and receive\nfaster assistance from your\ncommunity and local\nauthorities when every second\ncounts.",
        pageIndicator = "2/3",
        currentDotIndex = 1,
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        imageScale = 1.6f
    )
}

@Composable
fun OnboardingScreen3(onNextClick: () -> Unit, onBackClick: () -> Unit) {
    OnboardingLayout(
        imageRes = R.drawable.start3,
        title = "View real-time safety updates\nand incidents happening around\nyour area.\nKnow what’s going on before it\naffects you.",
        pageIndicator = "3/3",
        currentDotIndex = 2,
        onNextClick = onNextClick,
        onBackClick = onBackClick
    )
}
