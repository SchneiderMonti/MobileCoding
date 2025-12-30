package at.ustp.accessgate.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                onAddAuthClick = { navController.navigate("add_auth_wiz") },
                onEntryClick = { entryId ->
                    navController.navigate("gate/$entryId")   // ✅ changed
                }
            )
        }

        composable("add_auth_wiz") {
            AddAuthWizardScreen(
                viewModel = authViewModel,
                onDone = {
                    navController.popBackStack("list", inclusive = false)
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "gate/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L

            AuthGateScreen(
                entryId = entryId,
                viewModel = authViewModel,
                onAuthed = { navController.navigate("detail/$entryId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "detail/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            DetailAuthScreen(
                entryId = entryId,
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack("list", inclusive = false) },
                onUpdate = { id ->
                    navController.navigate("edit_auth/$id")
                }            )
        }

        composable(
            "edit_auth/{entryId}",
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            AddAuthWizardScreen(
                viewModel = authViewModel,
                onDone = { navController.popBackStack() },
                onCancel = {navController.popBackStack()}
            )
            LaunchedEffect(entryId) {
                authViewModel.startEdit(entryId)
            }
        }

    }
}
