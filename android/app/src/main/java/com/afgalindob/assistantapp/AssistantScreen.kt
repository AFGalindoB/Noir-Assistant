package com.afgalindob.assistantapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afgalindob.assistantapp.ui.screens.TaskListScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afgalindob.assistantapp.navigation.HomeGraph
import com.afgalindob.assistantapp.navigation.TaskList
import com.afgalindob.assistantapp.navigation.NoteList
import com.afgalindob.assistantapp.navigation.NavigationBottomBar
import com.afgalindob.assistantapp.navigation.TrashGraph
import com.afgalindob.assistantapp.ui.screens.AccountScreen
import com.afgalindob.assistantapp.ui.screens.NotesListScreen
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import kotlinx.coroutines.launch
import androidx.navigation.compose.navigation
import com.afgalindob.assistantapp.navigation.Account
import com.afgalindob.assistantapp.navigation.TrashScreen
import com.afgalindob.assistantapp.ui.screens.TrashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import com.afgalindob.assistantapp.navigation.NavigationSideBar
import com.afgalindob.assistantapp.navigation.NavigationActions
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.afgalindob.assistantapp.navigation.AccountGraph
import com.afgalindob.assistantapp.navigation.VoiceTaskGraph
import com.afgalindob.assistantapp.navigation.VoiceTaskScreen
import com.afgalindob.assistantapp.ui.screens.VoiceTaskScreen
import com.afgalindob.assistantapp.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val currentScreen by mainViewModel.currentScreen.collectAsState()
    val isTransitioning by mainViewModel.isTransitioning.collectAsState()
    val topBarActions by mainViewModel.topBarActions.collectAsState()
    val bottomBarContent by mainViewModel.bottomBarContent.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navActions = remember(navController, drawerState, scope) {
        NavigationActions(
            scope = scope,
            drawerState = drawerState,
            onStartTransition = { route -> mainViewModel.navigateTo(route) }
        )
    }

    LaunchedEffect(currentScreen) {
        val currentDest = navController.currentDestination

        if (currentDest?.hasRoute(currentScreen::class) == false) {
            navController.navigate(currentScreen) {
                launchSingleTop = true

                if (currentScreen is HomeGraph) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                    restoreState = false
                } else {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    restoreState = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            NavigationSideBar(
                drawerState = drawerState,
                showServerFunctions = mainViewModel.isUrlConfigured,
                currentDestination = currentDestination,
                navActions = navActions,
                scope = scope
            )
        }
    ) {
        Scaffold (
            topBar = {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!isTransitioning){
                                    scope.launch { drawerState.open() }
                                }
                            },
                            enabled = !isTransitioning
                        ) {
                            Icon(
                                painterResource(R.drawable.menu),
                                contentDescription = "Open Drawer",
                                tint = OnSurfaceSecondary.copy(
                                    alpha = if (isTransitioning) 0.5f else 1f
                                )
                            )
                        }
                    },
                    actions = { topBarActions() }
                )
            },
            bottomBar = {
                if (bottomBarContent != null) {
                    bottomBarContent?.invoke()
                } else {
                    val inHome = currentDestination?.hasRoute<TaskList>() == true ||
                            currentDestination?.hasRoute<NoteList>() == true

                    if (inHome) {
                        val isDisabled = drawerState.isOpen || drawerState.isAnimationRunning || isTransitioning
                        NavigationBottomBar(
                            navController = navController,
                            isInteractionDisabled = isDisabled,
                            navActions = navActions
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeGraph,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(SurfaceContainer),
                enterTransition = { androidx.compose.animation.EnterTransition.None },
                exitTransition = { androidx.compose.animation.ExitTransition.None }
            ) {
                val subViewModels = mainViewModel.subViewModels

                navigation<HomeGraph>(startDestination = TaskList) {
                    composable<TaskList> {
                        TaskListScreen(
                            viewModel = subViewModels.task,
                            mainViewModel = mainViewModel
                        )
                    }
                    composable<NoteList> {
                        NotesListScreen(
                            viewModel = subViewModels.note,
                            onRendered = { mainViewModel.notifyScreenRendered() }
                        )
                    }
                }
                navigation<AccountGraph>(startDestination = Account) {
                    composable<Account> {
                        AccountScreen(
                            viewModel = subViewModels.settings,
                            onRendered = { mainViewModel.notifyScreenRendered() }
                        )
                    }
                }
                navigation<TrashGraph>(startDestination = TrashScreen) {
                    composable<TrashScreen> {
                        TrashScreen(
                            viewModel = subViewModels.trash,
                            onRendered = { mainViewModel.notifyScreenRendered() }
                        )
                    }
                }
                navigation<VoiceTaskGraph>(startDestination = VoiceTaskScreen) {
                    composable<VoiceTaskScreen> {
                        VoiceTaskScreen(
                            viewModel = subViewModels.audio,
                            mainViewModel = mainViewModel
                        )
                    }
                }
            }
        }
    }
}