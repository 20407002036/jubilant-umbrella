package com.financialassistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.ui.details.DetailsScreen
import com.financialassistant.ui.home.HomeScreen
import com.financialassistant.ui.incoming.IncomingScreen
import com.financialassistant.ui.settings.SettingsScreen
import com.financialassistant.ui.transactions.TransactionsScreen
import com.financialassistant.viewmodel.HomeViewModel
import com.financialassistant.viewmodel.IncomingViewModel
import com.financialassistant.viewmodel.SettingsViewModel
import com.financialassistant.viewmodel.TransactionsViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object Details : Screen("details/{receiptNumber}") {
        fun createRoute(receiptNumber: String) = "details/$receiptNumber"
    }
    object Incoming : Screen("incoming")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    transactionsViewModel: TransactionsViewModel,
    settingsViewModel: SettingsViewModel,
    incomingViewModel: IncomingViewModel,
    repository: TransactionRepository
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToDetails = { receiptNumber ->
                    navController.navigate(Screen.Details.createRoute(receiptNumber))
                },
                onNavigateToIncoming = { navController.navigate(Screen.Incoming.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.Transactions.route) {
            TransactionsScreen(
                viewModel = transactionsViewModel,
                onNavigateToDetails = { receiptNumber ->
                    navController.navigate(Screen.Details.createRoute(receiptNumber))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("receiptNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val receiptNumber = backStackEntry.arguments?.getString("receiptNumber") ?: ""
            DetailsScreen(
                receiptNumber = receiptNumber,
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Incoming.route) {
            IncomingScreen(
                viewModel = incomingViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
