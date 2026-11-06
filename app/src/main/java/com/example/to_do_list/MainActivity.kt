package com.example.to_do_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.to_do_list.ui.navigation.AppNavHost
import com.example.to_do_list.ui.theme.TO_DO_ListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TO_DO_ListTheme {
                AppNavHost()
            }
        }
    }
}
