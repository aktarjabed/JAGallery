package com.aktarjabed.jagallery.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.jagallery.domain.TrashRetentionPolicy
import com.aktarjabed.jagallery.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentPolicy by viewModel.retentionPolicy.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isFirstEmission by remember { mutableStateOf(true) }

    LaunchedEffect(currentPolicy) {
        if (isFirstEmission) {
            isFirstEmission = false
        } else {
            snackbarHostState.showSnackbar("Trash retention policy updated")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.trash_retention_policy), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.trash_retention_description), style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(16.dp))

            TrashRetentionPolicy.entries.forEach { policy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentPolicy == policy,
                            role = Role.RadioButton,
                            onClick = { viewModel.setRetentionPolicy(policy) }
                        ),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentPolicy == policy,
                        onClick = { viewModel.setRetentionPolicy(policy) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (policy == TrashRetentionPolicy.NEVER) stringResource(R.string.retention_never) else stringResource(R.string.retention_days, policy.days))
                }
            }
        }
    }
}
