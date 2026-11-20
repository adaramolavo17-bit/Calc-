package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleMode {
    DEG,
    RAD,
}

enum class EvaluationErrorType {
    DIVIDE_BY_ZERO,
    INVALID_EXPRESSION,
    DOMAIN_ERROR,
    OVERFLOW,
}

sealed interface EvaluationResult {
    data class Success(
        val formatted: String,
        val rawValue: String,
    ) : EvaluationResult

    data class Error(
        val type: EvaluationErrorType,
    ) : EvaluationResult
}

/**
 * Pure recursive-descent mathematical expression evaluator.
 * Combines exact BigDecimal arithmetic for basic operations (+, -, ×, ÷, %)
 * with high-precision Double evaluation for transcendental scientific functions.
 */
object CalculatorEngine {

    private val MATH_CONTEXT = MathContext(16, RoundingMode.HALF_UP)
    private const val EPSILON = 1e-12
    private const val MAX_FACTORIAL_INPUT = 170

    private sealed interface Token {
        data class Number(val text: String) : Token
        data class Operator(val symbol: Char) : Token
        data class Function(val name: String) : Token
        data class Constant(val symbol: Char) : Token
        data object LeftParen : Token
        data object RightParen : Token
        data object Percent : Token
        data object Factorial : Token
    }

    private sealed class CalcValue {
        abstract fun toDouble(): Double
        abstract fun toBigDecimalOrNull(): BigDecimal?

        data class Exact(val value: BigDecimal) : CalcValue() {
            override fun toDouble(): Double = value.toDouble()
            override fun toBigDecimalOrNull(): BigDecimal = value
        }

        data class Approx(val value: Double) : CalcValue() {
            override fun toDouble(): Double = value
            override fun toBigDecimalOrNull(): BigDecimal? = null
        }
    }

    private class CalcException(val errorType: EvaluationErrorType) : RuntimeException()

