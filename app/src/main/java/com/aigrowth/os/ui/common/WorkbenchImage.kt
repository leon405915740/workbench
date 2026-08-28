package com.aigrowth.os.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aigrowth.os.util.WorkbenchImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BrandGreen = Color(0xFF397565)

/**
 * 渲染一张 App 私有图片：source 可为文件绝对路径或 content:// Uri 字符串。
 * 按目标尺寸采样解码，避免大图 OOM。
 */
@Composable
fun WorkbenchImage(
    source: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "图片",
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    if (source.isNullOrBlank()) {
        ImagePlaceholder(modifier)
        return
    }
    val bitmap by produceState<Bitmap?>(initialValue = null, source) {
        value = withContext(Dispatchers.IO) { loadBitmap(context, source, 800) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        ImagePlaceholder(modifier)
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 编辑弹层中的图片附件控件：内置 Photo Picker，负责预览、选择与删除。
 * - [current]：当前展示源（null | 文件路径 | content:// Uri 字符串）
 * - [onPick]：选择成功回调（返回 content Uri，由调用方决定何时落盘）
 * - [onRemove]：删除回调（仅改状态，不改文件；文件由调用方在保存时清理）
 */
@Composable
fun WorkbenchImagePicker(
    current: String?,
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
    label: String = "图片",
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPick(uri)
    }
    val launchPicker = {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launchPicker() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = BrandGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("选择图片", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorkbenchImage(current, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column {
                    TextButton(onClick = { launchPicker() }) {
                        Text("更换", color = BrandGreen)
                    }
                    TextButton(onClick = onRemove) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

private fun loadBitmap(context: Context, source: String, reqSize: Int): Bitmap? {
    return try {
        if (source.startsWith("content://")) {
            val uri = Uri.parse(source)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > reqSize || bounds.outHeight / sample > reqSize) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > reqSize || bounds.outHeight / sample > reqSize) sample *= 2
            BitmapFactory.decodeFile(source, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * 编辑弹层图片附件的状态管理：在保存时才把选中的 content Uri 落盘为私有文件，
 * 并在最终路径变化时清理被替换的旧文件。避免临时文件与取消时的清理负担。
 */
class ImageAttachmentState internal constructor(
    private val initialPath: String?,
    private val context: Context
) {
    private val draftState = mutableStateOf<String?>(initialPath)
    val draft: String? get() = draftState.value
    private var pickedUri: Uri? = null

    fun onPick(uri: Uri) {
        pickedUri = uri
        draftState.value = uri.toString()
    }

    fun onRemove() {
        pickedUri = null
        draftState.value = null
    }

    /** 返回最终持久化路径；链路上清理旧文件。 */
    fun resolve(): String? {
        val final = when {
            pickedUri != null -> WorkbenchImageStore.save(context, pickedUri!!) ?: initialPath
            else -> draftState.value
        }
        if (final != initialPath) WorkbenchImageStore.delete(context, initialPath)
        return final
    }
}

@Composable
fun rememberImageAttachment(initial: String?): ImageAttachmentState {
    val context = LocalContext.current
    return remember(context, initial) { ImageAttachmentState(initial, context) }
}