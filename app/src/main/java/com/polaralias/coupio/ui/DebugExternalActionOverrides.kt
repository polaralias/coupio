package com.polaralias.coupio.ui

import android.content.Context

internal object DebugExternalActionOverrides {
    fun maybeHandleImport(context: Context, viewModel: AppViewModel): Boolean =
        invokeBoolean("maybeHandleImport", context, viewModel)

    fun maybeHandleCapture(context: Context, viewModel: AppViewModel): Boolean =
        invokeBoolean("maybeHandleCapture", context, viewModel)

    private fun invokeBoolean(methodName: String, context: Context, viewModel: AppViewModel): Boolean =
        runCatching {
            val clazz = Class.forName("com.polaralias.coupio.debug.DebugExternalActionOverrides")
            val method = clazz.getMethod(methodName, Context::class.java, AppViewModel::class.java)
            method.invoke(null, context, viewModel) as? Boolean ?: false
        }.getOrDefault(false)
}
