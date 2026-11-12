package com.avioverse.materialcalc

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialCalcApp() }
    }
}

private data class CalcSettings(
    val theme: String = "system",
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val precision: Int = 8,
    val groupDigits: Boolean = false,
    val saveHistory: Boolean = true
)

@Composable
private fun MaterialCalcApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("material_calc_settings", 0) }
    var settings by remember {
        mutableStateOf(CalcSettings(
            theme = prefs.getString("theme", "system") ?: "system",
            dynamicColor = prefs.getBoolean("dynamicColor", true),
            haptics = prefs.getBoolean("haptics", true),
            sound = prefs.getBoolean("sound", false),
            precision = prefs.getInt("precision", 8).coerceIn(2, 12),
            groupDigits = prefs.getBoolean("groupDigits", false),
            saveHistory = prefs.getBoolean("saveHistory", true)
        ))
    }
    fun updateSettings(next: CalcSettings) {
        settings = next
        prefs.edit().putString("theme", next.theme)
            .putBoolean("dynamicColor", next.dynamicColor)
            .putBoolean("haptics", next.haptics)
            .putBoolean("sound", next.sound)
            .putInt("precision", next.precision)
            .putBoolean("groupDigits", next.groupDigits)
            .putBoolean("saveHistory", next.saveHistory).apply()
    }
    val dark = when (settings.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val colors = when {
        settings.dynamicColor && android.os.Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        settings.dynamicColor && android.os.Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var page by remember { mutableStateOf(false) }
            if (page) SettingsScreen(settings, ::updateSettings) { page = false }
            else CalculatorScreen(settings) { page = true }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(settings: CalcSettings, onSettingsChange: (CalcSettings) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Настройки", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } })
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionTitle("Внешний вид")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Тема приложения", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная").forEach { (value, label) ->
                                FilterChip(selected = settings.theme == value, onClick = { onSettingsChange(settings.copy(theme = value)) }, label = { Text(label) })
                            }
                        }
                        SettingsSwitch("Динамические цвета Material You", "Подстраивать палитру под обои Android", settings.dynamicColor) {
                            onSettingsChange(settings.copy(dynamicColor = it))
                        }
                    }
                }
            }
            item {
                SettingsSectionTitle("Управление")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        SettingsSwitch("Виброотклик", "Тактильная отдача при нажатии", settings.haptics) { onSettingsChange(settings.copy(haptics = it)) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch("Звук нажатий", "Системный звук клавиш", settings.sound) { onSettingsChange(settings.copy(sound = it)) }
                    }
                }
            }
            item {
                SettingsSectionTitle("Вычисления")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Точность результата", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text("${settings.precision} знаков", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = settings.precision.toFloat(), onValueChange = {
                            onSettingsChange(settings.copy(precision = it.toInt().coerceIn(2, 12)))
                        }, valueRange = 2f..12f, steps = 9)
                        SettingsSwitch("Разделять большие числа", "Например: 1 000 000", settings.groupDigits) {
                            onSettingsChange(settings.copy(groupDigits = it))
                        }
                    }
                }
            }
            item {
                SettingsSectionTitle("История")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        SettingsSwitch("Сохранять историю вычислений", "Показывать последние операции", settings.saveHistory) {
                            onSettingsChange(settings.copy(saveHistory = it))
                        }
                    }
                }
            }
            item {
                Text("Material Calc 2.0 • Material 3 Expressive", Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CalculatorScreen(settings: CalcSettings, onSettings: () -> Unit) {
    var expression by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf("0") }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Pair<String, String>>() }
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    fun tapFeedback() {
        if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (settings.sound) view.playSoundEffect(SoundEffectConstants.CLICK)
    }
    fun press(key: String) {
        tapFeedback()
        when (key) {
            "AC" -> { expression = "0"; result = "0" }
            "⌫" -> expression = if (expression.length <= 1) "0" else expression.dropLast(1)
            "±" -> {
                val start = expression.indexOfLast { it in "+−×÷(" } + 1
                val tail = expression.substring(start)
                expression = if (tail.startsWith("−")) expression.substring(0, start) + tail.drop(1)
                    else expression.substring(0, start) + "−" + tail
            }
            "=" -> runCatching { ExpressionEvaluator.evaluate(expression) }.onSuccess {
                result = formatNumber(it, settings)
                if (settings.saveHistory) {
                    history.add(0, expression to result)
                    if (history.size > 30) history.removeAt(history.lastIndex)
                }
                expression = result
            }.onFailure { result = "Ошибка" }
            else -> {
                expression = if (expression == "0" && key !in listOf(".", "+", "−", "×", "÷", "%", ")")) key else expression + key
                runCatching { result = formatNumber(ExpressionEvaluator.evaluate(expression), settings) }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Material Calc", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Калькулятор", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showHistory = !showHistory }) { Icon(Icons.Rounded.History, contentDescription = "История") }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Настройки") }
        }
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                if (showHistory) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("История", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { history.clear() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Очистить")
                        }
                    }
                    if (history.isEmpty()) Text("Здесь появятся вычисления", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    history.take(5).forEach { (e, r) ->
                        Text("$e = $r", Modifier.fillMaxWidth().clickable { expression = e; result = r; showHistory = false }.padding(vertical = 5.dp),
                            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(expression, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("= ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    AnimatedContent(result, label = "result") {
                        Text(it, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 1)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { press("⌫") }) { Icon(Icons.Rounded.Backspace, contentDescription = "Удалить последний символ") }
                    FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(result)); tapFeedback() }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Скопировать результат")
                    }
                }
            }
        }
        val rows = listOf(
            listOf("AC", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { key ->
                    val operator = key in listOf("÷", "×", "−", "+", "=")
                    val utility = key in listOf("AC", "(", ")", "±")
                    val bg = when {
                        key == "=" -> MaterialTheme.colorScheme.primary
                        operator -> MaterialTheme.colorScheme.secondaryContainer
                        utility -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val fg = when {
                        key == "=" -> MaterialTheme.colorScheme.onPrimary
                        operator -> MaterialTheme.colorScheme.onSecondaryContainer
                        utility -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Button(onClick = { press(key) }, modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(if (key == "=") 24.dp else 21.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg)) {
                        Text(key, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatNumber(value: Double, settings: CalcSettings): String {
    if (!value.isFinite()) return "Ошибка"
    val raw = if (abs(value - value.toLong()) < 1e-10) value.toLong().toString()
        else String.format(Locale.US, "%.${settings.precision}f", value).trimEnd('0').trimEnd('.')
    if (!settings.groupDigits || raw.contains('E') || raw.contains('e')) return raw
    val parts = raw.split(".")
    val grouped = parts[0].reversed().chunked(3).joinToString(" ").reversed()
    return if (parts.size > 1) grouped + "." + parts[1] else grouped
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
                val op = s[i++]; val rhs = term()
                v = if (op == '+') v + rhs else v - rhs
            }
            return v
        }
        private fun term(): Double {
            var v = factor()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]; val rhs = factor()
                v = if (op == '*') v * rhs else v / rhs
            }
            return v
        }
        private fun factor(): Double {
            if (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val negative = s[i++] == '-'; val v = factor()
                return if (negative) -v else v
            }
            var v = if (i < s.length && s[i] == '(') {
                i++; val inside = expression()
                require(i < s.length && s[i] == ')') { "Missing )" }; i++; inside
            } else number()
            if (i < s.length && s[i] == '%') { i++; v /= 100.0 }
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
