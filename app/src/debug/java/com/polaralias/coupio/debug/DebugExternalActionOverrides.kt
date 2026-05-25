package com.polaralias.coupio.debug

import android.content.Context
import android.util.Base64
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.ui.AppViewModel
import java.io.File

object DebugExternalActionOverrides {
    @JvmStatic
    fun maybeHandleImport(context: Context, viewModel: AppViewModel): Boolean {
        val prefs = context.getSharedPreferences(DebugExternalSourceConfig.PREFERENCES_NAME, Context.MODE_PRIVATE)
        val kind = prefs.getString(DebugExternalSourceConfig.KEY_PICKER_KIND, DebugExternalSourceConfig.PICKER_IMAGE)
            ?: DebugExternalSourceConfig.PICKER_IMAGE

        val source = when (kind) {
            DebugExternalSourceConfig.PICKER_PDF -> DebugImportSource(
                mediaType = CouponMediaType.PDF,
                mimeType = "application/pdf",
                fileName = "Debug Picker Coupon.pdf",
                bytes = DEBUG_PDF_BYTES,
            )
            else -> DebugImportSource(
                mediaType = CouponMediaType.IMAGE,
                mimeType = "image/png",
                fileName = "Debug Picker Coupon.png",
                bytes = DEBUG_PNG_BYTES,
            )
        }

        val file = ensureDebugFile(context, source.fileName, source.bytes)
        viewModel.openImportEditor(
            tempFilePath = file.absolutePath,
            mediaMimeType = source.mimeType,
            mediaType = source.mediaType,
            mediaDisplayName = file.name,
        )
        return true
    }

    @JvmStatic
    fun maybeHandleCapture(context: Context, viewModel: AppViewModel): Boolean {
        val file = ensureDebugFile(
            context = context,
            fileName = "Debug Camera Capture.jpg",
            bytes = DEBUG_JPEG_BYTES,
        )
        viewModel.openImportEditor(
            tempFilePath = file.absolutePath,
            mediaMimeType = "image/jpeg",
            mediaType = CouponMediaType.IMAGE,
            mediaDisplayName = file.name,
        )
        return true
    }

    private fun ensureDebugFile(context: Context, fileName: String, bytes: ByteArray): File {
        val directory = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(directory, fileName).apply {
            writeBytes(bytes)
        }
    }
}

private data class DebugImportSource(
    val mediaType: CouponMediaType,
    val mimeType: String,
    val fileName: String,
    val bytes: ByteArray,
)

private val DEBUG_PNG_BYTES: ByteArray = Base64.decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9sN4T2kAAAAASUVORK5CYII=",
    Base64.DEFAULT,
)

private val DEBUG_JPEG_BYTES: ByteArray = Base64.decode(
    "/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBxAQEBAQEBAPEA8QDw8PDw8PDw8QEA8PFREWFhURFRUYHSggGBolGxUVITEhJSkrLi4uFx8zODMsNygtLisBCgoKDQ0NDg0NDisZFRkrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrKysrK//AABEIAAEAAgMBIgACEQEDEQH/xAAbAAADAQEBAQEAAAAAAAAAAAAABQYDBAcCAf/EADUQAAEDAgQDBgUEAwAAAAAAAAECAwQFEQASITEGE0FRImFxFDKBkaEHQrHB0fAiUmKy8P/EABkBAQADAQEAAAAAAAAAAAAAAAABAgMEBf/EACQRAQEAAgICAgIDAQAAAAAAAAABAhEDIRIxBEETIlFhBRQy/9oADAMBAAIRAxEAPwD9YREQEREBERAREQEREBERAREQEREBERA//Z",
    Base64.DEFAULT,
)

private val DEBUG_PDF_BYTES: ByteArray = """
    %PDF-1.4
    1 0 obj
    << /Type /Catalog /Pages 2 0 R >>
    endobj
    2 0 obj
    << /Type /Pages /Count 1 /Kids [3 0 R] >>
    endobj
    3 0 obj
    << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
    endobj
    4 0 obj
    << /Length 39 >>
    stream
    BT
    /F1 18 Tf
    72 96 Td
    (Debug Picker Coupon) Tj
    ET
    endstream
    endobj
    5 0 obj
    << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
    endobj
    xref
    0 6
    0000000000 65535 f 
    0000000009 00000 n 
    0000000058 00000 n 
    0000000115 00000 n 
    0000000241 00000 n 
    0000000330 00000 n 
    trailer
    << /Size 6 /Root 1 0 R >>
    startxref
    400
    %%EOF
""".trimIndent().toByteArray()
