package com.aigrowth.os.feature.profile

import android.content.Context

/**
 * 工作台用户信息存储：昵称 + 头像路径。
 * 头像文件由 WorkbenchImageStore 落盘（filesDir/workbench_images/），这里只存路径。
 * API Key 不在此存储——记账解析的 Key 由 feature/accounting 模块 UserPreferences 持有，
 * 个人中心页通过 AccountingBridge/repository 读写，避免双份配置。
 */
object ProfileStore {

    private const val PREFS_NAME = "workbench_profile"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR_PATH = "avatar_path"
    private const val DEFAULT_NICKNAME = "工作台用户"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNickname(context: Context): String =
        prefs(context).getString(KEY_NICKNAME, DEFAULT_NICKNAME) ?: DEFAULT_NICKNAME

    fun setNickname(context: Context, nickname: String) {
        prefs(context).edit().putString(KEY_NICKNAME, nickname.trim()).apply()
    }

    fun getAvatarPath(context: Context): String? =
        prefs(context).getString(KEY_AVATAR_PATH, null)

    fun setAvatarPath(context: Context, path: String?) {
        prefs(context).edit().putString(KEY_AVATAR_PATH, path).apply()
    }
}