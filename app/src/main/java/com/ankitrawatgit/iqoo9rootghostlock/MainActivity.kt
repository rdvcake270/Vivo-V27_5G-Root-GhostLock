package com.ankitrawatgit.iqoo9rootghostlock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ankitrawatgit.iqoo9rootghostlock.ui.theme.GhostLockTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val installViewModel by viewModels<InstallViewModel>()
    private var resumedOnce = false
    private var accentColor by mutableStateOf(AccentColor.Dynamic)
    private var themeMode by mutableStateOf(AppThemeMode.Light)
    private var shizukuMode by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accentColor = AppPreferences.accentColor(this)
        themeMode = AppPreferences.themeMode(this)
        shizukuMode = AppPreferences.shizukuMode(this)
        setContent {
            GhostLockTheme(accentColor = accentColor, themeMode = themeMode) {
                RootApp(
                    installViewModel = installViewModel,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    shizukuMode = shizukuMode,
                    onAccentColorChanged = { color ->
                        AppPreferences.setAccentColor(this, color)
                        accentColor = color
                    },
                    onThemeModeChanged = { mode ->
                        AppPreferences.setThemeMode(this, mode)
                        themeMode = mode
                    },
                    onShizukuModeChanged = { enabled ->
                        AppPreferences.setShizukuMode(this, enabled)
                        shizukuMode = enabled
                    },
                    openInstaller = {
                        val installer = Intent(this, InstallActivity::class.java)
                            .putExtra(InstallActivity.EXTRA_INSTALL_REQUEST_ID, UUID.randomUUID().toString())
                        startActivity(installer)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (resumedOnce) installViewModel.refresh() else resumedOnce = true
    }
}

private enum class AppPage(@StringRes val label: Int, val icon: ImageVector) {
    Overview(R.string.nav_overview, Icons.Rounded.Home),
    History(R.string.nav_history, Icons.Rounded.History),
    Settings(R.string.nav_settings, Icons.Rounded.Settings),
}

private data class LanguageOption(@StringRes val label: Int, val tag: String)

private val languageOptions = listOf(
    LanguageOption(R.string.language_system, ""),
    LanguageOption(R.string.language_korean, "ko"),
    LanguageOption(R.string.language_english, "en"),
    LanguageOption(R.string.language_japanese, "ja"),
    LanguageOption(R.string.language_chinese, "zh-CN"),
)

private const val KERNEL_SU_MANAGER_URL =
    "https://github.com/tiann/KernelSU/releases"
private const val SHIZUKU_MANAGER_URL =
    "https://github.com/RikkaApps/Shizuku/releases"

@Composable
private fun RootApp(
    installViewModel: InstallViewModel,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    shizukuMode: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
    openInstaller: () -> Unit,
) {
    val installState by installViewModel.state.collectAsStateWithLifecycle()
    val history by installViewModel.history.collectAsStateWithLifecycle()
    var selectedPage by remember { mutableStateOf(AppPage.Overview) }
    var showInstallConfirmation by remember { mutableStateOf(false) }
    val device = remember { DeviceSnapshot.current() }

    if (showInstallConfirmation) {
        AlertDialog(
            onDismissRequest = { showInstallConfirmation = false },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
            title = {
                DialogDimAmount(0.24f)
                Text(stringResource(R.string.install_confirm_title))
            },
            text = {
                Text(stringResource(R.string.install_confirm_body))
            },
            confirmButton = {
                FilledTonalButton(onClick = {
                    showInstallConfirmation = false
                    openInstaller()
                }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(72.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                AppPage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = { selectedPage = page },
                        modifier = Modifier.padding(top = 4.dp),
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(stringResource(page.label)) },
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        AnimatedContent(targetState = selectedPage, label = "page") { page ->
            when (page) {
                AppPage.Overview -> OverviewPage(
                    padding = padding,
                    device = device,
                    installState = installState,
                    onInstall = { showInstallConfirmation = true },
                )
                AppPage.History -> HistoryPage(
                    padding = padding,
                    history = history,
                    onDeleteEntry = installViewModel::deleteHistoryEntry,
                    onClearHistory = installViewModel::clearHistory,
                )
                AppPage.Settings -> SettingsPage(
                    padding = padding,
                    accentColor = accentColor,
                    themeMode = themeMode,
                    shizukuMode = shizukuMode,
                    onAccentColorChanged = onAccentColorChanged,
                    onThemeModeChanged = onThemeModeChanged,
                    onShizukuModeChanged = onShizukuModeChanged,
                )
            }
        }
    }
}

@Composable
private fun DialogDimAmount(amount: Float) {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    SideEffect { window.setDimAmount(amount) }
}

@Composable
private fun OverviewPage(
    padding: PaddingValues,
    device: DeviceSnapshot,
    installState: InstallUiState,
    onInstall: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 54.dp, bottom = 14.dp),
            )
        }
        item { InstallStatusCard(installState, onInstall) }
        // Only once a profile has been resolved: before that the app does not
        // know which module this device gets, and a version it made up would
        // be worse than none.
        installState.kernelSu?.let { kernelSu ->
            if (kernelSu.managerVersionCode != null) {
                item { KernelSuManagerCard(kernelSu) }
            }
        }
        item { DeviceCard(device) }
        item { ProjectGitHubCard() }
    }
}

@Composable
private fun InstallStatusCard(installState: InstallUiState, onInstall: () -> Unit) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    )
    val modifier = Modifier.fillMaxWidth().animateContentSize()
    // Once installed this card only reports, so it stops being a button: the
    // manager it still wants has its own card below, and sending a tap here to
    // a download page read as if the install had not finished. A static Card
    // rather than onClick = {}, so the ripple goes with the destination.
    if (installState.phase == InstallPhase.Installed) {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors) {
            InstallStatusContent(installState)
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = { if (!installState.busy) onInstall() },
        modifier = modifier,
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = colors,
    ) {
        InstallStatusContent(installState)
    }
}

