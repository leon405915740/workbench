package com.aigrowth.os.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 网络配置
 */
@Singleton
class NetworkConfig @Inject constructor() {
    val connectTimeout = 30L
    val readTimeout = 30L
    val writeTimeout = 30L
}