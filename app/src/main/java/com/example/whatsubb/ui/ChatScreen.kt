package com.example.whatsubb.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.R
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.ai.Turn
import com.example.whatsubb.data.KnowledgeBase
import com.example.whatsubb.data.Role
import com.example.whatsubb.privacy.Anonymizer
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.PdfExporter
import com.example.whatsubb.util.Phones
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Chat over de afspraken en regels van UBB2.
 * Gesprekken worden niet opgeslagen: na uitloggen of terug is alles weg.
 */
@Composable
fun ChatScreen(
    services: AppServices,
    role: Role,
    actionLabel: String,
    onAction: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var questionText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var lastScrubCount by remember { mutableIntStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val history = remember { mutableStateListOf<Turn>() } // geanonimiseerde versie voor de AI
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        val count = messages.size + if (isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            // Eerst in het tekstveld, zodat je kunt controleren wat er is verstaan.
            if (!spoken.isNullOrBlank()) questionText = spoken
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Stel je vraag...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Spraakherkenning is niet beschikbaar op dit toestel", Toast.LENGTH_SHORT).show()
        }
    }

    // Overdag de begeleiding, tussen 23:00 en 08:00 de bereikbare dienst (elke minuut opnieuw bepaald).
    var callTarget by remember { mutableStateOf(services.settings.currentCallTarget()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            callTarget = services.settings.currentCallTarget()
        }
    }
    // 's Nachts eerst een controlevraag; dit is de actie (bellen of chatten) die daarna wordt uitgevoerd.
    var confirmNightAction by remember { mutableStateOf<ContactAction?>(null) }
    var pendingPhone by remember { mutableStateOf<String?>(null) }

    // Antwoord dat als PDF wordt opgeslagen zodra de gebruiker een plek heeft gekozen.
    var pendingPdf by remember { mutableStateOf<String?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val markdown = pendingPdf ?: return@rememberLauncherForActivityResult
        pendingPdf = null
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    PdfExporter.write(context, it, PDF_TITLE, markdown, PdfExporter.FOOTER_ANSWER)
                }
            }.onSuccess {
                Toast.makeText(context, "PDF opgeslagen", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "PDF maken mislukt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun savePdf(message: ChatMessage) {
        pendingPdf = answerMarkdown(message)
        pdfLauncher.launch(answerFileName())
    }

    fun sharePdf(message: ChatMessage) {
        runCatching {
            PdfExporter.share(context, answerFileName(), PDF_TITLE, answerMarkdown(message), PdfExporter.FOOTER_ANSWER)
        }.onFailure {
            Toast.makeText(context, "PDF delen mislukt", Toast.LENGTH_SHORT).show()
        }
    }

    fun call(phone: String, direct: Boolean) {
        val uri = Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })
        try {
            context.startActivity(Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
        } catch (e: SecurityException) {
            call(phone, direct = false)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Bellen kan niet op dit apparaat. Het nummer is $phone", Toast.LENGTH_LONG).show()
        }
    }

    // Eerste keer: toestemming vragen om direct te bellen; zonder toestemming opent de telefoon-app met het nummer.
    val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingPhone?.let { call(it, direct = granted) }
        pendingPhone = null
    }

    fun startCall(phone: String? = callTarget?.phone) {
        if (phone == null) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        if (granted) call(phone, direct = true)
        else {
            pendingPhone = phone
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    fun open113Chat() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HELP_113_CHAT_URL)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Open 113.nl/chat in je browser", Toast.LENGTH_LONG).show()
        }
    }

    fun openWhatsApp() {
        val phone = callTarget?.phone ?: return
        val uri = Uri.parse("https://wa.me/" + Phones.whatsAppNumber(phone))
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp openen lukt niet. Het nummer is $phone", Toast.LENGTH_LONG).show()
        }
    }

    fun runContact(action: ContactAction) = when (action) {
        ContactAction.CALL -> startCall()
        ContactAction.CHAT -> openWhatsApp()
    }

    fun onContactButton(action: ContactAction) {
        if (callTarget?.confirmFirst == true) confirmNightAction = action else runContact(action)
    }

    fun send(text: String = questionText) {
        val query = text.trim()
        if (query.isEmpty() || isLoading) return
        questionText = ""

        val scrubbed = Anonymizer.scrub(query)
        lastScrubCount = scrubbed.replacements
        messages.add(ChatMessage(query, isUser = true))
        history.add(Turn(fromUser = true, text = scrubbed.text))
        isLoading = true

        scope.launch {
            try {
                val reply = services.aiClient().generate(
                    systemPrompt = services.knowledgeBase.systemPrompt(role),
                    history = history.takeLast(MAX_TURNS).dropWhile { !it.fromUser }
                )
                // De AI geeft met een vaste markering aan dat 113 erbij hoort; als vangnet kijken we ook naar de vraag zelf.
                val answer = reply.replace(KnowledgeBase.HELP_113_MARKER, "").trim()
                val show113 = role == Role.BEWONER &&
                    (reply.contains(KnowledgeBase.HELP_113_MARKER) || mentions113Topic(query))
                messages.add(ChatMessage(answer, isUser = false, show113 = show113, question = query))
                history.add(Turn(fromUser = false, text = answer))
            } catch (e: AiException) {
                messages.add(ChatMessage(e.message.orEmpty(), isUser = false, isError = true))
                history.removeAt(history.lastIndex) // mislukte vraag niet meesturen
            } finally {
                isLoading = false
            }
        }
    }

    val isBewoner = role == Role.BEWONER

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        TextButton(onClick = onAction, modifier = Modifier.align(Alignment.TopEnd)) {
            Text(text = actionLabel, fontSize = 14.sp, color = PeachAccent, fontWeight = FontWeight.SemiBold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Zonder berichten kan de welkomsttekst op kleine schermen scrollen.
                .then(if (messages.isEmpty() && !isLoading) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isBewoner) "Hey Whats Ubb ?!" else "Afspraken UBB2",
                fontSize = if (messages.isEmpty()) 32.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (messages.isEmpty()) 20.dp else 12.dp))

            val target = callTarget
            if (isBewoner && target != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    ContactButton(
                        emoji = "📞", verb = "Bel", name = target.name, color = PeachAccent,
                        onClick = { onContactButton(ContactAction.CALL) }, modifier = Modifier.weight(1f)
                    )
                    ContactButton(
                        icon = R.drawable.ic_whatsapp, verb = "Chat met", name = target.name, color = SagePrimary,
                        onClick = { onContactButton(ContactAction.CHAT) }, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(if (messages.isEmpty()) 20.dp else 12.dp))
            }

            // Zwevende witte kaart met de vraag en het invoerveld
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = if (messages.isEmpty()) 28.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (messages.isEmpty()) {
                        Text(
                            text = if (isBewoner) "Waar kan ik je\nmee helpen?"
                            else "Vraag iets over de afspraken,\nregels of doelgroep",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                        if (isBewoner) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Je kunt mij alles vragen. Over de regels en afspraken, wat we eten of wie er werkt. " +
                                    "Maar ook als je niet weet hoe iets moet, als je je niet zo fijn voelt, " +
                                    "ergens bang voor bent of gewoon even wilt praten. Ik denk met je mee.",
                                fontSize = 15.sp,
                                color = TextDark.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    ChatInputPill(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = "Typ hier je vraag...",
                        enabled = !isLoading,
                        onSend = { send() },
                        onMic = ::startListening
                    )

                    if (messages.isEmpty() && isBewoner) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bijvoorbeeld:", fontSize = 13.sp, color = TextDark.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EXAMPLE_QUESTIONS.forEach { (emoji, question) ->
                                SuggestionChip(
                                    onClick = { send(question) },
                                    enabled = !isLoading,
                                    label = { Text("$emoji $question", fontSize = 13.sp) }
                                )
                            }
                        }
                    }

                    if (messages.isEmpty()) {
                        Text(
                            text = "Noem geen namen of persoonlijke gegevens. Je vraag blijft anoniem.",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }

            PrivacyNotice(lastScrubCount)

            if (messages.isNotEmpty() || isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    items(messages) { msg ->
                        Column {
                            ChatBubble(message = msg)
                            if (!msg.isUser && !msg.isError) {
                                AnswerPdfActions(onSave = { savePdf(msg) }, onShare = { sharePdf(msg) })
                            }
                            if (msg.show113) Help113Card(onCall = { startCall(HELP_113_PHONE) }, onChat = ::open113Chat)
                        }
                    }
                    if (isLoading) item { TypingBubble() }
                }
            }
        }
    }

    confirmNightAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmNightAction = null },
            title = { Text("Kan het echt niet wachten tot morgen?") },
            text = {
                Text(
                    "'s Nachts is de begeleiding er niet. De bereikbare dienst is er voor dingen die " +
                        "echt niet kunnen wachten tot morgen. Is er gevaar? Bel dan 112.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmNightAction = null; runContact(action) }) {
                    Text(
                        if (action == ContactAction.CALL) "Ja, bellen" else "Ja, chatten",
                        color = SagePrimary, fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNightAction = null }) { Text("Nee, het kan wachten") }
            }
        )
    }
}

