// Crea questo file: app/src/main/java/com/fitgymtrack/app/utils/FileAttachmentManager.kt
package com.fitgymtrack.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.fitgymtrack.app.models.LocalAttachment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

object FileAttachmentManager {

    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    private const val MAX_FILES = 3

    /**
     * Converte un URI in LocalAttachment
     */
    fun uriToLocalAttachment(context: Context, uri: Uri): LocalAttachment? {
        return try {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                it.moveToFirst()

                val name = if (nameIndex >= 0) it.getString(nameIndex) else "unknown_file"
                val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                Log.d("FileAttachmentManager", "File: $name, Size: $size, Type: $mimeType")

                LocalAttachment(
                    uri = uri.toString(),
                    name = name,
                    size = size,
                    mimeType = mimeType
                )
            }
        } catch (e: Exception) {
            Log.e("FileAttachmentManager", "Errore conversione URI: ${e.message}", e)
            null
        }
    }

    /**
     * Valida un file attachment
     */
    fun validateAttachment(attachment: LocalAttachment): ValidationResult {
        val errors = mutableListOf<String>()

        // Controlla dimensione
        if (attachment.size > MAX_FILE_SIZE) {
            errors.add("File ${attachment.name} è troppo grande (max 5MB)")
        }

        // Controlla tipo di file
        val allowedTypes = listOf(
            "image/jpeg", "image/png", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )

        if (!allowedTypes.contains(attachment.mimeType)) {
            errors.add("Tipo file ${attachment.mimeType} non supportato")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valida una lista di attachment
     */
    fun validateAttachments(attachments: List<LocalAttachment>): ValidationResult {
        val errors = mutableListOf<String>()

        // Controlla numero massimo
        if (attachments.size > MAX_FILES) {
            errors.add("Massimo $MAX_FILES file consentiti")
        }

        // Valida singoli file
        attachments.forEach { attachment ->
            val validation = validateAttachment(attachment)
            if (!validation.isValid) {
                errors.addAll(validation.errors)
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Copia un file dal URI al cache directory per l'upload
     */
    fun copyFileToCache(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val cacheDir = File(context.cacheDir, "feedback_attachments")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("FileAttachmentManager", "File copiato: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("FileAttachmentManager", "Errore copia file: ${e.message}", e)
            null
        }
    }

    /**
     * Pulisce i file temporanei
     */
    fun cleanupTempFiles(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "feedback_attachments")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                        Log.d("FileAttachmentManager", "File rimosso: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileAttachmentManager", "Errore pulizia file: ${e.message}", e)
        }
    }

    /**
     * Formatta la dimensione del file
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> String.format(Locale.getDefault(),"%.1f MB", mb)
            kb >= 1 -> String.format(Locale.getDefault(),"%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)