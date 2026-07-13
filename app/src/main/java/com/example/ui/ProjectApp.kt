package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.theme.MyApplicationTheme

@Composable
fun ProjectApp(viewModel: ProjectViewModel) {
    MyApplicationTheme {
        val navController = rememberNavController()
        val currentBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = currentBackStackEntry?.destination?.route

        Scaffold(
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onProjectClick = { projectId ->
                            navController.navigate("add_edit/$projectId")
                        },
                        onAddFromRepoClick = { repoId ->
                            navController.navigate("add_edit/-1?repoId=$repoId")
                        }
                    )
                }
                composable(
                    route = "add_edit/{projectId}?repoId={repoId}",
                    arguments = listOf(
                        navArgument("projectId") { type = NavType.IntType },
                        navArgument("repoId") { type = NavType.LongType; defaultValue = -1L }
                    )
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getInt("projectId") ?: -1
                    val repoId = backStackEntry.arguments?.getLong("repoId") ?: -1L
                    AddEditProjectScreen(
                        viewModel = viewModel,
                        projectId = if (projectId == -1) null else projectId,
                        githubRepoId = if (repoId == -1L) null else repoId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
