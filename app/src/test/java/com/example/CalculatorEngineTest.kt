package com.example

import com.example.domain.AngleMode
import com.example.domain.CalculatorEngine
import com.example.domain.EvaluationErrorType
import com.example.domain.EvaluationResult
import com.example.ui.CalculatorAction
import com.example.ui.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun basicArithmeticAndPrecedence() {
        val result = CalculatorEngine.evaluate("2+3×4−6÷2", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("11", "11"), result)
    }

    @Test
    fun exactDecimalPrecision() {
        val result = CalculatorEngine.evaluate("0.1+0.2", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("0.3", "0.3"), result)
    }

    @Test
    fun parenthesesAndImplicitMultiplication() {
        val result = CalculatorEngine.evaluate("2(3+4)−(1+2)(2)", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("8", "8"), result)
    }

    @Test
    fun percentageEvaluation() {
        val result1 = CalculatorEngine.evaluate("200×15%", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("30", "30"), result1)

        val result2 = CalculatorEngine.evaluate("50%", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("0.5", "0.5"), result2)
    }

    @Test
    fun scientificFunctionsDegAndRad() {
        val sin30 = CalculatorEngine.evaluate("sin(30)", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("0.5", "0.5"), sin30)

        val cos90 = CalculatorEngine.evaluate("cos(90)", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("0", "0"), cos90)

        val sinPiHalf = CalculatorEngine.evaluate("sin(π÷2)", AngleMode.RAD)
        assertEquals(EvaluationResult.Success("1", "1"), sinPiHalf)

        val log100 = CalculatorEngine.evaluate("log(100)+√(16)", AngleMode.DEG)
        assertEquals(EvaluationResult.Success("6", "6"), log100)
    }

    @Test
    fun factorialAndPowers() {
        val fact = CalculatorEngine.evaluate("5!+2^3^2", AngleMode.DEG)
        // 5! = 120, 2^(3^2) = 2^9 = 512 -> 632
        assertEquals(EvaluationResult.Success("632", "632"), fact)
    }

    @Test
    fun divisionByZeroAndDomainErrors() {
        val divZero = CalculatorEngine.evaluate("9÷0", AngleMode.DEG)
        assertEquals(EvaluationResult.Error(EvaluationErrorType.DIVIDE_BY_ZERO), divZero)

        val sqrtNeg = CalculatorEngine.evaluate("√(−4)", AngleMode.DEG)
        assertEquals(EvaluationResult.Error(EvaluationErrorType.DOMAIN_ERROR), sqrtNeg)

        val tan90 = CalculatorEngine.evaluate("tan(90)", AngleMode.DEG)
        assertEquals(EvaluationResult.Error(EvaluationErrorType.DOMAIN_ERROR), tan90)
    }

    @Test
    fun livePreviewBehavior() {
        assertNull(CalculatorEngine.evaluatePreview("123", AngleMode.DEG))
        assertEquals("14", CalculatorEngine.evaluatePreview("2×(3+4", AngleMode.DEG))
    }

    @Test
    fun viewModelWorkflowAndHistory() {
        val vm = CalculatorViewModel()
        vm.onAction(CalculatorAction.AppendDigit('1'))
        vm.onAction(CalculatorAction.AppendDigit('2'))
        vm.onAction(CalculatorAction.AppendOperator('+'))
        vm.onAction(CalculatorAction.AppendDigit('8'))
        assertEquals("20", vm.uiState.value.previewResult)

        vm.onAction(CalculatorAction.Evaluate)
        assertEquals("20", vm.uiState.value.expression)
        assertTrue(vm.uiState.value.isResultDisplayed)
        assertEquals(1, vm.uiState.value.history.size)
        assertEquals("12+8", vm.uiState.value.history.first().expression)

        // Pressing operator after result continues from 20
        vm.onAction(CalculatorAction.AppendOperator('×'))
        vm.onAction(CalculatorAction.AppendDigit('3'))
        vm.onAction(CalculatorAction.Evaluate)
        assertEquals("60", vm.uiState.value.expression)
        assertEquals(2, vm.uiState.value.history.size)

        // Pressing digit after result starts fresh
        vm.onAction(CalculatorAction.AppendDigit('7'))
        assertEquals("7", vm.uiState.value.expression)
    }
}
