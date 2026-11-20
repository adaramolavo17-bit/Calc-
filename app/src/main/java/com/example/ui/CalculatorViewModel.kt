package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.domain.AngleMode
import com.example.domain.CalculatorEngine
import com.example.domain.EvaluationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var nextHistoryId = 1L

    fun initializeTheme(systemIsDark: Boolean) {
        _uiState.update { state ->
            if (state.expression.isEmpty() && state.history.isEmpty() && !state.isResultDisplayed) {
                state.copy(isDarkTheme = systemIsDark)
            } else {
                state
            }
        }
    }

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.AppendDigit -> handleDigit(action.digit)
            CalculatorAction.AppendDecimal -> handleDecimal()
            is CalculatorAction.AppendOperator -> handleOperator(action.operator)
            is CalculatorAction.AppendFunction -> handleFunction(action.functionPrefix)
            is CalculatorAction.AppendConstant -> handleConstant(action.symbol)
            CalculatorAction.AppendPercent -> handlePostfix('%')
            CalculatorAction.AppendFactorial -> handlePostfix('!')
            CalculatorAction.AppendSmartParenthesis -> handleSmartParenthesis()
            CalculatorAction.ToggleSign -> handleToggleSign()
            CalculatorAction.Reciprocal -> handleReciprocal()
            CalculatorAction.Backspace -> handleBackspace()
            CalculatorAction.ClearAll -> handleClearAll()
            CalculatorAction.Evaluate -> handleEvaluate()
            CalculatorAction.ToggleAngleMode -> handleToggleAngleMode()
            CalculatorAction.ToggleScientific -> _uiState.update {
                it.copy(isScientificExpanded = !it.isScientificExpanded)
            }
            CalculatorAction.ToggleTheme -> _uiState.update {
                it.copy(isDarkTheme = !it.isDarkTheme)
            }
            is CalculatorAction.SetHistoryVisible -> _uiState.update {
                it.copy(isHistorySheetVisible = action.visible)
            }
            is CalculatorAction.ReuseHistoryExpression -> {
                updateExpression(
                    newExpression = action.entry.expression,
                    lastBanner = "",
                    isResultDisplayed = false,
                    closeSheet = true,
                )
            }
            is CalculatorAction.ReuseHistoryResult -> {
                val value = action.entry.rawResult
                _uiState.update { state ->
                    val base = if (state.isResultDisplayed) "" else state.expression
                    val combined = if (base.isEmpty()) value else base + value
                    val preview = CalculatorEngine.evaluatePreview(combined, state.angleMode)
                    state.copy(
                        expression = combined,
                        lastExpressionBanner = if (state.isResultDisplayed) "" else state.lastExpressionBanner,
                        previewResult = preview,
                        errorType = null,
                        isResultDisplayed = false,
                        isHistorySheetVisible = false,
                    )
                }
            }
            CalculatorAction.ClearHistory -> _uiState.update {
                it.copy(history = emptyList())
            }
        }
    }

    private fun handleDigit(digit: Char) {
        _uiState.update { state ->
            val base = if (state.isResultDisplayed) "" else state.expression
            val next = when {
                base == "0" -> digit.toString()
                base.endsWith("0") && isStandaloneZeroAtEnd(base) -> base.dropLast(1) + digit
                else -> base + digit
            }
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun isStandaloneZeroAtEnd(expr: String): Boolean {
        if (expr.length < 2) return expr == "0"
        val prev = expr[expr.length - 2]
        return !prev.isDigit() && prev != '.'
    }

    private fun handleDecimal() {
        _uiState.update { state ->
            val base = if (state.isResultDisplayed) "" else state.expression
            if (currentNumberHasDecimal(base)) return@update state
            val next = if (base.isEmpty() || !base.last().isDigit()) {
                "${base}0."
            } else {
                "$base."
            }
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun currentNumberHasDecimal(expr: String): Boolean {
        for (i in expr.length - 1 downTo 0) {
            val c = expr[i]
            if (c == '.') return true
            if (!c.isDigit()) return false
        }
        return false
    }

    private fun handleOperator(op: Char) {
        _uiState.update { state ->
            val base = state.expression
            if (base.isEmpty()) {
                return@update if (op == '−') {
                    state.withUpdatedExpression("−", isResultDisplayed = false)
                } else {
                    state
                }
            }
            val last = base.last()
            val next = when {
                last == '(' -> if (op == '−') "$base−" else return@update state
                isBinaryOperator(last) -> {
                    if (op == '−' && last != '−' && last != '+') {
                        // Allow negative exponent or factor, e.g. 5 × −2
                        "$base−"
                    } else if (base.length >= 2 && isBinaryOperator(base[base.length - 2]) && last == '−') {
                        // Replace two-char operator sequence like ×− with new operator
                        base.dropLast(2) + op
                    } else if (base.length == 1 && last == '−') {
                        // Don't replace a lone leading minus with × or ÷
                        return@update state
                    } else {
                        base.dropLast(1) + op
                    }
                }
                last == '.' -> "${base.dropLast(1)}$op"
                else -> "$base$op"
            }
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handleFunction(prefix: String) {
        _uiState.update { state ->
            val base = if (state.isResultDisplayed) "" else state.expression
            val next = "$base$prefix"
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handleConstant(symbol: Char) {
        _uiState.update { state ->
            val base = if (state.isResultDisplayed) "" else state.expression
            val next = "$base$symbol"
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handlePostfix(symbol: Char) {
        _uiState.update { state ->
            val base = state.expression
            if (base.isEmpty()) return@update state
            val last = base.last()
            val canAttach = last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!'
            if (!canAttach) return@update state
            state.withUpdatedExpression("$base$symbol", isResultDisplayed = false)
        }
    }

    private fun handleSmartParenthesis() {
        _uiState.update { state ->
            val base = if (state.isResultDisplayed) "" else state.expression
            val openCount = base.count { it == '(' }
            val closeCount = base.count { it == ')' }
            val last = base.lastOrNull()
            val shouldClose = openCount > closeCount &&
                last != null &&
                (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')
            val next = if (shouldClose) "$base)" else "$base("
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handleToggleSign() {
        _uiState.update { state ->
            val base = state.expression
            if (base.isEmpty()) {
                return@update state.withUpdatedExpression("−", isResultDisplayed = false)
            }
            if (base == "−") {
                return@update state.withUpdatedExpression("", isResultDisplayed = false)
            }
            // Check if trailing token is (−number)
            val wrappedNegRegex = Regex("""\(−(\d+(\.\d+)?)\)$""")
            val wrappedMatch = wrappedNegRegex.find(base)
            if (wrappedMatch != null) {
                val inner = wrappedMatch.groupValues[1]
                val next = base.substring(0, wrappedMatch.range.first) + inner
                return@update state.withUpdatedExpression(next, isResultDisplayed = false)
            }
            // Check if entire expression is a leading negative number
            val leadingNegRegex = Regex("""^−(\d+(\.\d+)?)$""")
            val leadingMatch = leadingNegRegex.find(base)
            if (leadingMatch != null) {
                return@update state.withUpdatedExpression(leadingMatch.groupValues[1], isResultDisplayed = false)
            }
            // Check if trailing is a positive number
            val trailingNumRegex = Regex("""(\d+(\.\d+)?)$""")
            val numMatch = trailingNumRegex.find(base)
            if (numMatch != null) {
                val start = numMatch.range.first
                val num = numMatch.value
                val next = if (start == 0) {
                    "−$num"
                } else {
                    base.substring(0, start) + "(−$num)"
                }
                return@update state.withUpdatedExpression(next, isResultDisplayed = false)
            }
            state
        }
    }

    private fun handleReciprocal() {
        _uiState.update { state ->
            val base = state.expression
            if (base.isEmpty()) return@update state
            val next = "1÷($base)"
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handleBackspace() {
        _uiState.update { state ->
            val base = state.expression
            if (base.isEmpty()) {
                return@update state.copy(errorType = null, lastExpressionBanner = "")
            }
            val functionPrefixes = listOf("asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "√(")
            val matchedPrefix = functionPrefixes.firstOrNull { base.endsWith(it) }
            val next = if (matchedPrefix != null) {
                base.dropLast(matchedPrefix.length)
            } else {
                base.dropLast(1)
            }
            state.withUpdatedExpression(next, isResultDisplayed = false)
        }
    }

    private fun handleClearAll() {
        _uiState.update { state ->
            state.copy(
                expression = "",
                lastExpressionBanner = "",
                previewResult = null,
                errorType = null,
                isResultDisplayed = false,
            )
        }
    }

    private fun handleEvaluate() {
        _uiState.update { state ->
            val expr = state.expression.trim()
            if (expr.isEmpty()) return@update state

            // Auto-close unmatched open parentheses when pressing '='
            val openParens = expr.count { it == '(' }
            val closeParens = expr.count { it == ')' }
            val balanced = if (openParens > closeParens) {
                expr + ")".repeat(openParens - closeParens)
            } else {
                expr
            }

            when (val result = CalculatorEngine.evaluate(balanced, state.angleMode)) {
                is EvaluationResult.Success -> {
                    val shouldRecordHistory = balanced != result.rawValue && !state.isResultDisplayed
                    val updatedHistory = if (shouldRecordHistory) {
                        val entry = HistoryEntry(
                            id = nextHistoryId++,
                            expression = balanced,
                            formattedResult = result.formatted,
                            rawResult = result.rawValue,
                        )
                        (listOf(entry) + state.history).take(50)
                    } else {
                        state.history
                    }
                    state.copy(
                        expression = result.rawValue,
                        lastExpressionBanner = "$balanced =",
                        previewResult = null,
                        errorType = null,
                        isResultDisplayed = true,
                        history = updatedHistory,
                    )
                }
                is EvaluationResult.Error -> {
                    state.copy(
                        errorType = result.type,
                        previewResult = null,
                    )
                }
            }
        }
    }

    private fun handleToggleAngleMode() {
        _uiState.update { state ->
            val nextMode = if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            val preview = CalculatorEngine.evaluatePreview(state.expression, nextMode)
            state.copy(
                angleMode = nextMode,
                previewResult = if (state.isResultDisplayed) null else preview,
            )
        }
    }

    private fun updateExpression(
        newExpression: String,
        lastBanner: String,
        isResultDisplayed: Boolean,
        closeSheet: Boolean = false,
    ) {
        _uiState.update { state ->
            val preview = CalculatorEngine.evaluatePreview(newExpression, state.angleMode)
            state.copy(
                expression = newExpression,
                lastExpressionBanner = lastBanner,
                previewResult = preview,
                errorType = null,
                isResultDisplayed = isResultDisplayed,
                isHistorySheetVisible = if (closeSheet) false else state.isHistorySheetVisible,
            )
        }
    }

    private fun CalculatorUiState.withUpdatedExpression(
        newExpression: String,
        isResultDisplayed: Boolean,
    ): CalculatorUiState {
        val preview = CalculatorEngine.evaluatePreview(newExpression, angleMode)
        return copy(
            expression = newExpression,
            lastExpressionBanner = if (!isResultDisplayed && this.isResultDisplayed) "" else lastExpressionBanner,
            previewResult = preview,
            errorType = null,
            isResultDisplayed = isResultDisplayed,
        )
    }

    private fun isBinaryOperator(c: Char): Boolean {
        return c == '+' || c == '−' || c == '-' || c == '×' || c == '*' || c == '÷' || c == '/' || c == '^'
    }
}
