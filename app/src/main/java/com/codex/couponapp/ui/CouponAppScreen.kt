package com.polaralias.coupio.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polaralias.coupio.CameraCaptureTarget
import com.polaralias.coupio.createCameraCaptureTarget
import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.local.displayTitle
import com.polaralias.coupio.data.local.expiryDate
import com.polaralias.coupio.data.local.isExpired
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class RootTab {
    COUPONS,
    ADMIN,
}

@Composable
fun CouponAppScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.COUPONS) }
    var pendingCapture by remember { mutableStateOf<CameraCaptureTarget?>(null) }

    val documentLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val mediaType = if (mimeType == "application/pdf") CouponMediaType.PDF else CouponMediaType.IMAGE
        viewModel.openImportEditor(
            sourceUri = uri.toString(),
            mediaMimeType = mimeType,
            mediaType = mediaType,
            mediaDisplayName = queryDisplayName(context, uri) ?: "coupon",
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        val capture = pendingCapture ?: return@rememberLauncherForActivityResult
        if (success) {
            viewModel.openImportEditor(
                tempFilePath = capture.filePath,
                mediaMimeType = "image/jpeg",
                mediaType = CouponMediaType.IMAGE,
                mediaDisplayName = capture.displayName,
            )
        } else {
            File(capture.filePath).delete()
        }
        pendingCapture = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppEvent.ShareCoupon -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.payload.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.payload.uri)
                        putExtra(Intent.EXTRA_SUBJECT, event.payload.subject)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Send coupon"))
                }

                is AppEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White.copy(alpha = 0.72f),
                    tonalElevation = 0.dp,
                ) {
                    NavigationBarItem(
                        selected = selectedTab == RootTab.COUPONS,
                        onClick = { selectedTab = RootTab.COUPONS },
                        icon = { Icon(Icons.Rounded.Style, contentDescription = null) },
                        label = { Text("Coupons") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == RootTab.ADMIN,
                        onClick = { selectedTab = RootTab.ADMIN },
                        icon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                        label = { Text("Admin") },
                    )
                }
            },
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "tab-crossfade",
            ) { tab ->
                when (tab) {
                    RootTab.COUPONS -> CouponsTab(
                        uiState = uiState,
                        onQueryChange = viewModel::onBrowserQueryChange,
                        onFilterChange = viewModel::onBrowserFilterChange,
                        onShare = viewModel::shareCoupon,
                    )

                    RootTab.ADMIN -> AdminTab(
                        uiState = uiState,
                        onPinSubmit = { pin -> viewModel.submitAdminPin(pin, uiState.adminConfigured) },
                        onSaveIssuerName = viewModel::saveIssuerName,
                        onImportFile = { documentLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onCapture = {
                            val capture = createCameraCaptureTarget(context)
                            pendingCapture = capture
                            cameraLauncher.launch(capture.uri)
                        },
                        onLockAdmin = viewModel::lockAdmin,
                        onEdit = viewModel::openEditEditor,
                        onConfirmPending = viewModel::confirmPending,
                        onRevertPending = viewModel::revertPending,
                        onReissue = viewModel::reissueCoupon,
                    )
                }
            }
        }
    }

    uiState.editor?.let { editor ->
        CouponEditorSheet(
            editor = editor,
            onDismiss = viewModel::dismissEditor,
            onSave = viewModel::saveEditor,
            onTitleChange = viewModel::updateEditorTitle,
            onDescriptionChange = viewModel::updateEditorDescription,
            onCategoryChange = viewModel::updateEditorCategory,
            onPolicyChange = viewModel::updateEditorPolicy,
            onExpiryChange = viewModel::updateEditorExpiry,
        )
    }
}

