package com.jobtracker.android.feature.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jobtracker.android.core.di.AppContainer
import com.jobtracker.android.feature.applications.create.NewApplicationScreen
import com.jobtracker.android.feature.applications.create.NewApplicationViewModel
import com.jobtracker.android.feature.applications.detail.ApplicationDetailScreen
import com.jobtracker.android.feature.applications.detail.ApplicationDetailViewModel
import com.jobtracker.android.feature.applications.list.ApplicationsScreen
import com.jobtracker.android.feature.applications.list.ApplicationsViewModel
import com.jobtracker.android.feature.dashboard.DashboardScreen
import com.jobtracker.android.feature.dashboard.DashboardViewModel
import com.jobtracker.android.feature.login.LoginScreen
import com.jobtracker.android.feature.login.LoginViewModel
import com.jobtracker.android.feature.profile.DebugOverrideScreen
import com.jobtracker.android.feature.profile.DebugOverrideViewModel
import com.jobtracker.android.feature.profile.ProfileScreen
import com.jobtracker.android.feature.profile.ProfileViewModel
import com.jobtracker.android.feature.register.RegisterScreen
import com.jobtracker.android.feature.register.RegisterViewModel

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val session by container.sessionManager.session.collectAsStateWithLifecycle()

    LaunchedEffect(session) {
        if (session == null) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (session != null) Routes.APPLICATIONS else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = viewModelFactory {
                initializer { LoginViewModel(container.authRepository) }
            })
            LoginScreen(
                viewModel = vm,
                onAuthenticated = {
                    navController.navigate(Routes.APPLICATIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            val vm: RegisterViewModel = viewModel(factory = viewModelFactory {
                initializer { RegisterViewModel(container.authRepository) }
            })
            RegisterScreen(
                viewModel = vm,
                onRegistered = {
                    navController.navigate(Routes.APPLICATIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.APPLICATIONS) {
            val vm: ApplicationsViewModel = viewModel(factory = viewModelFactory {
                initializer { ApplicationsViewModel(container.applicationsRepository) }
            })
            ApplicationsScreen(
                viewModel = vm,
                onOpenDetail = { id -> navController.navigate(Routes.applicationDetail(id)) },
                onNewApplication = { navController.navigate(Routes.NEW_APPLICATION) },
                onOpenDashboard = { navController.navigate(Routes.DASHBOARD) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
            )
        }

        composable(Routes.NEW_APPLICATION) {
            val vm: NewApplicationViewModel = viewModel(factory = viewModelFactory {
                initializer { NewApplicationViewModel(container.applicationsRepository) }
            })
            NewApplicationScreen(
                viewModel = vm,
                onCreated = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.APPLICATION_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStack ->
            val id = backStack.arguments?.getString("id").orEmpty()
            val vm: ApplicationDetailViewModel = viewModel(factory = viewModelFactory {
                initializer { ApplicationDetailViewModel(container.applicationsRepository, id) }
            })
            ApplicationDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(Routes.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(factory = viewModelFactory {
                initializer { DashboardViewModel(container.statsRepository) }
            })
            DashboardScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            val vm: ProfileViewModel = viewModel(factory = viewModelFactory {
                initializer { ProfileViewModel(container.sessionManager) }
            })
            ProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSignedOut = { /* session flow handles redirect */ },
                onOpenDebugOverride = { navController.navigate(Routes.DEBUG_OVERRIDE) },
            )
        }

        composable(Routes.DEBUG_OVERRIDE) {
            val vm: DebugOverrideViewModel = viewModel(factory = viewModelFactory {
                initializer { DebugOverrideViewModel(container.baseUrlProvider) }
            })
            DebugOverrideScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
