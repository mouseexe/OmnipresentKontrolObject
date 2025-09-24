package gay.spiders.data

import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import kotlin.random.Random

private data class Term(val coefficient: Int, val exponent: Int) {
    fun format(isFirstTerm: Boolean = false): String {
        val sign = when {
            isFirstTerm && coefficient < 0 -> "-"
            isFirstTerm -> ""
            coefficient < 0 -> " - "
            else -> " + "
        }

        val absCoeff = kotlin.math.abs(coefficient)

        if (exponent == 0) {
            return "$sign$absCoeff"
        }

        val coeffStr = if (absCoeff == 1) "" else absCoeff.toString()

        val variableStr = when (exponent) {
            1 -> "x"
            else -> "x^$exponent"
        }

        return "$sign$coeffStr$variableStr"
    }
}

fun generateIntegralProblem(): Pair<String, List<String>> {
    val positiveExponents = (2..8).shuffled().take(Random.nextInt(1, 3))
    val negativeExponents = (-3..-1).shuffled().take(Random.nextInt(1, 3))
    val exponents = (positiveExponents + negativeExponents).shuffled().toMutableSet()

    if (exponents.isEmpty()) {
        exponents.add(2)
    }

    if (Random.nextBoolean()) {
        exponents.add(1)
    }

    val integratedTerms = exponents.map { exponent ->
        Term(
            coefficient = Random.nextInt(-15, 16).let { if (it == 0) 1 else it },
            exponent = exponent
        )
    }.sortedByDescending { it.exponent }

    val problemTerms = integratedTerms.mapNotNull {
        if (it.exponent == 0) return@mapNotNull null
        Term(
            coefficient = it.coefficient * it.exponent,
            exponent = it.exponent - 1
        )
    }

    val problemString = "∫ (${problemTerms.formatToString()}) dx"
    val correctAnswer = "${integratedTerms.formatToString()} + C"

    val incorrectAnswerGenerators = mutableListOf<() -> String?>()

    incorrectAnswerGenerators.add {
        val twiceIntegratedTerms = integratedTerms.mapNotNull { term ->
            val newExponent = term.exponent + 1
            if (newExponent == 0) return@mapNotNull null
            Term(coefficient = term.coefficient, exponent = newExponent)
        }
        if (twiceIntegratedTerms.size == integratedTerms.size) "${twiceIntegratedTerms.formatToString()} + C" else null
    }

    incorrectAnswerGenerators.add {
        val derivativeTerms = problemTerms.mapNotNull { term ->
            if (term.exponent == 0) return@mapNotNull null
            Term(coefficient = term.coefficient * term.exponent, exponent = term.exponent - 1)
        }.filter { it.coefficient != 0 }
        if (derivativeTerms.isNotEmpty()) "${derivativeTerms.formatToString()} + C" else null
    }

    if (integratedTerms.size > 1) {
        incorrectAnswerGenerators.add {
            val coefficients = integratedTerms.map { it.coefficient }.shuffled()
            val exponents = integratedTerms.map { it.exponent }.shuffled()
            val shuffledTerms = coefficients.zip(exponents)
                .map { (coeff, exp) -> Term(coeff, exp) }
                .sortedByDescending { it.exponent }
            "${shuffledTerms.formatToString()} + C"
        }
    }

    incorrectAnswerGenerators.add {
        val numTerms = integratedTerms.size
        val randomExponents = ((-3..-1) + (1..8)).shuffled().take(numTerms).toSet()
        if (randomExponents.isEmpty()) return@add null
        val randomTerms = randomExponents.map { exp ->
            Term(coefficient = Random.nextInt(-15, 16).let { if (it == 0) 1 else it }, exponent = exp)
        }.sortedByDescending { it.exponent }
        "${randomTerms.formatToString()} + C"
    }

    val incorrectAnswers = mutableSetOf<String>()
    incorrectAnswerGenerators.shuffled().forEach { generator ->
        if (incorrectAnswers.size < 4) {
            generator()?.let { incorrectAnswer ->
                if (incorrectAnswer != correctAnswer) {
                    incorrectAnswers.add(incorrectAnswer)
                }
            }
        }
    }

    while (incorrectAnswers.size < 4 && incorrectAnswerGenerators.isNotEmpty()) {
        val generator = incorrectAnswerGenerators.random()
        generator()?.let { incorrectAnswer ->
            if (incorrectAnswer != correctAnswer) {
                incorrectAnswers.add(incorrectAnswer)
            }
        }
    }

    val finalAnswers = (listOf(correctAnswer) + incorrectAnswers.toList()).shuffled()
    val correctFirstList = finalAnswers.sortedBy { it != correctAnswer }

    return problemString to correctFirstList
}

private fun List<Term>.formatToString(): String {
    if (this.isEmpty()) return "0"
    return this.mapIndexed { index, term -> term.format(isFirstTerm = index == 0) }.joinToString("")
}

fun MessageBuilder.mathAnswers(answers: List<String>) {
    actionRow {
        val shuffledAnswers = answers.mapIndexed { index, answer -> answer to (index == 0) }.shuffled()

        stringSelect("mine_answer_select") {
            shuffledAnswers.forEachIndexed { index, (answerText, isCorrect) ->
                option(answerText, if (isCorrect) "correct" else "incorrect$index")
            }
        }
    }
}
