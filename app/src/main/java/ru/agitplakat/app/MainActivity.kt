package ru.agitplakat.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Paper = Color(0xFFF0E5CE)
private val Ink = Color(0xFF292B31)
private val Red = Color(0xFFAD342B)
private val Blue = Color(0xFF34465A)
private val Olive = Color(0xFF73765A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgitPlakatApp() }
    }
}

@Composable
fun AgitPlakatApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var plot by remember { mutableStateOf("Рабочая девушка на заводе ест пиццу") }
    var headline by remember { mutableStateOf("ТЫ СЕГОДНЯ ПИЦЦУ ЖРАЛ, А ЗАВТРА РОДИНУ ПРОДАЛ") }
    var style by remember { mutableStateOf("Винтажная живописная иллюстрация") }
    var composition by remember { mutableStateOf("Центральный персонаж") }
    var palette by remember { mutableStateOf("Приглушённая") }
    var artwork by remember { mutableStateOf<Bitmap?>(null) }
    var poster by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("Выберите иллюстрацию, чтобы проверить редактор и экспорт. Генератор нейросети ещё подключается.") }
    var modelInfo by remember { mutableStateOf<String?>(null) }
    val modelStore = remember { LocalModelStore(context) }
    var modelInstalled by remember { mutableStateOf(modelStore.installedModel() != null) }
    val nativeDiagnostic = remember { MnnNativeBridge.diagnostic() }

    val importModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { modelStore.importPack(uri) }
                modelInstalled = true
                modelInfo = "Компоненты: ${result.components.joinToString()}; ${result.bytes / (1024 * 1024)} МБ; SHA-256 ${result.sha256.take(16)}…"
                status = "Структура SD 1.5 проверена. Совместимость с MNN runtime ещё не подтверждена; генерация пока выключена."
            } catch (e: Exception) {
                status = "Не удалось импортировать модель: ${e.localizedMessage ?: "ошибка"}"
            }
        }
    }

    val openArtwork = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                    if (loaded == null) status = "Не удалось открыть изображение."
                    else {
                        artwork = loaded
                        poster = PosterRenderer.render(loaded, headline)
                        status = "Иллюстрация загружена. Текст наложен отдельно и сохранён точно."
                    }
                } catch (e: Exception) {
                    status = "Ошибка загрузки: ${e.localizedMessage ?: "неизвестная ошибка"}"
                }
            }
        }
    }

    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val current = poster
        if (uri != null && current != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { PosterExportStreams.png(current, it) }
                        ?: error("Не удалось создать PNG-файл")
                }
                status = "PNG сохранён: А3, 1403×1984 px, метаданные 120 DPI."
            } catch (e: Exception) { status = "Ошибка PNG: ${e.localizedMessage}" }
        }
    }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val current = poster
        if (uri != null && current != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { PosterExportStreams.pdf(current, it) }
                        ?: error("Не удалось создать PDF-файл")
                }
                status = "PDF сохранён: одна страница, вертикальный А3."
            } catch (e: Exception) { status = "Ошибка PDF: ${e.localizedMessage}" }
        }
    }

    LaunchedEffect(headline, artwork) {
        val source = artwork
        if (source != null) {
            poster = withContext(Dispatchers.Default) { PosterRenderer.render(source, headline) }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Red, secondary = Blue, background = Paper, surface = Paper)) {
        Column(
            Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text("★  АГИТПЛАКАТ", color = Red, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("МАСТЕРСКАЯ СОВРЕМЕННОГО РЕТРОПЛАКАТА", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4D5B9))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("НОВЫЙ ПЛАКАТ", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Сюжет изображения", color = Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(plot, { plot = it }, Modifier.fillMaxWidth(), minLines = 2)
                    Text("Точная надпись", color = Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(headline, { headline = it }, Modifier.fillMaxWidth(), minLines = 2)
                    Text("Художественный стиль", color = Ink, fontWeight = FontWeight.Bold)
                    ChoiceRow(listOf("Винтажная живописная иллюстрация", "Конструктивизм", "Советский пинап", "Индустриальный ретро"), style) { style = it }
                    Text("Тип композиции", color = Ink, fontWeight = FontWeight.Bold)
                    ChoiceRow(listOf("Центральный персонаж", "Сюжетная сцена", "Предметная композиция", "Пейзаж и атмосфера"), composition) { composition = it }
                    Text("Цветовая гамма", color = Ink, fontWeight = FontWeight.Bold)
                    ChoiceRow(listOf("Приглушённая", "Яркая", "Тёплая винтажная", "Холодная индустриальная"), palette) { palette = it }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4D5B9))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ЛОКАЛЬНАЯ МОДЕЛЬ", color = Red, fontWeight = FontWeight.Black)
                    Text(
                        if (modelInstalled) "Пакет SD 1.5 сохранён в памяти приложения; запуск генерации пока недоступен." else "Пакет модели ещё не импортирован.",
                        color = Ink, fontSize = 12.sp
                    )
                    Button(onClick = { importModel.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                        Text("ИМПОРТИРОВАТЬ ZIP-ПАКЕТ SD 1.5")
                    }
                    if (modelInfo != null) Text(modelInfo!!, color = Olive, fontSize = 10.sp)
                    Text("Диагностика JNI-моста (не проверка MNN): $nativeDiagnostic", color = Olive, fontSize = 10.sp)
                    Text("Ожидаются text_encoder.mnn, unet.mnn, vae_decoder.mnn и tokenizer.mtok. Проверка состава архива не подтверждает совместимость весов. JNI-мост пока не запускает MNN.", color = Ink, fontSize = 11.sp)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Blue)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ФОРМАТ ПЕЧАТИ", color = Paper, fontWeight = FontWeight.Black)
                    Text("А3 · вертикальный · 297 × 420 мм · 120 DPI", color = Paper, fontSize = 13.sp)
                    Text("PNG: 1403 × 1984 px  |  PDF: точная страница А3", color = Paper, fontSize = 12.sp)
                }
            }

            Button(onClick = { openArtwork.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Text("ВЫБРАТЬ ИЛЛЮСТРАЦИЮ ИЗ ТЕЛЕФОНА", fontWeight = FontWeight.Bold)
            }

            poster?.let { image ->
                Text("ПРЕДПРОСМОТР", color = Red, fontWeight = FontWeight.Black)
                Image(image.asImageBitmap(), contentDescription = "Предпросмотр плаката А3",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { savePng.launch("agitplakat-a3.png") }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("СОХРАНИТЬ PNG") }
                    Button(onClick = { savePdf.launch("agitplakat-a3.pdf") }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("СОХРАНИТЬ PDF") }
                }
            }

            Text(status, color = Olive, fontSize = 12.sp)
            Text("ГЕНЕРАЦИЯ", color = Red, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Стиль настроен на детализированную винтажную живопись, состаренную печать и выразительную композицию. Нейросеть должна создавать только иллюстрацию — без букв, цифр и псевдотекста. Настоящий локальный diffusion backend и совместимые веса пока не подключены.", color = Ink, fontSize = 12.sp)
            Text("Локальная коллекция • 3 интерьерных мокапа • автономная нейросеть — следующие этапы", color = Ink.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == option, onClick = { onSelect(option) }, colors = RadioButtonDefaults.colors(selectedColor = Red))
                Text(option, color = Ink, fontSize = 13.sp)
            }
        }
    }
}
