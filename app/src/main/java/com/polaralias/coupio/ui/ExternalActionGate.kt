package com.polaralias.coupio.ui

enum class ExternalAction(
    val unavailableMessage: String,
) {
    IMPORT_FILE("No file picker available on this device."),
    CAMERA_CAPTURE("No camera app available on this device."),
}

class ExternalActionGate(
    private val isSupported: (ExternalAction) -> Boolean,
) {
    fun blockedReason(action: ExternalAction): String? =
        if (isSupported(action)) null else action.unavailableMessage
}
