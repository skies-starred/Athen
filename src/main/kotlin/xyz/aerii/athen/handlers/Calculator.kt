package xyz.aerii.athen.handlers

import com.mojang.brigadier.arguments.StringArgumentType
import xyz.aerii.athen.annotations.Load
import xyz.aerii.athen.events.CommandRegistration
import xyz.aerii.athen.events.core.on
import xyz.aerii.athen.handlers.Typo.modMessage
import xyz.aerii.athen.utils.formatted
import kotlin.math.pow

/**
 * Calc stands for calculator, I'm just using slang guys.
 */
@Load
object Calculator {
    private val tokenRegex = Regex("""\d+(\.\d+)?|[+\-*/x^()]""")
    private val priority = mapOf("+" to 1, "-" to 1, "*" to 2, "x" to 2, "/" to 2, "^" to 3)

    init {
        on<CommandRegistration> {
            event.register("calc") {
                thenCallback("operation", StringArgumentType.greedyString()) {
                    val str = StringArgumentType.getString(this, "operation")
                    if (str.isEmpty()) return@thenCallback "Empty operation!".modMessage(Typo.PrefixType.ERROR)

                    val result = calc(str).formatted()
                    "<gray>$str = <green>$result".parse().modMessage(Typo.PrefixType.SUCCESS)
                }
            }
        }
    }

    fun calc(str: String): Double {
        val str = str.tokenize()

        val out = mutableListOf<String>()
        val ops = mutableListOf<String>()

        for (s in str) {
            when {
                s.toDoubleOrNull() != null -> out += s

                s in priority -> {
                    while (ops.isNotEmpty() && ops.last() != "(") {
                        val t = priority[ops.last()] ?: break
                        val c = priority[s] ?: break

                        if (t > c || (t == c && s != "^")) out += ops.removeAt(ops.lastIndex)
                        else break
                    }

                    ops += s
                }

                s == "(" -> ops += s

                s == ")" -> {
                    while (ops.isNotEmpty() && ops.last() != "(") out.add(ops.removeAt(ops.lastIndex))

                    if (ops.isNotEmpty() && ops.last() == "(") ops.removeAt(ops.lastIndex)
                }
            }
        }

        while (ops.isNotEmpty()) out.add(ops.removeAt(ops.lastIndex))

        val stack = mutableListOf<Double>()
        for (o in out) {
            o.toDoubleOrNull()?.let {
                stack.add(it)
                continue
            }

            if (o in priority) {
                val b = stack.removeAt(stack.lastIndex)
                val a = stack.removeAt(stack.lastIndex)

                val res = when (o) {
                    "+" -> a + b
                    "-" -> a - b
                    "*", "x" -> a * b
                    "/" -> a / b
                    "^" -> a.pow(b)
                    else -> 0.0
                }

                stack += res
            }
        }

        return stack.first()
    }

    private fun String.tokenize(): List<String> {
        val str = tokenRegex.findAll(replace(" ", "")).map { it.value }.toMutableList()
        val tokens = mutableListOf<String>()

        for (i in str.indices) {
            val token = str[i]
            if (token == "") continue

            val unary = i == 0 || str[i - 1] in priority.keys || str[i - 1] == "("

            if (token == "+" && unary) continue

            if (token == "-" && unary) {
                val next = str.getOrNull(i + 1)
                if (next == "(") {
                    tokens += "0"
                    tokens += "-"
                    continue
                }

                tokens += "-${next ?: "0"}"
                str[i + 1] = ""
                continue
            }

            tokens += token
        }

        return tokens
    }
}