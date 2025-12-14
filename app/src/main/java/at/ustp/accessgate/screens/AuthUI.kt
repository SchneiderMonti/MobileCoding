package at.ustp.accessgate.screens


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.userinterfaces.AuthViewModelFactory

@Composable
fun AuthUI() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )

    NavHost(navController = navController, startDestination = "methods") {

        composable("methods") {
            MethodsListScreen(
                onTapJingleClick = { navController.navigate("tap") }
            )
        }

        composable("tap") {
            TapAuthScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}