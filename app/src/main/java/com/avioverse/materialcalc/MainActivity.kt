package com.avioverse.materialcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialCalcApp() }
    }
}

@Composable
private fun MaterialCalcApp() {
    var dark by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = when {
        android.os.Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        android.os.Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CalculatorScreen(dark = dark, onDarkChange = { dark = it })
        }
    }
}

@Composable
private fun CalculatorScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    var expression by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf("0") }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    val clipboard = LocalClipboardManager.current

    fun press(key: String) {
        when (key) {
            "AC" -> { expression = "0"; result = "0" }
            "⌫" -> expression = if (expression.length <= 1) "0" else expression.dropLast(1)
            "=" -> {
                runCatching { ExpressionEvaluator.evaluate(expression) }.onSuccess {
                    result = formatNumber(it)
                    history.add(0, expression to result)
                    if (history.size > 20) history.removeAt(history.lastIndex)
                    expression = result
                }.onFailure { result = "Ошибка" }
            }
            else -> {
                expression = if (expression == "0" && key !in listOf(".", "+", "−", "×", "÷", "%", ")")) key
                else expression + key
                runCatching { result = formatNumber(ExpressionEvaluator.evaluate(expression)) }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Material Calc", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Калькулятор", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.IconButton(onClick = { showHistory = !showHistory }) {
                Icon(Icons.Rounded.History, contentDescription = "История")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                if (showHistory) {
                    Text("История", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    if (history.isEmpty()) Text("Здесь появятся вычисления", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    history.take(6).forEach { (e, r) ->
                        Text("$e = $r", modifier = Modifier.fillMaxWidth().clickable { expression = e; result = r }.padding(vertical = 5.dp), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(expression, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("= ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    AnimatedContent(result, label = "result") {
                        Text(it, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.IconButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(result)) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Скопировать результат")
                }
            }
        }

        val rows = listOf(
            listOf("AC", "( )", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val display = if (key == "( )") "(" else key
                    val isOperator = key in listOf("÷", "×", "−", "+", "=")
                    val isUtility = key in listOf("AC", "%", "( )", "±")
                    Button(
                        onClick = {
                            when (key) {
                                "( )" -> press(if (expression.count { it == '(' } > expression.count { it == ')' }) ")" else "(")
                                "±" -> press("−")
                                else -> press(display)
                            }
                        },
                        modifier = Modifier.weight(1f).height(62.dp),
                        shape = RoundedCornerShape(if (key == "=") 22.dp else 20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                key == "=" -> MaterialTheme.colorScheme.primary
                                isOperator -> MaterialTheme.colorScheme.secondaryContainer
                                isUtility -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = when {
                                key == "=" -> MaterialTheme.colorScheme.onPrimary
                                isOperator -> MaterialTheme.colorScheme.onSecondaryContainer
                                isUtility -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        if (key == "⌫") Icon(Icons.Rounded.Backspace, contentDescription = "Удалить")
                        else Text(key, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text("Тёмная тема", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.size(8.dp))
            Switch(checked = dark, onCheckedChange = onDarkChange)
        }
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "Ошибка"
    val rounded = if (kotlin.math.abs(value - value.toLong()) < 1e-10) value.toLong().toString()
    else "%.8f".format(value).trimEnd('0').trimEnd('.')
    return rounded
}

private object ExpressionEvaluator {
    fun evaluate(input: String): Double {
        val normalized = input.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        return Parser(normalized).parse()
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun parse(): Double {
            val value = expression()
            require(i == s.length) { "Unexpected input" }
            return value
        }
        private fun expression(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val rhs = term()
                v = if (op == '+') v + rhs else v - rhs
            }
            return v
        }
        private fun term(): Double {
            var v = factor()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]
                val rhs = factor()
                v = if (op == '*') v * rhs else v / rhs
            }
            return v
        }
        private fun factor(): Double {
            if (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val negative = s[i++] == '-'
                val v = factor()
                return if (negative) -v else v
            }
            var v = if (i < s.length && s[i] == '(') {
                i++
                val inside = expression()
                require(i < s.length && s[i] == ')') { "Missing )" }
                i++
                inside
            } else number()
            if (i < s.length && s[i] == '%') {
                i++
                v /= 100.0
            }
            return v
        }
        private fun number(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(i > start) { "Number expected" }
            return s.substring(start, i).toDouble()
        }
    }
}
