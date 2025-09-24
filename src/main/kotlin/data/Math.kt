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
        val coeffStr = if (absCoeff == 1 && exponent != 0) "" else absCoeff.toString()

        val variableStr = when (exponent) {
            0 -> if (absCoeff == 1 && !isFirstTerm) "1" else ""
            1 -> "x"
            else -> "x^$exponent"
        }

        if (exponent == 0 && absCoeff != 1) return "$sign$absCoeff"
        if (exponent == 0 && isFirstTerm) return "$sign$absCoeff"


        return "$sign$coeffStr$variableStr"
    }
}

// TODO: improve math problems
fun generateIntegralProblem(): Pair<String, List<String>> {
    val exponents = (2..7).shuffled().toMutableSet()
    exponents.add(1)

    val integratedTerms = exponents.map { exponent ->
        Term(
            coefficient = Random.nextInt(-7, 8).let { if (it == 0) 1 else it },
            exponent = exponent
        )
    }.sortedByDescending { it.exponent }

    val problemTerms = integratedTerms.map {
        Term(
            coefficient = it.coefficient * it.exponent,
            exponent = it.exponent - 1
        )
    }

    val problemString = "∫ (${problemTerms.formatToString()}) dx"
    val correctAnswer = "${integratedTerms.formatToString()} + C"

    val incorrectAnswers = mutableSetOf<String>()

    val derivativeAsAnswer = problemTerms.map { Term(it.coefficient, it.exponent) }
    incorrectAnswers.add("${derivativeAsAnswer.formatToString()} + C")

    if (integratedTerms.size > 1) {
        val partialErrorTerms = integratedTerms.toMutableList()
        val termToFlipIndex = Random.nextInt(1, partialErrorTerms.size)
        val originalTerm = partialErrorTerms[termToFlipIndex]
        partialErrorTerms[termToFlipIndex] = originalTerm.copy(coefficient = -originalTerm.coefficient)
        incorrectAnswers.add("${partialErrorTerms.formatToString()} + C")
    }

    val coefficientErrorTerms = problemTerms.map {
        Term(coefficient = it.coefficient, exponent = it.exponent + 1)
    }
    incorrectAnswers.add("${coefficientErrorTerms.formatToString()} + C")

    val divisionErrorTerms = problemTerms.mapNotNull {
        if (it.exponent == 0) return@mapNotNull Term(it.coefficient, 1)
        if (it.coefficient % it.exponent != 0) return@mapNotNull null
        Term(coefficient = it.coefficient / it.exponent, exponent = it.exponent + 1)
    }

    if (divisionErrorTerms.size == problemTerms.size) {
        incorrectAnswers.add("${divisionErrorTerms.formatToString()} + C")
    }

    val finalAnswers = (listOf(correctAnswer) + incorrectAnswers.shuffled().take(3)).shuffled()
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