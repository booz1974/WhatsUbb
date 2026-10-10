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
import com.example.whatsubb.data.Device
import com.example.whatsubb.data.DeviceStore
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Waar de foto voor is: een nieuw apparaat, of extra info bij een bestaand apparaat. */
private sealed interface DeviceTarget {
    val name: String
    data class New(override val name: String) : DeviceTarget
    data class Extra(val device: Device) : DeviceTarget { override val name get() = device.name }
}

/** Foto's van apparaten (wasmachine, droger, koffieapparaat...) zodat bewoners gerichte uitleg krijgen. */
@Composable
fun DevicesSection(services: AppServices) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = services.knowledgeBase.devices
    var devices by remember { mutableStateOf(store.list()) }
    var newName by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<DeviceTarget?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var processing by remember { mutableStateOf<DeviceTarget?>(null) } // de foto die nu wordt uitgelezen
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Device?>(null) }
    var toDelete by remember { mutableStateOf<Device?>(null) }

    fun photosDir() = File(context.cacheDir, "photos").apply { mkdirs() }

    fun process(target: DeviceTarget, uri: Uri, cleanup: File?) {
        processing = target
        error = null
        scope.launch {
            val upload = File(photosDir(), "upload_${System.currentTimeMillis()}.jpg")
            try {
                withContext(Dispatchers.IO) { ImageUtils.prepareForUpload(context, uri, upload) }
                val text = services.aiClient().processFile(upload, "image/jpeg", DeviceStore.instruction(target.name))
                when (target) {
                    is DeviceTarget.New -> { store.add(target.name, text); newName = "" }
                    is DeviceTarget.Extra -> store.append(target.device.id, text)
                }
                devices = store.list()
                Toast.makeText(context, "${target.name} opgeslagen", Toast.LENGTH_SHORT).show()
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
        val target = pending
        val file = cameraFile
        if (saved && target != null && file != null) process(target, Uri.fromFile(file), cleanup = file)
        else file?.delete()
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pending
        if (uri != null && target != null) process(target, uri, cleanup = null)
    }

    fun takePhoto(target: DeviceTarget) {
        pending = target
        val file = File(photosDir(), "camera_${System.currentTimeMillis()}.jpg")
        cameraFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        try {
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            error = "Er is geen camera-app gevonden."
        }
    }

    fun pickPhoto(target: DeviceTarget) {
        pending = target
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Text(
        "Maak een foto van een apparaat, het liefst van de knoppen of het bedieningspaneel. De AI leest uit welke " +
            "knoppen en programma's er zijn, zodat bewoners stap voor stap uitleg krijgen. Met \"Extra foto\" voeg je " +
            "meer toe, bijvoorbeeld een andere kant of een bladzijde uit de handleiding. Alleen de tekst wordt bewaard, niet de foto.",
        fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
    )

    if (devices.isEmpty()) {
        Text("Nog geen apparaten toegevoegd.", fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
    }
    devices.forEach { device ->
        Surface(color = SagePrimary.copy(alpha = 0.07f), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔌 ${device.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { preview = device }) { Text("Bekijk", color = SagePrimary) }
                    TextButton(onClick = { toDelete = device }) { Text("Wis", color = Color(0xFFC0392B)) }
                }
                if ((processing as? DeviceTarget.Extra)?.device?.id == device.id) {
                    ProcessingRow()
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { takePhoto(DeviceTarget.Extra(device)) }, enabled = processing == null) {
                            Text("📷 Extra foto", color = SagePrimary)
                        }
                        TextButton(onClick = { pickPhoto(DeviceTarget.Extra(device)) }, enabled = processing == null) {
                            Text("🖼️ Uit galerij", color = SagePrimary)
                        }
                    }
                }
            }
        }
    }

    Text("Nieuw apparaat", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
    OutlinedTextField(
        value = newName,
        onValueChange = { newName = it },
        label = { Text("Naam, bijv. Wasmachine of Koffieapparaat") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    val isNewProcessing = processing is DeviceTarget.New
    if (isNewProcessing) {
        ProcessingRow()
    } else {
        val canAdd = newName.isNotBlank() && processing == null
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { takePhoto(DeviceTarget.New(newName.trim())) },
                enabled = canAdd,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
            ) { Text("📷 Foto maken") }
            OutlinedButton(
                onClick = { pickPhoto(DeviceTarget.New(newName.trim())) },
                enabled = canAdd,
                shape = CircleShape
            ) { Text("🖼️ Kies foto") }
        }
    }

    error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFC0392B)) }

    preview?.let { device ->
        TextPreviewDialog(title = device.name, text = device.text, onDismiss = { preview = null })
    }

    toDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("${device.name} verwijderen?") },
            text = { Text("De uitleg over dit apparaat wordt verwijderd. De AI weet er dan niets meer over.") },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(device.id)
                    devices = store.list()
                    toDelete = null
                }) { Text("Verwijderen", color = Color(0xFFC0392B)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuleren") } }
        )
    }
}

@Composable
private fun ProcessingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SagePrimary)
        Spacer(Modifier.width(10.dp))
        Text("Foto wordt uitgelezen...", fontSize = 13.sp, color = TextDark)
    }
}
