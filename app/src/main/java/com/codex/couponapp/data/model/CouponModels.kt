package com.polaralias.coupio.data.model

import android.net.Uri
import java.time.LocalDate

const val DEFAULT_PENDING_WINDOW_MILLIS = 60L * 60L * 1000L

enum class CouponMediaType {
    IMAGE,
    PDF,
}

enum class CouponState {
    AVAILABLE,
    PENDING,
    LOCKED,
}

enum class CouponReusePolicy(val label: String) {
    SINGLE_USE("Single use"),
    DAILY("Once per day"),
    WEEKLY("Once per week"),
    MONTHLY("Once per month"),
    ALWAYS("Always"),
}

data class CouponDraftInput(
    val sourceUri: String? = null,
    val tempFilePath: String? = null,
    val mediaMimeType: String,
    val mediaType: CouponMediaType,
    val mediaDisplayName: String,
    val title: String?,
    val description: String?,
    val category: String?,
    val expiryDate: LocalDate?,
    val reusePolicy: CouponReusePolicy = CouponReusePolicy.SINGLE_USE,
)

data class CouponMetadataUpdate(
    val couponId: String,
    val title: String?,
    val description: String?,
    val category: String?,
    val expiryDate: LocalDate?,
    val reusePolicy: CouponReusePolicy,
)

data class SharePayload(
    val uri: Uri,
    val mimeType: String,
    val subject: String,
)
