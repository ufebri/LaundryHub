package com.raylabs.laundryhub.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raylabs.laundryhub.R
import com.raylabs.laundryhub.ui.history.HistoryScreenView
import com.raylabs.laundryhub.ui.home.HomeScreen
import com.raylabs.laundryhub.ui.inventory.InventoryScreenView
import com.raylabs.laundryhub.ui.navigation.BottomNavItem
import com.raylabs.laundryhub.ui.navigation.OrderScreen
import com.raylabs.laundryhub.ui.profile.ProfileScreenView

@Composable
fun LaundryHubStarter(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = { BottomBar(navController) },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.screenRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.screenRoute) {
                HomeScreen()
            }
            composable(BottomNavItem.History.screenRoute) {
                HistoryScreenView()
            }
            composable(BottomNavItem.Order.screenRoute) {
                OrderScreen()
            }
            composable(BottomNavItem.Inventory.screenRoute) {
                InventoryScreenView()
            }
            composable(BottomNavItem.Profile.screenRoute) {
                ProfileScreenView()
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController, modifier: Modifier = Modifier) {
    BottomNavigation(
        modifier = modifier,
        backgroundColor = colorResource(id = R.color.colorPrimary),
        contentColor = Color.White
    ) {
        //List Menu Item
        val items = listOf(
            BottomNavItem.Home,
            BottomNavItem.History,
            BottomNavItem.Order,
            BottomNavItem.Inventory,
            BottomNavItem.Profile
        )
        BottomNavigation {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.map { item ->
                BottomNavigationItem(
                    icon = {
                        Icon(
                            painterResource(id = item.icon),
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 9.sp
                        )
                    },
                    selectedContentColor = Color.Black,
                    unselectedContentColor = Color.Black.copy(0.4f),
                    alwaysShowLabel = true,
                    selected = currentRoute == item.screenRoute,
                    onClick = {
                        navController.navigate(item.screenRoute) {

                            navController.graph.startDestinationRoute?.let { screenRoute ->
                                popUpTo(screenRoute) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Preview(apiLevel = 33)
@Composable
fun BottomNavigationPreview() {
    LaundryHubStarter()
}