package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AngleMode
import com.example.domain.EvaluationErrorType
import com.example.ui.CalculatorUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCalculatorPalette
import com.example.ui.theme.SpaceGroteskFontFamily
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun CalculatorDisplay(
    state: CalculatorUiState,
    onCopyResult: (String) -> Unit,
    onToggleAngleMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalCalculatorPalette.current
    val exprScrollState = rememberScrollState()

    val shownMainText = when {
        state.expression.isEmpty() -> "0"
        state.isResultDisplayed -> formatStandaloneNumber(state.expression)
        else -> state.expression
    }

    LaunchedEffect(shownMainText) {
        exprScrollState.animateScrollTo(exprScrollState.maxValue)
    }

    val dynamicFontSize = when {
        shownMainText.length <= 9 -> 54.sp
        shownMainText.length <= 13 -> 42.sp
        shownMainText.length <= 18 -> 34.sp
        else -> 28.sp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calculator_display"),
        shape = RoundedCornerShape(28.dp),
        color = palette.displayContainer,
        border = BorderStroke(1.dp, palette.displayBorder),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top micro-status row: Angle mode pill + Copy result button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onToggleAngleMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("angle_mode_button"),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.utilityKeyContainer.copy(alpha = 0.7f),
                ) {
                    Text(
                        text = if (state.angleMode == AngleMode.DEG) {
                            stringResource(R.string.angle_mode_deg)
                        } else {
                            stringResource(R.string.angle_mode_rad)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.operatorKeyContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                AnimatedVisibility(
                    visible = state.expression.isNotEmpty() || state.previewResult != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val copyValue = state.previewResult ?: shownMainText
                    IconButton(
                        onClick = { onCopyResult(copyValue) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("copy_result_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.copy_result_cd),
                            tint = palette.subtleText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Previous solved expression banner (e.g. "24 × 5 =")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AnimatedContent(
                    targetState = state.lastExpressionBanner,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "lastExpressionBanner",
                ) { banner ->
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 15.sp,
                        ),
                        color = palette.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("last_expression_text"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary expression / result readout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(exprScrollState),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = shownMainText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = dynamicFontSize,
                        lineHeight = dynamicFontSize * 1.15f,
                        fontWeight = if (state.isResultDisplayed) FontWeight.SemiBold else FontWeight.Medium,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("expression_text"),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live preview or Error feedback row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                when {
                    state.errorType != null -> {
                        val errorText = when (state.errorType) {
                            EvaluationErrorType.DIVIDE_BY_ZERO -> stringResource(R.string.error_divide_by_zero)
                            EvaluationErrorType.INVALID_EXPRESSION -> stringResource(R.string.error_invalid_expression)
                            EvaluationErrorType.DOMAIN_ERROR -> stringResource(R.string.error_domain)
                            EvaluationErrorType.OVERFLOW -> stringResource(R.string.error_overflow)
                        }
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("error_text"),
                        )
                    }
                    state.previewResult != null -> {
                        Text(
                            text = "= ${state.previewResult}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 20.sp,
                                fontFeatureSettings = "tnum",
                            ),
                            color = palette.subtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("preview_result_text"),
                        )
                    }
                }
            }
        }
    }
}

private fun formatStandaloneNumber(raw: String): String {
    if (raw.contains('E')) return raw
    return try {
        val bd = BigDecimal(raw)
        val absBd = bd.abs()
        if (bd.compareTo(BigDecimal.ZERO) != 0 &&
            (absBd >= BigDecimal("1e12") || absBd < BigDecimal("1e-9"))
        ) {
            DecimalFormat("0.##########E0", DecimalFormatSymbols(Locale.US)).format(bd)
        } else {
            val plain = bd.stripTrailingZeros().toPlainString()
            val parts = plain.split('.')
            val intPart = parts[0]
            val neg = intPart.startsWith("-")
            val digits = if (neg) intPart.substring(1) else intPart
            val grouped = buildString {
                for (i in digits.indices) {
                    if (i > 0 && (digits.length - i) % 3 == 0) append(',')
                    append(digits[i])
                }
            }
            val prefix = if (neg) "-" else ""
            if (parts.size > 1) "$prefix$grouped.${parts[1]}" else "$prefix$grouped"
        }
    } catch (_: NumberFormatException) {
        raw
    }
}
