package com.inialpha.executiveai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.inialpha.executiveai.ui.ExecutiveAIApp
import com.inialpha.executiveai.ui.theme.ExecutiveAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExecutiveAITheme {
                ExecutiveAIApp()
            }
        }
    }
}
