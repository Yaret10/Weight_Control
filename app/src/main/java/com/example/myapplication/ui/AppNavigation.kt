package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.screens.*

@Composable
fun AppNavigation(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = "pesaje") {
        composable("pesaje") { 
            WeightScreen(viewModel, onNavigateToSetup = { navController.navigate("setup") }) 
        }
        composable("setup") { 
            BluetoothSetupScreen(viewModel, onBack = { navController.popBackStack() }) 
        }
        composable("gestion") { 
            ManagementScreen(viewModel) 
        }
        composable("historial") { 
            HistoryScreen(viewModel) 
        }
    }
}