@Composable
private fun InstallStatusContent(installState: InstallUiState) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            installState.busy -> LoadingIndicator(modifier = Modifier.size(44.dp))
            installState.phase == InstallPhase.Installed -> Icon(
                Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(44.dp),
            )
            installState.phase == InstallPhase.Failed -> Icon(
                Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
            )
            else -> Icon(
                Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(44.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (installState.phase) {
                    InstallPhase.Ready -> stringResource(R.string.status_not_installed)
                    InstallPhase.Installed -> stringResource(R.string.status_ksu_active)
                    else -> installState.message
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = when (installState.phase) {
                    InstallPhase.Installed -> stringResource(R.string.install_ksu_running)
                    InstallPhase.Failed -> stringResource(R.string.install_tap_retry)
                    else -> stringResource(R.string.install_tap_start)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                maxLines = 1,
            )
        }
    }
}

/**
 * Which KernelSU manager this device's module pairs with, and a way to it.
 *
 * The two carry the same version number and the manager refuses a module below
 * its own minimum, so installing whichever manager turns up first is how a
 * working root ends up looking broken. The number comes from the feed, which is
 * written by the build that produced the module, so it cannot drift from it.
 */
@Composable
private fun KernelSuManagerCard(kernelSu: KernelSuArtifact) {
    val interactionSource = remember { MutableInteractionSource() }
    val uriHandler = LocalUriHandler.current
    val url = kernelSu.managerUrl ?: KERNEL_SU_MANAGER_URL
    Card(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            // A manager only this build works with is not the same errand as
            // fetching the matching upstream release, and a card that looked
            // identical would read as one.
            containerColor = if (kernelSu.managerCustom) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (kernelSu.managerCustom) {
                            R.string.kernelsu_manager_custom_required
                        } else {
                            R.string.kernelsu_manager_required
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    // The name is upstream's tag and the code is what the
                    // manager shows for itself, so both are worth printing:
                    // one identifies the release, the other is what a mismatch
                    // is argued about.
                    kernelSu.managerVersionName?.let { name ->
                        "$name (${kernelSu.managerVersionCode})"
                    } ?: kernelSu.managerVersionCode.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Why this build needs its own, in the feed's words rather than
                // the app's: the app cannot know the reason, and a warning with
                // no reason reads as a nag to be dismissed.
                kernelSu.managerNote?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (kernelSu.managerCustom) {
                    Text(
                        stringResource(R.string.kernelsu_manager_custom_replace),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Text(
                    stringResource(R.string.kernelsu_manager_open),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(Icons.Rounded.Memory, stringResource(R.string.device), "${device.manufacturer} ${device.model} (${device.device})")
            InfoRow(Icons.Rounded.Code, stringResource(R.string.firmware), device.buildId)
            // What the vendor calls this build. HyperOS keeps it out of the
            // firmware row above, which carries the AOSP build id, so this is
            // the only place the version the user knows appears -- and where a
            // vendor has nothing of its own to say, the row stays away.
            device.displayOsVersion?.let { osVersion ->
                InfoRow(Icons.Rounded.Android, stringResource(R.string.os_version), osVersion)
            }
            InfoRow(Icons.Rounded.Info, stringResource(R.string.system), "Android ${device.androidRelease} (API ${device.sdk})")
            InfoRow(Icons.Rounded.Terminal, stringResource(R.string.kernel), device.kernelRelease)
            InfoRow(Icons.Rounded.Security, stringResource(R.string.system_abi), "${device.abi} (${device.pageSize / 1024}K)")
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        // Weighted so the kernel release, which is longer than the card is
        // wide, wraps inside the row instead of running past its edge.
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectGitHubCard() {
    val interactionSource = remember { MutableInteractionSource() }
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = { uriHandler.openUri(PROJECT_GITHUB_URL) },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.github_card_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    stringResource(R.string.github_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.Link,
                contentDescription = stringResource(R.string.open_github),
            )
        }
    }
}

@Composable
private fun HistoryPage(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
    onDeleteEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    var selectedHistoryId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingClear by remember { mutableStateOf(false) }
    val selectedEntry = history.firstOrNull { it.id == selectedHistoryId }
    BackHandler(enabled = selectedEntry != null) { selectedHistoryId = null }

    pendingDeleteId?.let { id ->
        HistoryRemovalDialog(
            title = R.string.history_delete_title,
            body = R.string.history_delete_body,
            onConfirm = {
                pendingDeleteId = null
                // Leaving the detail view here rather than waiting for the entry
                // to disappear from the flow: the delete is a round trip to disk.
                selectedHistoryId = null
                onDeleteEntry(id)
            },
            onDismiss = { pendingDeleteId = null },
        )
    }

    if (pendingClear) {
        HistoryRemovalDialog(
            title = R.string.history_clear_title,
            body = R.string.history_clear_body,
            onConfirm = {
                pendingClear = false
                selectedHistoryId = null
                onClearHistory()
            },
            onDismiss = { pendingClear = false },
        )
    }

    AnimatedContent(
        targetState = selectedEntry,
        contentKey = { it?.id ?: "history-list" },
        label = "history-detail",
    ) { entry ->
        if (entry == null) {
            HistoryList(
                padding = padding,
                history = history,
                onEntryClick = { selectedHistoryId = it.id },
                onClearRequest = { pendingClear = true },
            )
        } else {
            HistoryDetail(
                padding = padding,
                entry = entry,
                onBack = { selectedHistoryId = null },
                onDeleteRequest = { pendingDeleteId = entry.id },
            )
        }
    }
}

@Composable
private fun HistoryRemovalDialog(
    @StringRes title: Int,
    @StringRes body: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
        title = {
            DialogDimAmount(0.24f)
            Text(stringResource(title))
        },
        text = { Text(stringResource(body)) },
        confirmButton = {
            FilledTonalButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun HistoryList(
    padding: PaddingValues,
    history: List<InstallHistoryEntry>,
    onEntryClick: (InstallHistoryEntry) -> Unit,
    onClearRequest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                if (history.isNotEmpty()) {
                    IconButton(onClick = onClearRequest) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = stringResource(R.string.history_clear_title),
                        )
                    }
                }
            }
        }
        if (history.isEmpty()) {
            item { EmptyHistoryCard() }
        } else {
            itemsIndexed(history, key = { _, entry -> entry.id }) { _, entry ->
                HistoryEntryCard(entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.history_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: InstallHistoryEntry, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor = when (entry.result) {
        InstallRunResult.Running -> MaterialTheme.colorScheme.tertiaryContainer
        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.primaryContainer
        InstallRunResult.Failed -> MaterialTheme.colorScheme.errorContainer
        InstallRunResult.Skipped -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (entry.result) {
        InstallRunResult.Running -> MaterialTheme.colorScheme.onTertiaryContainer
        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.onPrimaryContainer
        InstallRunResult.Failed -> MaterialTheme.colorScheme.onErrorContainer
        InstallRunResult.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Icon(historyResultIcon(entry.result), contentDescription = null, modifier = Modifier.size(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(historyResultLabel(entry.result), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatHistoryTime(entry.startedAtMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun HistoryDetail(
    padding: PaddingValues,
    entry: InstallHistoryEntry,
    onBack: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Text(
                    stringResource(R.string.history_detail_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.history_delete_title),
                    )
                }
            }
        }
        item { HistoryResultCard(entry) }
        item { SectionLabel(stringResource(R.string.history_log)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Text(
                    text = entry.log.ifBlank { stringResource(R.string.history_log_empty) },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HistoryResultCard(entry: InstallHistoryEntry) {
    val containerColor = when (entry.result) {
        InstallRunResult.Running -> MaterialTheme.colorScheme.tertiaryContainer
        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.primaryContainer
        InstallRunResult.Failed -> MaterialTheme.colorScheme.errorContainer
        InstallRunResult.Skipped -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (entry.result) {
        InstallRunResult.Running -> MaterialTheme.colorScheme.onTertiaryContainer
        InstallRunResult.Succeeded -> MaterialTheme.colorScheme.onPrimaryContainer
        InstallRunResult.Failed -> MaterialTheme.colorScheme.onErrorContainer
        InstallRunResult.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(historyResultIcon(entry.result), contentDescription = null, modifier = Modifier.size(38.dp))
            Column {
                Text(historyResultLabel(entry.result), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.history_started, formatHistoryTime(entry.startedAtMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                )
                entry.completedAtMillis?.let { completedAt ->
                    Text(
                        stringResource(R.string.history_completed, formatHistoryTime(completedAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
                entry.payloadTag?.let { tag ->
                    Text(
                        stringResource(R.string.history_payload, tag),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}

@Composable
private fun historyResultLabel(result: InstallRunResult): String = stringResource(
    when (result) {
        InstallRunResult.Running -> R.string.history_running
        InstallRunResult.Succeeded -> R.string.history_succeeded
        InstallRunResult.Failed -> R.string.history_failed
        InstallRunResult.Skipped -> R.string.history_skipped
    },
)

private fun historyResultIcon(result: InstallRunResult): ImageVector = when (result) {
    InstallRunResult.Running -> Icons.Rounded.Schedule
    InstallRunResult.Succeeded -> Icons.Rounded.CheckCircle
    InstallRunResult.Failed -> Icons.Rounded.Error
    InstallRunResult.Skipped -> Icons.Rounded.Schedule
}

@Composable
private fun formatHistoryTime(timestamp: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(timestamp, locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)
            .format(Date(timestamp))
    }
}

@Composable
private fun SettingsPage(
    padding: PaddingValues,
    accentColor: AccentColor,
    themeMode: AppThemeMode,
    shizukuMode: Boolean,
    onAccentColorChanged: (AccentColor) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    onShizukuModeChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showShizukuMissingDialog by remember { mutableStateOf(false) }
    var showDirectModeWarningDialog by remember { mutableStateOf(false) }
    var languageMenuTop by remember { mutableStateOf(32.dp) }
    var colorMenuTop by remember { mutableStateOf(32.dp) }
    val density = LocalDensity.current
    val currentLanguageTag = AppPreferences.languageTag(context)

    if (showLanguageDialog) {
        SideChoiceMenu(
            choices = languageOptions.map { stringResource(it.label) },
            selectedIndex = languageOptions.indexOfFirst {
                it.tag.isEmpty() && currentLanguageTag.isEmpty() ||
                    it.tag.isNotEmpty() && currentLanguageTag.startsWith(it.tag.substringBefore('-'))
            }.coerceAtLeast(0),
            topOffset = languageMenuTop,
            onSelected = { index ->
                showLanguageDialog = false
                AppPreferences.setLanguage(context, languageOptions[index].tag)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showColorDialog) {
        val colors = AccentColor.entries
        SideChoiceMenu(
            choices = colors.map { accentLabel(it) },
            selectedIndex = colors.indexOf(accentColor),
            topOffset = colorMenuTop,
            onSelected = { index ->
                showColorDialog = false
                onAccentColorChanged(colors[index])
            },
            onDismiss = { showColorDialog = false },
        )
    }

    if (showShizukuMissingDialog) {
        AlertDialog(
            onDismissRequest = { showShizukuMissingDialog = false },
            icon = { Icon(Icons.Rounded.Terminal, contentDescription = null) },
            title = { Text(stringResource(R.string.shizuku_not_running_title)) },
            text = { Text(stringResource(R.string.shizuku_not_running_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showShizukuMissingDialog = false
                    uriHandler.openUri(SHIZUKU_MANAGER_URL)
                }) {
                    Text(stringResource(R.string.action_download_shizuku))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShizukuMissingDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDirectModeWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDirectModeWarningDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.shizuku_disable_title)) },
            text = { Text(stringResource(R.string.shizuku_disable_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDirectModeWarningDialog = false
                    onShizukuModeChanged(false)
                }) {
                    Text(stringResource(R.string.action_disable_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectModeWarningDialog = false }) {
                    Text(stringResource(R.string.action_keep_shizuku))
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 18.dp)) {
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionLabel(stringResource(R.string.appearance)) }
        item {
            ThemeModeSelector(themeMode, onThemeModeChanged)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        colorMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.material_color),
                    description = stringResource(R.string.material_color_description),
                    value = accentLabel(accentColor),
                    position = SettingsCardPosition.Top,
                    onClick = { showColorDialog = true },
                )
                SettingsCard(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        languageMenuTop = with(density) { coordinates.positionInWindow().y.toDp() }
                    },
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.language_description),
                    value = languageLabel(currentLanguageTag),
                    position = SettingsCardPosition.Bottom,
                    onClick = { showLanguageDialog = true },
                )
            }
        }
        item { SectionLabel(stringResource(R.string.privilege)) }
        item {
            SettingsSwitchCard(
                icon = Icons.Rounded.Terminal,
                title = stringResource(R.string.shizuku_mode),
                description = stringResource(R.string.shizuku_mode_description),
                checked = shizukuMode,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        showDirectModeWarningDialog = true
                    } else {
                        coroutineScope.launch {
                            val running = ShizukuController.isRunning() ||
                                ShizukuController.pingUntilRunning()
                            if (!running) {
                                showShizukuMissingDialog = true
                                return@launch
                            }
                            if (!ShizukuController.isGranted() &&
                                !ShizukuController.requestPermission()
                            ) {
                                showShizukuMissingDialog = true
                                return@launch
                            }
                            onShizukuModeChanged(true)
                        }
                    }
                },
            )
        }
        item { SectionLabel(stringResource(R.string.about)) }
        item {
            SettingsCard(
                icon = Icons.Rounded.Link,
                title = stringResource(R.string.github_card_title),
                description = stringResource(R.string.github_card_description),
                value = "@ankitrawatgit",
                position = SettingsCardPosition.Single,
                onClick = { uriHandler.openUri(PROJECT_GITHUB_URL) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 6.dp, bottom = 2.dp),
    )
}

private enum class SettingsCardPosition {
    Single,
    GroupedSingle,
    Top,
    Middle,
    Bottom,
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    value: String,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    position: SettingsCardPosition = SettingsCardPosition.Single,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        shape = expressiveClickableCardShape(interactionSource, position),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val themeModes = AppThemeMode.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        themeModes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = themeMode == mode,
                onCheckedChange = { onThemeModeChanged(mode) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    themeModes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = when (mode) {
                        AppThemeMode.System -> Icons.Rounded.BrightnessAuto
                        AppThemeMode.Light -> Icons.Rounded.LightMode
                        AppThemeMode.Dark -> Icons.Rounded.DarkMode
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(themeModeLabel(mode), maxLines = 1)
            }
        }
    }
}

@Composable
private fun expressiveClickableCardShape(
    interactionSource: MutableInteractionSource,
    position: SettingsCardPosition = SettingsCardPosition.Single,
): RoundedCornerShape {
    val pressed by interactionSource.collectIsPressedAsState()
    val topRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Top) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-top-corner",
    )
    val bottomRadius by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            position == SettingsCardPosition.Single -> 16.dp
            position in setOf(SettingsCardPosition.GroupedSingle, SettingsCardPosition.Bottom) -> 24.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clickable-card-bottom-corner",
    )
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@Composable
private fun SideChoiceMenu(
    choices: List<String>,
    selectedIndex: Int,
    topOffset: Dp,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun closeMenu(afterAnimation: () -> Unit) {
        if (closing) return
        closing = true
        visible = false
        coroutineScope.launch {
            delay(MENU_EXIT_WAIT_MILLIS)
            afterAnimation()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    Popup(
        onDismissRequest = { closeMenu(onDismiss) },
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val estimatedHeight = 16.dp + 56.dp * choices.size
            val constrainedTop = minOf(
                topOffset,
                maxHeight - estimatedHeight - 24.dp,
            ).coerceAtLeast(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { closeMenu(onDismiss) },
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = constrainedTop, end = 18.dp),
                enter = scaleIn(
                    animationSpec = keyframes {
                        durationMillis = 200
                        1.025f at 95
                        0.995f at 155
                    },
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(1f, 0f),
                ),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = MENU_EXIT_ANIMATION_MILLIS),
                    targetScale = 0.86f,
                    transformOrigin = TransformOrigin(1f, 0.5f),
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 160,
                        delayMillis = 20,
                    ),
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .width(196.dp)
                        .heightIn(max = 620.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(choices) { index, choice ->
                            val selected = index == selectedIndex
                            Surface(
                                onClick = {
                                    closeMenu { onSelected(index) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = if (selected) {
                                    MaterialTheme.shapes.extraLarge
                                } else {
                                    MaterialTheme.shapes.medium
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Text(
                                        text = choice,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val MENU_EXIT_ANIMATION_MILLIS = 180
private const val MENU_EXIT_WAIT_MILLIS = 200L
private const val PROJECT_GITHUB_URL = "https://github.com/ankitrawatgit"

@Composable
private fun languageLabel(tag: String): String = when {
    tag.startsWith("ko") -> stringResource(R.string.language_korean)
    tag.startsWith("en") -> stringResource(R.string.language_english)
    tag.startsWith("ja") -> stringResource(R.string.language_japanese)
    tag.startsWith("zh") -> stringResource(R.string.language_chinese)
    else -> stringResource(R.string.language_system)
}

@Composable
private fun accentLabel(color: AccentColor): String = when (color) {
    AccentColor.Dynamic -> stringResource(R.string.color_dynamic)
    AccentColor.Blue -> stringResource(R.string.color_blue)
    AccentColor.Violet -> stringResource(R.string.color_violet)
    AccentColor.Green -> stringResource(R.string.color_green)
    AccentColor.Orange -> stringResource(R.string.color_orange)
}

@Composable
private fun themeModeLabel(themeMode: AppThemeMode): String = when (themeMode) {
    AppThemeMode.System -> stringResource(R.string.theme_system)
    AppThemeMode.Light -> stringResource(R.string.theme_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_dark)
}
