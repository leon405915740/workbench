package com.aigrowth.os

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.accounting.app.AccountingApp
import com.accounting.app.ui.MainViewModel
import com.accounting.app.ui.screens.FinanceScreen
import com.accounting.app.ui.screens.MemoryMappingManageScreen
import com.aigrowth.os.feature.clipping.ClippingScreen
import com.aigrowth.os.feature.essay.EssayScreen
import com.aigrowth.os.feature.exercise.ExerciseScreen
import com.aigrowth.os.feature.habit.HabitScreen
import com.aigrowth.os.feature.home.HomeScreen
import com.aigrowth.os.feature.insight.InsightScreen
import com.aigrowth.os.feature.plan.PlanScreen
import com.aigrowth.os.feature.profile.ProfileScreen
import com.aigrowth.os.feature.profile.ProfileStore
import com.aigrowth.os.feature.reading.ReadingScreen
import com.aigrowth.os.feature.settings.MemoryMappingViewModel
import com.aigrowth.os.feature.settings.SettingsScreen
import com.aigrowth.os.ui.common.WorkbenchImage
import com.aigrowth.os.ui.common.WorkbenchTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGrowthOSApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route?.substringBefore('?')
    val activity = LocalContext.current as? ComponentActivity ?: return
    val accountingViewModel = remember {
        ViewModelProvider(activity, MainViewModel.factory(AccountingApp.getInstance().appRepository))
            .get(MainViewModel::class.java)
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = false }
            launchSingleTop = true
        }
    }

    val navigateToProfile: () -> Unit = {
        if (currentRoute != Screen.Profile.route) {
            navController.navigate(Screen.Profile.route) {
                popUpTo(Screen.Profile.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sidebarWidth = if (maxWidth < 600.dp) 64.dp else 72.dp
        Row(Modifier.fillMaxSize()) {
            SideNavigationBar(
                currentRoute = currentRoute,
                width = sidebarWidth,
                onNavigate = navigateTo,
                onNavigateToProfile = navigateToProfile
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f)) {
                    NavHost(navController, startDestination = Screen.Home.route, modifier = Modifier.fillMaxSize()) {
                        composable(Screen.Home.route) { HomeScreen(onNavigate = navigateTo) }
                        composable(Screen.Plan.route) { PlanScreen() }
                        composable(Screen.Habits.route) { HabitScreen() }
                        composable(Screen.Reading.route) { ReadingScreen() }
                        composable(Screen.Exercise.route) { ExerciseScreen() }
                        composable(
                            route = "${Screen.Record.route}?openAi={openAi}",
                            arguments = listOf(navArgument("openAi") { type = NavType.BoolType; defaultValue = false })
                        ) { backStackEntry ->
                            RecordScreen(
                                vm = accountingViewModel,
                                openAiEntry = backStackEntry.arguments?.getBoolean("openAi") ?: false
                            )
                        }
                        composable(Screen.Essay.route) { EssayScreen() }
                        composable(Screen.Clipping.route) { ClippingScreen() }
                        composable(Screen.Insight.route) { InsightScreen() }
                        composable(Screen.Settings.route) { SettingsScreen(onNavigateToMemoryMapping = { navController.navigate(Screen.MemoryMapping.route) }) }
                        composable(Screen.Profile.route) { ProfileScreen(onNavigateToSettings = { navController.navigate(Screen.Settings.route) }) }
                        composable(Screen.MemoryMapping.route) {
                            val vm: MemoryMappingViewModel = viewModel()
                            MemoryMappingManageScreen(memories = vm.memories.collectAsState().value, mappings = vm.mappings.collectAsState().value, memoryTotalCount = vm.memories.value.size, memorySourceFilter = vm.memorySourceFilter.collectAsState().value, expandedCategories = vm.expandedCategories.collectAsState().value, expenseCategories = vm.expenseCategories, incomeCategories = vm.incomeCategories, onAddMemory = vm::addMemory, onDeleteMemory = vm::deleteMemory, onClearAllMemories = vm::clearAllMemories, onRestoreDefaultMemories = vm::restoreDefaultMemories, onSearchMemories = vm::searchMemories, onToggleExpand = vm::toggleExpand, onSourceFilter = vm::setSourceFilter, onAddMapping = vm::addMapping, onDeleteMapping = vm::deleteMapping, onToggleMappingEnabled = vm::toggleMappingEnabled, onPromoteMappingToManual = vm::promoteMappingToManual, onCleanStaleAutoMappings = vm::cleanStaleAutoMappings, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun RecordScreen(vm: MainViewModel, openAiEntry: Boolean = false) {
    Column(Modifier.fillMaxSize()) {
        WorkbenchTopBar("记账", "记录每一笔，让生活更有掌控感")
        Box(Modifier.weight(1f)) {
            FinanceScreen(viewModel = vm, openAiEntry = openAiEntry)
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun SideNavigationBar(currentRoute: String?, width: androidx.compose.ui.unit.Dp, onNavigate: (String) -> Unit, onNavigateToProfile: () -> Unit) {
    val items = listOf(
        NavItem(Screen.Home.route, "首页", Icons.Default.Home),
        NavItem(Screen.Plan.route, "今日计划", Icons.Default.Checklist),
        NavItem(Screen.Habits.route, "习惯打卡", Icons.Default.EventAvailable),
        NavItem(Screen.Reading.route, "阅读", Icons.Default.AutoStories),
        NavItem(Screen.Exercise.route, "运动", Icons.Default.FitnessCenter),
        NavItem(Screen.Record.route, "记账", Icons.Default.Payments),
        NavItem(Screen.Essay.route, "随笔", Icons.Default.DriveFileRenameOutline),
        NavItem(Screen.Clipping.route, "剪报", Icons.Default.Newspaper),
        NavItem(Screen.Insight.route, "洞察", Icons.Default.Leaderboard)
    )
    val selectedColor = Color(0xFF397565)
    val inactiveColor = Color(0xFF687069)

    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical)),
        color = Color(0xFFF0F5F1),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val profileSelected = currentRoute == Screen.Profile.route ||
                currentRoute == Screen.Settings.route ||
                currentRoute == Screen.MemoryMapping.route

            // 个人头像入口：点击进入个人中心；无头像时显示默认人像占位
            Box(
                modifier = Modifier
                    .padding(top = 20.dp, bottom = 24.dp)
                    .size(width - 12.dp)
                    .clip(CircleShape)
                    .background(if (profileSelected) Color(0xFFDCEBE5) else Color(0xFFD6EAE2))
                    .clickable { onNavigateToProfile() },
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatar(
                    selected = profileSelected,
                    size = (width - 12.dp) * 1f,
                    selectedColor = selectedColor,
                    inactiveColor = inactiveColor
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    DrawerItem(
                        label = item.label,
                        icon = item.icon,
                        selected = selected,
                        selectedColor = selectedColor,
                        inactiveColor = inactiveColor,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * 侧边栏个人头像：优先显示本地头像，未设置时显示人形占位图标。
 */
@Composable
private fun ProfileAvatar(
    selected: Boolean,
    size: androidx.compose.ui.unit.Dp,
    selectedColor: Color,
    inactiveColor: Color
) {
    val context = LocalContext.current
    val avatarPath = ProfileStore.getAvatarPath(context)
    if (avatarPath != null) {
        WorkbenchImage(
            source = avatarPath,
            contentDescription = "个人头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape)
        )
    } else {
        Icon(
            Icons.Default.Person,
            contentDescription = "个人中心",
            tint = if (selected) selectedColor else inactiveColor,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val contentColor = if (selected) selectedColor else inactiveColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (selected) Color(0xFFDCEBE5) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(25.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Plan : Screen("plan")
    data object Habits : Screen("habits")
    data object Reading : Screen("reading")
    data object Exercise : Screen("exercise")
    data object Record : Screen("record")
    data object Essay : Screen("essay")
    data object Clipping : Screen("clipping")
    data object Insight : Screen("insight")
    data object Settings : Screen("settings")
    data object Profile : Screen("profile")
    data object MemoryMapping : Screen("memory_mapping")
}