@Composable
private fun CouponsTab(
    uiState: AppUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (BrowserFilter) -> Unit,
    onShare: (String) -> Unit,
) {
    val availableCount = uiState.allCoupons.count { it.state == CouponState.AVAILABLE && !it.isExpired(uiState.nowEpochMillis) }
    val pendingCount = uiState.allCoupons.count { it.state == CouponState.PENDING }
    val lockedCount = uiState.allCoupons.count { it.state == CouponState.LOCKED || it.isExpired(uiState.nowEpochMillis) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GlassPanel {
                Text(
                    text = "Coupio",
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Take a look at what's available, then share to redeem your coupon!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryPill(label = "Available", value = availableCount.toString(), tint = Color(0xFF3D7462))
                    SummaryPill(label = "Pending", value = pendingCount.toString(), tint = Color(0xFFCC9A2A))
                    SummaryPill(label = "Locked", value = lockedCount.toString(), tint = Color(0xFF5B7A9A))
                }
            }
        }

        item {
            GlassPanel {
                OutlinedTextField(
                    value = uiState.browserQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Search coupons") },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FilterRow(
                    selectedFilter = uiState.browserFilter,
                    onSelect = onFilterChange,
                )
            }
        }

        if (uiState.visibleCoupons.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Nothing to show just yet",
                    body = "Once a coupon is loaded it will pop up here. If everything has disappeared, try another filter.",
                )
            }
        } else {
            items(uiState.visibleCoupons, key = { coupon: CouponEntity -> coupon.id }) { coupon: CouponEntity ->
                CouponCard(
                    coupon = coupon,
                    nowEpochMillis = uiState.nowEpochMillis,
                    issuerName = uiState.issuerName,
                    mode = CardMode.USER,
                    onShare = { onShare(coupon.id) },
                    onEdit = {},
                    onConfirmPending = {},
                    onRevertPending = {},
                    onReissue = {},
                )
            }
        }
    }
}

@Composable
private fun AdminTab(
    uiState: AppUiState,
    onPinSubmit: (String) -> Unit,
    onSaveIssuerName: (String) -> Unit,
    onImportFile: () -> Unit,
    onCapture: () -> Unit,
    onLockAdmin: () -> Unit,
    onEdit: (String) -> Unit,
    onConfirmPending: (String) -> Unit,
    onRevertPending: (String) -> Unit,
    onReissue: (String) -> Unit,
) {
    if (!uiState.adminConfigured || !uiState.adminUnlocked) {
        PinGateCard(
            configured = uiState.adminConfigured,
            onSubmit = onPinSubmit,
        )
        return
    }

    var issuerNameDraft by rememberSaveable(uiState.issuerName) { mutableStateOf(uiState.issuerName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GlassPanel {
                Text(
                    text = "Backstage",
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Load fresh coupons, tweak the details, and sort out anything that's waiting or locked.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Spacer(modifier = Modifier.height(18.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(onClick = onImportFile) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import file")
                    }
                    FilledTonalButton(onClick = onCapture) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera capture")
                    }
                    OutlinedButton(onClick = onLockAdmin) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Close backstage")
                    }
                }
            }
        }

        item {
            GlassPanel {
                Text(
                    text = "Who should people ask?",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set the name Coupio should mention when someone needs another coupon.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = issuerNameDraft,
                    onValueChange = { issuerNameDraft = it.take(32) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name to mention") },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(onClick = { onSaveIssuerName(issuerNameDraft) }) {
                        Text("Save name")
                    }
                    OutlinedButton(
                        onClick = {
                            issuerNameDraft = ""
                            onSaveIssuerName("")
                        },
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        val pendingCoupons = uiState.allCoupons.filter { it.state == CouponState.PENDING }
        if (pendingCoupons.isNotEmpty()) {
            item {
                SectionHeading(
                    title = "Waiting on a check-in",
                    subtitle = "These settle themselves after an hour unless you sort them sooner.",
                )
            }
            items(pendingCoupons, key = { coupon: CouponEntity -> coupon.id }) { coupon: CouponEntity ->
                CouponCard(
                    coupon = coupon,
                    nowEpochMillis = uiState.nowEpochMillis,
                    issuerName = uiState.issuerName,
                    mode = CardMode.ADMIN,
                    onShare = {},
                    onEdit = { onEdit(coupon.id) },
                    onConfirmPending = { onConfirmPending(coupon.id) },
                    onRevertPending = { onRevertPending(coupon.id) },
                    onReissue = { onReissue(coupon.id) },
                )
            }
        }

        item {
            SectionHeading(
                title = "All coupons",
                subtitle = "Everything stays editable, and locked coupons can be popped back into the list whenever you like.",
            )
        }

        if (uiState.allCoupons.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No coupons loaded yet",
                    body = "Drop in an image or PDF, or snap one with the camera to get the fun started.",
                )
            }
        } else {
            items(uiState.allCoupons, key = { coupon: CouponEntity -> coupon.id }) { coupon: CouponEntity ->
                CouponCard(
                    coupon = coupon,
                    nowEpochMillis = uiState.nowEpochMillis,
                    issuerName = uiState.issuerName,
                    mode = CardMode.ADMIN,
                    onShare = {},
                    onEdit = { onEdit(coupon.id) },
                    onConfirmPending = { onConfirmPending(coupon.id) },
                    onRevertPending = { onRevertPending(coupon.id) },
                    onReissue = { onReissue(coupon.id) },
                )
            }
        }
    }
}

