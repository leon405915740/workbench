package com.accounting.app.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

/**
 * 账单凭证图片的 App 私有存储：把相册/相机返回的 content Uri 复制到私有目录，
 * 并统一管理删除。数据库只保存返回的绝对路径，不保存临时 Uri 或云内容。
 */
object AttachmentStore {
    private const val DIR_NAME = "bill_attachments"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** 复制 content Uri 到私有目录，返回绝对路径；失败返回 null。 */
    fun save(context: Context, uri: Uri): String? {
        return try {
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(context.contentResolver.getType(uri))
                ?: "jpg"
            val file = File(dir(context), "bill_${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** 删除私有凭证文件；仅当路径位于本 store 目录内才删除，避免误删。 */
    fun delete(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists() && file.parentFile?.absolutePath == dir(context).absolutePath) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }
}