package com.aigrowth.os.core.aiengine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prompt管理器
 * 加载和管理所有Prompt，支持版本管理和参数填充
 */
@Singleton
class PromptManager @Inject constructor() {
    
    /**
     * 获取系统人格Prompt
     */
    fun getSystemPersonalityPrompt(): String {
        return """
你是一位专业的AI学习导师，名叫"成长助手"。

人格特征：
- 耐心：对初学者友好，从不嘲笑错误
- 专业：基于科学学习方法提供指导
- 鼓励：善于发现用户的进步并给予肯定
- 严格：对学习效果有高标准要求
- 个性化：根据用户的学习习惯调整教学方式

沟通风格：
- 使用简洁明了的语言
- 避免使用过于专业的术语
- 用比喻和案例帮助理解
- 给出具体的行动建议
- 用积极向上的语气

核心价值观：
- 学习是一个持续迭代的过程
- 实践比理论更重要
- 错误是学习的机会
- 每个人都有能力掌握新技能
""".trimIndent()
    }
    
    /**
     * 获取学习路线生成Prompt
     */
    fun getLearningPathPrompt(topic: String, userLevel: String): String {
        return """
基于以下信息，生成一个五级学习路线：

学习主题：$topic
用户当前水平：$userLevel

请按照以下格式输出JSON：
```json
{
  "title": "学习路线标题",
  "description": "学习路线描述",
  "levels": [
    {
      "levelNumber": 1,
      "title": "等级1标题",
      "objective": "学习目标",
      "knowledgePoints": ["知识点1", "知识点2"],
      "commonMistakes": ["常见错误1", "常见错误2"],
      "successCriteria": "达标标准"
    }
  ]
}
```

要求：
1. 生成5个等级，难度逐步递进
2. 每个等级包含明确的学习目标和知识点
3. 提供常见的错误和达标标准
4. 知识点要具体可执行
5. 确保学习路径科学合理
""".trimIndent()
    }
    
    /**
     * 获取每日任务生成Prompt
     */
    fun getDailyTaskPrompt(level: String, previousTasks: String): String {
        return """
基于当前学习等级和之前的任务，生成今日学习任务。

当前等级：$level
之前完成的任务：$previousTasks

请按照以下格式输出JSON：
```json
{
  "tasks": [
    {
      "taskType": "LEARNING/PRACTICE/TEST",
      "title": "任务标题",
      "description": "任务描述",
      "estimatedMinutes": 30,
      "content": "任务具体内容"
    }
  ]
}
```

要求：
1. 生成3-5个任务，总时长约60-90分钟
2. 任务类型多样化（学习、练习、测试）
3. 任务之间有递进关系
4. 每个任务目标明确
5. 考虑用户的学习疲劳度
""".trimIndent()
    }
    
    /**
     * 获取考核评分Prompt
     */
    fun getEvaluationPrompt(task: String, userAnswer: String): String {
        return """
请对用户的回答进行评分和分析。

任务：$task
用户回答：$userAnswer

请按照以下格式输出JSON：
```json
{
  "score": 85,
  "understandingLevel": "理解程度描述",
  "applicationAbility": "应用能力描述",
  "errorAnalysis": "错误分析",
  "supplementaryKnowledge": "补充知识"
}
```

要求：
1. 评分范围0-100分
2. 理解程度和应用能力要具体描述
3. 错误分析要指出具体问题
4. 补充知识要有针对性
5. 评分要客观公正
""".trimIndent()
    }
    
    /**
     * 获取知识卡片生成Prompt
     */
    fun getKnowledgeCardPrompt(topic: String, context: String): String {
        return """
请生成一个知识压缩卡片。

主题：$topic
学习上下文：$context

请按照以下格式输出JSON：
```json
{
  "topic": "知识点主题",
  "coreDefinition": "核心定义",
  "keyConcepts": ["关键概念1", "关键概念2"],
  "useCases": ["应用案例1", "应用案例2"],
  "commonMistakes": ["常见错误1", "常见错误2"],
  "checklist": ["检查项1", "检查项2"],
  "selfTestQuestions": ["自测问题1", "自测问题2"]
}
```

要求：
1. 核心定义简洁准确
2. 关键概念3-5个
3. 应用案例贴近实际
4. 常见错误有针对性
5. 检查清单可操作
6. 自测问题能验证理解
""".trimIndent()
    }
    
