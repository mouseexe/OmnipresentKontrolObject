package gay.spiders.data

import kotlin.math.sqrt

fun getPrimeMatrix(isHorizontal: Boolean = true): Array<IntArray> {
    val primesInRange = (100..1000).filter { isPrime(it) }
    val primes = primesInRange.shuffled().take(6)

    val rows: Int
    val cols: Int
    if (isHorizontal) {
        rows = 2
        cols = 3
    } else {
        rows = 3
        cols = 2
    }

    val matrix = Array(rows) { IntArray(cols) }
    for (i in 0 until rows) {
        for (j in 0 until cols) {
            matrix[i][j] = primes[i * cols + j]
        }
    }
    return matrix
}

fun isPrime(n: Int): Boolean {
    if (n <= 1) return false
    if (n % 2 == 0) return n == 2
    for (i in 3..sqrt(n.toDouble()).toInt() step 2) {
        if (n % i == 0) return false
    }
    return true
}

fun matrixMultiply(a: Array<IntArray>, b: Array<IntArray>): Array<IntArray> {
    val rowsA = a.size
    val colsA = a[0].size
    val rowsB = b.size
    val colsB = b[0].size

    if (colsA != rowsB) {
        throw IllegalArgumentException("Matrix dimensions are not compatible for multiplication: (${rowsA}x${colsA}) * (${rowsB}x${colsB})")
    }

    val result = Array(rowsA) { IntArray(colsB) }

    for (i in 0 until rowsA) {
        for (j in 0 until colsB) {
            for (k in 0 until colsA) {
                result[i][j] += a[i][k] * b[k][j]
            }
        }
    }

    return result
}

fun sumMatrix(matrix: Array<IntArray>): Int {
    return matrix.sumOf { it.sum() }
}

fun prettyPrintMatrices(a: Array<IntArray>, b: Array<IntArray>? = null): String {
    val allNumbers = a.flatMap { it.asIterable() } + (b?.flatMap { it.asIterable() } ?: emptyList())
    val maxWidth = allNumbers.maxOrNull()?.toString()?.length ?: 0

    val linesA = getPaddedLines(a, maxWidth)
    val widthA = linesA.firstOrNull()?.length ?: 0

    if (b == null) {
        return linesA.joinToString("\n")
    }

    val linesB = getPaddedLines(b, maxWidth)

    val numRows = maxOf(a.size, b.size)
    val middleRow = (numRows - 1) / 2

    val sb = StringBuilder()
    for (i in 0 until numRows) {
        val lineA = linesA.getOrNull(i) ?: "".padEnd(widthA)
        val lineB = linesB.getOrNull(i) ?: ""
        val separator = if (i == middleRow) "  x  " else "     "

        sb.append(lineA).append(separator).append(lineB).append("\n")
    }
    return sb.toString()
}

private fun getPaddedLines(matrix: Array<IntArray>, maxWidth: Int): List<String> {
    if (matrix.isEmpty() || matrix[0].isEmpty()) return emptyList()
    return matrix.map { row ->
        val content = row.joinToString(", ") { it.toString().padStart(maxWidth, ' ') }
        "[ $content ]"
    }
}

fun getProblem(): Pair<String, Int> {
    val matrixA = getPrimeMatrix(false)
    val matrixB = getPrimeMatrix(true)
    return Pair(prettyPrintMatrices(matrixA, matrixB), sumMatrix(matrixMultiply(matrixA, matrixB)))
}