package io.github.timpushkin.tapok.ui.activity.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.timpushkin.tapok.agent.AgentService
import io.github.timpushkin.tapok.R
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPane
import io.github.timpushkin.tapok.ui.activity.components.SegmentedListPaneDefaults
import io.github.timpushkin.tapok.ui.theme.DefaultOuterSpacing
import io.github.timpushkin.tapok.ui.utils.openAccessibilityServiceSettings
import io.github.timpushkin.tapok.ui.utils.rememberIsAccessibilityServiceEnabled

@Composable
fun SettingsPane(openTutorial: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    Scaffold(containerColor = Color.Transparent) { padding ->
        SegmentedListPane(
            contentPadding = PaddingValues(DefaultOuterSpacing) + padding,
            modifier = Modifier.consumeWindowInsets(padding)
        ) {
            item {
                val a11yUsageAccepted by viewModel.isAccessibilityUsageAccepted
                    .collectAsStateWithLifecycle()
                AgentStatusItem(0, 2, a11yUsageAccepted, viewModel::markAccessibilityUsageAccepted)
            }
            item {
                val serviceEnabled = rememberIsAccessibilityServiceEnabled(AgentService::class)
                val shouldShowScannerOverlay by viewModel.shouldShowScannerOverlay
                    .collectAsStateWithLifecycle()
                SwitchItem(
                    1, 2, stringResource(R.string.show_scanner_overlay),
                    checked = shouldShowScannerOverlay,
                    onCheckedChange = viewModel::setShouldShowScannerOverlay,
                    enabled = serviceEnabled
                )
            }

            item { SegmentedListPaneDefaults.GroupSpacer() }

            item { ClickItem(0, 1, stringResource(R.string.view_tutorial), openTutorial) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun AgentStatusItem(
    index: Int,
    count: Int,
    accessibilityUsageAccepted: Boolean,
    onAccessibilityUsageAccepted: () -> Unit
) {
    val context = LocalContext.current

    var showAccessibilityDisclosureDialog by retain { mutableStateOf(false) }
    if (showAccessibilityDisclosureDialog) {
        AccessibilityDisclosureDialog(
            onAccept = {
                onAccessibilityUsageAccepted()
                context.openAccessibilityServiceSettings(AgentService::class)
            },
            onHide = { showAccessibilityDisclosureDialog = false }
        )
    }

    val agentEnabled = rememberIsAccessibilityServiceEnabled(AgentService::class)
    val updatableAccessibilityUsageAccepted by rememberUpdatedState(accessibilityUsageAccepted)
    SegmentedListItem(
        content = { Text(stringResource(R.string.open_agent_settings)) },
        supportingContent = {
            val stringId = if (agentEnabled) {
                R.string.agent_is_enabled
            } else {
                R.string.enable_agent_to_run_automations
            }
            Text(stringResource(stringId))
        },
        trailingContent = {
            Icon(painterResource(R.drawable.open_in_new), contentDescription = null)
        },
        onClick = {
            if (updatableAccessibilityUsageAccepted) {
                context.openAccessibilityServiceSettings(AgentService::class)
            } else {
                showAccessibilityDisclosureDialog = true
            }
        },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = if (agentEnabled) {
            SegmentedListPaneDefaults.colors()
        } else {
            SegmentedListPaneDefaults.colors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                contentVariantColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun SwitchItem(
    index: Int,
    count: Int,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    SegmentedListItem(
        content = { Text(text) },
        trailingContent = { Switch(checked, onCheckedChange = null, enabled = enabled) },
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = SegmentedListPaneDefaults.colors(),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ClickItem(
    index: Int,
    count: Int,
    text: String,
    onClick: () -> Unit
) {
    SegmentedListItem(
        content = { Text(text) },
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = SegmentedListPaneDefaults.colors(),
        modifier = Modifier.fillMaxSize()
    )
}
