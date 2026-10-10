package com.example.whatsubb.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.ui.theme.*

@Composable
fun SettingsScreen(services: AppServices, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = services.settings
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var bewonerCode by remember { mutableStateOf("") }
    var knowledge by remember { mutableStateOf(services.knowledgeBase.load()) }
    var showPinDialog by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        ScreenHeader(title = "Instellingen", actionLabel = "Terug", onAction = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Section("📚 Kennisbank UBB2") {
                Text(
                    "Dit is wat de AI weet over de afdeling: afspraken, regels en de doelgroep. " +
                        "Schrijf hier nooit namen of gegevens van bewoners in.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = knowledge,
                    onValueChange = { knowledge = it },
                    minLines = 10,
                    maxLines = 20,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { services.knowledgeBase.save(knowledge); toast("Kennisbank opgeslagen") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                    ) { Text("Opslaan") }
                    OutlinedButton(onClick = {
                        services.knowledgeBase.resetToDefault()
                        knowledge = services.knowledgeBase.load()
                        toast("Standaardtekst teruggezet")
                    }, shape = CircleShape) { Text("Standaard herstellen") }
                }
            }

            Section("🤖 AI (Google Gemini)") {
                Text(
                    "Maak een API-sleutel aan op aistudio.google.com. Let op: gebruik een betaald account, " +
                        "bij de gratis versie mag Google ingestuurde tekst gebruiken om de AI te verbeteren.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API-sleutel") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    supportingText = { Text("Standaard: gemini-flash-latest") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { settings.apiKey = apiKey; settings.model = model; toast("AI-instellingen opgeslagen") },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                ) { Text("Opslaan") }
            }

            Section("🔑 Toegang") {
                Text(
                    if (settings.hasBewonerCode) "Bewoners hebben nu een code nodig om in te loggen."
                    else "Bewoners kunnen nu zonder code in de app.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = bewonerCode,
                    onValueChange = { bewonerCode = it },
                    label = { Text("Nieuwe bewonerscode (leeg = geen code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            settings.setBewonerCode(bewonerCode)
                            toast(if (bewonerCode.isBlank()) "Bewonerscode uitgezet" else "Bewonerscode ingesteld")
                            bewonerCode = ""
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                    ) { Text("Code opslaan") }
                    OutlinedButton(onClick = { showPinDialog = true }, shape = CircleShape) { Text("Begeleider-PIN wijzigen") }
                }
            }
        }
    }

    if (showPinDialog) {
        NewPinDialog(
            title = "Nieuwe begeleider-PIN",
            onDismiss = { showPinDialog = false },
            onPinSet = { pin -> settings.setBegeleiderPin(pin); showPinDialog = false; toast("PIN gewijzigd") }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            content()
        }
    }
}
