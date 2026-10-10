package com.example.whatsubb.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.data.KnowledgeDoc
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.DocumentImporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Beheer van geüploade documenten met regels, afspraken en werkwijze. */
@Composable
fun DocumentsSection(services: AppServices) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val documents = services.knowledgeBase.documents
    var docs by remember { mutableStateOf(documents.list()) }
    var importing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<KnowledgeDoc?>(null) }
    var preview by remember { mutableStateOf<KnowledgeDoc?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        error = null
        scope.launch {
            try {
                val imported = DocumentImporter.import(context, uri, services.aiClient())
                documents.add(imported.name, imported.text)
                docs = documents.list()
                Toast.makeText(context, "\"${imported.name}\" toegevoegd", Toast.LENGTH_SHORT).show()
            } catch (e: AiException) {
                error = e.message
            } catch (e: Exception) {
                error = "Het document kon niet worden gelezen."
            } finally {
                importing = false
            }
        }
    }

    Text(
        "Voeg documenten toe met regels, afspraken of werkwijze (PDF, Word .docx of tekst). " +
            "De AI gebruikt ze bij elke vraag en elk document. Zet er geen gegevens van bewoners in.",
        fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
    )

    if (docs.isEmpty()) {
        Text("Nog geen documenten toegevoegd.", fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
    }

    docs.forEach { doc ->
        Surface(color = SagePrimary.copy(alpha = 0.07f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📄 ${doc.name}", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatDate(doc.addedAt)}  ·  ${"%,d".format(Locale("nl", "NL"), doc.chars)} tekens",
                        fontSize = 12.sp, color = TextDark.copy(alpha = 0.6f))
                }
                TextButton(onClick = { preview = doc }) { Text("Bekijk", color = SagePrimary) }
                TextButton(onClick = { toDelete = doc }) { Text("Wis", color = Color(0xFFC0392B)) }
            }
        }
    }

    val total = docs.sumOf { it.chars }
    if (total > 300_000) {
        Text(
            "Let op: veel tekst (${total / 1000}k tekens). Elke vraag wordt dan trager en duurder.",
            fontSize = 12.sp, color = Color(0xFFC0392B)
        )
    }

    if (importing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SagePrimary)
            Spacer(Modifier.width(10.dp))
            Text("Document wordt gelezen... (PDF's kunnen even duren)", fontSize = 13.sp, color = TextDark)
        }
    } else {
        Button(
            onClick = { picker.launch(DocumentImporter.supportedMimeTypes) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
        ) { Text("＋  Document toevoegen") }
    }

    error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFC0392B)) }

    toDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Document verwijderen?") },
            text = { Text("\"${doc.name}\" wordt verwijderd uit de kennis van de app.") },
            confirmButton = {
                TextButton(onClick = {
                    documents.delete(doc.id)
                    docs = documents.list()
                    toDelete = null
                }) { Text("Verwijderen", color = Color(0xFFC0392B)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuleren") } }
        )
    }

    preview?.let { doc ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(doc.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Box(Modifier.heightIn(max = 420.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        item { Text(markdownToAnnotated(documents.text(doc.id)), fontSize = 13.sp, color = TextDark) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { preview = null }) { Text("Sluiten") } }
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("nl", "NL")).format(Date(millis))
