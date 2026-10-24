package com.aktarjabed.jagallery.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.domain.ThemeMode
import com.aktarjabed.jagallery.domain.TrashRetentionPolicy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val vaultTimeoutMinutes by viewModel.vaultTimeoutMinutes.collectAsStateWithLifecycle()
    val retentionPolicy by viewModel.retentionPolicy.collectAsStateWithLifecycle()

    val systemInDark = isSystemInDarkTheme()
    val isDarkActive = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showVaultTimeoutDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APPEARANCE & DISPLAY CATEGORY
            SettingsCategoryHeader(title = stringResource(R.string.settings_category_appearance))
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // App Theme Mode + Inline Segmented Control
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.theme_mode_title)) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    when (themeMode) {
                                        ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                        ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                    }
                                )
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("theme_mode_segmented_row")
                                ) {
                                    val modes = ThemeMode.entries
                                    modes.forEachIndexed { index, mode ->
                                        SegmentedButton(
                                            selected = themeMode == mode,
                                            onClick = { viewModel.setThemeMode(mode) },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = modes.size
                                            ),
                                            icon = {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.testTag("theme_segment_${mode.name.lowercase()}")
                                        ) {
                                            Text(
                                                text = when (mode) {
                                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                        modifier = Modifier
                            .clickable { showThemeDialog = true }
                            .testTag("theme_mode_item")
                    )
                    HorizontalDivider()

                    // Explicit Light / Dark Mode Switch Toggle
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dark_mode_toggle_title)) },
                        supportingContent = {
                            Text(
                                when (themeMode) {
                                    ThemeMode.DARK -> stringResource(R.string.dark_mode_toggle_desc_dark)
                                    ThemeMode.LIGHT -> stringResource(R.string.dark_mode_toggle_desc_light)
                                    ThemeMode.SYSTEM -> stringResource(R.string.dark_mode_toggle_desc_system)
                                }
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isDarkActive) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isDarkActive,
                                onCheckedChange = { enableDark ->
                                    viewModel.toggleExplicitDarkMode(enableDark)
                                },
                                modifier = Modifier.testTag("explicit_dark_mode_switch")
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.toggleExplicitDarkMode(!isDarkActive)
                        }
                    )
                    HorizontalDivider()

                    // Dynamic Colors
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dynamic_colors_title)) },
                        supportingContent = { Text(stringResource(R.string.dynamic_colors_desc)) },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = { viewModel.setDynamicColor(it) }
                            )
                        }
                    )
                    HorizontalDivider()

                    // Grid Columns
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.grid_columns_title)) },
                        supportingContent = {
                            Text(
                                if (gridColumns == 0) stringResource(R.string.grid_columns_auto)
                                else stringResource(R.string.grid_columns_n, gridColumns)
                            )
                        },
                        leadingContent = { Icon(Icons.Default.GridOn, contentDescription = null) },
                        modifier = Modifier.clickable { showGridDialog = true }
                    )
                }
            }

            // SECURITY & VAULT CATEGORY
            SettingsCategoryHeader(title = stringResource(R.string.settings_category_security))
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.vault_timeout_title)) },
                        supportingContent = {
                            Text(
                                if (vaultTimeoutMinutes == 0) stringResource(R.string.vault_timeout_immediately)
                                else stringResource(R.string.vault_timeout_minutes, vaultTimeoutMinutes)
                            )
                        },
                        leadingContent = { Icon(Icons.Default.LockClock, contentDescription = null) },
                        modifier = Modifier.clickable { showVaultTimeoutDialog = true }
                    )
                }
            }

            // STORAGE & TRASH CATEGORY
            SettingsCategoryHeader(title = stringResource(R.string.settings_category_storage))
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Trash Retention Policy
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.trash_retention_policy)) },
                        supportingContent = {
                            Text(
                                if (retentionPolicy == TrashRetentionPolicy.NEVER)
                                    stringResource(R.string.retention_never)
                                else
                                    stringResource(R.string.retention_days, retentionPolicy.days)
                            )
                        },
                        leadingContent = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
                        modifier = Modifier.clickable { showTrashDialog = true }
                    )
                    HorizontalDivider()

                    // Clear Cache
                    val cacheClearedMsg = stringResource(R.string.cache_cleared)
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.clear_cache_title)) },
                        supportingContent = { Text(stringResource(R.string.clear_cache_desc)) },
                        leadingContent = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                        modifier = Modifier.clickable {
                            viewModel.clearCache(context) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(cacheClearedMsg)
                                }
                            }
                        }
                    )
                }
            }

            // ABOUT CATEGORY
            SettingsCategoryHeader(title = stringResource(R.string.settings_category_about))
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.app_about_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Theme Choice Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.theme_mode_title)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themeMode == mode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Grid Choice Dialog
    if (showGridDialog) {
        AlertDialog(
            onDismissRequest = { showGridDialog = false },
            title = { Text(stringResource(R.string.grid_columns_title)) },
            text = {
                Column {
                    listOf(0, 2, 3, 4, 5).forEach { cols ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = gridColumns == cols,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setGridColumns(cols)
                                        showGridDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = gridColumns == cols,
                                onClick = {
                                    viewModel.setGridColumns(cols)
                                    showGridDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (cols == 0) stringResource(R.string.grid_columns_auto)
                                else stringResource(R.string.grid_columns_n, cols)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGridDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Vault Timeout Dialog
    if (showVaultTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showVaultTimeoutDialog = false },
            title = { Text(stringResource(R.string.vault_timeout_title)) },
            text = {
                Column {
                    listOf(0, 1, 5, 15).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = vaultTimeoutMinutes == mins,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setVaultTimeoutMinutes(mins)
                                        showVaultTimeoutDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = vaultTimeoutMinutes == mins,
                                onClick = {
                                    viewModel.setVaultTimeoutMinutes(mins)
                                    showVaultTimeoutDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (mins == 0) stringResource(R.string.vault_timeout_immediately)
                                else stringResource(R.string.vault_timeout_minutes, mins)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultTimeoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Trash Retention Dialog
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text(stringResource(R.string.trash_retention_policy)) },
            text = {
                Column {
                    TrashRetentionPolicy.entries.forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = retentionPolicy == policy,
                                    role = Role.RadioButton,
                                    onClick = {
                                        viewModel.setRetentionPolicy(policy)
                                        showTrashDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = retentionPolicy == policy,
                                onClick = {
                                    viewModel.setRetentionPolicy(policy)
                                    showTrashDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (policy == TrashRetentionPolicy.NEVER)
                                    stringResource(R.string.retention_never)
                                else
                                    stringResource(R.string.retention_days, policy.days)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}