    fun evaluate(expression: String, angleMode: AngleMode): EvaluationResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return EvaluationResult.Error(EvaluationErrorType.INVALID_EXPRESSION)
        }
        return try {
            val tokens = tokenize(trimmed)
            if (tokens.isEmpty()) {
                return EvaluationResult.Error(EvaluationErrorType.INVALID_EXPRESSION)
            }
            val normalizedTokens = insertImplicitMultiplication(tokens)
            val parser = Parser(normalizedTokens, angleMode)
            val result = parser.parseExpression()
            if (parser.hasRemaining()) {
                return EvaluationResult.Error(EvaluationErrorType.INVALID_EXPRESSION)
            }
            val raw = formatRaw(result)
            val formatted = formatForDisplay(result)
            EvaluationResult.Success(formatted = formatted, rawValue = raw)
        } catch (e: CalcException) {
            EvaluationResult.Error(e.errorType)
        } catch (_: ArithmeticException) {
            EvaluationResult.Error(EvaluationErrorType.OVERFLOW)
        } catch (_: NumberFormatException) {
            EvaluationResult.Error(EvaluationErrorType.INVALID_EXPRESSION)
        }
    }

    fun evaluatePreview(expression: String, angleMode: AngleMode): String? {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return null
        // Don't show preview if expression is just a single plain number without operators/functions
        val hasOperatorOrFunc = trimmed.any {
            it in charArrayOf('+', '-', '−', '×', '*', '÷', '/', '^', '%', '!', '(', 'π', 'e')
        } || trimmed.any { it.isLetter() }
        if (!hasOperatorOrFunc) return null

        // Auto-close unclosed parentheses for live preview
        val openParens = trimmed.count { it == '(' }
        val closeParens = trimmed.count { it == ')' }
        val balanced = if (openParens > closeParens) {
            trimmed + ")".repeat(openParens - closeParens)
        } else {
            trimmed
        }

        return when (val result = evaluate(balanced, angleMode)) {
            is EvaluationResult.Success -> result.formatted
            is EvaluationResult.Error -> null
        }
    }

    private fun tokenize(input: String): List<Token> {
        val clean = input.replace(",", "")
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    var dotSeen = false
                    while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                        if (clean[i] == '.') {
                            if (dotSeen) throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                            dotSeen = true
                        }
                        i++
                    }
                    // Handle scientific notation like 1.5E-4 or 1.5E+4
                    if (i < clean.length && clean[i] == 'E') {
                        val nextIdx = i + 1
                        val hasSign = nextIdx < clean.length && (clean[nextIdx] == '+' || clean[nextIdx] == '-')
                        val digitStart = if (hasSign) nextIdx + 1 else nextIdx
                        if (digitStart < clean.length && clean[digitStart].isDigit()) {
                            i = digitStart
                            while (i < clean.length && clean[i].isDigit()) {
                                i++
                            }
                        }
                    }
                    val numStr = clean.substring(start, i)
                    if (numStr == ".") throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                    tokens.add(Token.Number(numStr))
                }
                c == '+' -> {
                    tokens.add(Token.Operator('+'))
                    i++
                }
                c == '-' || c == '−' -> {
                    tokens.add(Token.Operator('-'))
                    i++
                }
                c == '*' || c == '×' -> {
                    tokens.add(Token.Operator('*'))
                    i++
                }
                c == '/' || c == '÷' -> {
                    tokens.add(Token.Operator('/'))
                    i++
                }
                c == '^' -> {
                    tokens.add(Token.Operator('^'))
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.LeftParen)
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.RightParen)
                    i++
                }
                c == '%' -> {
                    tokens.add(Token.Percent)
                    i++
                }
                c == '!' -> {
                    tokens.add(Token.Factorial)
                    i++
                }
                c == 'π' -> {
                    tokens.add(Token.Constant('π'))
                    i++
                }
                c == '√' -> {
                    tokens.add(Token.Function("sqrt"))
                    i++
                }
                c.isLetter() -> {
                    val start = i
                    while (i < clean.length && clean[i].isLetter()) {
                        i++
                    }
                    val word = clean.substring(start, i).lowercase(Locale.US)
                    when (word) {
                        "e" -> tokens.add(Token.Constant('e'))
                        "pi" -> tokens.add(Token.Constant('π'))
                        "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt", "abs" -> {
                            tokens.add(Token.Function(word))
                        }
                        else -> throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                    }
                }
                else -> throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
            }
        }
        return tokens
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) return tokens
        val result = ArrayList<Token>(tokens.size + 4)
        for (idx in tokens.indices) {
            val current = tokens[idx]
            if (idx > 0) {
                val prev = tokens[idx - 1]
                val prevEndsValue = prev is Token.Number ||
                    prev is Token.RightParen ||
                    prev is Token.Constant ||
                    prev is Token.Percent ||
                    prev is Token.Factorial

                val currStartsValue = current is Token.Number ||
                    current is Token.LeftParen ||
                    current is Token.Constant ||
                    current is Token.Function

                if (prevEndsValue && currStartsValue) {
                    result.add(Token.Operator('*'))
                }
            }
            result.add(current)
        }
        return result
    }

    private class Parser(
        private val tokens: List<Token>,
        private val angleMode: AngleMode,
    ) {
        private var pos = 0

        fun hasRemaining(): Boolean = pos < tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        private fun consume(): Token = tokens[pos++]

        fun parseExpression(): CalcValue {
            var left = parseTerm()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.symbol == '+' || next.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (next.symbol == '+') add(left, right) else subtract(left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseTerm(): CalcValue {
            var left = parsePower()
            while (true) {
                val next = peek()
                if (next is Token.Operator && (next.symbol == '*' || next.symbol == '/')) {
                    consume()
                    val right = parsePower()
                    left = if (next.symbol == '*') multiply(left, right) else divide(left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parsePower(): CalcValue {
            val base = parseUnary()
            val next = peek()
            if (next is Token.Operator && next.symbol == '^') {
                consume()
                // Right-associative: 2^3^2 = 2^(3^2)
                val exponent = parsePower()
                return power(base, exponent)
            }
            return base
        }

        private fun parseUnary(): CalcValue {
            val next = peek()
            if (next is Token.Operator && (next.symbol == '+' || next.symbol == '-')) {
                consume()
                val operand = parseUnary()
                return if (next.symbol == '-') negate(operand) else operand
            }
            return parsePostfix()
        }

        private fun parsePostfix(): CalcValue {
            var value = parsePrimary()
            while (true) {
                when (peek()) {
                    is Token.Percent -> {
                        consume()
                        value = divide(value, CalcValue.Exact(BigDecimal(100)))
                    }
                    is Token.Factorial -> {
                        consume()
                        value = factorial(value)
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parsePrimary(): CalcValue {
            val token = peek() ?: throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
            return when (token) {
                is Token.Number -> {
                    consume()
                    val bd = BigDecimal(token.text)
                    CalcValue.Exact(bd)
                }
                is Token.Constant -> {
                    consume()
                    when (token.symbol) {
                        'π' -> CalcValue.Approx(PI)
                        'e' -> CalcValue.Approx(E)
                        else -> throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                    }
                }
                is Token.LeftParen -> {
                    consume()
                    val inner = parseExpression()
                    val closing = peek()
                    if (closing !is Token.RightParen) {
                        throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                    }
                    consume()
                    inner
                }
                is Token.Function -> {
                    consume()
                    val arg = if (peek() is Token.LeftParen) {
                        consume()
                        val inner = parseExpression()
                        if (peek() !is Token.RightParen) {
                            throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
                        }
                        consume()
                        inner
                    } else {
                        parseUnary()
                    }
                    applyFunction(token.name, arg)
                }
                else -> throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
            }
        }

        private fun applyFunction(name: String, arg: CalcValue): CalcValue {
            val x = arg.toDouble()
            if (x.isNaN() || x.isInfinite()) {
                throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
            }
            val result = when (name) {
                "sin" -> {
                    val rad = if (angleMode == AngleMode.DEG) Math.toRadians(x) else x
                    snapZero(sin(rad))
                }
                "cos" -> {
                    val rad = if (angleMode == AngleMode.DEG) Math.toRadians(x) else x
                    snapZero(cos(rad))
                }
                "tan" -> {
                    if (angleMode == AngleMode.DEG) {
                        val mod = ((x % 180.0) + 180.0) % 180.0
                        if (abs(mod - 90.0) < 1e-9) {
                            throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                        }
                        snapZero(tan(Math.toRadians(x)))
                    } else {
                        val cosVal = cos(x)
                        if (abs(cosVal) < 1e-12) {
                            throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                        }
                        snapZero(tan(x))
                    }
                }
                "asin" -> {
                    if (x < -1.0 || x > 1.0) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                    val rad = asin(x)
                    if (angleMode == AngleMode.DEG) Math.toDegrees(rad) else rad
                }
                "acos" -> {
                    if (x < -1.0 || x > 1.0) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                    val rad = acos(x)
                    if (angleMode == AngleMode.DEG) Math.toDegrees(rad) else rad
                }
                "atan" -> {
                    val rad = atan(x)
                    if (angleMode == AngleMode.DEG) Math.toDegrees(rad) else rad
                }
                "ln" -> {
                    if (x <= 0.0) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                    ln(x)
                }
                "log" -> {
                    if (x <= 0.0) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                    log10(x)
                }
                "sqrt" -> {
                    if (x < 0.0) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
                    val bd = arg.toBigDecimalOrNull()
                    if (bd != null) {
                        val root = sqrt(x)
                        val rootBd = BigDecimal.valueOf(root)
                        if (rootBd.multiply(rootBd).compareTo(bd) == 0) {
                            return CalcValue.Exact(rootBd)
                        }
                    }
                    sqrt(x)
                }
                "abs" -> {
                    val bd = arg.toBigDecimalOrNull()
                    if (bd != null) return CalcValue.Exact(bd.abs())
                    abs(x)
                }
                else -> throw CalcException(EvaluationErrorType.INVALID_EXPRESSION)
            }
            if (result.isNaN()) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
            if (result.isInfinite()) throw CalcException(EvaluationErrorType.OVERFLOW)
            return CalcValue.Approx(result)
        }

        private fun add(a: CalcValue, b: CalcValue): CalcValue {
            val bdA = a.toBigDecimalOrNull()
            val bdB = b.toBigDecimalOrNull()
            return if (bdA != null && bdB != null) {
                CalcValue.Exact(bdA.add(bdB, MATH_CONTEXT))
            } else {
                checkedApprox(a.toDouble() + b.toDouble())
            }
        }

        private fun subtract(a: CalcValue, b: CalcValue): CalcValue {
            val bdA = a.toBigDecimalOrNull()
            val bdB = b.toBigDecimalOrNull()
            return if (bdA != null && bdB != null) {
                CalcValue.Exact(bdA.subtract(bdB, MATH_CONTEXT))
            } else {
                checkedApprox(a.toDouble() - b.toDouble())
            }
        }

        private fun multiply(a: CalcValue, b: CalcValue): CalcValue {
            val bdA = a.toBigDecimalOrNull()
            val bdB = b.toBigDecimalOrNull()
            return if (bdA != null && bdB != null) {
                CalcValue.Exact(bdA.multiply(bdB, MATH_CONTEXT))
            } else {
                checkedApprox(a.toDouble() * b.toDouble())
            }
        }

        private fun divide(a: CalcValue, b: CalcValue): CalcValue {
            val bdA = a.toBigDecimalOrNull()
            val bdB = b.toBigDecimalOrNull()
            if (bdB != null) {
                if (bdB.compareTo(BigDecimal.ZERO) == 0) {
                    throw CalcException(EvaluationErrorType.DIVIDE_BY_ZERO)
                }
                if (bdA != null) {
                    return CalcValue.Exact(bdA.divide(bdB, MATH_CONTEXT))
                }
            }
            val dB = b.toDouble()
            if (dB == 0.0) throw CalcException(EvaluationErrorType.DIVIDE_BY_ZERO)
            return checkedApprox(a.toDouble() / dB)
        }

        private fun negate(a: CalcValue): CalcValue {
            val bd = a.toBigDecimalOrNull()
            return if (bd != null) {
                CalcValue.Exact(bd.negate())
            } else {
                CalcValue.Approx(-a.toDouble())
            }
        }

        private fun power(base: CalcValue, exp: CalcValue): CalcValue {
            val bdBase = base.toBigDecimalOrNull()
            val bdExp = exp.toBigDecimalOrNull()
            if (bdBase != null && bdExp != null) {
                try {
                    val intExp = bdExp.intValueExact()
                    if (intExp in -9999..9999) {
                        if (bdBase.compareTo(BigDecimal.ZERO) == 0 && intExp < 0) {
                            throw CalcException(EvaluationErrorType.DIVIDE_BY_ZERO)
                        }
                        return CalcValue.Exact(bdBase.pow(intExp, MATH_CONTEXT))
                    }
                } catch (_: ArithmeticException) {
                    // Fractional or huge exponent falls through to Double pow
                }
            }
            val dBase = base.toDouble()
            val dExp = exp.toDouble()
            if (dBase == 0.0 && dExp < 0.0) {
                throw CalcException(EvaluationErrorType.DIVIDE_BY_ZERO)
            }
            if (dBase < 0.0 && dExp % 1.0 != 0.0) {
                throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
            }
            return checkedApprox(dBase.pow(dExp))
        }

        private fun factorial(a: CalcValue): CalcValue {
            val d = a.toDouble()
            if (d < 0.0 || d % 1.0 != 0.0) {
                throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
            }
            if (d > MAX_FACTORIAL_INPUT) {
                throw CalcException(EvaluationErrorType.OVERFLOW)
            }
            val n = d.toInt()
            if (n <= 20) {
                var res = BigDecimal.ONE
                for (k in 2..n) {
                    res = res.multiply(BigDecimal.valueOf(k.toLong()))
                }
                return CalcValue.Exact(res)
            }
            var res = 1.0
            for (k in 2..n) {
                res *= k
            }
            return checkedApprox(res)
        }

        private fun checkedApprox(value: Double): CalcValue {
            if (value.isNaN()) throw CalcException(EvaluationErrorType.DOMAIN_ERROR)
            if (value.isInfinite()) throw CalcException(EvaluationErrorType.OVERFLOW)
            return CalcValue.Approx(value)
        }

        private fun snapZero(value: Double): Double {
            if (abs(value) < EPSILON) return 0.0
            val rounded = kotlin.math.round(value)
            if (abs(value - rounded) < EPSILON) return rounded
            return value
        }
    }

    private fun toNormalizedBigDecimal(value: CalcValue): BigDecimal {
        return when (value) {
            is CalcValue.Exact -> {
                if (value.value.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
                else value.value.stripTrailingZeros()
            }
            is CalcValue.Approx -> {
                val d = value.value
                if (d == 0.0) return BigDecimal.ZERO
                // Round to 12 significant digits to eliminate IEEE-754 representation artifacts
                val rounded = BigDecimal.valueOf(d).round(MathContext(12, RoundingMode.HALF_UP))
                if (rounded.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
                else rounded.stripTrailingZeros()
            }
        }
    }

    private fun formatRaw(value: CalcValue): String {
        val bd = toNormalizedBigDecimal(value)
        val absBd = bd.abs()
        if (bd.compareTo(BigDecimal.ZERO) != 0 &&
            (absBd >= BigDecimal("1e12") || absBd < BigDecimal("1e-9"))
        ) {
            val sciFormat = DecimalFormat("0.##########E0", DecimalFormatSymbols(Locale.US))
            return sciFormat.format(bd)
        }
        return bd.toPlainString()
    }

    private fun formatForDisplay(value: CalcValue): String {
        val bd = toNormalizedBigDecimal(value)
        if (bd.compareTo(BigDecimal.ZERO) == 0) return "0"
        val absBd = bd.abs()
        if (absBd >= BigDecimal("1e12") || absBd < BigDecimal("1e-9")) {
            val sciFormat = DecimalFormat("0.##########E0", DecimalFormatSymbols(Locale.US))
            return sciFormat.format(bd)
        }
        val plain = bd.toPlainString()
        val parts = plain.split('.')
        val intPart = parts[0]
        val isNegative = intPart.startsWith("-")
        val digits = if (isNegative) intPart.substring(1) else intPart
        val grouped = buildString {
            for (i in digits.indices) {
                if (i > 0 && (digits.length - i) % 3 == 0) append(',')
                append(digits[i])
            }
        }
        val prefix = if (isNegative) "-" else ""
        return if (parts.size > 1) {
            "$prefix$grouped.${parts[1]}"
        } else {
            "$prefix$grouped"
        }
    }
}
