package at.ustp.accessgate.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.ustp.accessgate.userinterfaces.AuthViewModel
import at.ustp.accessgate.userinterfaces.AuthViewModelFactory

@Composable
fun AuthUI() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )

    NavHost(navController = navController, startDestination = "list") {

        composable("list") {
            AuthListScreen(
                viewModel = authViewModel,
                onAddAuthClick = { navController.navigate("add_auth") },
                onEntryClick = { entryId ->
                    navController.navigate("detail/$entryId")
                }
            )
        }

        composable("add_auth") {
            AddAuthWizardScreen(
                viewModel = authViewModel,
                onDone = {
                    navController.popBackStack("list", inclusive = false)
                }
            )
        }

        composable(
            "detail/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            DetailAuthScreen(entryId = entryId, viewModel = authViewModel, onDeleted = {navController.popBackStack()})
        }

    }
}
