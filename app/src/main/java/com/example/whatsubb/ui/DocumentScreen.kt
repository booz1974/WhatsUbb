package com.example.whatsubb.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.ai.Turn
import com.example.whatsubb.data.DocumentType
import com.example.whatsubb.data.Role
import com.example.whatsubb.privacy.Anonymizer
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun DocumentScreen(services: AppServices, type: DocumentType, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val values = remember(type.id) { mutableStateListOf(*Array(type.fields.size) { "" }) }
    var namesToHide by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var scrubCount by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val text = result ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { PdfExporter.write(context, it, type.title, text) }
            }.onSuccess {
                Toast.makeText(context, "PDF opgeslagen", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "PDF maken mislukt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generate(revision: String? = null) {
        if (values.all { it.isBlank() }) {
            error = "Vul minimaal één veld in."
            return
        }
        val names = Anonymizer.parseNames(namesToHide)
        val input = type.fields.indices
            .filter { values[it].isNotBlank() }
            .joinToString("\n\n") { "${type.fields[it].label}:\n${values[it].trim()}" }
        val scrubbedInput = Anonymizer.scrub(input, names)
        val scrubbedRevision = revision?.let { Anonymizer.scrub(it, names) }
        scrubCount = scrubbedInput.replacements + (scrubbedRevision?.replacements ?: 0)

        val turns = buildList {
            add(Turn(true, "Opdracht: ${type.title}\n\n${type.instructions}\n\nGegevens van de begeleider:\n\n${scrubbedInput.text}"))
            if (scrubbedRevision != null && result != null) {
                add(Turn(false, result!!))
                add(Turn(true, "Pas het document aan: ${scrubbedRevision.text}"))
            }
        }

        isLoading = true
        error = null
        scope.launch {
            try {
                result = services.aiClient().generate(services.knowledgeBase.systemPrompt(Role.BEGELEIDER), turns)
                feedback = ""
            } catch (e: AiException) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        ScreenHeader(title = "${type.emoji} ${type.title}", actionLabel = "Terug", onAction = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(type.description, fontSize = 14.sp, color = TextDark.copy(alpha = 0.75f))
            Spacer(Modifier.height(12.dp))

            Surface(color = SagePrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("🔒 Anoniem werken", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SagePrimary)
                    Text(
                        "Schrijf 'de bewoner' in plaats van een naam. Staat er toch een naam in je aantekeningen? " +
                            "Vul die hieronder in, dan wordt hij vervangen door [naam] voordat er iets naar de AI gaat. " +
                            "Telefoonnummers, data, adressen en BSN worden automatisch weggefilterd.",
                        fontSize = 13.sp,
                        color = TextDark.copy(alpha = 0.8f),
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = namesToHide,
                        onValueChange = { namesToHide = it },
                        label = { Text("Namen om te verbergen (komma-gescheiden)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            type.transcriptFieldIndex?.let { target ->
                RecordingSection(services) { transcript ->
                    values[target] = listOf(values[target].trim(), transcript.trim())
                        .filter { it.isNotEmpty() }
                        .joinToString("\n\n")
                }
                Spacer(Modifier.height(8.dp))
            }

            type.fields.forEachIndexed { index, field ->
                OutlinedTextField(
                    value = values[index],
                    onValueChange = { values[index] = it },
                    label = { Text(field.label) },
                    placeholder = { Text(field.hint) },
                    singleLine = !field.multiline,
                    minLines = if (field.multiline) 3 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { generate() },
                enabled = !isLoading,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SagePrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (result == null) "Maak ${type.title.lowercase()}" else "Opnieuw maken", fontSize = 16.sp)
            }

            PrivacyNotice(scrubCount)

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                TypingBubble()
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Surface(color = Color(0xFFFFF1E6), shape = RoundedCornerShape(12.dp)) {
                    Text(it, modifier = Modifier.padding(12.dp), fontSize = 14.sp, color = TextDark)
                }
            }

            result?.let { text ->
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        SelectionContainer {
                            Text(markdownToAnnotated(text), fontSize = 15.sp, lineHeight = 21.sp, color = TextDark)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Controleer altijd de inhoud en vul [AANVULLEN]-plekken zelf in.",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                        Button(
                            onClick = { pdfLauncher.launch(pdfFileName(type.title)) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("📄  Download als PDF") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedButton(onClick = { copyToClipboard(context, text) }, shape = CircleShape) { Text("Kopiëren") }
                            OutlinedButton(onClick = { shareText(context, type.title, text) }, shape = CircleShape) { Text("Delen") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Iets aanpassen? Bijv. 'korter' of 'meer nadruk op dagritme'") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { generate(revision = feedback) },
                    enabled = feedback.isNotBlank() && !isLoading
                ) { Text("Aanpassen", color = SagePrimary) }
            }
        }
    }
}

private fun pdfFileName(title: String): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    return "${title.replace(' ', '_')}_$date.pdf"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Whats Ubb", text))
    Toast.makeText(context, "Gekopieerd", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, title: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Delen via"))
}