private enum class ContactAction { CALL, CHAT }

/**
 * Knop om de begeleiding of bereikbare dienst te bellen of te appen, bijv. "📞 Bel / de begeleiding".
 * Met [icon] staat er een afbeelding (zoals het WhatsApp-logo) in plaats van de [emoji].
 */
@Composable
private fun ContactButton(
    emoji: String = "",
    @DrawableRes icon: Int? = null,
    verb: String,
    name: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier.height(60.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (icon != null) verb else "$emoji  $verb", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            }
            Text(name, fontSize = 12.sp, color = Color.White, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

/** Kleine knoppen onder een antwoord: opslaan of delen als PDF. */
@Composable
private fun AnswerPdfActions(onSave: () -> Unit, onShare: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = onSave, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("📄 Opslaan als PDF", fontSize = 13.sp, color = SagePrimary)
        }
        TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("📤 Delen als PDF", fontSize = 13.sp, color = SagePrimary)
        }
    }
}

private const val PDF_TITLE = "Antwoord van Whats Ubb"

/** Vraag en antwoord samen, als Markdown voor de PDF. */
private fun answerMarkdown(message: ChatMessage): String = buildString {
    message.question?.let { append("## Je vraag\n\n").append(it.trim()).append("\n\n") }
    append("## Antwoord\n\n").append(message.text.trim())
}

