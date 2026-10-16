package com.smartjirani.app

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartjirani.core.designsystem.component.AppBottomBar
import com.smartjirani.core.designsystem.component.AppTab
import com.smartjirani.core.navigation.Screen
import com.smartjirani.feature.auth.ui.AuthViewModel
import com.smartjirani.feature.auth.ui.LoginScreen
import com.smartjirani.feature.auth.ui.RegisterScreen
import com.smartjirani.feature.baraza.ui.BarazaRoute
import com.smartjirani.feature.calls.ui.CallRoute
import com.smartjirani.feature.calls.ui.CallsHubRoute
import com.smartjirani.feature.civic.ui.CivicAdminRoute
import com.smartjirani.feature.civic.ui.CivicAssistantRoute
import com.smartjirani.feature.civic.ui.CivicHubRoute
import com.smartjirani.feature.civic.ui.CivicMapRoute
import com.smartjirani.feature.civic.ui.CivicMyReportsRoute
import com.smartjirani.feature.civic.ui.CivicNewReportRoute
import com.smartjirani.feature.civic.ui.CivicReportDetailRoute
import com.smartjirani.feature.civic.ui.DepartmentOfficerConsoleRoute
import com.smartjirani.feature.comments.ui.CommentsRoute
import com.smartjirani.feature.conference.ui.ConferenceRoute
import com.smartjirani.feature.conference.ui.CreateConferenceRoute
import com.smartjirani.feature.feed.ui.FeedRoute
import com.smartjirani.feature.feed.ui.PostDetailRoute
import com.smartjirani.feature.messaging.ui.ChatRoute
import com.smartjirani.feature.messaging.ui.ConversationsRoute
import com.smartjirani.feature.neighborhood.ui.ExploreRoute
import com.smartjirani.feature.neighborhood.ui.NeighborhoodSelectionScreen
import com.smartjirani.feature.notifications.ui.NotificationsRoute
import com.smartjirani.feature.posts.ui.CreatePostScreen
import com.smartjirani.feature.profile.ui.ProfileScreen
import com.smartjirani.feature.ujamaa.ui.MtaaMapRoute
import com.smartjirani.feature.ujamaa.ui.UjamaaRoute
import com.smartjirani.feature.voice.ui.VoiceMessageRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SmartJiraniNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    // Instantiates the shell-scoped owner; it self-triggers reconciliation
    // once the session becomes Authenticated (see ShellViewModel.init).
    hiltViewModel<ShellViewModel>()
    // Shell-scoped civic role probe: decides Serikali tab visibility.
    val civicAccessViewModel = hiltViewModel<CivicAccessViewModel>()
    val isCivicOfficial by civicAccessViewModel.isOfficial.collectAsState()
    // Shell-scoped auth owner: Profile's "Toka Account" logs out through this
    // instance, and the logoutCompleted event routes the whole graph to Login.
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.uiState.collectAsState()
    LaunchedEffect(authState.logoutCompleted) {
        if (authState.logoutCompleted) {
            authViewModel.consumeLogoutEvent()
            civicAccessViewModel.reset()
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) civicAccessViewModel.ensureLoaded()
    }
    // In-app update check against the published release (app-version edge function).
    val appUpdateViewModel = hiltViewModel<AppUpdateViewModel>()
    val pendingUpdate by appUpdateViewModel.update.collectAsState()
    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForUpdate(BuildConfig.VERSION_CODE)
    }
    pendingUpdate?.let { info ->
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = appUpdateViewModel::dismiss,
            title = { Text("Toleo Jipya ${info.version ?: ""}") },
            text = {
                Column {
                    if (info.changelog.isNotEmpty()) {
                        info.changelog.forEach { entry ->
                            Text("• $entry", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("Toleo jipya la SmartJirani linapatikana.")
                    }
                    info.sizeMb?.let { size ->
                        Text(
                            "$size MB",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    info.apkUrl?.let { url ->
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    }
                    appUpdateViewModel.dismiss()
                }) { Text("Pakua") }
            },
            dismissButton = {
                TextButton(onClick = appUpdateViewModel::dismiss) { Text("Baadaye") }
            },
        )
    }
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Screen.Feed.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = { navController.navigate(Screen.Feed.route) { popUpTo(Screen.Login.route) { inclusive = true } } },
                onNavigateToProfileSetup = { navController.navigate(Screen.NeighborhoodSelection.route) },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToProfileSetup = { navController.navigate(Screen.NeighborhoodSelection.route) { popUpTo(Screen.Register.route) { inclusive = true } } },
            )
        }

        composable(Screen.NeighborhoodSelection.route) {
            NeighborhoodSelectionScreen(
                onNeighborhoodSelected = { navController.navigate(Screen.Feed.route) { popUpTo(Screen.NeighborhoodSelection.route) { inclusive = true } } },
            )
        }

        composable(Screen.Feed.route) { entry ->
            FeedRoute(
                onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToCreate = { navController.navigate(Screen.CreatePost.route) },
                onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                bottomBar = {
                    ShellBottomBar(
                        currentTab = AppTab.FEED,
                        civicVisible = isCivicOfficial,
                        navController = navController,
                    )
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }

        composable(Screen.Ujamaa.route) { entry ->
            UjamaaRoute(
                onNavigateBack = { navController.popBackStack() },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onAvatarClick = { navController.navigate(Screen.Profile.route) },
                onOpenMap = { navController.navigate(Screen.MtaaMap.route) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                bottomBar = {
                    ShellBottomBar(
                        currentTab = AppTab.UJAMAA,
                        civicVisible = isCivicOfficial,
                        navController = navController,
                    )
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }

        composable(Screen.MtaaMap.route) {
            MtaaMapRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Baraza.route) { entry ->
            BarazaRoute(
                onNavigateBack = { navController.popBackStack() },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onAvatarClick = { navController.navigate(Screen.Profile.route) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                bottomBar = {
                    ShellBottomBar(
                        currentTab = AppTab.BARAZA,
                        civicVisible = isCivicOfficial,
                        navController = navController,
                    )
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { entry ->
            PostDetailRoute(
                postId = entry.arguments?.getString("postId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Explore.route) {
            ExploreRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Calls.route) {
            CallsHubRoute(
                onNavigateBack = { navController.popBackStack() },
                onCallStarted = { callId ->
                    navController.navigate(Screen.CallScreen.createRoute(callId))
                },
                onNavigateToCreateConference = {
                    navController.navigate(Screen.CreateConference.route)
                },
                onNavigateToVoiceMessage = { userId ->
                    navController.navigate(Screen.VoiceMessage.createRoute(userId))
                },
            )
        }

        composable(
            route = Screen.CallScreen.route,
            arguments = listOf(navArgument("callId") { type = NavType.StringType }),
        ) {
            CallRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.VoiceMessage.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { entry ->
            VoiceMessageRoute(
                peerId = entry.arguments?.getString("userId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
        ) { entry ->
            CommentsRoute(
                postId = entry.arguments?.getString("postId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CreateConference.route) {
            CreateConferenceRoute(
                onNavigateBack = { navController.popBackStack() },
                onConferenceCreated = { conferenceId ->
                    navController.navigate(Screen.ConferenceScreen.createRoute(conferenceId)) {
                        popUpTo(Screen.CreateConference.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Screen.ConferenceScreen.route,
            arguments = listOf(navArgument("conferenceId") { type = NavType.StringType }),
        ) { entry ->
            ConferenceRoute(
                conferenceId = entry.arguments?.getString("conferenceId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CivicHub.route) { entry ->
            CivicHubRoute(
                onReportClick = { reportId ->
                    navController.navigate(Screen.CivicReportDetail.createRoute(reportId))
                },
                onOpenMap = { navController.navigate(Screen.CivicMap.route) },
                onOpenAdmin = { navController.navigate(Screen.CivicAdmin.route) },
                onOpenDepartmentConsole = { navController.navigate(Screen.DepartmentOfficerConsole.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onAvatarClick = { navController.navigate(Screen.Profile.route) },
                bottomBar = {
                    ShellBottomBar(
                        currentTab = AppTab.CIVIC,
                        civicVisible = isCivicOfficial,
                        navController = navController,
                    )
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }

        composable(
            route = Screen.CivicReportDetail.route,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType }),
        ) { entry ->
            CivicReportDetailRoute(
                reportId = entry.arguments?.getString("reportId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CivicMap.route) {
            CivicMapRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CivicNewReport.route) {
            CivicNewReportRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CivicMyReports.route) {
            CivicMyReportsRoute(
                onNavigateBack = { navController.popBackStack() },
                onReportClick = { reportId ->
                    navController.navigate(Screen.CivicReportDetail.createRoute(reportId))
                },
            )
        }

        composable(Screen.CivicAssistant.route) {
            CivicAssistantRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CivicAdmin.route) {
            CivicAdminRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.DepartmentOfficerConsole.route) {
            DepartmentOfficerConsoleRoute(
                onReportClick = { reportId ->
                    navController.navigate(Screen.CivicReportDetail.createRoute(reportId))
                },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToCivic =
                    if (isCivicOfficial) {
                        { navController.navigate(Screen.CivicHub.route) }
                    } else {
                        null
                    },
                onNavigateToCivicNewReport = { navController.navigate(Screen.CivicNewReport.route) },
                onNavigateToCivicMyReports = { navController.navigate(Screen.CivicMyReports.route) },
                onNavigateToCivicAssistant = { navController.navigate(Screen.CivicAssistant.route) },
                onLogoutClick = { authViewModel.logout() },
                bottomBar = {
                    ShellBottomBar(
                        currentTab = AppTab.PROFILE,
                        civicVisible = isCivicOfficial,
                        navController = navController,
                    )
                },
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Messages.route) {
            ConversationsRoute(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { entry ->
            ChatRoute(
                conversationId = entry.arguments?.getString("conversationId").orEmpty(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun ShellBottomBar(
    currentTab: AppTab,
    navController: NavHostController,
    civicVisible: Boolean = false,
) {
    AppBottomBar(
        currentTab = currentTab,
        civicVisible = civicVisible,
        onTabSelected = { tab ->
            val route =
                when (tab) {
                    AppTab.FEED -> Screen.Feed.route
                    AppTab.UJAMAA -> Screen.Ujamaa.route
                    AppTab.BARAZA -> Screen.Baraza.route
                    AppTab.CIVIC -> Screen.CivicHub.route
                    AppTab.MESSAGES -> Screen.Messages.route
                    AppTab.PROFILE -> Screen.Profile.route
                }
            navController.navigate(route) {
                popUpTo(Screen.Feed.route) { inclusive = false }
                launchSingleTop = true
                restoreState = true
            }
        },
    )
}

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        delay(1500)
        // restoreSession() can answer after the splash delay; wait for it so a signed-in
        // resident is never dropped on the login screen.
        withTimeoutOrNull(4_000) {
            while (!uiState.isSessionResolved) delay(100)
        }
        if (uiState.isLoggedIn) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "SmartJirani",
            style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        )
    }
}
