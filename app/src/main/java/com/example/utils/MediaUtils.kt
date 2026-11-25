package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object MediaUtils {

    fun compressImageUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 72
    ): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            val longest = max(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            while (longest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height).coerceAtLeast(1)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled !== bitmap) bitmap.recycle()
            bitmap.recycle()
            val bytes = out.toByteArray()
            if (bytes.size > 310_000) {
                // Re-compress gently if large
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val out2 = ByteArrayOutputStream()
                decoded.compress(Bitmap.CompressFormat.JPEG, 50, out2)
                decoded.recycle()
                Base64.encodeToString(out2.toByteArray(), Base64.NO_WRAP)
            } else {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64Bitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    data class DocumentPayload(
        val name: String,
        val mimeType: String,
        val base64Data: String,
        val sizeBytes: Long
    )

    fun readDocumentUri(context: Context, uri: Uri): DocumentPayload? {
        return try {
            var name = "Document"
            var sizeBytes = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "Document"
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(minOf(sizeBytes.toInt().takeIf { it > 0 } ?: 300_000, 300_000))
                val read = input.read(buffer)
                if (read > 0) buffer.copyOf(read) else ByteArray(0)
            } ?: ByteArray(0)
            val encoded = if (bytes.size <= 300_000) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                ""
            }
            DocumentPayload(name = name, mimeType = mime, base64Data = encoded, sizeBytes = sizeBytes)
        } catch (_: Exception) {
            null
        }
    }

    fun encodeBytesToBase64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun decodeBase64ToBytes(base64: String): ByteArray? {
        if (base64.isBlank()) return null
        return try {
            Base64.decode(base64, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}
