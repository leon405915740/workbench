package com.aigrowth.os.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

/**
 * 工作台普通记录的 App 私有图片存储：把 content Uri 复制到私有目录并管理删除。
 * 与账单附件（feature/accounting 的 AttachmentStore）分离，各自独立目录。
 */
object WorkbenchImageStore {

    private const val DIR_NAME = "workbench_images"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** 复制 content Uri 到私有目录，返回绝对路径；失败返回 null。 */
    fun save(context: Context, uri: Uri): String? {
        return try {
            val ext = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(context.contentResolver.getType(uri))
                ?: "jpg"
            val file = File(dir(context), "img_${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** 仅删除位于本 store 目录内的私有文件，避免误删。 */
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