package com.accounting.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.accounting.app.ai.service.DeepSeekApi as PlannerDeepSeekApi
import com.accounting.app.data.local.pref.AiProviders

/**
 * Retrofit 客户端单例工厂。
 *
 * 设计要点：
 * - BASE_URL 固定为 DeepSeek 官方域名
 * - Authorization 不在拦截器中固定，由调用方每次请求动态传入，
 *   以支持用户在设置页随时修改 API Key
 * - 日志拦截器仅用于调试，发布版可通过 BuildConfig.DEBUG 控制
 * - 超时配置：连接 30s，读取 60s，适配大模型较长响应时间
 */
object RetrofitClient {
    private const val DEEPSEEK_URL = "https://api.deepseek.com/"
    private const val OPENCODE_GO_URL = "https://opencode.ai/zen/go/"

    /**
     * 按 AI 提供商返回 base url：deepseek / opencode_go。
     */
    fun baseUrlFor(provider: String): String =
        if (provider == AiProviders.OPENCODE_GO) OPENCODE_GO_URL else DEEPSEEK_URL

    /**
     * 创建 DeepSeekApi 实例。
     * 每次调用都会构造新的 Retrofit 实例，
     * 在调用频次不高的记账场景下性能可接受。
     */
    fun create(provider: String = AiProviders.DEEPSEEK): DeepSeekApi {
        return Retrofit.Builder()
            .baseUrl(baseUrlFor(provider))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }

    fun createPlannerApi(provider: String = AiProviders.DEEPSEEK): PlannerDeepSeekApi {
        return Retrofit.Builder()
            .baseUrl(baseUrlFor(provider))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlannerDeepSeekApi::class.java)
    }
}
