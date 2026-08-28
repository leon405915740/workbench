package com.aigrowth.os.feature.creator.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aigrowth.os.core.aiengine.*
import com.aigrowth.os.core.aiengine.ApiKeyService
import com.aigrowth.os.core.database.entity.Content
import com.aigrowth.os.core.database.entity.ContentType
import com.aigrowth.os.feature.creator.domain.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 创作ViewModel
 * 管理自媒体创作相关的UI状态和业务逻辑
 */
@HiltViewModel
class CreatorViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    // 内容列表
    private val _contents = MutableStateFlow<List<Content>>(emptyList())
    val contents: StateFlow<List<Content>> = _contents

    // 当前筛选类型
    private val _selectedType = MutableStateFlow<ContentType?>(null)
    val selectedType: StateFlow<ContentType?> = _selectedType

    // AI生成状态
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    // 创意生成结果
    private val _ideaResponse = MutableStateFlow<ContentIdeaResponse?>(null)
    val ideaResponse: StateFlow<ContentIdeaResponse?> = _ideaResponse

    // 成长报告结果
    private val _reportResponse = MutableStateFlow<GrowthReportResponse?>(null)
    val reportResponse: StateFlow<GrowthReportResponse?> = _reportResponse

    // 爆款分析结果
    private val _viralResponse = MutableStateFlow<ViralAnalysisResponse?>(null)
    val viralResponse: StateFlow<ViralAnalysisResponse?> = _viralResponse

    // 脚本生成结果
    private val _scriptResponse = MutableStateFlow<ContentScriptResponse?>(null)
    val scriptResponse: StateFlow<ContentScriptResponse?> = _scriptResponse

    // 资源推荐结果
    private val _resourceResponse = MutableStateFlow<ResourceRecommendationResponse?>(null)
    val resourceResponse: StateFlow<ResourceRecommendationResponse?> = _resourceResponse

    // 周计划结果
    private val _weeklyPlanResponse = MutableStateFlow<WeeklyPlanResponse?>(null)
    val weeklyPlanResponse: StateFlow<WeeklyPlanResponse?> = _weeklyPlanResponse

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadContents()
    }

    /**
     * 加载内容列表
     */
    fun loadContents() {
        viewModelScope.launch {
            _selectedType.value?.let { type ->
                contentRepository.getContentsByType(type)
                    .catch { e -> _error.value = e.message }
                    .collect { contents ->
                        _contents.value = contents
                    }
            } ?: run {
                contentRepository.getAllContents()
                    .catch { e -> _error.value = e.message }
                    .collect { contents ->
                        _contents.value = contents
                    }
            }
        }
    }

    /**
     * 按类型筛选
     */
    fun filterByType(contentType: ContentType?) {
        _selectedType.value = contentType
        loadContents()
    }

    /**
     * 生成内容创意
     */
    fun generateContentIdea(
        topic: String,
        targetAudience: String,
        contentType: String
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _ideaResponse.value = null

            val result = contentRepository.generateContentIdea(
                topic, targetAudience, contentType, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _ideaResponse.value = response
                    // 自动保存第一个创意
                    response.ideas.firstOrNull()?.let { idea ->
                        contentRepository.saveContent(
                            title = idea.title,
                            contentType = ContentType.IDEA,
                            content = idea.hook,
                            structure = idea.keyPoints.joinToString(","),
                            tags = idea.targetPlatforms.joinToString(",")
                        )
                    }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成创意失败"
                }
            )

            _isGenerating.value = false
        }
    }

    /**
     * 生成成长报告
     */
    fun generateGrowthReport(
        learningData: String,
        reportType: String
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _reportResponse.value = null

            val result = contentRepository.generateGrowthReport(
                learningData, reportType, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _reportResponse.value = response
                    contentRepository.saveContent(
                        title = response.title,
                        contentType = ContentType.GROWTH_REPORT,
                        content = response.summary,
                        structure = response.keyAchievements.joinToString(","),
                        tags = response.hashtags.joinToString(",")
                    )
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成报告失败"
                }
            )

            _isGenerating.value = false
        }
    }

    /**
     * 分析爆款内容
     */
    fun analyzeViralContent(
        contentTitle: String,
        contentUrl: String?
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _viralResponse.value = null

            val result = contentRepository.analyzeViralContent(
                contentTitle, contentUrl, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _viralResponse.value = response
                    contentRepository.saveContent(
                        title = contentTitle,
                        contentType = ContentType.ANALYSIS,
                        content = response.structureAnalysis,
                        structure = response.actionableInsights.joinToString(","),
                        tags = response.targetAudience
                    )
                },
                onFailure = { e ->
                    _error.value = e.message ?: "分析失败"
                }
            )

            _isGenerating.value = false
        }
    }

    /**
     * 生成内容脚本
     */
    fun generateContentScript(
        idea: String,
        platform: String,
        durationMinutes: Int
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _scriptResponse.value = null

            val result = contentRepository.generateContentScript(
                idea, platform, durationMinutes, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _scriptResponse.value = response
                    val scriptContent = response.scenes.joinToString("\n") { scene ->
                        "[${scene.order}] ${scene.duration}秒: ${scene.visual}\n${scene.narration}"
                    }
                    contentRepository.saveContent(
                        title = response.title,
                        contentType = ContentType.SCRIPT,
                        content = scriptContent,
                        structure = response.scenes.joinToString(",") { it.visual },
                        tags = response.hashtags.joinToString(",")
                    )
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成脚本失败"
                }
            )

            _isGenerating.value = false
        }
    }

    /**
     * 保存内容
     */
    fun saveContent(
        title: String,
        contentType: ContentType,
        content: String
    ) {
        viewModelScope.launch {
            contentRepository.saveContent(title, contentType, content)
            loadContents()
        }
    }

    /**
     * 删除内容
     */
    fun deleteContent(content: Content) {
        viewModelScope.launch {
            contentRepository.deleteContent(content)
            loadContents()
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearIdeaResponse() {
        _ideaResponse.value = null
    }

    fun clearReportResponse() {
        _reportResponse.value = null
    }

    fun clearViralResponse() {
        _viralResponse.value = null
    }

    fun clearScriptResponse() {
        _scriptResponse.value = null
    }

    /**
     * 推荐学习资源
     */
    fun recommendResources(
        topic: String,
        userLevel: String
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _resourceResponse.value = null

            val result = contentRepository.recommendResources(
                topic, userLevel, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _resourceResponse.value = response
                },
                onFailure = { e ->
                    _error.value = e.message ?: "资源推荐失败"
                }
            )

            _isGenerating.value = false
        }
    }

    /**
     * 生成7天学习计划
     */
    fun generateWeeklyPlan(
        goal: String,
        availableDays: Int = 7
    ) {
        val apiKey = apiKeyService.getApiKey()
        if (apiKey.isBlank()) {
            _error.value = "请先在设置中配置API Key"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            _weeklyPlanResponse.value = null

            val result = contentRepository.generateWeeklyPlan(
                goal, availableDays, apiKey
            )

            result.fold(
                onSuccess = { response ->
                    _weeklyPlanResponse.value = response
                },
                onFailure = { e ->
                    _error.value = e.message ?: "生成计划失败"
                }
            )

            _isGenerating.value = false
        }
    }

    fun clearResourceResponse() {
        _resourceResponse.value = null
    }

    fun clearWeeklyPlanResponse() {
        _weeklyPlanResponse.value = null
    }
}
