package com.polaralias.coupio

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import com.polaralias.coupio.ui.AppViewModel
import com.polaralias.coupio.ui.AppViewModelFactory
import com.polaralias.coupio.ui.CouponAppScreen
import com.polaralias.coupio.ui.theme.CouponGlassTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CouponGlassTheme {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AppViewModel>(
                    factory = AppViewModelFactory(application),
                )
                CouponAppScreen(viewModel = viewModel)
            }
        }
    }
}

data class CameraCaptureTarget(
    val filePath: String,
    val uri: Uri,
    val displayName: String,
)

fun createCameraCaptureTarget(context: Context): CameraCaptureTarget {
    val capturesDirectory = File(context.cacheDir, "captures").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(capturesDirectory, "coupon_capture_$timestamp.jpg")
    return CameraCaptureTarget(
        filePath = file.absolutePath,
        uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        ),
        displayName = file.name,
    )
}
