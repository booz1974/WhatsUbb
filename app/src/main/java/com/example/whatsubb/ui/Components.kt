package com.example.whatsubb.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.ui.theme.*

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    /** Onder dit antwoord de knoppen naar 113 Zelfmoordpreventie tonen. */
    val show113: Boolean = false,
    /** De vraag waar dit antwoord bij hoort (voor de PDF). */
    val question: String? = null
)

/** Bovenbalk met titel en een tekstknop rechts (terug / uitloggen). */
@Composable
fun ScreenHeader(title: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(text = actionLabel, fontSize = 14.sp, color = PeachAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Grijze invoer-pil met een groene ronde knop: microfoon als het veld leeg is, anders verzenden.
 * Enter verstuurt ook. Het veld blijft typbaar als [enabled] false is (bijv. tijdens wachten op een antwoord),
 * zodat de cursor en het toetsenbord blijven staan; alleen versturen kan dan niet.
 */
@Composable
fun ChatInputPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    onSend: () -> Unit,
    onMic: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    fun trySend() {
        if (enabled && value.isNotBlank()) onSend()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(Color(0xFFEAEFEF), RoundedCornerShape(28.dp))
            .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = Color(0xFF8E9B95), fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 16.sp, color = TextDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    // Enter op een fysiek toetsenbord verstuurt; Shift+Enter geeft een nieuwe regel.
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Enter && !event.isShiftPressed) {
                            if (event.type == KeyEventType.KeyDown) trySend()
                            true
                        } else false
                    },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { trySend() })
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (enabled) SagePrimary else SagePrimary.copy(alpha = 0.4f), CircleShape)
                .clickable(enabled = enabled) { if (value.isNotBlank()) onSend() else onMic() },
            contentAlignment = Alignment.Center
        ) {
            if (value.isNotBlank()) {
                Text(text = "➤", color = Color.White, fontSize = 18.sp)
            } else {
                MicrophoneIcon(modifier = Modifier.size(22.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun MicrophoneIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Microfoon behuizing (capsule)
        val bodyWidth = width * 0.38f
        val bodyHeight = height * 0.52f
        val bodyLeft = (width - bodyWidth) / 2f
        val bodyTop = height * 0.08f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(bodyWidth / 2f, bodyWidth / 2f)
        )

        // U-boog om de behuizing
        val arcTop = height * 0.22f
        val arcSize = height * 0.46f
        val arcLeft = (width - arcSize) / 2f
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = Size(arcSize, arcSize),
            style = Stroke(width = width * 0.10f, cap = StrokeCap.Round)
        )

        // Verticale standaard
        val strokeW = width * 0.10f
        val lineStartY = arcTop + arcSize
        val lineEndY = height * 0.85f
        drawLine(
            color = tint,
            start = Offset(width / 2f, lineStartY),
            end = Offset(width / 2f, lineEndY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Voetje
        val baseWidth = width * 0.38f
        drawLine(
            color = tint,
            start = Offset((width - baseWidth) / 2f, lineEndY),
            end = Offset((width + baseWidth) / 2f, lineEndY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = when {
                message.isUser -> SagePrimary
                message.isError -> Color(0xFFFFF1E6)
                else -> Color.White
            },
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(
                    elevation = if (message.isUser) 0.dp else 4.dp,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Text(
                text = if (message.isUser) AnnotatedString(message.text) else markdownToAnnotated(message.text),
                fontSize = 15.sp,
                color = if (message.isUser) Color.White else TextDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                lineHeight = 20.sp
            )
        }
    }
}

/** Klein "aan het typen..." bubbeltje terwijl de AI nadenkt. */
@Composable
fun TypingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.shadow(4.dp, RoundedCornerShape(18.dp))) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SagePrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Even nadenken...", fontSize = 14.sp, color = TextDark.copy(alpha = 0.7f))
            }
        }
    }
}

/** Kleine melding dat er gegevens zijn geanonimiseerd. */
@Composable
fun PrivacyNotice(count: Int) {
    if (count <= 0) return
    Text(
        text = "🔒 $count gegeven(s) automatisch geanonimiseerd voor verzending",
        fontSize = 12.sp,
        color = SagePrimary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
