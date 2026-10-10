package com.example.whatsubb.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whatsubb.AppServices
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.audio.AudioRecorder
import com.example.whatsubb.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TRANSCRIBE_INSTRUCTION = """
Transcribeer dit gesprek letterlijk in het Nederlands.
- Geef sprekers aan met hun rol: "Begeleider:", "Bewoner:", of "Spreker 3:" enz. Gebruik NOOIT namen.
- Vervang alle namen van personen door [naam]. Vervang ook adressen door [adres], geboortedata en andere data door [datum],
  telefoonnummers door [telefoon] en andere herleidbare gegevens door [gegeven].
- Laat stopwoordjes zoals "eh" weg. Markeer onverstaanbare stukken als [onverstaanbaar].
- Geef alleen de transcriptie, zonder inleiding of samenvatting.
"""

private enum class RecState { IDLE, RECORDING, RECORDED, TRANSCRIBING }

/**
 * Opnemen van een gesprek + transcriptie via Gemini.
 * De opname blijft alleen op het toestel zolang dit scherm open is, tenzij de begeleider hem zelf opslaat.
 */
@Composable
fun RecordingSection(services: AppServices, onTranscript: (String) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val recorder = remember { AudioRecorder(context) }
    var state by remember { mutableStateOf(RecState.IDLE) }
    var file by remember { mutableStateOf<File?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var consent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun startRecording() {
        try {
            file = recorder.start()
            seconds = 0
            error = null
            state = RecState.RECORDING
        } catch (e: Exception) {
            error = "Opnemen lukt niet: ${e.message ?: "microfoon niet beschikbaar"}"
        }
    }

    fun discard() {
        file?.delete()
        file = null
        state = RecState.IDLE
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else error = "Zonder toestemming voor de microfoon kan er niet worden opgenomen."
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(AudioRecorder.MIME_TYPE)) { uri ->
        val source = file ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
            }.onSuccess { toast("Opname opgeslagen") }.onFailure { toast("Opslaan mislukt") }
        }
    }

    // Tijdens opnemen: tijd bijhouden en scherm aan laten.
    LaunchedEffect(state) {
        while (state == RecState.RECORDING) {
            delay(1_000)
            seconds++
        }
    }
    DisposableEffect(state) {
        view.keepScreenOn = state == RecState.RECORDING
        onDispose { view.keepScreenOn = false }
    }
    // Scherm verlaten = opname stoppen en wissen.
    DisposableEffect(Unit) {
        onDispose {
            recorder.stop()
            file?.delete()
        }
    }

    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎙️ Gesprek opnemen", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)

            when (state) {
                RecState.IDLE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consent, onCheckedChange = { consent = it })
                        Text(
                            "De bewoner (en andere aanwezigen) weten dat het gesprek wordt opgenomen en geven toestemming.",
                            fontSize = 13.sp, color = TextDark, lineHeight = 17.sp
                        )
                    }
                    Text(
                        "De opname gaat naar Google Gemini om uit te schrijven en wordt daarna daar verwijderd. " +
                            "Op dit toestel wordt hij gewist zodra je dit scherm verlaat, tenzij je hem opslaat.",
                        fontSize = 12.sp, color = TextDark.copy(alpha = 0.65f), lineHeight = 16.sp
                    )
                    Button(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        enabled = consent,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B))
                    ) { Text("●  Opname starten") }
                }

                RecState.RECORDING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(Color(0xFFC0392B), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Opname loopt  ${formatDuration(seconds)}", fontSize = 15.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Laat de app open staan tijdens het gesprek.", fontSize = 12.sp, color = TextDark.copy(alpha = 0.65f))
                    Button(
                        onClick = {
                            state = if (recorder.stop()) RecState.RECORDED else {
                                error = "De opname is mislukt of was te kort."
                                file?.delete(); file = null
                                RecState.IDLE
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) { Text("■  Stoppen") }
                }

                RecState.RECORDED, RecState.TRANSCRIBING -> {
                    val sizeMb = (file?.length() ?: 0L) / 1_048_576.0
                    Text(
                        "Opname klaar: ${formatDuration(seconds)}  (${"%.1f".format(sizeMb)} MB, AAC)",
                        fontSize = 14.sp, color = TextDark
                    )
                    if (state == RecState.TRANSCRIBING) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SagePrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Bezig met uitschrijven... dit kan even duren.", fontSize = 13.sp, color = TextDark)
                        }
                    } else {
                        Button(
                            onClick = {
                                val audio = file ?: return@Button
                                state = RecState.TRANSCRIBING
                                error = null
                                scope.launch {
                                    try {
                                        val text = services.aiClient().processFile(audio, AudioRecorder.MIME_TYPE, TRANSCRIBE_INSTRUCTION)
                                        onTranscript(text)
                                        toast("Transcriptie toegevoegd aan de aantekeningen")
                                    } catch (e: AiException) {
                                        error = e.message
                                    } finally {
                                        state = RecState.RECORDED
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Uitschrijven met Gemini") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { saveLauncher.launch("gesprek_${System.currentTimeMillis() / 1000}.aac") },
                                shape = CircleShape
                            ) { Text("Opslaan") }
                            OutlinedButton(onClick = ::discard, shape = CircleShape) { Text("Verwijderen") }
                        }
                    }
                }
            }

            error?.let {
                Text(it, fontSize = 13.sp, color = Color(0xFFC0392B))
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
