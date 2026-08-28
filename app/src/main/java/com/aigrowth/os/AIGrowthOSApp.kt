package com.aigrowth.os

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import com.aigrowth.os.ui.common.WorkbenchTopBar

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
        WorkbenchTopBar("记账", onOpenDrawer, "记录每一笔，让生活更有掌控感")
        Box(Modifier.weight(1f)) {
            MainScreen(vm)
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)
@Composable private fun SideNavigationDrawer(currentRoute: String?, onNavigate: (String) -> Unit) {
    val items = listOf(NavItem(Screen.CheckIn.route, "健身打卡", Icons.Default.FitnessCenter), NavItem(Screen.Media.route, "自媒体", Icons.Default.VideoLibrary), NavItem(Screen.English.route, "学英语", Icons.Default.Translate), NavItem(Screen.Record.route, "记账", Icons.Default.AccountBalanceWallet))
    ModalDrawerSheet(modifier = Modifier.width(132.dp), drawerContainerColor = Color(0xFFF0F5F1)) {
        Column(Modifier.fillMaxHeight().padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 20.dp, bottom = 24.dp).size(64.dp).background(Color(0xFFD6EAE2), RoundedCornerShape(32.dp)), contentAlignment = Alignment.Center) { Text("AI", style = MaterialTheme.typography.titleLarge, color = Color(0xFF397565)) }
            items.forEach { item ->
                val selected = currentRoute == item.route
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (selected) Color(0xFFDCEBE5) else Color.Transparent, RoundedCornerShape(18.dp)).clickable { onNavigate(item.route) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(item.icon, item.label, tint = if (selected) Color(0xFF397565) else Color(0xFF89918A), modifier = Modifier.size(25.dp))
                    Text(item.label.substringBefore("打卡").take(3), style = MaterialTheme.typography.labelSmall, color = if (selected) Color(0xFF397565) else Color(0xFF687069))
                }
            }
            Spacer(Modifier.weight(1f))
            val selected = currentRoute == Screen.Settings.route
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (selected) Color(0xFFDCEBE5) else Color.Transparent, RoundedCornerShape(18.dp)).clickable { onNavigate(Screen.Settings.route) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Settings, "设置", tint = Color(0xFF89918A)); Text("设置", style = MaterialTheme.typography.labelSmall, color = Color(0xFF687069)) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

sealed class Screen(val route: String) { data object CheckIn : Screen("checkin"); data object Media : Screen("media"); data object English : Screen("english"); data object Record : Screen("record"); data object Settings : Screen("settings"); data object MemoryMapping : Screen("memory_mapping") }
