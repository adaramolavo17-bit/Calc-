package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.CalculatorAction
import com.example.ui.theme.LocalCalculatorPalette

enum class KeyCategory {
    DIGIT,
    UTILITY,
    CLEAR,
    OPERATOR,
    SCIENTIFIC,
    EQUALS,
}

data class CalcKeySpec(
    val label: String,
    val testTag: String,
    val category: KeyCategory,
    val action: CalculatorAction,
    val icon: ImageVector? = null,
    val contentDescription: String? = null,
)

@Composable
fun StandardKeypadGrid(
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    keySpacing: Dp = 10.dp,
    fillHeight: Boolean = true,
) {
    val backspaceCd = stringResource(R.string.backspace_cd)

    val rows = remember(backspaceCd) {
        listOf(
            listOf(
                CalcKeySpec("AC", "key_ac", KeyCategory.CLEAR, CalculatorAction.ClearAll, contentDescription = "All clear"),
                CalcKeySpec("( )", "key_parens", KeyCategory.UTILITY, CalculatorAction.AppendSmartParenthesis, contentDescription = "Parentheses"),
                CalcKeySpec("%", "key_percent", KeyCategory.UTILITY, CalculatorAction.AppendPercent, contentDescription = "Percent"),
                CalcKeySpec("÷", "key_divide", KeyCategory.OPERATOR, CalculatorAction.AppendOperator('÷'), contentDescription = "Divide"),
            ),
            listOf(
                CalcKeySpec("7", "key_7", KeyCategory.DIGIT, CalculatorAction.AppendDigit('7')),
                CalcKeySpec("8", "key_8", KeyCategory.DIGIT, CalculatorAction.AppendDigit('8')),
                CalcKeySpec("9", "key_9", KeyCategory.DIGIT, CalculatorAction.AppendDigit('9')),
                CalcKeySpec("×", "key_multiply", KeyCategory.OPERATOR, CalculatorAction.AppendOperator('×'), contentDescription = "Multiply"),
            ),
            listOf(
                CalcKeySpec("4", "key_4", KeyCategory.DIGIT, CalculatorAction.AppendDigit('4')),
                CalcKeySpec("5", "key_5", KeyCategory.DIGIT, CalculatorAction.AppendDigit('5')),
                CalcKeySpec("6", "key_6", KeyCategory.DIGIT, CalculatorAction.AppendDigit('6')),
                CalcKeySpec("−", "key_subtract", KeyCategory.OPERATOR, CalculatorAction.AppendOperator('−'), contentDescription = "Subtract"),
            ),
            listOf(
                CalcKeySpec("1", "key_1", KeyCategory.DIGIT, CalculatorAction.AppendDigit('1')),
                CalcKeySpec("2", "key_2", KeyCategory.DIGIT, CalculatorAction.AppendDigit('2')),
                CalcKeySpec("3", "key_3", KeyCategory.DIGIT, CalculatorAction.AppendDigit('3')),
                CalcKeySpec("+", "key_add", KeyCategory.OPERATOR, CalculatorAction.AppendOperator('+'), contentDescription = "Add"),
            ),
            listOf(
                CalcKeySpec("0", "key_0", KeyCategory.DIGIT, CalculatorAction.AppendDigit('0')),
                CalcKeySpec(".", "key_decimal", KeyCategory.DIGIT, CalculatorAction.AppendDecimal, contentDescription = "Decimal point"),
                CalcKeySpec("⌫", "key_backspace", KeyCategory.UTILITY, CalculatorAction.Backspace, icon = Icons.AutoMirrored.Outlined.Backspace, contentDescription = backspaceCd),
                CalcKeySpec("=", "key_equals", KeyCategory.EQUALS, CalculatorAction.Evaluate, contentDescription = "Equals"),
            ),
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(keySpacing),
    ) {
        rows.forEach { row ->
            Row(
                modifier = if (fillHeight) {
                    Modifier.fillMaxWidth().weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                },
                horizontalArrangement = Arrangement.spacedBy(keySpacing),
            ) {
                row.forEach { spec ->
                    CalculatorKeyButton(
                        spec = spec,
                        onClick = { onAction(spec.action) },
                        modifier = if (fillHeight) {
                            Modifier.weight(1f).fillMaxHeight()
                        } else {
                            Modifier.weight(1f).height(56.dp)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ScientificKeypadPanel(
    visible: Boolean,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    keySpacing: Dp = 8.dp,
) {
    val rows = remember {
        listOf(
            listOf(
                CalcKeySpec("sin", "key_sin", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("sin("), contentDescription = "Sine"),
                CalcKeySpec("cos", "key_cos", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("cos("), contentDescription = "Cosine"),
                CalcKeySpec("tan", "key_tan", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("tan("), contentDescription = "Tangent"),
                CalcKeySpec("ln", "key_ln", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("ln("), contentDescription = "Natural logarithm"),
                CalcKeySpec("log", "key_log", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("log("), contentDescription = "Base-10 logarithm"),
            ),
            listOf(
                CalcKeySpec("√", "key_sqrt", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("√("), contentDescription = "Square root"),
                CalcKeySpec("xʸ", "key_power", KeyCategory.SCIENTIFIC, CalculatorAction.AppendOperator('^'), contentDescription = "Power"),
                CalcKeySpec("π", "key_pi", KeyCategory.SCIENTIFIC, CalculatorAction.AppendConstant('π'), contentDescription = "Pi"),
                CalcKeySpec("e", "key_e", KeyCategory.SCIENTIFIC, CalculatorAction.AppendConstant('e'), contentDescription = "Euler's number"),
                CalcKeySpec("x!", "key_factorial", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFactorial, contentDescription = "Factorial"),
            ),
            listOf(
                CalcKeySpec("+/−", "key_toggle_sign", KeyCategory.SCIENTIFIC, CalculatorAction.ToggleSign, contentDescription = "Toggle positive or negative sign"),
                CalcKeySpec("1/x", "key_reciprocal", KeyCategory.SCIENTIFIC, CalculatorAction.Reciprocal, contentDescription = "Reciprocal"),
                CalcKeySpec("asin", "key_asin", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("asin("), contentDescription = "Inverse sine"),
                CalcKeySpec("acos", "key_acos", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("acos("), contentDescription = "Inverse cosine"),
                CalcKeySpec("atan", "key_atan", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("atan("), contentDescription = "Inverse tangent"),
            ),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
            shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(keySpacing),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(keySpacing),
                ) {
                    row.forEach { spec ->
                        CalculatorKeyButton(
                            spec = spec,
                            onClick = { onAction(spec.action) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            fontSize = 15.sp,
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedScientificColumn(
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    keySpacing: Dp = 10.dp,
) {
    val rows = remember {
        listOf(
            listOf(
                CalcKeySpec("sin", "key_sin", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("sin("), contentDescription = "Sine"),
                CalcKeySpec("cos", "key_cos", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("cos("), contentDescription = "Cosine"),
                CalcKeySpec("tan", "key_tan", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("tan("), contentDescription = "Tangent"),
            ),
            listOf(
                CalcKeySpec("asin", "key_asin", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("asin("), contentDescription = "Inverse sine"),
                CalcKeySpec("acos", "key_acos", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("acos("), contentDescription = "Inverse cosine"),
                CalcKeySpec("atan", "key_atan", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("atan("), contentDescription = "Inverse tangent"),
            ),
            listOf(
                CalcKeySpec("ln", "key_ln", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("ln("), contentDescription = "Natural logarithm"),
                CalcKeySpec("log", "key_log", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("log("), contentDescription = "Base-10 logarithm"),
                CalcKeySpec("√", "key_sqrt", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFunction("√("), contentDescription = "Square root"),
            ),
            listOf(
                CalcKeySpec("xʸ", "key_power", KeyCategory.SCIENTIFIC, CalculatorAction.AppendOperator('^'), contentDescription = "Power"),
                CalcKeySpec("π", "key_pi", KeyCategory.SCIENTIFIC, CalculatorAction.AppendConstant('π'), contentDescription = "Pi"),
                CalcKeySpec("e", "key_e", KeyCategory.SCIENTIFIC, CalculatorAction.AppendConstant('e'), contentDescription = "Euler's number"),
            ),
            listOf(
                CalcKeySpec("x!", "key_factorial", KeyCategory.SCIENTIFIC, CalculatorAction.AppendFactorial, contentDescription = "Factorial"),
                CalcKeySpec("+/−", "key_toggle_sign", KeyCategory.SCIENTIFIC, CalculatorAction.ToggleSign, contentDescription = "Toggle sign"),
                CalcKeySpec("1/x", "key_reciprocal", KeyCategory.SCIENTIFIC, CalculatorAction.Reciprocal, contentDescription = "Reciprocal"),
            ),
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(keySpacing),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(keySpacing),
            ) {
                row.forEach { spec ->
                    CalculatorKeyButton(
                        spec = spec,
                        onClick = { onAction(spec.action) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        fontSize = 16.sp,
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorKeyButton(
    spec: CalcKeySpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit? = null,
    shape: Shape? = null,
) {
    val palette = LocalCalculatorPalette.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "keyPressScale",
    )

    val containerColor: Color
    val contentColor: Color
    val defaultShape: Shape
    val defaultFontSize: TextUnit
    val fontWeight: FontWeight

    when (spec.category) {
        KeyCategory.DIGIT -> {
            containerColor = palette.digitKeyContainer
            contentColor = palette.digitKeyContent
            defaultShape = RoundedCornerShape(22.dp)
            defaultFontSize = 26.sp
            fontWeight = FontWeight.Medium
        }
        KeyCategory.UTILITY -> {
            containerColor = palette.utilityKeyContainer
            contentColor = palette.utilityKeyContent
            defaultShape = RoundedCornerShape(22.dp)
            defaultFontSize = 21.sp
            fontWeight = FontWeight.SemiBold
        }
        KeyCategory.CLEAR -> {
            containerColor = palette.clearKeyContainer
            contentColor = palette.clearKeyContent
            defaultShape = RoundedCornerShape(22.dp)
            defaultFontSize = 20.sp
            fontWeight = FontWeight.Bold
        }
        KeyCategory.OPERATOR -> {
            containerColor = palette.operatorKeyContainer
            contentColor = palette.operatorKeyContent
            defaultShape = RoundedCornerShape(22.dp)
            defaultFontSize = 28.sp
            fontWeight = FontWeight.SemiBold
        }
        KeyCategory.SCIENTIFIC -> {
            containerColor = palette.scientificKeyContainer
            contentColor = palette.scientificKeyContent
            defaultShape = RoundedCornerShape(16.dp)
            defaultFontSize = 15.sp
            fontWeight = FontWeight.Medium
        }
        KeyCategory.EQUALS -> {
            containerColor = palette.equalsKeyContainer
            contentColor = palette.equalsKeyContent
            defaultShape = RoundedCornerShape(24.dp)
            defaultFontSize = 30.sp
            fontWeight = FontWeight.Bold
        }
    }

    val resolvedShape = shape ?: defaultShape
    val resolvedFontSize = fontSize ?: defaultFontSize

    Surface(
        onClick = {
            haptic.performHapticFeedback(
                if (spec.category == KeyCategory.EQUALS || spec.category == KeyCategory.CLEAR) {
                    HapticFeedbackType.LongPress
                } else {
                    HapticFeedbackType.TextHandleMove
                },
            )
            onClick()
        },
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(spec.testTag)
            .semantics {
                spec.contentDescription?.let { contentDescription = it }
            },
        shape = resolvedShape,
        color = containerColor,
        contentColor = contentColor,
        border = if (spec.category == KeyCategory.EQUALS) {
            null
        } else {
            BorderStroke(0.75.dp, palette.keyShadowBorder)
        },
        tonalElevation = if (spec.category == KeyCategory.DIGIT) 2.dp else 0.dp,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (spec.icon != null) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = spec.contentDescription ?: spec.label,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Text(
                    text = spec.label,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = resolvedFontSize,
                        fontWeight = fontWeight,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}
