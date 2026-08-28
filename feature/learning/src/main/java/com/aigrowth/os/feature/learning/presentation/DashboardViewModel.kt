package com.aigrowth.os.feature.learning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.database.entity.DailyTask
import com.aigrowth.os.core.database.entity.Goal
import com.aigrowth.os.core.database.entity.GrowthRecord
import com.aigrowth.os.core.database.entity.TaskStatus
import com.aigrowth.os.feature.learning.domain.DailyTaskRepository
import com.aigrowth.os.feature.learning.domain.GoalRepository
import com.aigrowth.os.feature.learning.domain.GrowthRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar

/**
 * 今日成长概览
 */
data class DashboardOverview(
    val todayGrowthScore: Int = 0,        // 今日成长值 0-100
    val tasksCompleted: Int = 0,
    val tasksTotal: Int = 0,
    val learningMinutes: Int = 0,
    val knowledgeCardsCreated: Int = 0,
    val masteryScore: Int = 0,
    val activeGoalsCount: Int = 0,
    val dailyQuote: String = "每一天的积累，都是未来的底气。"
)

/**
 * 模块进度
 */
data class ModuleProgress(
    val name: String,
    val progress: Float,        // 0-1
    val statusText: String,
    val isAvailable: Boolean,
    val iconName: String
)

/**
 * 今日成长驾驶舱 ViewModel
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dailyTaskRepository: DailyTaskRepository,
    private val growthRecordRepository: GrowthRecordRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _todayTasks = MutableStateFlow<List<DailyTask>>(emptyList())
    val todayTasks: StateFlow<List<DailyTask>> = _todayTasks

    private val _activeGoals = MutableStateFlow<List<Goal>>(emptyList())
    val activeGoals: StateFlow<List<Goal>> = _activeGoals

    private val _todayRecord = MutableStateFlow<GrowthRecord?>(null)
    val todayRecord: StateFlow<GrowthRecord?> = _todayRecord

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * 今日成长概览（响应式聚合）
     */
    val overview: StateFlow<DashboardOverview> = combine(
        _todayTasks,
        _activeGoals,
        _todayRecord
    ) { tasks, goals, record ->
        computeOverview(tasks, goals, record)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardOverview()
    )

    /**
     * 各模块进度（响应式聚合）
     */
    val moduleProgresses: StateFlow<List<ModuleProgress>> = combine(
        _todayTasks,
        _activeGoals
    ) { tasks, goals ->
        computeModuleProgresses(tasks, goals)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadDashboard()
    }

    /**
     * 加载首页数据
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true

            // 加载今日任务
            launch {
                dailyTaskRepository.getTodayTasks()
                    .catch { }
                    .collect { tasks ->
                        _todayTasks.value = tasks
                    }
            }

            // 加载活跃目标
            launch {
                goalRepository.getActiveGoals()
                    .catch { }
                    .collect { goals ->
                        _activeGoals.value = goals
                    }
            }

            // 观察今日成长记录（完成任务后自动刷新）
            launch {
                growthRecordRepository.observeTodayRecord()
                    .catch { e ->
                        android.util.Log.e("DashboardViewModel", "Failed to load today's record", e)
                    }
                    .collect { record ->
                        _todayRecord.value = record
                        _isLoading.value = false
                    }
            }
        }
    }

    /**
     * 计算今日成长概览
     * 成长值 = 任务完成率(40) + 时长达标率(30) + 知识卡片(20) + 掌握度(10)
     */
    private fun computeOverview(
        tasks: List<DailyTask>,
        goals: List<Goal>,
        record: GrowthRecord?
    ): DashboardOverview {
        val tasksCompleted = tasks.count { it.status == TaskStatus.COMPLETED }
        val tasksTotal = tasks.size
        val learningMinutes = record?.learningMinutes ?: 0
        val knowledgeCardsCreated = record?.knowledgeCardsCreated ?: 0
        val masteryScore = record?.masteryScore ?: 0
        val activeGoalsCount = goals.size

        // 今日成长值计算
        val taskScore = if (tasksTotal > 0) {
            (tasksCompleted.toFloat() / tasksTotal * 40).toInt()
        } else 0
        val timeScore = (learningMinutes.toFloat() / 60 * 30).toInt().coerceAtMost(30)
        val cardScore = (knowledgeCardsCreated * 10).coerceAtMost(20)
        val masteryScoreValue = (masteryScore * 0.1f).toInt().coerceAtMost(10)
        val todayGrowthScore = (taskScore + timeScore + cardScore + masteryScoreValue)
            .coerceIn(0, 100)

        return DashboardOverview(
            todayGrowthScore = todayGrowthScore,
            tasksCompleted = tasksCompleted,
            tasksTotal = tasksTotal,
            learningMinutes = learningMinutes,
            knowledgeCardsCreated = knowledgeCardsCreated,
            masteryScore = masteryScore,
            activeGoalsCount = activeGoalsCount
        )
    }

    /**
     * 计算各模块进度
     * AI开发学习模块基于真实数据，其余模块占位
     */
    private fun computeModuleProgresses(
        tasks: List<DailyTask>,
        goals: List<Goal>
    ): List<ModuleProgress> {
        // AI开发学习模块 - 基于实际目标与任务
        val hasAIGoal = goals.any {
            it.title.contains("AI", ignoreCase = true) ||
            it.title.contains("开发", ignoreCase = true) ||
            it.title.contains("学习", ignoreCase = true) ||
            it.title.contains("Python", ignoreCase = true)
        }
        val aiDevProgress = if (tasks.isNotEmpty()) {
            val completed = tasks.count { it.status == TaskStatus.COMPLETED }
            completed.toFloat() / tasks.size
        } else 0f

        return listOf(
            ModuleProgress(
                name = "AI开发学习",
                progress = aiDevProgress,
                statusText = if (hasAIGoal) "进行中" else "未开始",
                isAvailable = true,
                iconName = "ai"
            ),
            ModuleProgress(
                name = "英语提升",
                progress = 0f,
                statusText = "即将上线",
                isAvailable = false,
                iconName = "english"
            ),
            ModuleProgress(
                name = "健身管理",
                progress = 0f,
                statusText = "即将上线",
                isAvailable = false,
                iconName = "fitness"
            ),
            ModuleProgress(
                name = "自媒体创作",
                progress = 0f,
                statusText = "即将上线",
                isAvailable = false,
                iconName = "creator"
            )
        )
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