private fun answerFileName(): String {
    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
    return "Whats_Ubb_antwoord_$stamp.pdf"
}

private const val MAX_TURNS = 20

/** 113 Zelfmoordpreventie: gratis, anoniem, dag en nacht. */
private const val HELP_113_PHONE = "0800-0113"
private const val HELP_113_CHAT_URL = "https://www.113.nl/chat"

private val HELP_113_WORDS = listOf(
    "zelfmoord", "suïcid", "suicid", "zelfdoding", "dood wil", "dood zijn", "niet meer leven",
    "niet meer wil leven", "einde maken", "er niet meer zijn", "er een eind aan", "mezelf iets aandoen",
    "somber", "depressief", "hopeloos", "geen zin meer in", "geen zin in het leven", "alles is zinloos"
)

private fun mentions113Topic(text: String): Boolean {
    val lower = text.lowercase()
    return HELP_113_WORDS.any { it in lower }
}

/** Kaartje onder een antwoord over somberheid of suïcidale gedachten. */
@Composable
private fun Help113Card(onCall: () -> Unit, onChat: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Praten met 113 kan altijd",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center
            )
            Text(
                "Gratis en anoniem, dag en nacht. Je kunt ook altijd naar de begeleiding gaan.",
                fontSize = 13.sp, color = TextDark.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(
                onClick = onCall,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PeachAccent),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("📞  Bel 113", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onChat,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("💬  Chat met 113", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SagePrimary)
            }
        }
    }
}

private val EXAMPLE_QUESTIONS = listOf(
    "🍽️" to "Wat eten we vanavond?",
    "👥" to "Wie werkt er donderdag?",
    "📋" to "Hoe laat moet ik binnen zijn?",
    "🧺" to "Hoe doe ik de was?",
    "💬" to "Ik voel me niet zo goed"
)