    /**
     * 获取费曼学习Prompt
     * 支持多轮对话
     */
    fun getFeynmanPrompt(
        topic: String,
        userExplanation: String,
        conversationHistory: List<FeynmanMessage> = emptyList()
    ): String {
        val historyText = if (conversationHistory.isNotEmpty()) {
            conversationHistory.joinToString("\n") { msg ->
                val role = when (msg.role) {
                    FeynmanRole.USER -> "用户"
                    FeynmanRole.AI_CHILD -> "12岁的孩子"
                }
                "$role：${msg.content}"
            }
        } else {
            "（这是第一轮对话）"
        }

        return """
你正在扮演一个聪明但没什么专业知识的12岁孩子。

主题：$topic

之前的对话：
$historyText

用户最新的解释/回答：
$userExplanation

任务：
1. 以12岁孩子的身份回应用户的解释
2. 如果用户用了你不懂的专业术语，天真地提问"这是什么意思？"
3. 如果解释有逻辑跳跃，问"为什么是这样？"
4. 如果解释很清晰，表现出"啊我懂了！"的样子
5. 给出0-100的评分，判断用户是否真正理解了这个概念
6. 如果评分>=90，表示你完全理解了，给出肯定

请按照以下格式输出JSON：
```json
{
  "score": 85,
  "childResponse": "孩子的回应内容（提问或表示理解）",
  "improvementSuggestions": ["改进建议1", "改进建议2"],
  "isUnderstood": false
}
```

注意：
- childResponse要用孩子的口吻，简短自然
- score必须客观公正，低于90分说明还有没理解的地方
- isUnderstood只在score>=90时为true
""".trimIndent()
    }
    
    /**
     * 获取成长复盘Prompt
     */
    fun getGrowthReviewPrompt(periodSummary: String): String {
        return """
基于以下学习数据，生成一份成长复盘报告。

学习数据：
$periodSummary

请按照以下格式输出JSON：
```json
{
  "overallRating": "整体评价（如：进步明显/保持稳定/需要加油）",
  "keyHighlights": ["亮点1", "亮点2"],
  "areasForImprovement": ["改进点1", "改进点2"],
  "nextWeekRecommendations": ["建议1", "建议2"],
  "encouragement": "鼓励的话"
}
```

要求：
1. 整体评价要客观但有温度
2. 亮点要具体，引用数据支撑
3. 改进点要有建设性，不打击积极性
4. 下周建议要可执行
5. 鼓励的话要真诚个性化
""".trimIndent()
    }

    /**
     * 获取记忆提取Prompt
     */
    fun getMemoryExtractionPrompt(conversationText: String): String {
        return """
分析以下用户与AI的对话，提取关于用户的记忆信息。

对话内容：
$conversationText

请提取以下类型的记忆：
1. WEAKNESS（薄弱点）：用户理解困难的概念或经常犯的错误
2. PREFERENCE（偏好）：用户喜欢的学习方式、时间、内容类型
3. HABIT（习惯）：用户的学习习惯、行为模式
4. ACHIEVEMENT（成就）：用户取得的进步和完成的目标

要求：
- 只提取明确、具体、有价值的记忆，不要猜测
- 内容要简洁，每条记忆不超过50字
- 重要性评分1-5，5分为最重要
- 如果对话中没有值得提取的记忆，返回空数组

请按照以下格式输出JSON：
```json
{
  "memories": [
    {
      "type": "WEAKNESS",
      "content": "用户对递归概念理解有困难",
      "importance": 4
    }
  ]
}
```
""".trimIndent()
    }

    /**
     * 获取内容创意生成Prompt
     */
    fun getContentIdeaPrompt(topic: String, targetAudience: String, contentType: String): String {
        return """
基于以下信息，生成自媒体内容创意。

学习主题：$topic
目标受众：$targetAudience
内容类型：$contentType

请按照以下格式输出JSON：
```json
{
  "ideas": [
    {
      "title": "视频/文章标题",
      "hook": "开场3秒钩子，吸引注意力",
      "keyPoints": ["核心观点1", "核心观点2", "核心观点3"],
      "estimatedDuration": 60,
      "targetPlatforms": ["抖音", "B站", "小红书"],
      "difficulty": "简单"
    }
  ],
  "topicAnalysis": "对主题的分析",
  "targetAudienceInsights": "受众洞察"
}
```

要求：
1. 生成3-5个创意，每个创意要有不同的切入角度
2. hook要在3秒内抓住注意力
3. 标题要包含关键词，便于SEO
4. 平台选择要考虑内容特点
5. 难度评估要客观
""".trimIndent()
    }

    /**
     * 获取成长报告生成Prompt
     */
    fun getGrowthReportPrompt(learningData: String, reportType: String): String {
        return """
基于以下学习数据，生成一份成长报告作为自媒体内容。

学习数据：
$learningData

报告类型：$reportType

请按照以下格式输出JSON：
```json
{
  "title": "报告标题",
  "summary": "内容摘要（50字以内）",
  "keyAchievements": ["关键成就1", "关键成就2"],
  "lessonsLearned": ["经验教训1", "经验教训2"],
  "growthCurve": "成长曲线描述（如：稳步上升/波动上升/突破平台期）",
  "nextSteps": ["下一步计划1", "下一步计划2"],
  "shareableContent": "可直接分享的内容片段（金句或亮点）"
}
```

要求：
1. 报告要有故事性，能够引起共鸣
2. 用具体数据支撑成就
3. 经验教训要有价值，不是空洞的道理
4. 成长曲线要形象化描述
5. shareableContent要适合社交媒体传播
""".trimIndent()
    }

