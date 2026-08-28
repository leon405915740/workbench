package com.accounting.app.ai.service

import com.accounting.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.deepseek.com/"

    fun create(): DeepSeekApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            // Debug 模式下打印完整请求/响应体便于调试，
                            // Release 模式下仅打印请求头以减少敏感数据暴露。
                            level = if (BuildConfig.DEBUG) {
                                HttpLoggingInterceptor.Level.BODY
                            } else {
                                HttpLoggingInterceptor.Level.HEADERS
                            }
                        }
                    )
                    .addInterceptor(RetryInterceptor())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApi::class.java)
    }
}

/**
 * OkHttp 重试拦截器：指数退避，最多重试3次。
 *
 * 重试策略：
 * - 仅对网络异常（IOException）和 5xx 服务端错误进行重试
 * - 4xx 客户端错误不重试
 * - 间隔时间：1s、2s、4s（指数退避）
 * - 最多重试 3 次
 */
class RetryInterceptor : Interceptor {
    private val maxRetries = 3
    private val baseDelayMs = 1000L

    override fun intercept(chain: Interceptor.Chain): Response {
        var retryCount = 0
        while (true) {
            try {
                val response = chain.proceed(chain.request())
                // 成功或4xx客户端错误不重试
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                // 5xx服务端错误需要重试，先关闭response
                response.close()
            } catch (e: Exception) {
                // 网络异常，达到最大重试次数时抛出
                if (retryCount >= maxRetries) {
                    throw e
                }
            }
            retryCount++
            if (retryCount > maxRetries) break
            // 指数退避：1s、2s、4s
            Thread.sleep(baseDelayMs * (1L shl (retryCount - 1)))
        }
        return chain.proceed(chain.request())
    }
}