package com.afgalindob.assistantapp.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afgalindob.assistantapp.R
import com.afgalindob.assistantapp.ui.theme.AccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnAccentPrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfacePrimary
import com.afgalindob.assistantapp.ui.theme.OnSurfaceSecondary
import com.afgalindob.assistantapp.ui.theme.SurfaceContainer
import androidx.navigation.NavDestination.Companion.hasRoute

@Composable
fun NavigationBottomBar(
    navController: NavHostController,
    isInteractionDisabled: Boolean,
    navActions: NavigationActions
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar (
        containerColor = SurfaceContainer,
        tonalElevation = 8.dp
    ) {
        val items = remember {
            listOf(
                Triple(TaskList, R.drawable.task_list, R.string.tasks),
                Triple(NoteList, R.drawable.notes, R.string.notes)
            )
        }

        items.forEach { (destination, iconRes, labelRes) ->
            val isSelected = currentDestination?.hasRoute(destination::class) ?: false

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected && !isInteractionDisabled){
                        navActions.navigateInHome(destination)
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(labelRes)
                    )
                },
                label = { Text(stringResource(labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OnAccentPrimary,
                    indicatorColor = AccentPrimary,
                    selectedTextColor = OnSurfacePrimary,
                    unselectedIconColor = OnSurfaceSecondary,
                    unselectedTextColor = OnSurfaceSecondary
                ),
                modifier = Modifier.graphicsLayer { alpha = if (isInteractionDisabled) 0.5f else 1f }
            )
        }
    }
}