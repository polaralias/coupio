package com.polaralias.coupio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.polaralias.coupio.CouponApp
import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.local.displayTitle
import com.polaralias.coupio.data.local.isExpired
import com.polaralias.coupio.data.model.CouponDraftInput
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponMetadataUpdate
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import com.polaralias.coupio.data.model.PreparedShare
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BrowserFilter(val label: String) {
    ALL("All"),
    AVAILABLE("Available"),
    PENDING("Pending"),
    LOCKED("Locked"),
}

enum class EditorMode {
    CREATE,
    EDIT,
}

data class CouponEditorState(
    val mode: EditorMode,
    val couponId: String? = null,
    val sourceUri: String? = null,
    val tempFilePath: String? = null,
    val mediaPath: String? = null,
    val mediaMimeType: String,
    val mediaType: CouponMediaType,
    val mediaDisplayName: String,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val expiryDate: LocalDate? = null,
    val reusePolicy: CouponReusePolicy = CouponReusePolicy.SINGLE_USE,
)

data class AppUiState(
    val allCoupons: List<CouponEntity> = emptyList(),
    val visibleCoupons: List<CouponEntity> = emptyList(),
    val browserQuery: String = "",
    val browserFilter: BrowserFilter = BrowserFilter.ALL,
    val adminConfigured: Boolean = false,
    val adminUnlocked: Boolean = false,
    val issuerName: String = "",
    val editor: CouponEditorState? = null,
    val nowEpochMillis: Long = System.currentTimeMillis(),
)