@Composable
private fun PinGateCard(
    configured: Boolean,
    onSubmit: (String) -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        GlassPanel {
            Text(
                text = if (configured) "Unlock backstage" else "Set the backstage PIN",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (configured) {
                    "Use the PIN to load coupons, tidy up anything pending, and unlock used ones."
                } else {
                    "Create a PIN for the admin area. It stays on this device."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { value -> pin = value.filter(Char::isDigit).take(8) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                label = { Text("Backstage PIN") },
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSubmit(pin)
                    pin = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (configured) "Open backstage" else "Save PIN")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    selectedFilter: BrowserFilter,
    onSelect: (BrowserFilter) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BrowserFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.78f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    tint: Color,
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = tint,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String,
) {
    GlassPanel {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun AuroraBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4F7FB),
                        Color(0xFFE5F3F0),
                        Color(0xFFF8EFE6),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(top = 48.dp, start = 24.dp)
                .background(Color(0x55B8E2D3), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(340.dp)
                .padding(end = 20.dp, bottom = 72.dp)
                .background(Color(0x44F1D6B8), CircleShape),
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CouponEditorSheet(
    editor: CouponEditorState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPolicyChange: (CouponReusePolicy) -> Unit,
    onExpiryChange: (LocalDate?) -> Unit,
) {
    var showDatePicker by remember(editor) { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editor.expiryDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7FAFC),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = if (editor.mode == EditorMode.CREATE) "Add coupon" else "Edit coupon",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlassPanel {
                MediaPreview(
                    coupon = null,
                    editor = editor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title (optional)") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editor.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                shape = RoundedCornerShape(18.dp),
                minLines = 3,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = editor.category,
                onValueChange = onCategoryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category (optional)") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reuse policy",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CouponReusePolicy.entries.forEach { policy ->
                    FilterChip(
                        selected = editor.reusePolicy == policy,
                        onClick = { onPolicyChange(policy) },
                        label = { Text(policy.label) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            GlassPanel {
                Text(
                    text = "Expiry date",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = editor.expiryDate?.format(HUMAN_DATE) ?: "No expiry date set",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick date")
                    }
                    OutlinedButton(onClick = { onExpiryChange(null) }) {
                        Text("Clear date")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Every share gives a coupon a one-hour waiting window. During that time you can issue it properly or roll it back.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (editor.mode == EditorMode.CREATE) "Save coupon" else "Save changes")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        onExpiryChange(
                            millis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            },
                        )
                        showDatePicker = false
                    },
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private enum class CardMode {
    USER,
    ADMIN,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CouponCard(
    coupon: CouponEntity,
    nowEpochMillis: Long,
    issuerName: String,
    mode: CardMode,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onConfirmPending: () -> Unit,
    onRevertPending: () -> Unit,
    onReissue: () -> Unit,
) {
    val expired = coupon.isExpired(nowEpochMillis)
    val canShare = coupon.state == CouponState.AVAILABLE && !expired
    val canReissue = coupon.state == CouponState.LOCKED || expired

    GlassPanel {
        MediaPreview(
            coupon = coupon,
            editor = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coupon.displayTitle(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                coupon.category?.takeIf { it.isNotBlank() }?.let { category ->
                    Spacer(modifier = Modifier.height(6.dp))
                    SummaryPill(
                        label = "Category",
                        value = category,
                        tint = Color(0xFF5B7A9A),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            StatusBadge(
                label = when {
                    expired -> "Expired"
                    coupon.state == CouponState.PENDING -> "Pending"
                    coupon.state == CouponState.LOCKED -> "Locked"
                    else -> "Ready"
                },
            )
        }
        coupon.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryPill(
                label = "Policy",
                value = coupon.reusePolicy.label,
                tint = Color(0xFF3D7462),
            )
            coupon.expiryDate()?.let { expiryDate ->
                SummaryPill(
                    label = "Expires",
                    value = expiryDate.format(HUMAN_DATE),
                    tint = Color(0xFFCC9A2A),
                )
            }
            if (coupon.mediaType == CouponMediaType.PDF) {
                SummaryPill(
                    label = "File",
                    value = "PDF",
                    tint = Color(0xFFBA4A4A),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = couponStatusText(
                coupon = coupon,
                nowEpochMillis = nowEpochMillis,
                issuerName = issuerName,
                mode = mode,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (mode) {
                CardMode.USER -> {
                    Button(
                        onClick = onShare,
                        enabled = canShare,
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share to redeem")
                    }
                }

                CardMode.ADMIN -> {
                    OutlinedButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                    if (coupon.state == CouponState.PENDING) {
                        FilledTonalButton(onClick = onConfirmPending) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Issue now")
                        }
                        OutlinedButton(onClick = onRevertPending) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Roll back")
                        }
                    } else if (canReissue) {
                        FilledTonalButton(onClick = onReissue) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaPreview(
    coupon: CouponEntity?,
    editor: CouponEditorState?,
    modifier: Modifier = Modifier,
) {
    val data = when {
        editor?.sourceUri != null -> Uri.parse(editor.sourceUri)
        editor?.tempFilePath != null -> File(editor.tempFilePath)
        editor?.mediaPath != null -> File(editor.mediaPath)
        coupon != null -> File(coupon.mediaPath)
        else -> null
    }
    val mediaType = editor?.mediaType ?: coupon?.mediaType

    if (mediaType == CouponMediaType.IMAGE && data != null) {
        AsyncImage(
            model = data,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF8D7DA),
                            Color(0xFFFFF4E1),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFB03A3A),
                    modifier = Modifier.size(54.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PDF coupon",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.75f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun couponStatusText(
    coupon: CouponEntity,
    nowEpochMillis: Long,
    issuerName: String,
    mode: CardMode,
): String {
    val issuer = issuerName.ifBlank { "the person running Coupio" }

    if (coupon.isExpired(nowEpochMillis)) {
        return if (mode == CardMode.ADMIN) {
            "Expired on ${coupon.expiryDate()?.format(HUMAN_DATE)}. Update the details if it should still be usable."
        } else {
            "Expired on ${coupon.expiryDate()?.format(HUMAN_DATE)}. Ask $issuer if it should still be available."
        }
    }

    return when (coupon.state) {
        CouponState.AVAILABLE -> when (coupon.reusePolicy) {
            CouponReusePolicy.SINGLE_USE -> "Ready to go. Once it's issued after the one-hour wait, this one locks until it's unlocked again."
            CouponReusePolicy.DAILY -> "Ready to go. After the waiting window, it naps for a day."
            CouponReusePolicy.WEEKLY -> "Ready to go. After the waiting window, it rests for a week."
            CouponReusePolicy.MONTHLY -> "Ready to go. After the waiting window, it rests for a month."
            CouponReusePolicy.ALWAYS -> "Ready to go. After the waiting window, it pops right back into the list."
        }

        CouponState.PENDING -> {
            val pendingUntil = coupon.pendingUntilEpochMillis?.let(::formatDateTime) ?: "soon"
            if (mode == CardMode.ADMIN) {
                "Waiting until $pendingUntil. You can issue it now or roll it back before the hour ends."
            } else {
                "Waiting until $pendingUntil. $issuer can finish it or roll it back before the hour ends."
            }
        }

        CouponState.LOCKED -> {
            val availableAgain = coupon.availableAgainAtEpochMillis?.let(::formatDateTime)
            if (availableAgain != null) {
                if (mode == CardMode.ADMIN) {
                    "Locked until $availableAgain. Unlock it sooner if you want it back in the mix."
                } else {
                    "Back on $availableAgain. If you need it sooner, ask $issuer."
                }
            } else {
                if (mode == CardMode.ADMIN) {
                    "This one's used up for now. Unlock it again whenever you want it back."
                } else {
                    "That one's been used already. Ask $issuer to issue you some more."
                }
            }
        }
    }
}

private fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DATE_TIME)

private val HUMAN_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
