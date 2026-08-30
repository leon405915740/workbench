package com.aigrowth.os.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.accounting.app.AccountingApp
import com.accounting.app.data.local.pref.AiProviders
import com.accounting.app.log.AppLogger
import com.aigrowth.os.ui.common.WorkbenchCard
import com.aigrowth.os.ui.common.WorkbenchImage
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.ui.theme.AccentGreen
import com.aigrowth.os.ui.theme.DangerInk
import com.aigrowth.os.ui.theme.InkSecondary
import com.aigrowth.os.ui.theme.InkText
import com.aigrowth.os.ui.theme.ModuleGreen
import com.aigrowth.os.ui.theme.PaperBg
import com.aigrowth.os.ui.theme.PaperBorder
import com.aigrowth.os.ui.theme.PaperCard
import com.aigrowth.os.util.WorkbenchImageStore
import com.aigrowth.os.util.isNotificationAccessEnabled
import com.aigrowth.os.util.isOverlayGranted
import com.aigrowth.os.util.openNotificationAccessSettings
import com.aigrowth.os.util.openOverlaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 个人中心页：头像（相册上传）/ 昵称修改 / 服务配置（API Key）。
 * 头像与昵称存入 ProfileStore；API Key 存记账模块 UserPreferences（经 AppRepository），
 * 保存后记账解析与问答链路即可使用，避免双份配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bridge = remember { AccountingApp.getBridge() }

    // 记账设置：自动学习 / 付款后唤起
    val autoLearn by bridge.isAutoLearnEnabled().catch { emit(true) }.collectAsState(initial = true)
    val quickRecord by bridge.isQuickRecordEnabled().catch { emit(true) }.collectAsState(initial = true)

    // 权限状态（通知监听 / 悬浮窗），仅在 ON_RESUME 时刷新
    var notifGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf(ProfileStore.getNickname(context)) }
    var avatarPath by remember { mutableStateOf(ProfileStore.getAvatarPath(context)) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    // 服务配置编辑态
    var selectedProvider by remember { mutableStateOf(AiProviders.DEEPSEEK) }
    var deepSeekKeyInput by remember { mutableStateOf("") }
    var openCodeKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("deepseek-v4-flash-vision-exp") }
    var configLoaded by remember { mutableStateOf(false) }
    var testingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    var serviceConfigExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val repo = AccountingApp.getInstance().appRepository
        selectedProvider = repo.getProvider()
        modelInput = repo.getModel()
        deepSeekKeyInput = repo.getDeepSeekApiKey()
        openCodeKeyInput = repo.getOpenCodeApiKey()
        val activeKey = repo.getApiKey()
        serviceConfigExpanded = activeKey.isBlank() || modelInput.isBlank()
        configLoaded = true
    }

    // 权限状态初始化
    LaunchedEffect(Unit) {
        notifGranted = context.isNotificationAccessEnabled()
        overlayGranted = context.isOverlayGranted()
    }

    // 从系统设置页返回时刷新权限开关状态
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifGranted = context.isNotificationAccessEnabled()
                overlayGranted = context.isOverlayGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }
    val launchAvatarPicker = {
        avatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun saveAvatar() {
        val uri = pickedUri
        if (uri == null) return
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, "ProfileScreen", "saveAvatar 入口")
        val newPath = WorkbenchImageStore.save(context, uri)
        if (newPath != null) {
            WorkbenchImageStore.delete(context, avatarPath)
            avatarPath = newPath
            ProfileStore.setAvatarPath(context, newPath)
            pickedUri = null
            AppLogger.d(requestId, "ProfileScreen", "saveAvatar 成功: $newPath")
        } else {
            AppLogger.e(requestId, "ProfileScreen", "saveAvatar 失败：复制图片失败", null)
        }
    }

    fun saveNickname() {
        val requestId = AppLogger.generateRequestId()
        AppLogger.i(requestId, "ProfileScreen", "saveNickname 入口")
        ProfileStore.setNickname(context, nickname)
        AppLogger.d(requestId, "ProfileScreen", "saveNickname 成功")
    }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = AccentGreen,
        checkedTrackColor = AccentGreen.copy(alpha = 0.3f),
        uncheckedThumbColor = InkSecondary,
        uncheckedTrackColor = PaperBorder
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBg)
    ) {
        WorkbenchTopBar(
            title = "个人中心",
            subtitle = "管理头像、昵称与服务配置",
            icon = Icons.Default.Person,
            iconTint = ModuleGreen
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== 卡片 1: 个人信息 =====
            WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PaperCard)
                            .clickable { launchAvatarPicker() },
                        contentAlignment = Alignment.Center
                    ) {
                        WorkbenchImage(
                            source = pickedUri?.toString() ?: avatarPath,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // 无头像时显示相机图标引导
                        if (pickedUri == null && avatarPath == null) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "上传头像",
                                tint = InkSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击更换头像",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("昵称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            saveNickname()
                            saveAvatar()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("保存")
                    }
                }
            }

            // ===== 卡片 2: 服务配置 =====
            WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                val activeKeyInput = if (selectedProvider == AiProviders.OPENCODE_GO) openCodeKeyInput else deepSeekKeyInput
                val maskedActiveKey = if (activeKeyInput.isBlank()) "未配置" else AppLogger.maskApiKey(activeKeyInput)

                if (serviceConfigExpanded) {
                    Text(
                        text = "服务配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 提供商下拉框
                    var providerMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = providerMenuExpanded,
                        onExpandedChange = { providerMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedProvider == AiProviders.OPENCODE_GO) "OpenCode Go" else "DeepSeek",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AI 提供商") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = providerMenuExpanded,
                            onDismissRequest = { providerMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("DeepSeek") },
                                onClick = {
                                    selectedProvider = AiProviders.DEEPSEEK
                                    providerMenuExpanded = false
                                    connectionResult = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("OpenCode Go") },
                                onClick = {
                                    selectedProvider = AiProviders.OPENCODE_GO
                                    providerMenuExpanded = false
                                    connectionResult = null
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 按选中提供商显示对应 API Key 输入框（双 Key 分开存，切换互不影响）
                    if (selectedProvider == AiProviders.OPENCODE_GO) {
                        OutlinedTextField(
                            value = openCodeKeyInput,
                            onValueChange = { openCodeKeyInput = it; connectionResult = null },
                            label = { Text("OpenCode Go API Key") },
                            placeholder = { Text("opc-xxx...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "用于 OpenCode Go 记账解析与分类推断，仅本地保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    } else {
                        OutlinedTextField(
                            value = deepSeekKeyInput,
                            onValueChange = { deepSeekKeyInput = it; connectionResult = null },
                            label = { Text("DeepSeek API Key") },
                            placeholder = { Text("sk-xxx...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "用于 DeepSeek 记账解析与分类推断，仅本地保存",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 模型名（默认 deepseek-v4-flash-vision-exp）
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it; connectionResult = null },
                        label = { Text("模型") },
                        placeholder = { Text("deepseek-v4-flash-vision-exp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val saveEnabled = configLoaded && !testingConnection && modelInput.isNotBlank() && activeKeyInput.trim().isNotBlank()

                    Button(
                        onClick = {
                            val requestId = AppLogger.generateRequestId()
                            AppLogger.i(requestId, "ProfileScreen", "saveAndTestConnection 入口, provider=$selectedProvider")
                            scope.launch(Dispatchers.IO) {
                                testingConnection = true
                                connectionResult = null
                                val repo = AccountingApp.getInstance().appRepository
                                val keyToSave = activeKeyInput.trim()
                                val modelToSave = modelInput.trim()
                                runCatching {
                                    repo.testConnection(selectedProvider, keyToSave, modelToSave, requestId)
                                }.onSuccess { result ->
                                    result.fold(
                                        onSuccess = { msg ->
                                            // 测试通过 → 保存配置并收起服务配置区
                                            val saveRequestId = AppLogger.generateRequestId()
                                            repo.setProvider(selectedProvider, saveRequestId)
                                            if (selectedProvider == AiProviders.OPENCODE_GO) {
                                                repo.setOpenCodeApiKey(keyToSave, saveRequestId)
                                            } else {
                                                repo.setApiKey(keyToSave, saveRequestId)
                                            }
                                            repo.setModel(modelToSave, saveRequestId)
                                            AppLogger.d(saveRequestId, "ProfileScreen", "保存并测试连接成功：$msg")
                                            connectionResult = "✅ 保存成功，$msg"
                                            serviceConfigExpanded = false
                                        },
                                        onFailure = { e ->
                                            AppLogger.e(requestId, "ProfileScreen", "保存并测试连接失败：${e.message}", e)
                                            connectionResult = "❌ ${e.message}"
                                        }
                                    )
                                }.onFailure {
                                    AppLogger.e(requestId, "ProfileScreen", "保存并测试连接异常：${it.message}", it)
                                    connectionResult = "❌ ${it.message}"
                                }
                                testingConnection = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = saveEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text(if (testingConnection) "测试连接中..." else "保存并测试连接")
                    }

                    connectionResult?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.startsWith("✅")) ModuleGreen else DangerInk
                        )
                    }
                } else {
                    // 收起态：仅展示当前配置摘要，点击展开以修改
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { serviceConfigExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "服务配置",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InkText
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "展开服务配置",
                                tint = InkSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI 提供商：${if (selectedProvider == AiProviders.OPENCODE_GO) "OpenCode Go" else "DeepSeek"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkText
                        )
                        Text(
                            text = "模型：$modelInput",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                        Text(
                            text = "API Key：$maskedActiveKey",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击展开可修改配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen
                        )
                    }
                }
            }

            // ===== 卡片 2.5: 记账设置 =====
            WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "记账设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkText
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 自动学习分类
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "自动学习分类",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkText
                    )
                    Switch(
                        checked = autoLearn,
                        onCheckedChange = { enabled ->
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "ProfileScreen", "setAutoLearnEnabled 入口: enabled=$enabled")
                                    bridge.setAutoLearnEnabled(enabled, requestId)
                                    AppLogger.d(requestId, "ProfileScreen", "setAutoLearnEnabled 出口: 成功")
                                }.onFailure {
                                    AppLogger.e(AppLogger.generateRequestId(), "ProfileScreen", "setAutoLearnEnabled 失败", it)
                                }
                            }
                        },
                        colors = switchColors
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "记账后自动记忆分类选择，提升下次匹配准确率",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 付款后唤起记账
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "付款后唤起记账",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkText
                    )
                    Switch(
                        checked = quickRecord,
                        onCheckedChange = { enabled ->
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    val requestId = AppLogger.generateRequestId()
                                    AppLogger.i(requestId, "ProfileScreen", "setQuickRecordEnabled 入口: enabled=$enabled")
                                    bridge.setQuickRecordEnabled(enabled, requestId)
                                    AppLogger.d(requestId, "ProfileScreen", "setQuickRecordEnabled 出口: 成功")
                                }.onFailure {
                                    AppLogger.e(AppLogger.generateRequestId(), "ProfileScreen", "setQuickRecordEnabled 失败", it)
                                }
                            }
                        },
                        colors = switchColors
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "微信/支付宝/云闪付付款成功后，自动唤起记账卡片（需开启通知监听与悬浮窗权限）",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 通知监听权限
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "通知监听权限",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkText
                        )
                        Text(
                            text = if (notifGranted) "已开启" else "去开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (notifGranted) ModuleGreen else AccentGreen
                        )
                    }
                    Switch(
                        checked = notifGranted,
                        onCheckedChange = { context.openNotificationAccessSettings() },
                        colors = switchColors
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 悬浮窗权限
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "悬浮窗权限",
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkText
                        )
                        Text(
                            text = if (overlayGranted) "已开启" else "去开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overlayGranted) ModuleGreen else AccentGreen
                        )
                    }
                    Switch(
                        checked = overlayGranted,
                        onCheckedChange = { context.openOverlaySettings() },
                        colors = switchColors
                    )
                }
            }

            // ===== 卡片 3: 更多设置入口 =====
            WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSettings() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "全部设置（记账开关 / 权限 / 数据导出）",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkText
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = InkSecondary
                    )
                }
            }
        }
    }
}
