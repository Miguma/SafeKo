package com.example.safeko.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safeko.R

@Composable
fun WelcomeScreen(onGetStartedClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Section - Pushed up and shrunk
        Spacer(modifier = Modifier.weight(1f))
        
        Image(
            painter = painterResource(id = R.drawable.safekolog),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp) // Shrink a little
                .offset(y = (-50).dp) // Move slightly up
        )
        
        Spacer(modifier = Modifier.weight(1.0f))
        
        // Button Section
        Button(
            onClick = onGetStartedClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4FC3F7)
            ),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(50.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Placeholder for "IT - AYASIB Company" logo/text
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "IT - AYASIB",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Company",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Vertical Divider
            androidx.compose.material3.VerticalDivider(
                modifier = Modifier.height(24.dp),
                thickness = 1.dp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Placeholder for "Safeko Group" logo/text
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Safeko",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Group",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(0.3f))
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(onGetStartedClick = {})
}
