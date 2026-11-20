package com.example.ui

import com.example.domain.AngleMode
import com.example.domain.EvaluationErrorType

data class HistoryEntry(
    val id: Long,
    val expression: String,
    val formattedResult: String,
    val rawResult: String,
)

data class CalculatorUiState(
    val expression: String = "",
    val lastExpressionBanner: String = "",
    val previewResult: String? = null,
    val errorType: EvaluationErrorType? = null,
    val isResultDisplayed: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val isScientificExpanded: Boolean = false,
    val isDarkTheme: Boolean = false,
    val isHistorySheetVisible: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
)

sealed interface CalculatorAction {
    data class AppendDigit(val digit: Char) : CalculatorAction
    data object AppendDecimal : CalculatorAction
    data class AppendOperator(val operator: Char) : CalculatorAction
    data class AppendFunction(val functionPrefix: String) : CalculatorAction
    data class AppendConstant(val symbol: Char) : CalculatorAction
    data object AppendPercent : CalculatorAction
    data object AppendFactorial : CalculatorAction
    data object AppendSmartParenthesis : CalculatorAction
    data object ToggleSign : CalculatorAction
    data object Reciprocal : CalculatorAction
    data object Backspace : CalculatorAction
    data object ClearAll : CalculatorAction
    data object Evaluate : CalculatorAction
    data object ToggleAngleMode : CalculatorAction
    data object ToggleScientific : CalculatorAction
    data object ToggleTheme : CalculatorAction
    data class SetHistoryVisible(val visible: Boolean) : CalculatorAction
    data class ReuseHistoryExpression(val entry: HistoryEntry) : CalculatorAction
    data class ReuseHistoryResult(val entry: HistoryEntry) : CalculatorAction
    data object ClearHistory : CalculatorAction
}
