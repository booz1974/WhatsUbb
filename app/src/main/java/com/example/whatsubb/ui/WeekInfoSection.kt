package com.example.whatsubb.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.whatsubb.AppServices
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.data.WeekInfoEntry
import com.example.whatsubb.data.WeekInfoType
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.ImageUtils
import com.example.whatsubb.util.Weeks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Foto's van het weekmenu en het werkrooster uploaden; verlopen info verdwijnt automatisch. */
@Composable
fun WeekInfoSection(services: AppServices, initialWeekOffset: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = services.knowledgeBase.weekInfo
    var entries by remember { mutableStateOf(store.current()) }
    var weekOffset by remember { mutableIntStateOf(initialWeekOffset) }
    var pendingType by remember { mutableStateOf<WeekInfoType?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var processing by remember { mutableStateOf<WeekInfoType?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<WeekInfoEntry?>(null) }
    var toDelete by remember { mutableStateOf<WeekInfoEntry?>(null) }

    fun photosDir() = File(context.cacheDir, "photos").apply { mkdirs() }

    fun process(type: WeekInfoType, uri: Uri, cleanup: File?) {
        val weekStart = Weeks.start(weekOffset)
        processing = type
        error = null
        scope.launch {
            val upload = File(photosDir(), "upload_${System.currentTimeMillis()}.jpg")
            try {
                withContext(Dispatchers.IO) { ImageUtils.prepareForUpload(context, uri, upload) }
                val text = services.aiClient().processFile(upload, "image/jpeg", type.instruction)
                store.put(type, weekStart, text)
                entries = store.current()
                Toast.makeText(context, "${type.title} voor ${Weeks.label(weekStart)} opgeslagen", Toast.LENGTH_SHORT).show()
            } catch (e: AiException) {
                error = e.message
            } catch (e: Exception) {
                error = "De foto kon niet worden gelezen."
            } finally {
                // De foto zelf wordt nooit bewaard.
                upload.delete()
                cleanup?.delete()
                processing = null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val type = pendingType
        val file = cameraFile
        if (saved && type != null && file != null) process(type, Uri.fromFile(file), cleanup = file)
        else file?.delete()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val type = pendingType
        if (uri != null && type != null) process(type, uri, cleanup = null)
    }

    fun takePhoto(type: WeekInfoType) {
        pendingType = type
        val file = File(photosDir(), "camera_${System.currentTimeMillis()}.jpg")
        cameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        try {
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            error = "Er is geen camera-app gevonden."
        }
    }

    Text(
        "Maak een foto van het weekmenu en het werkrooster zoals ze op de afdeling hangen. " +
            "De AI leest ze uit, zodat bewoners er vragen over kunnen stellen. Alleen de tekst wordt bewaard, niet de foto. " +
            "Na afloop van de week wordt de info automatisch verwijderd.",
        fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
    )

    Text("Voor welke week?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to "Deze week", 1 to "Volgende week").forEach { (offset, label) ->
            FilterChip(
                selected = weekOffset == offset,
                onClick = { weekOffset = offset },
                label = { Text("$label\n${Weeks.label(Weeks.start(offset))}", fontSize = 12.sp, lineHeight = 15.sp) }
            )
        }
    }

    WeekInfoType.entries.forEach { type ->
        Surface(color = SagePrimary.copy(alpha = 0.07f), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${type.emoji} ${type.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)

                val ofType = entries.filter { it.type == type }
                if (ofType.isEmpty()) {
                    Text("Geen actueel ${type.title.lowercase()}.", fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
                }
                ofType.forEach { entry ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "✓ ${Weeks.label(entry.weekStart)}",
                            fontSize = 13.sp, color = TextDark, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { preview = entry }) { Text("Bekijk", color = SagePrimary) }
                        TextButton(onClick = { toDelete = entry }) { Text("Wis", color = Color(0xFFC0392B)) }
                    }
                }

                if (processing == type) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SagePrimary)
                        Spacer(Modifier.width(10.dp))
                        Text("Foto wordt uitgelezen...", fontSize = 13.sp, color = TextDark)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { takePhoto(type) },
                            enabled = processing == null,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                        ) { Text("📷 Foto maken") }
                        OutlinedButton(
                            onClick = {
                                pendingType = type
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = processing == null,
                            shape = CircleShape
                        ) { Text("🖼️ Kies foto") }
                    }
                }
            }
        }
    }

    error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFC0392B)) }

    preview?.let { entry ->
        TextPreviewDialog(
            title = "${entry.type.title} ${Weeks.label(entry.weekStart)}",
            text = entry.text,
            onDismiss = { preview = null }
        )
    }

    toDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("${entry.type.title} verwijderen?") },
            text = { Text("Het ${entry.type.title.lowercase()} van ${Weeks.label(entry.weekStart)} wordt verwijderd.") },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(entry.type, entry.weekStart)
                    entries = store.current()
                    toDelete = null
                }) { Text("Verwijderen", color = Color(0xFFC0392B)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuleren") } }
        )
    }
}

/** Dialoog om uitgelezen tekst te controleren. */
@Composable
fun TextPreviewDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 2) },
        text = {
            Box(Modifier.heightIn(max = 420.dp)) {
                androidx.compose.foundation.lazy.LazyColumn {
                    item { Text(markdownToAnnotated(text), fontSize = 13.sp, color = TextDark) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}
