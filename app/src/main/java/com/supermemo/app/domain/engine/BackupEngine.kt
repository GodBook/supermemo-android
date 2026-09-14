package com.supermemo.app.domain.engine

import android.content.Context
import android.net.Uri
import com.supermemo.app.data.local.model.NoteWithDetails
import com.supermemo.app.domain.model.AttachmentDto
import com.supermemo.app.domain.model.BackupManifest
import com.supermemo.app.domain.model.CategoryDto
import com.supermemo.app.domain.model.FullBackupData
import com.supermemo.app.domain.model.NoteDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupEngine {

    /**
     * 将备忘录列表转换为全量备份结构对象
     */
    fun buildBackupData(notes: List<NoteWithDetails>): FullBackupData {
        val categoryMap = mutableMapOf<String, CategoryDto>()
        val tagSet = mutableSetOf<String>()
        var totalAttachments = 0

        val noteDtos = notes.map { item ->
            val note = item.note
            val category = item.category
            if (category != null) {
                categoryMap[category.name] = CategoryDto(
                    name = category.name,
                    colorHex = category.colorHex,
                    iconName = category.iconName,
                    sortOrder = category.sortOrder
                )
            }

            val tags = item.tags.map { it.name }
            tagSet.addAll(tags)

            totalAttachments += item.attachments.size

            val attachmentDtos = item.attachments.map {
                AttachmentDto(
                    fileName = it.fileName,
                    relativePath = it.relativePath,
                    fileSize = it.fileSize
                )
            }

            NoteDto(
                title = note.title,
                content = note.content,
                categoryName = category?.name,
                tags = tags,
                isPinned = note.isPinned,
                isArchived = note.isArchived,
                isLocked = note.isLocked,
                colorHex = note.colorHex,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                attachments = attachmentDtos
            )
        }

        val manifest = BackupManifest(
            version = 1,
            totalNotes = notes.size,
            totalCategories = categoryMap.size,
            totalTags = tagSet.size,
            totalAttachments = totalAttachments
        )

        return FullBackupData(
            manifest = manifest,
            categories = categoryMap.values.toList(),
            tags = tagSet.toList(),
            notes = noteDtos
        )
    }

    /**
     * 序列化备份数据为标准 JSON 字符串
     */
    fun toJsonString(backupData: FullBackupData): String {
        val root = JSONObject()

        // manifest
        val manifestObj = JSONObject().apply {
            put("version", backupData.manifest.version)
            put("appName", backupData.manifest.appName)
            put("exportTime", backupData.manifest.exportTime)
            put("totalNotes", backupData.manifest.totalNotes)
            put("totalCategories", backupData.manifest.totalCategories)
            put("totalTags", backupData.manifest.totalTags)
            put("totalAttachments", backupData.manifest.totalAttachments)
        }
        root.put("manifest", manifestObj)

        // categories
        val catArray = JSONArray()
        backupData.categories.forEach { cat ->
            val cObj = JSONObject().apply {
                put("name", cat.name)
                put("colorHex", cat.colorHex)
                put("iconName", cat.iconName)
                put("sortOrder", cat.sortOrder)
            }
            catArray.put(cObj)
        }
        root.put("categories", catArray)

        // tags
        val tagArray = JSONArray()
        backupData.tags.forEach { tagArray.put(it) }
        root.put("tags", tagArray)

        // notes
        val notesArray = JSONArray()
        backupData.notes.forEach { n ->
            val nObj = JSONObject().apply {
                put("title", n.title)
                put("content", n.content)
                put("categoryName", n.categoryName ?: JSONObject.NULL)
                val nTags = JSONArray()
                n.tags.forEach { nTags.put(it) }
                put("tags", nTags)
                put("isPinned", n.isPinned)
                put("isArchived", n.isArchived)
                put("isLocked", n.isLocked)
                put("colorHex", n.colorHex ?: JSONObject.NULL)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)

                val attArray = JSONArray()
                n.attachments.forEach { a ->
                    val aObj = JSONObject().apply {
                        put("fileName", a.fileName)
                        put("relativePath", a.relativePath)
                        put("fileSize", a.fileSize)
                    }
                    attArray.put(aObj)
                }
                put("attachments", attArray)
            }
            notesArray.put(nObj)
        }
        root.put("notes", notesArray)

        return root.toString(2)
    }

    /**
     * 解析 JSON 字符串为备份数据对象
     */
    fun parseFromJson(jsonStr: String): FullBackupData {
        val root = JSONObject(jsonStr)
        val manifestObj = root.optJSONObject("manifest") ?: JSONObject()
        val manifest = BackupManifest(
            version = manifestObj.optInt("version", 1),
            appName = manifestObj.optString("appName", "超级备忘录"),
            exportTime = manifestObj.optLong("exportTime", System.currentTimeMillis()),
            totalNotes = manifestObj.optInt("totalNotes", 0),
            totalCategories = manifestObj.optInt("totalCategories", 0),
            totalTags = manifestObj.optInt("totalTags", 0),
            totalAttachments = manifestObj.optInt("totalAttachments", 0)
        )

        val categories = mutableListOf<CategoryDto>()
        val catArray = root.optJSONArray("categories")
        if (catArray != null) {
            for (i in 0 until catArray.length()) {
                val cObj = catArray.getJSONObject(i)
                categories.add(
                    CategoryDto(
                        name = cObj.getString("name"),
                        colorHex = cObj.optString("colorHex", "#6750A4"),
                        iconName = cObj.optString("iconName", "folder"),
                        sortOrder = cObj.optInt("sortOrder", 0)
                    )
                )
            }
        }

        val tags = mutableListOf<String>()
        val tagArray = root.optJSONArray("tags")
        if (tagArray != null) {
            for (i in 0 until tagArray.length()) {
                tags.add(tagArray.getString(i))
            }
        }

        val notes = mutableListOf<NoteDto>()
        val notesArray = root.optJSONArray("notes")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val nObj = notesArray.getJSONObject(i)
                val nTags = mutableListOf<String>()
                val ntArray = nObj.optJSONArray("tags")
                if (ntArray != null) {
                    for (j in 0 until ntArray.length()) {
                        nTags.add(ntArray.getString(j))
                    }
                }

                val attachments = mutableListOf<AttachmentDto>()
                val attArray = nObj.optJSONArray("attachments")
                if (attArray != null) {
                    for (k in 0 until attArray.length()) {
                        val aObj = attArray.getJSONObject(k)
                        attachments.add(
                            AttachmentDto(
                                fileName = aObj.getString("fileName"),
                                relativePath = aObj.getString("relativePath"),
                                fileSize = aObj.optLong("fileSize", 0L)
                            )
                        )
                    }
                }

                notes.add(
                    NoteDto(
                        title = nObj.optString("title", ""),
                        content = nObj.optString("content", ""),
                        categoryName = if (nObj.isNull("categoryName")) null else nObj.optString("categoryName"),
                        tags = nTags,
                        isPinned = nObj.optBoolean("isPinned", false),
                        isArchived = nObj.optBoolean("isArchived", false),
                        isLocked = nObj.optBoolean("isLocked", false),
                        colorHex = if (nObj.isNull("colorHex")) null else nObj.optString("colorHex"),
                        createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis()),
                        attachments = attachments
                    )
                )
            }
        }

        return FullBackupData(manifest, categories, tags, notes)
    }

    /**
     * 导出为全量 ZIP 包到指定的输出流 (SAF DocumentUri)
     */
    suspend fun exportToZip(
        context: Context,
        destinationUri: Uri,
        backupData: FullBackupData,
        filesDir: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(destinationUri) ?: return@withContext false
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 1. 写入 backup.json
                val jsonBytes = toJsonString(backupData).toByteArray(Charsets.UTF_8)
                zipOut.putNextEntry(ZipEntry("backup.json"))
                zipOut.write(jsonBytes)
                zipOut.closeEntry()

                // 2. 写入独立的 Markdown 文件夹体系
                backupData.notes.forEachIndexed { index, noteDto ->
                    val folder = noteDto.categoryName?.replace("/", "_") ?: "未分类"
                    val safeTitle = (noteDto.title.ifBlank { "备忘录_${index + 1}" })
                        .replace(Regex("""[\\/:*?"<>|]"""), "_")
                    val entryName = "markdown/$folder/$safeTitle.md"

                    val mdContent = buildString {
                        appendLine("---")
                        appendLine("title: ${noteDto.title}")
                        appendLine("created: ${noteDto.createdAt}")
                        appendLine("updated: ${noteDto.updatedAt}")
                        if (!noteDto.categoryName.isNullOrBlank()) appendLine("category: ${noteDto.categoryName}")
                        if (noteDto.tags.isNotEmpty()) appendLine("tags: [${noteDto.tags.joinToString(", ")}]")
                        appendLine("---")
                        appendLine()
                        appendLine(noteDto.content)
                    }

                    zipOut.putNextEntry(ZipEntry(entryName))
                    zipOut.write(mdContent.toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }

                // 3. 写入图片附件
                val attachmentsDir = File(filesDir, "attachments")
                if (attachmentsDir.exists()) {
                    backupData.notes.flatMap { it.attachments }.forEach { att ->
                        val localFile = File(filesDir, att.relativePath)
                        if (localFile.exists() && localFile.isFile) {
                            zipOut.putNextEntry(ZipEntry("attachments/${localFile.name}"))
                            localFile.inputStream().buffered().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 从 ZIP 备份恢复数据与附件图片
     */
    suspend fun importFromZip(
        context: Context,
        sourceUri: Uri,
        filesDir: File
    ): FullBackupData? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
            val attachmentsDir = File(filesDir, "attachments").apply { if (!exists()) mkdirs() }
            var backupData: FullBackupData? = null

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        if (entryName == "backup.json") {
                            val jsonStr = zipIn.bufferedReader(Charsets.UTF_8).readText()
                            backupData = parseFromJson(jsonStr)
                        } else if (entryName.startsWith("attachments/")) {
                            val fileName = File(entryName).name
                            val destFile = File(attachmentsDir, fileName)
                            FileOutputStream(destFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            backupData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 单篇备忘录导出为标准 Markdown 格式内容
     */
    fun exportSingleNoteToMarkdown(note: NoteWithDetails): String {
        return buildString {
            appendLine("# ${note.note.title.ifEmpty { "未命名备忘录" }}")
            appendLine()
            appendLine("> 创建于: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(note.note.createdAt))}")
            if (note.category != null) {
                appendLine("> 分类: ${note.category.name}")
            }
            if (note.tags.isNotEmpty()) {
                appendLine("> 标签: ${note.tags.joinToString(", ") { it.name }}")
            }
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(note.note.content)
        }
    }
}
