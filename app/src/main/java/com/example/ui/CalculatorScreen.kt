package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.CalculatorDisplay
import com.example.ui.components.ExpandedScientificColumn
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.HistoryPanelContent
import com.example.ui.components.ScientificKeypadPanel
import com.example.ui.components.StandardKeypadGrid
import com.example.ui.theme.LocalCalculatorPalette
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.copied_to_clipboard)

    val onCopyResult: (String) -> Unit = { valueToCopy ->
        clipboardManager.setText(AnnotatedString(valueToCopy))
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(copiedMessage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWideLayout = maxWidth >= 680.dp && maxWidth > maxHeight * 1.05f

            if (isWideLayout) {
                WideCalculatorLayout(
                    state = state,
                    onAction = onAction,
                    onCopyResult = onCopyResult,
                )
            } else {
                CompactCalculatorLayout(
                    state = state,
                    onAction = onAction,
                    onCopyResult = onCopyResult,
                )
            }
        }
    }

    if (state.isHistorySheetVisible) {
        HistoryBottomSheet(
            history = state.history,
            onDismiss = { onAction(CalculatorAction.SetHistoryVisible(false)) },
            onReuseExpression = { onAction(CalculatorAction.ReuseHistoryExpression(it)) },
            onReuseResult = { onAction(CalculatorAction.ReuseHistoryResult(it)) },
            onClearHistory = { onAction(CalculatorAction.ClearHistory) },
        )
    }
}

@Composable
private fun CompactCalculatorLayout(
    state: CalculatorUiState,
    onAction: (CalculatorAction) -> Unit,
    onCopyResult: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalculatorTopToolbar(
            state = state,
            onAction = onAction,
            showScientificToggle = true,
            showHistoryButton = true,
        )

        CalculatorDisplay(
            state = state,
            onCopyResult = onCopyResult,
            onToggleAngleMode = { onAction(CalculatorAction.ToggleAngleMode) },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            ScientificKeypadPanel(
                visible = state.isScientificExpanded,
                onAction = onAction,
            )

            StandardKeypadGrid(
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                fillHeight = true,
            )
        }
    }
}

@Composable
private fun WideCalculatorLayout(
    state: CalculatorUiState,
    onAction: (CalculatorAction) -> Unit,
    onCopyResult: (String) -> Unit,
) {
    val palette = LocalCalculatorPalette.current

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Left pane: Toolbar + Display + Live History Tape
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorTopToolbar(
                state = state,
                onAction = onAction,
                showScientificToggle = false,
                showHistoryButton = false,
            )

            CalculatorDisplay(
                state = state,
                onCopyResult = onCopyResult,
                onToggleAngleMode = { onAction(CalculatorAction.ToggleAngleMode) },
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = palette.displayContainer,
                border = BorderStroke(1.dp, palette.displayBorder),
            ) {
                HistoryPanelContent(
                    history = state.history,
                    onReuseExpression = { onAction(CalculatorAction.ReuseHistoryExpression(it)) },
                    onReuseResult = { onAction(CalculatorAction.ReuseHistoryResult(it)) },
                    onClearHistory = { onAction(CalculatorAction.ClearHistory) },
                    onClose = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Right pane: Scientific + Standard keypad side-by-side
        Row(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExpandedScientificColumn(
                onAction = onAction,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            )
            StandardKeypadGrid(
                onAction = onAction,
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                fillHeight = true,
            )
        }
    }
}

@Composable
private fun CalculatorTopToolbar(
    state: CalculatorUiState,
    onAction: (CalculatorAction) -> Unit,
    showScientificToggle: Boolean,
    showHistoryButton: Boolean,
) {
    val palette = LocalCalculatorPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showScientificToggle) {
                Surface(
                    onClick = { onAction(CalculatorAction.ToggleScientific) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_scientific_button"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.isScientificExpanded) {
                        palette.operatorKeyContainer
                    } else {
                        palette.utilityKeyContainer.copy(alpha = 0.55f)
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isScientificExpanded) {
                                Icons.Filled.Functions
                            } else {
                                Icons.Outlined.Functions
                            },
                            contentDescription = stringResource(R.string.toggle_scientific_cd),
                            tint = if (state.isScientificExpanded) {
                                palette.operatorKeyContent
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (showHistoryButton) {
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = { onAction(CalculatorAction.SetHistoryVisible(true)) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_history_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.open_history_cd),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (state.history.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, end = 10.dp)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }

            IconButton(
                onClick = { onAction(CalculatorAction.ToggleTheme) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_theme_button"),
            ) {
                Icon(
                    imageVector = if (state.isDarkTheme) {
                        Icons.Outlined.LightMode
                    } else {
                        Icons.Outlined.DarkMode
                    },
                    contentDescription = stringResource(R.string.toggle_theme_cd),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
