package com.weibosave.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

object MediaStoreHelper {

    // Saves image bytes to Pictures/WeiboSave using IS_PENDING pattern (Android 10+)
    // so the file is invisible to gallery apps until fully written.
    fun saveImage(context: Context, bytes: ByteArray, filename: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType(filename))
            put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/WeiboSave")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun mimeType(filename: String) = when {
        filename.endsWith(".png", ignoreCase = true) -> "image/png"
        filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
        filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
        else -> "image/jpeg"
    }
}
