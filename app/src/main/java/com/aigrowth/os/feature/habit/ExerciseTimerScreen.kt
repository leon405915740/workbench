package com.aigrowth.os.feature.habit

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aigrowth.os.core.database.workbench.entity.Habit
import com.aigrowth.os.ui.common.WorkbenchTopBar
import com.aigrowth.os.ui.theme.AccentGreenSoft
import com.aigrowth.os.ui.theme.InkSecondary
import com.aigrowth.os.ui.theme.InkText
import com.aigrowth.os.ui.theme.ModuleGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTimerScreen(
    onNavigateBack: () -> Unit,
    vm: ExerciseTimerViewModel = hiltViewModel()
) {
    val habits by vm.habits.collectAsState()
    val elapsedSeconds by vm.elapsedSeconds.collectAsState()
    val isRunning by vm.isRunning.collectAsState()
    val editRequest by vm.editRequest.collectAsState()
    val toast by vm.toast.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WorkbenchTopBar(
                title = "记运动",
                subtitle = "专注当下，记录每一步",
                icon = Icons.Default.DirectionsRun,
                iconTint = ModuleGreen
            )
            Spacer(Modifier.weight(1f))

            TimerDisplay(elapsedSeconds = elapsedSeconds)

            Spacer(Modifier.weight(1f))

            if (isRunning) {
                Button(
                    onClick = vm::end,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModuleGreen)
                ) {
                    Text("结束", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = vm::start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModuleGreen)
                ) {
                    Text("开始", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editRequest?.let { request ->
        LogEditorSheet(
            habits = habits,
            request = request,
            onSave = { habitId, date, duration, note, category ->
                vm.saveLog(habitId, date, duration, note, category)
                onNavigateBack()
            },
            onDelete = null,
            onDismiss = {
                vm.dismissEdit()
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun TimerDisplay(elapsedSeconds: Long) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(256.dp)) {
        CircularProgressIndicator(
            progress = { ((elapsedSeconds % 60) / 60f).coerceIn(0f, 1f) },
            modifier = Modifier.size(256.dp),
            strokeWidth = 6.dp,
            color = ModuleGreen,
            trackColor = AccentGreenSoft
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatClock(elapsedSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = InkText
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "已进行 ${formatExerciseElapsed(elapsedSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = InkSecondary
            )
        }
    }
}

private fun formatClock(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
