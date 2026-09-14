package com.supermemo.app.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {

    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * 将用户选择的图片从 contentUri 安全复制到应用的私有内部沙盒目录中
     */
    suspend fun saveImageToInternalStorage(
        context: Context,
        imageUri: Uri
    ): Pair<String, Long>? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, ATTACHMENTS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileExt = context.contentResolver.getType(imageUri)?.substringAfterLast("/") ?: "jpg"
            val uniqueFileName = "img_${UUID.randomUUID()}.$fileExt"
            val destFile = File(dir, uniqueFileName)

            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val outputStream = FileOutputStream(destFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val relativePath = "$ATTACHMENTS_DIR/$uniqueFileName"
            val fileSize = destFile.length()

            Pair(relativePath, fileSize)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取附件的完整绝对文件句柄
     */
    fun getAttachmentFile(context: Context, relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    /**
     * 删除内部存储中的图片文件
     */
    suspend fun deleteImageFile(context: Context, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
