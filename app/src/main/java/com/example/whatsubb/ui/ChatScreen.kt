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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.ai.AiException
import com.example.whatsubb.ai.Turn
import com.example.whatsubb.data.KnowledgeBase
import com.example.whatsubb.data.Role
import com.example.whatsubb.privacy.Anonymizer
import com.example.whatsubb.ui.theme.*
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
    var confirmNightCall by remember { mutableStateOf(false) }
    var pendingPhone by remember { mutableStateOf<String?>(null) }

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

    fun onCallButton() {
        if (callTarget?.confirmFirst == true) confirmNightCall = true else startCall()
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
                messages.add(ChatMessage(answer, isUser = false, show113 = show113))
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
                text = if (isBewoner) "Hallo! 👋" else "Afspraken UBB2",
                fontSize = if (messages.isEmpty()) 32.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (messages.isEmpty()) 20.dp else 12.dp))

            val target = callTarget
            if (isBewoner && target != null) {
                Button(
                    onClick = ::onCallButton,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PeachAccent),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(52.dp)
                ) {
                    Text("📞  ${target.label}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                            if (msg.show113) Help113Card(onCall = { startCall(HELP_113_PHONE) }, onChat = ::open113Chat)
                        }
                    }
                    if (isLoading) item { TypingBubble() }
                }
            }
        }
    }

    if (confirmNightCall) {
        AlertDialog(
            onDismissRequest = { confirmNightCall = false },
            title = { Text("Kan het echt niet wachten tot morgen?") },
            text = {
                Text(
                    "'s Nachts is de begeleiding er niet. De bereikbare dienst is er voor dingen die " +
                        "echt niet kunnen wachten tot morgen. Is er gevaar? Bel dan 112.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmNightCall = false; startCall() }) {
                    Text("Ja, bellen", color = SagePrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNightCall = false }) { Text("Nee, het kan wachten") }
            }
        )
    }
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