    /**
     * 获取爆款分析Prompt
     */
    fun getViralAnalysisPrompt(contentTitle: String, contentUrl: String?): String {
        return """
请分析以下爆款内容，拆解其成功要素。

内容标题：$contentTitle
内容链接：${contentUrl ?: "（无链接，基于标题分析）"}

请按照以下格式输出JSON：
```json
{
  "titleAnalysis": "标题拆解：关键词、情绪、好奇心缺口",
  "hookAnalysis": "开场3秒分析：视觉冲击、悬念、共鸣",
  "structureAnalysis": "结构分析：起承转合、节奏、信息密度",
  "emotionalAppeal": "情感诉求：痛点、爽点、共鸣点",
  "targetAudience": "精准目标受众画像",
  "conversionPath": "转化路径：观看→互动→关注→转化",
  "actionableInsights": ["可操作建议1", "可操作建议2", "可操作建议3"]
}
```

要求：
1. 每个维度分析要具体，引用原文例子
2. 给出3条以上可操作的改进建议
3. 分析要深入，不停留在表面
4. 考虑当前平台算法特性
""".trimIndent()
    }

    /**
     * 获取内容脚本生成Prompt
     */
    fun getContentScriptPrompt(idea: String, platform: String, durationMinutes: Int): String {
        return """
基于以下创意，生成详细的自媒体内容脚本。

创意：$idea
目标平台：$platform
时长要求：${durationMinutes}分钟

请按照以下格式输出JSON：
```json
{
  "title": "最终标题",
  "hook": "开场钩子脚本（前3秒）",
  "scenes": [
    {
      "order": 1,
      "duration": 5,
      "visual": "视觉描述：画面、字幕、特效",
      "narration": "旁白/字幕内容",
      "notes": "拍摄注意事项"
    }
  ],
  "callToAction": "结尾引导：关注/点赞/评论",
  "hashtags": ["#标签1", "#标签2", "#标签3"]
}
```

要求：
1. 场景数量根据时长合理安排，每个场景3-5秒
2. 视觉描述要具体，包括画面、字幕位置
3. 旁白要口语化，适合配音
4. CTA要自然不生硬
5. 标签要精准，覆盖核心关键词
""".trimIndent()
    }

    /**
     * 获取资源推荐Prompt
     */
    fun getResourceRecommendationPrompt(topic: String, userLevel: String): String {
        return """
基于以下学习信息，推荐适合的学习资源。

学习主题：$topic
用户水平：$userLevel

请按照以下格式输出JSON：
```json
{
  "topic": "学习主题",
  "userLevel": "用户水平",
  "recommendedResources": [
    {
      "name": "资源名称",
      "type": "书籍/课程/文章/视频",
      "description": "资源描述",
      "suitableFor": "适合人群",
      "difficulty": "入门/进阶/高级",
      "duration": "预估学习时长",
      "url": "链接（可选）"
    }
  ],
  "learningPathSuggestion": "学习路径建议"
}
```

要求：
1. 推荐3-5个资源，涵盖不同类型
2. 资源描述要具体，说明核心价值
3. 难度评估要匹配用户水平
4. 学习路径建议要可执行
5. 优先推荐经典和高质量资源
""".trimIndent()
    }

    /**
     * 获取7天学习计划Prompt
     */
    fun getWeeklyPlanPrompt(goal: String, availableDays: Int): String {
        return """
基于以下学习目标，生成${availableDays}天的详细学习计划。

学习目标：$goal
可用天数：${availableDays}天

请按照以下格式输出JSON：
```json
{
  "goal": "学习目标",
  "planSummary": "计划摘要",
  "dailyPlans": [
    {
      "day": 1,
      "theme": "当日主题",
      "tasks": ["任务1", "任务2"],
      "estimatedMinutes": 60,
      "resources": ["推荐资源1"]
    }
  ],
  "tips": ["学习建议1", "学习建议2"]
}
```

要求：
1. 每天有明确的学习主题和任务
2. 任务之间有递进关系，循序渐进
3. 每天学习时长控制在60-90分钟
4. 适当安排复习和实践环节
5. 最后一天安排总结和输出
6. 给出2-3条实用学习建议
""".trimIndent()
    }

    /**
     * 填充Prompt模板
     */
    fun fillTemplate(template: String, params: Map<String, String>): String {
        var result = template
        params.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }
        return result
    }
}