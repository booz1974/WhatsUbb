package com.example.whatsubb.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.whatsubb.data.Role
import com.example.whatsubb.privacy.Anonymizer
import com.example.whatsubb.ui.theme.*
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

    fun send() {
        val query = questionText.trim()
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
                messages.add(ChatMessage(reply, isUser = false))
                history.add(Turn(fromUser = false, text = reply))
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

            Spacer(modifier = Modifier.height(if (messages.isEmpty()) 32.dp else 16.dp))

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
                            text = if (isBewoner) "Wat wil je weten over de\nregels op UBB2?"
                            else "Vraag iets over de afspraken,\nregels of doelgroep",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    ChatInputPill(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = "Typ hier je vraag...",
                        enabled = !isLoading,
                        onSend = ::send,
                        onMic = ::startListening
                    )

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
                    items(messages) { msg -> ChatBubble(message = msg) }
                    if (isLoading) item { TypingBubble() }
                }
            }
        }
    }
}

private const val MAX_TURNS = 20