sealed interface AppEvent {
    data class ShareCoupon(val payload: PreparedShare) : AppEvent
    data class Snackbar(val message: String) : AppEvent
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as CouponApp).container
    private val couponRepository = container.couponRepository
    private val adminPinRepository = container.adminPinRepository
    private val appPreferencesRepository = container.appPreferencesRepository

    private val browserQuery = MutableStateFlow("")
    private val browserFilter = MutableStateFlow(BrowserFilter.ALL)
    private val adminUnlocked = MutableStateFlow(false)
    private val editorState = MutableStateFlow<CouponEditorState?>(null)
    private val nowEpochMillis = MutableStateFlow(System.currentTimeMillis())
    private val eventsMutable = MutableSharedFlow<AppEvent>()

    val events = eventsMutable.asSharedFlow()

    private val uiInputs = combine(
        browserQuery,
        browserFilter,
        adminUnlocked,
        editorState,
        nowEpochMillis,
    ) { query, filter, unlocked, editor, now ->
        UiInputs(
            query = query,
            filter = filter,
            adminUnlocked = unlocked,
            editor = editor,
            nowEpochMillis = now,
        )
    }

    val uiState = combine(
        couponRepository.observeCoupons(),
        adminPinRepository.hasPin,
        appPreferencesRepository.issuerName,
        uiInputs,
    ) { coupons, hasPin, issuerName, inputs ->
        val sortedCoupons = coupons.sortedWith(
            compareBy<CouponEntity> { coupon -> coupon.rank(inputs.nowEpochMillis) }
                .thenByDescending { coupon -> coupon.updatedAtEpochMillis },
        )
        val visibleCoupons = sortedCoupons.filter { coupon ->
            coupon.matchesQuery(inputs.query) && coupon.matchesFilter(inputs.filter, inputs.nowEpochMillis)
        }

        AppUiState(
            allCoupons = sortedCoupons,
            visibleCoupons = visibleCoupons,
            browserQuery = inputs.query,
            browserFilter = inputs.filter,
            adminConfigured = hasPin,
            adminUnlocked = inputs.adminUnlocked,
            issuerName = issuerName,
            editor = inputs.editor,
            nowEpochMillis = inputs.nowEpochMillis,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        viewModelScope.launch {
            couponRepository.reconcileDueCoupons()
        }
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                val now = System.currentTimeMillis()
                nowEpochMillis.value = now
                couponRepository.reconcileDueCoupons(now)
            }
        }
    }

    fun onBrowserQueryChange(value: String) {
        browserQuery.value = value
    }

    fun onBrowserFilterChange(filter: BrowserFilter) {
        browserFilter.value = filter
    }

    fun lockAdmin() {
        adminUnlocked.value = false
    }

    fun saveIssuerName(name: String) {
        viewModelScope.launch {
            appPreferencesRepository.setIssuerName(name)
            eventsMutable.emit(
                AppEvent.Snackbar(
                    if (name.isBlank()) "Name cleared." else "Name saved.",
                ),
            )
        }
    }

    fun dismissEditor() {
        editorState.value = null
    }

    fun openImportEditor(
        sourceUri: String? = null,
        tempFilePath: String? = null,
        mediaMimeType: String,
        mediaType: CouponMediaType,
        mediaDisplayName: String,
    ) {
        editorState.value = CouponEditorState(
            mode = EditorMode.CREATE,
            sourceUri = sourceUri,
            tempFilePath = tempFilePath,
            mediaMimeType = mediaMimeType,
            mediaType = mediaType,
            mediaDisplayName = mediaDisplayName,
        )
    }

    fun openEditEditor(couponId: String) {
        viewModelScope.launch {
            val coupon = couponRepository.getCoupon(couponId) ?: return@launch
            editorState.value = CouponEditorState(
                mode = EditorMode.EDIT,
                couponId = coupon.id,
                mediaPath = coupon.mediaPath,
                mediaMimeType = coupon.mediaMimeType,
                mediaType = coupon.mediaType,
                mediaDisplayName = coupon.mediaDisplayName,
                title = coupon.title.orEmpty(),
                description = coupon.description.orEmpty(),
                category = coupon.category.orEmpty(),
                expiryDate = coupon.expiryEpochDay?.let(LocalDate::ofEpochDay),
                reusePolicy = coupon.reusePolicy,
            )
        }
    }

    fun updateEditorTitle(value: String) = mutateEditor { copy(title = value) }
    fun updateEditorDescription(value: String) = mutateEditor { copy(description = value) }
    fun updateEditorCategory(value: String) = mutateEditor { copy(category = value) }
    fun updateEditorPolicy(value: CouponReusePolicy) = mutateEditor { copy(reusePolicy = value) }
    fun updateEditorExpiry(value: LocalDate?) = mutateEditor { copy(expiryDate = value) }

    fun submitAdminPin(pin: String, adminConfigured: Boolean) {
        viewModelScope.launch {
            if (pin.length < 4 || pin.any { !it.isDigit() }) {
                eventsMutable.emit(AppEvent.Snackbar("Use a PIN with at least 4 digits."))
                return@launch
            }

            if (adminConfigured) {
                if (adminPinRepository.verifyPin(pin)) {
                    adminUnlocked.value = true
                    eventsMutable.emit(AppEvent.Snackbar("Backstage unlocked."))
                } else {
                    eventsMutable.emit(AppEvent.Snackbar("Incorrect PIN."))
                }
            } else {
                adminPinRepository.setPin(pin)
                adminUnlocked.value = true
                eventsMutable.emit(AppEvent.Snackbar("PIN saved. Backstage is open."))
            }
        }
    }

    fun saveEditor() {
        val editor = editorState.value ?: return
        viewModelScope.launch {
            runCatching {
                if (editor.mode == EditorMode.CREATE) {
                    couponRepository.importCoupon(
                        CouponDraftInput(
                            sourceUri = editor.sourceUri,
                            tempFilePath = editor.tempFilePath,
                            mediaMimeType = editor.mediaMimeType,
                            mediaType = editor.mediaType,
                            mediaDisplayName = editor.mediaDisplayName,
                            title = editor.title,
                            description = editor.description,
                            category = editor.category,
                            expiryDate = editor.expiryDate,
                            reusePolicy = editor.reusePolicy,
                        ),
                    )
                } else {
                    couponRepository.updateCouponMetadata(
                        CouponMetadataUpdate(
                            couponId = editor.couponId ?: error("Missing coupon ID"),
                            title = editor.title,
                            description = editor.description,
                            category = editor.category,
                            expiryDate = editor.expiryDate,
                            reusePolicy = editor.reusePolicy,
                        ),
                    )
                }
            }.onSuccess {
                editorState.value = null
                eventsMutable.emit(
                    AppEvent.Snackbar(
                        if (editor.mode == EditorMode.CREATE) "Coupon added." else "Details updated.",
                    ),
                )
            }.onFailure {
                eventsMutable.emit(AppEvent.Snackbar("Couldn't save that coupon."))
            }
        }
    }

    fun shareCoupon(couponId: String) {
        viewModelScope.launch {
            val payload = couponRepository.prepareShare(couponId)
            if (payload == null) {
                eventsMutable.emit(AppEvent.Snackbar("That coupon is no longer available."))
                return@launch
            }
            eventsMutable.emit(AppEvent.ShareCoupon(payload))
        }
    }

    fun confirmPending(couponId: String) {
        viewModelScope.launch {
            couponRepository.confirmPending(couponId)
            eventsMutable.emit(AppEvent.Snackbar("Coupon issued."))
        }
    }

    fun revertPending(couponId: String) {
        viewModelScope.launch {
            couponRepository.revertPending(couponId)
            eventsMutable.emit(AppEvent.Snackbar("Coupon rolled back."))
        }
    }

    fun reissueCoupon(couponId: String) {
        viewModelScope.launch {
            couponRepository.reissueCoupon(couponId)
            eventsMutable.emit(AppEvent.Snackbar("Coupon unlocked again."))
        }
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            eventsMutable.emit(AppEvent.Snackbar(message))
        }
    }

    private fun mutateEditor(transform: CouponEditorState.() -> CouponEditorState) {
        editorState.value = editorState.value?.transform()
    }
}

private fun CouponEntity.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim().lowercase()
    return listOfNotNull(title, description, category, displayTitle())
        .any { candidate -> candidate.lowercase().contains(needle) }
}

private fun CouponEntity.matchesFilter(filter: BrowserFilter, nowEpochMillis: Long): Boolean {
    return when (filter) {
        BrowserFilter.ALL -> true
        BrowserFilter.AVAILABLE -> state == CouponState.AVAILABLE && !isExpired(nowEpochMillis)
        BrowserFilter.PENDING -> state == CouponState.PENDING
        BrowserFilter.LOCKED -> state == CouponState.LOCKED || isExpired(nowEpochMillis)
    }
}

private fun CouponEntity.rank(nowEpochMillis: Long): Int = when {
    state == CouponState.PENDING -> 0
    state == CouponState.AVAILABLE && !isExpired(nowEpochMillis) -> 1
    state == CouponState.LOCKED -> 2
    else -> 3
}

private data class UiInputs(
    val query: String,
    val filter: BrowserFilter,
    val adminUnlocked: Boolean,
    val editor: CouponEditorState?,
    val nowEpochMillis: Long,
)
