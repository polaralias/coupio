package com.polaralias.coupio.debug

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.IntentCompat
import com.polaralias.coupio.createShareUri
import java.io.File

class DebugOpenDocumentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kind = getSharedPreferences(DebugExternalSourceConfig.PREFERENCES_NAME, MODE_PRIVATE)
            .getString(DebugExternalSourceConfig.KEY_PICKER_KIND, DebugExternalSourceConfig.PICKER_IMAGE)

        val source = when (kind) {
            DebugExternalSourceConfig.PICKER_PDF -> DebugSource(
                mimeType = "application/pdf",
                file = ensureDebugFile("Debug Picker Coupon.pdf", DEBUG_PDF_BYTES),
            )
            else -> DebugSource(
                mimeType = "image/png",
                file = ensureDebugFile("Debug Picker Coupon.png", DEBUG_PNG_BYTES),
            )
        }

        setResult(
            RESULT_OK,
            Intent().apply {
                data = createShareUri(this@DebugOpenDocumentActivity, source.file.absolutePath)
                type = source.mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
        finish()
    }

    private fun ensureDebugFile(fileName: String, bytes: ByteArray): File {
        val directory = File(cacheDir, "captures").apply { mkdirs() }
        return File(directory, fileName).apply {
            if (!exists()) {
                writeBytes(bytes)
            }
        }
    }
}

class DebugImageCaptureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val outputUri = IntentCompat.getParcelableExtra(intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)
        if (outputUri != null) {
            contentResolver.openOutputStream(outputUri)?.use { stream ->
                stream.write(DEBUG_JPEG_BYTES)
            }
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }
}

private data class DebugSource(
    val mimeType: String,
    val file: File,
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
