package com.financialassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.financialassistant.data.repository.TransactionRepository
import com.financialassistant.ui.navigation.AppNavigation
import com.financialassistant.ui.theme.FinancialAssistantTheme
import com.financialassistant.viewmodel.HomeViewModel
import com.financialassistant.viewmodel.IncomingViewModel
import com.financialassistant.viewmodel.SettingsViewModel
import com.financialassistant.viewmodel.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: TransactionRepository

    private val homeViewModel: HomeViewModel by viewModels()
    private val transactionsViewModel: TransactionsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val incomingViewModel: IncomingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FinancialAssistantTheme {
                val navController = rememberNavController()
                
                AppNavigation(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    transactionsViewModel = transactionsViewModel,
                    settingsViewModel = settingsViewModel,
                    incomingViewModel = incomingViewModel,
                    repository = repository
                )
            }
        }
    }
}
