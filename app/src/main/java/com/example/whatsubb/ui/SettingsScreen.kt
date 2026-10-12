package com.example.whatsubb.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.BuildConfig
import com.example.whatsubb.R
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
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf(settings.begeleidingPhone) }
    var nightPhone by remember { mutableStateOf(settings.bereikbareDienstPhone) }
    var begeleiderName by remember { mutableStateOf(settings.begeleiderName) }
    var persona by remember { mutableStateOf(services.knowledgeBase.loadPersona()) }

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
        ScreenHeader(title = "Beheer", actionLabel = "Terug", onAction = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Section("🧠 AI-rol") {
                Text(
                    "Deze tekst gaat bij elke vraag en elk document als eerste mee naar de AI: wie de AI is, " +
                        "voor welke doelgroep, met welke methodieken en wanneer de begeleiding aanwezig is.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = persona,
                    onValueChange = { persona = it },
                    minLines = 6,
                    maxLines = 14,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { services.knowledgeBase.savePersona(persona); toast("AI-rol opgeslagen") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                    ) { Text("Opslaan") }
                    OutlinedButton(onClick = {
                        services.knowledgeBase.resetPersona()
                        persona = services.knowledgeBase.loadPersona()
                        toast("Standaardtekst teruggezet")
                    }, shape = CircleShape) { Text("Standaard herstellen") }
                }
            }

            Section("👤 Jouw voornaam (begeleider)") {
                Text(
                    "Alleen je voornaam. De app begroet je ermee en de AI weet wie er vraagt, " +
                        "zodat je kunt vragen \"wanneer werk ik?\". Bewoners zien je naam niet.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = begeleiderName,
                    onValueChange = { begeleiderName = it.filter { c -> c.isLetter() || c == '-' }.take(30) },
                    label = { Text("Voornaam") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        settings.begeleiderName = begeleiderName
                        begeleiderName = settings.begeleiderName
                        toast(if (begeleiderName.isBlank()) "Naam verwijderd" else "Naam opgeslagen")
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                ) { Text("Opslaan") }
            }

            Section("📞 Bel- en chatknop voor bewoners") {
                Text(
                    "Vul het 06-nummer van de begeleiding in. Bewoners zien dan knoppen waarmee ze met één tik bellen " +
                        "of een WhatsApp-chat openen met dat nummer. " +
                        "Tussen 23:00 en 08:00 gaan de knoppen naar de bereikbare dienst (als dat nummer is ingevuld). " +
                        "Laat leeg om de knop te verbergen.",
                    fontSize = 13.sp, color = TextDark.copy(alpha = 0.75f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c in "+ -" } },
                    label = { Text("Telefoonnummer begeleiding") },
                    placeholder = { Text("06 12345678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nightPhone,
                    onValueChange = { nightPhone = it.filter { c -> c.isDigit() || c in "+ -" } },
                    label = { Text("Bereikbare dienst (23:00 - 08:00)") },
                    supportingText = { Text("'s Nachts bellen of appen de knoppen dit nummer, na een controlevraag") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        settings.begeleidingPhone = phone
                        settings.bereikbareDienstPhone = nightPhone
                        toast("Telefoonnummers opgeslagen")
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                ) { Text("Opslaan") }
            }

            Section("📎 Documenten: regels, afspraken, werkwijze") {
                DocumentsSection(services)
            }

            Section("🔌 Apparaten: wasmachine, droger, koffie...") {
                DevicesSection(services)
            }

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
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPinDialog = true }, shape = CircleShape) { Text("Begeleider-PIN wijzigen") }
                    OutlinedButton(onClick = { showAdminPinDialog = true }, shape = CircleShape) { Text("Beheerders-PIN wijzigen") }
                }
            }

            AboutFooter()
        }
    }

    if (showPinDialog) {
        NewPinDialog(
            title = "Nieuwe begeleider-PIN",
            onDismiss = { showPinDialog = false },
            onPinSet = { pin -> settings.setBegeleiderPin(pin); showPinDialog = false; toast("PIN gewijzigd") }
        )
    }

    if (showAdminPinDialog) {
        NewPinDialog(
            title = "Nieuwe beheerders-PIN",
            message = "Met deze PIN kom je bij het beheer. Gebruik een andere PIN dan de begeleider-PIN.",
            onDismiss = { showAdminPinDialog = false },
            onPinSet = { pin -> settings.setAdminPin(pin); showAdminPinDialog = false; toast("Beheerders-PIN gewijzigd") }
        )
    }
}

/** Maker, versie en datum onderaan het beheerscherm. */
@Composable
private fun AboutFooter() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.whats_ubb_logo),
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Whats Ubb", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
        Text(
            "Versie ${BuildConfig.VERSION_NAME}  ·  ${BuildConfig.RELEASE_DATE}",
            fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f)
        )
        Text(
            "Gemaakt door Jeroen van Sonsbeek",
            fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f)
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
