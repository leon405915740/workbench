package com.accounting.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.accounting.app.ai.service.DeepSeekApi as PlannerDeepSeekApi

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
    private const val BASE_URL = "https://api.deepseek.com/"

    /**
     * 创建 DeepSeekApi 实例。
     * 每次调用都会构造新的 Retrofit 实例，
     * 在调用频次不高的记账场景下性能可接受。
     */
    fun create(): DeepSeekApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
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

    fun createPlannerApi(): PlannerDeepSeekApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
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
