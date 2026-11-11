package foo.starred.athen.api.math

import foo.starred.athen.annotations.impl.LoadableAthenModule
import foo.starred.athen.api.messaging.impl.MessagingAPI.mod
import foo.starred.athen.modules.impl.ModSettings
import foo.starred.athen.utils.AthenCommandSource
import foo.starred.kontext.api.format.extension.number.format
import java.math.BigDecimal
import kotlin.math.pow

@LoadableAthenModule
object Calculator {
    private val tokenRegex = Regex("""\d+(\.\d+)?[kKmMbBtT]?|[+\-*/x^()]""")
    private val priority = mapOf("+" to 1, "-" to 1, "*" to 2, "x" to 2, "/" to 2, "^" to 3)

    init {
        AthenCommandSource.command("calc") {
            "calc" / greedyString("operation") {
                render(string("operation")).mod()
            }
        }

        run {
            if (!ModSettings.calculator) return@run

            AthenCommandSource.command("calc") {
                greedyString("operation") {
                    render(string("operation")).mod()
                }
            }
        }
    }

    fun render(operation: String): String {
        return "<gray>$operation = <green>${compute(operation).format(true)}"
    }

    fun compute(expression: String): Double {
        val output = mutableListOf<String>()
        val operators = mutableListOf<String>()

        for (token in expression.tokenize()) {
            when {
                token.toDoubleOrNull() != null -> {
                    output += token
                }

                token in priority -> {
                    val current = priority.getValue(token)

                    while (operators.isNotEmpty() && operators.last() != "(") {
                        val top = priority[operators.last()] ?: break
                        if (top < current) break
                        if (top == current && token == "^") break

                        output += operators.pop()
                    }

                    operators += token
                }

                token == "(" -> {
                    operators += token
                }

                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") output += operators.pop()
                    if (operators.isNotEmpty()) operators.pop()
                }
            }
        }

        output.addAll(operators.asReversed())

        val stack = mutableListOf<Double>()
        for (token in output) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack += number
                continue
            }

            if (token !in priority) continue

            val right = stack.pop()
            val left = stack.pop()

            stack += when (token) {
                "+" -> left + right
                "-" -> left - right
                "*", "x" -> left * right
                "/" -> left / right
                "^" -> left.pow(right)
                else -> 0.0
            }
        }

        return stack.first()
    }

    private fun String.tokenize(): List<String> {
        val parts = tokenRegex.findAll(replace(" ", "")).map { expand(it.value) }.toMutableList()
        val tokens = mutableListOf<String>()

        for (index in parts.indices) {
            val token = parts[index]
            if (token.isEmpty()) continue

            val unary = when (val previous = parts.getOrNull(index - 1)) {
                null, "(" -> true
                else -> previous in priority
            }

            if (unary && token == "+") {
                continue
            }

            if (unary && token == "-") {
                val next = parts.getOrNull(index + 1)
                if (next == "(") {
                    tokens += "0"
                    tokens += "-"
                    continue
                }

                tokens += "-${next ?: "0"}"
                parts[index + 1] = ""
                continue
            }

            tokens += token
        }

        return tokens
    }

    private fun <T> MutableList<T>.pop(): T {
        return removeAt(lastIndex)
    }

    private fun expand(string: String): String {
        val multiplier = when (string.last().lowercaseChar()) {
            'k' -> 1_000L
            'm' -> 1_000_000L
            'b' -> 1_000_000_000L
            't' -> 1_000_000_000_000L
            else -> return string
        }

        return BigDecimal(string.dropLast(1)).multiply(BigDecimal(multiplier)).toPlainString()
    }
}
