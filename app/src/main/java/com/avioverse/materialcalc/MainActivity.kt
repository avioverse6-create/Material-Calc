package com.avioverse.materialcalc

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
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
    val saveHistory: Boolean = true,
    val buttonSize: Int = 60,
    val buttonShape: String = "expressive"
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
            saveHistory = prefs.getBoolean("saveHistory", true),
            buttonSize = prefs.getInt("buttonSize", 60).coerceIn(48, 76),
            buttonShape = prefs.getString("buttonShape", "expressive") ?: "expressive"
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
            .putBoolean("saveHistory", next.saveHistory)
            .putInt("buttonSize", next.buttonSize)
            .putString("buttonShape", next.buttonShape).apply()
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionTitle("Внешний вид")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Тема приложения", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
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
                SettingsSectionTitle("Кнопки калькулятора")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Размер кнопок", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Text("${settings.buttonSize} dp", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = settings.buttonSize.toFloat(), onValueChange = {
                            onSettingsChange(settings.copy(buttonSize = it.toInt().coerceIn(48, 76)))
                        }, valueRange = 48f..76f, steps = 27)
                        Text("Форма кнопок", style = MaterialTheme.typography.titleMedium)
                        val shapes = listOf(
                            "expressive" to "Expressive",
                            "squircle" to "Squircle",
                            "rounded" to "Скруглённая",
                            "circle" to "Круглая",
                            "pill" to "Капсула"
                        )
                        shapes.chunked(2).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                line.forEach { (value, label) ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = settings.buttonShape == value,
                                        onClick = { onSettingsChange(settings.copy(buttonShape = value)) },
                                        label = { Text(label, maxLines = 1) }
                                    )
                                }
                                if (line.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        Text("Предпросмотр", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("1", "2", "3", "+").forEachIndexed { index, label ->
                                val shape = calculatorButtonShape(settings.buttonShape, index < 3, false)
                                Box(
                                    Modifier.weight(1f).height(settings.buttonSize.dp).clip(shape)
                                        .background(if (index == 3) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (index == 3) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Text("В режиме Expressive цифры круглые, а функциональные клавиши — мягкие squircle. При нажатии форма плавно меняется.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                SettingsSectionTitle("Управление")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingsSwitch("Виброотклик", "Тактильная отдача при нажатии", settings.haptics) { onSettingsChange(settings.copy(haptics = it)) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch("Звук нажатий", "Системный звук клавиш", settings.sound) { onSettingsChange(settings.copy(sound = it)) }
                    }
                }
            }
            item {
                SettingsSectionTitle("Вычисления")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingsSwitch("Сохранять историю вычислений", "Показывать последние операции", settings.saveHistory) {
                            onSettingsChange(settings.copy(saveHistory = it))
                        }
                    }
                }
            }
            item {
                Text("Material Calc 3.0 • Material 3 Expressive", Modifier.fillMaxWidth().padding(top = 10.dp),
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

private fun calculatorButtonShape(mode: String, isDigit: Boolean, pressed: Boolean): RoundedCornerShape {
    val percent = when (mode) {
        "circle" -> 50
        "pill" -> 50
        "rounded" -> if (pressed) 34 else 24
        "squircle" -> if (pressed) 24 else 38
        else -> if (isDigit) { if (pressed) 30 else 50 } else { if (pressed) 24 else 38 }
    }
    return RoundedCornerShape(percent = percent)
}

@Composable
private fun CalculatorKey(label: String, settings: CalcSettings, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "keyScale")
    val corner by animateFloatAsState(
        when (settings.buttonShape) {
            "circle", "pill" -> 50f
            "rounded" -> if (pressed) 34f else 24f
            "squircle" -> if (pressed) 24f else 38f
            else -> if (label.length == 1 && label[0].isDigit()) { if (pressed) 30f else 50f } else { if (pressed) 24f else 38f }
        }, label = "keyShape"
    )
    val operator = label in listOf("÷", "×", "−", "+", "=")
    val utility = label in listOf("AC", "(", ")", "±")
    val bg = when {
        label == "=" -> MaterialTheme.colorScheme.primary
        operator -> MaterialTheme.colorScheme.secondaryContainer
        utility -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when {
        label == "=" -> MaterialTheme.colorScheme.onPrimary
        operator -> MaterialTheme.colorScheme.onSecondaryContainer
        utility -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val animatedBg by animateColorAsState(bg, label = "keyColor")
    Button(
        onClick = onClick,
        modifier = modifier.scale(scale).height(settings.buttonSize.dp),
        interactionSource = interaction,
        shape = RoundedCornerShape(percent = corner.toInt()),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = fg)
    ) {
        Text(label, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    fun refreshPreview() {
        result = runCatching { formatNumber(ExpressionEvaluator.evaluate(expression), settings) }.getOrDefault("")
    }
    fun press(key: String) {
        tapFeedback()
        when (key) {
            "AC" -> { expression = "0"; result = "0" }
            "⌫" -> {
                expression = if (expression.length <= 1) "0" else expression.dropLast(1)
                refreshPreview()
            }
            "±" -> {
                val start = expression.indexOfLast { it in "+−×÷(" } + 1
                val tail = expression.substring(start)
                expression = if (tail.startsWith("−")) expression.substring(0, start) + tail.drop(1)
                    else expression.substring(0, start) + "−" + tail
                refreshPreview()
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
                refreshPreview()
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        val horizontal = if (maxWidth < 360.dp) 12.dp else 16.dp
        val gap = if (compact) 7.dp else 9.dp
        Column(Modifier.fillMaxSize().padding(horizontal = horizontal, vertical = if (compact) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Material Calc", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Калькулятор", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showHistory = !showHistory }) { Icon(Icons.Rounded.History, contentDescription = "История") }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Настройки") }
            }
            Card(Modifier.fillMaxWidth().weight(1f).heightIn(min = 110.dp), shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxSize().padding(if (compact) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                    if (showHistory) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("История", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { history.clear() }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text("Очистить")
                            }
                        }
                        if (history.isEmpty()) Text("Здесь появятся вычисления", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        history.take(3).forEach { (e, r) ->
                            Text("$e = $r", Modifier.fillMaxWidth().clickable { expression = e; result = r; showHistory = false }.padding(vertical = 3.dp),
                                textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(5.dp))
                    }
                    Text(expression, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("= ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        AnimatedContent(result, label = "result") {
                            Text(if (it.isEmpty()) " " else it, style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { press("⌫") }) { Icon(Icons.Rounded.Backspace, contentDescription = "Удалить последний символ") }
                        FilledTonalIconButton(onClick = { clipboard.setText(AnnotatedString(result)); tapFeedback() }, enabled = result.isNotEmpty()) {
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { key ->
                        CalculatorKey(key, settings, Modifier.weight(1f)) { press(key) }
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
