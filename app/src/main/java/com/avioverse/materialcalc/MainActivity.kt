package com.avioverse.materialcalc

import android.os.Bundle
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
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
    val buttonShape: String = "expressive",
    val fontFamily: String = "sans",
    val fontSize: Int = 16,
    val fontWeight: Int = 600
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
            buttonShape = prefs.getString("buttonShape", "expressive") ?: "expressive",
            fontFamily = prefs.getString("fontFamily", "sans") ?: "sans",
            fontSize = prefs.getInt("fontSize", 16).coerceIn(13, 22),
            fontWeight = prefs.getInt("fontWeight", 600).coerceIn(400, 800)
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
            .putString("buttonShape", next.buttonShape)
            .putString("fontFamily", next.fontFamily)
            .putInt("fontSize", next.fontSize)
            .putInt("fontWeight", next.fontWeight).apply()
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
    MaterialTheme(colorScheme = colors, typography = calculatorTypography(settings)) {
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
                            "expressive" to "Expressive", "circle" to "Круг",
                            "squircle" to "Squircle", "pill" to "Капсула",
                            "soft" to "Мягкая", "wave" to "Wave",
                            "leaf" to "Лепесток", "cut" to "Срез"
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
                        Text("Формы вдохновлены выразительной геометрией Material 3. При касании клавиши мягко сжимаются и меняют силуэт.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                SettingsSectionTitle("Типографика")
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Семейство шрифта", style = MaterialTheme.typography.titleMedium)
                        val fontChoices = listOf("sans" to "Sans", "serif" to "Serif", "mono" to "Mono", "cursive" to "Cursive")
                        fontChoices.chunked(2).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                line.forEach { (value, label) ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = settings.fontFamily == value,
                                        onClick = { onSettingsChange(settings.copy(fontFamily = value)) },
                                        label = { Text(label, maxLines = 1, fontFamily = calculatorFontFamily(value)) }
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Размер текста", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text("${settings.fontSize} sp", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = settings.fontSize.toFloat(), onValueChange = {
                            onSettingsChange(settings.copy(fontSize = it.toInt().coerceIn(13, 22)))
                        }, valueRange = 13f..22f, steps = 8)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Жирность", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text("${settings.fontWeight}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(value = settings.fontWeight.toFloat(), onValueChange = {
                            onSettingsChange(settings.copy(fontWeight = (it.toInt() / 100 * 100).coerceIn(400, 800)))
                        }, valueRange = 400f..800f, steps = 3)
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Предпросмотр", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Material Calc", fontFamily = calculatorFontFamily(settings.fontFamily),
                                    fontSize = (settings.fontSize * 1.55f).sp, fontWeight = FontWeight(settings.fontWeight))
                                Text("Красивый текст. 123 + 456 = 579",
                                    fontFamily = calculatorFontFamily(settings.fontFamily),
                                    fontSize = settings.fontSize.sp, fontWeight = FontWeight(settings.fontWeight),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
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

private fun calculatorFontFamily(value: String): FontFamily = when (value) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

private fun calculatorTypography(settings: CalcSettings): Typography {
    val base = Typography()
    val family = calculatorFontFamily(settings.fontFamily)
    val weight = FontWeight(settings.fontWeight)
    val size = settings.fontSize.sp
    return base.copy(
        displayMedium = base.displayMedium.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 2.15f).sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 1.65f).sp),
        titleLarge = base.titleLarge.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 1.25f).sp),
        titleMedium = base.titleMedium.copy(fontFamily = family, fontWeight = weight),
        titleSmall = base.titleSmall.copy(fontFamily = family, fontWeight = weight),
        bodyLarge = base.bodyLarge.copy(fontFamily = family, fontWeight = weight, fontSize = size),
        bodyMedium = base.bodyMedium.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 0.9f).sp),
        bodySmall = base.bodySmall.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 0.8f).sp),
        labelLarge = base.labelLarge.copy(fontFamily = family, fontWeight = weight, fontSize = (size.value * 0.9f).sp),
        labelMedium = base.labelMedium.copy(fontFamily = family, fontWeight = weight),
        labelSmall = base.labelSmall.copy(fontFamily = family, fontWeight = weight)
    )
}

private fun calculatorButtonShape(mode: String, isDigit: Boolean, pressed: Boolean): Shape {
    return when (mode) {
        "cut" -> CutCornerShape(percent = if (pressed) 8 else 18)
        "wave" -> GenericShape { size, _ ->
            val w = size.width; val h = size.height
            moveTo(w * 0.18f, 0f)
            cubicTo(w * 0.36f, h * 0.12f, w * 0.64f, h * 0.12f, w * 0.82f, 0f)
            cubicTo(w * 1.04f, h * 0.20f, w * 1.04f, h * 0.80f, w * 0.82f, h)
            cubicTo(w * 0.64f, h * 0.88f, w * 0.36f, h * 0.88f, w * 0.18f, h)
            cubicTo(-w * 0.04f, h * 0.80f, -w * 0.04f, h * 0.20f, w * 0.18f, 0f)
            close()
        }
        "leaf" -> GenericShape { size, _ ->
            moveTo(size.width * 0.5f, 0f)
            cubicTo(size.width * 0.92f, size.height * 0.08f, size.width, size.height * 0.56f, size.width * 0.5f, size.height)
            cubicTo(0f, size.height * 0.56f, size.width * 0.08f, size.height * 0.08f, size.width * 0.5f, 0f)
            close()
        }
        "circle", "pill" -> RoundedCornerShape(percent = 50)
        "soft" -> RoundedCornerShape(percent = if (pressed) 38 else 28)
        "squircle" -> RoundedCornerShape(percent = if (pressed) 26 else 40)
        else -> RoundedCornerShape(percent = if (isDigit) { if (pressed) 34 else 50 } else { if (pressed) 25 else 38 })
    }
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
        Text(label, fontFamily = calculatorFontFamily(settings.fontFamily), fontSize = (settings.fontSize * 1.3f).sp, fontWeight = FontWeight(settings.fontWeight), maxLines = 1)
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
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_SYSTEM, 55) }
    DisposableEffect(toneGenerator) { onDispose { toneGenerator.release() } }
    fun tapFeedback() {
        if (settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (settings.sound) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 28)
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
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        val compact = maxHeight < 700.dp
        val horizontal = if (maxWidth < 360.dp) 12.dp else 16.dp
        val gap = if (compact) 6.dp else 8.dp
        val fittedButtonSize = ((maxHeight - 390.dp) / 5).coerceIn(46.dp, settings.buttonSize.dp).value.toInt()
        val fittedSettings = settings.copy(buttonSize = fittedButtonSize)
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
                val historyInteraction = remember { MutableInteractionSource() }
                val historyPressed by historyInteraction.collectIsPressedAsState()
                val historyScale by animateFloatAsState(if (historyPressed) 0.86f else 1f, label = "historyButtonScale")
                IconButton(onClick = { showHistory = !showHistory }, modifier = Modifier.scale(historyScale), interactionSource = historyInteraction) { Icon(Icons.Rounded.History, contentDescription = "История") }
                val settingsInteraction = remember { MutableInteractionSource() }
                val settingsPressed by settingsInteraction.collectIsPressedAsState()
                val settingsScale by animateFloatAsState(if (settingsPressed) 0.86f else 1f, label = "settingsButtonScale")
                IconButton(onClick = onSettings, modifier = Modifier.scale(settingsScale), interactionSource = settingsInteraction) { Icon(Icons.Rounded.Settings, contentDescription = "Настройки") }
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
                        CalculatorKey(key, fittedSettings, Modifier.weight(1f)) { press(key) }
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
