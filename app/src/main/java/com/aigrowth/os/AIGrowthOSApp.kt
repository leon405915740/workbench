package com.aigrowth.os

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.accounting.app.AccountingApp
import com.accounting.app.MainScreen
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.screens.MemoryMappingManageScreen
import com.aigrowth.os.feature.settings.MemoryMappingViewModel
import com.aigrowth.os.feature.settings.SettingsScreen
import com.aigrowth.os.feature.simple.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGrowthOSApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        SideNavigationDrawer(currentRoute) { route ->
            scope.launch { drawerState.close() }
            navController.navigate(route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true }
        }
    }) {
        NavHost(navController, startDestination = Screen.CheckIn.route, modifier = Modifier.fillMaxSize()) {
            composable(Screen.CheckIn.route) { CheckInScreen(openDrawer) }
            composable(Screen.Media.route) { MediaScreen(openDrawer) }
            composable(Screen.English.route) { EnglishScreen(openDrawer) }
            composable(Screen.Record.route) { RecordScreen(openDrawer) }
            composable(Screen.Settings.route) { SettingsScreen(onOpenDrawer = openDrawer, onNavigateToMemoryMapping = { navController.navigate(Screen.MemoryMapping.route) }) }
            composable(Screen.MemoryMapping.route) {
                val vm: MemoryMappingViewModel = viewModel()
                MemoryMappingManageScreen(memories = vm.memories.collectAsState().value, mappings = vm.mappings.collectAsState().value, memoryTotalCount = vm.memories.value.size, memorySourceFilter = vm.memorySourceFilter.collectAsState().value, expandedCategories = vm.expandedCategories.collectAsState().value, expenseCategories = vm.expenseCategories, incomeCategories = vm.incomeCategories, onAddMemory = vm::addMemory, onDeleteMemory = vm::deleteMemory, onClearAllMemories = vm::clearAllMemories, onRestoreDefaultMemories = vm::restoreDefaultMemories, onSearchMemories = vm::searchMemories, onToggleExpand = vm::toggleExpand, onSourceFilter = vm::setSourceFilter, onAddMapping = vm::addMapping, onDeleteMapping = vm::deleteMapping, onToggleMappingEnabled = vm::toggleMappingEnabled, onPromoteMappingToManual = vm::promoteMappingToManual, onCleanStaleAutoMappings = vm::cleanStaleAutoMappings, onBack = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RecordScreen(onOpenDrawer: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity ?: return
    val vm = remember { ViewModelProvider(activity, MainViewModel.factory(AccountingApp.getInstance().appRepository)).get(MainViewModel::class.java) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("记账") },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, "打开导航")
                }
            },
        )
        Box(Modifier.weight(1f)) {
            MainScreen(vm)
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)
@Composable private fun SideNavigationDrawer(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(NavItem(Screen.CheckIn.route, "健身打卡", Icons.Default.FitnessCenter), NavItem(Screen.Media.route, "自媒体", Icons.Default.VideoLibrary), NavItem(Screen.English.route, "学英语", Icons.Default.Translate), NavItem(Screen.Record.route, "记账", Icons.Default.AccountBalanceWallet))
    ModalDrawerSheet { Spacer(Modifier.height(24.dp)); Text("工作台", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)); items.forEach { item -> NavigationDrawerItem(label = { Text(item.label) }, icon = { Icon(item.icon, item.label) }, selected = currentRoute == item.route, onClick = { onNavigate(item.route) }, modifier = Modifier.padding(horizontal = 8.dp)) }; Spacer(Modifier.weight(1f)); NavigationDrawerItem(label = { Text("设置") }, icon = { Icon(Icons.Default.Settings, "设置") }, selected = currentRoute == Screen.Settings.route, onClick = { onNavigate(Screen.Settings.route) }, modifier = Modifier.padding(horizontal = 8.dp)); Spacer(Modifier.height(12.dp)) }
}

sealed class Screen(val route: String) { data object CheckIn : Screen("checkin"); data object Media : Screen("media"); data object English : Screen("english"); data object Record : Screen("record"); data object Settings : Screen("settings"); data object MemoryMapping : Screen("memory_mapping") }
