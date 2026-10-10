package com.example.whatsubb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.R
import com.example.whatsubb.data.AppSettings
import com.example.whatsubb.data.Role
import com.example.whatsubb.ui.theme.*

private enum class LoginDialog { NONE, BEWONER_CODE, BEGELEIDER_PIN, NEW_PIN }

@Composable
fun LoginScreen(settings: AppSettings, onLoggedIn: (Role) -> Unit) {
    var dialog by remember { mutableStateOf(LoginDialog.NONE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo (bevat de naam "WhatsUbb")
        Image(
            painter = painterResource(R.drawable.whats_ubb_logo),
            contentDescription = "Whats Ubb",
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .widthIn(max = 280.dp)
                .aspectRatio(900f / 937f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slogan
        Text(
            text = "Anoniem, duidelijk en altijd bereikbaar",
            fontSize = 14.sp,
            color = TextDark.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(56.dp))

        // Knop voor Bewoner (Gevulde Saliegroene Ovaal)
        Button(
            onClick = {
                if (settings.hasBewonerCode) dialog = LoginDialog.BEWONER_CODE
                else onLoggedIn(Role.BEWONER)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
        ) {
            Text(
                text = "Ik ben bewoner",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Knop voor Begeleider (Outlined Saliegroene Ovaal)
        OutlinedButton(
            onClick = {
                dialog = if (settings.hasBegeleiderPin) LoginDialog.BEGELEIDER_PIN else LoginDialog.NEW_PIN
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            border = BorderStroke(1.5.dp, SagePrimary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SagePrimary,
                containerColor = Color.Transparent
            )
        ) {
            Text(
                text = "Ik ben begeleider",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SagePrimary
            )
        }
    }

    when (dialog) {
        LoginDialog.NONE -> Unit
        LoginDialog.BEWONER_CODE -> CodeDialog(
            title = "Code van de afdeling",
            message = "Vraag de code aan de begeleiding. Je hoeft je naam niet in te vullen.",
            onDismiss = { dialog = LoginDialog.NONE },
            onSubmit = { code ->
                settings.checkBewonerCode(code).also { ok ->
                    if (ok) { dialog = LoginDialog.NONE; onLoggedIn(Role.BEWONER) }
                }
            }
        )
        LoginDialog.BEGELEIDER_PIN -> CodeDialog(
            title = "Begeleider-PIN",
            message = "Voer de PIN van het team in.",
            numeric = true,
            onDismiss = { dialog = LoginDialog.NONE },
            onSubmit = { pin ->
                settings.checkBegeleiderPin(pin).also { ok ->
                    if (ok) { dialog = LoginDialog.NONE; onLoggedIn(Role.BEGELEIDER) }
                }
            }
        )
        LoginDialog.NEW_PIN -> NewPinDialog(
            onDismiss = { dialog = LoginDialog.NONE },
            onPinSet = { pin ->
                settings.setBegeleiderPin(pin)
                dialog = LoginDialog.NONE
                onLoggedIn(Role.BEGELEIDER)
            }
        )
    }
}

/** Dialoog voor een code; [onSubmit] geeft true terug als de code klopt. */
@Composable
fun CodeDialog(
    title: String,
    message: String,
    numeric: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean
) {
    var code by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; wrong = false },
                    singleLine = true,
                    isError = wrong,
                    supportingText = { if (wrong) Text("Deze code klopt niet") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSubmit(code)) wrong = true }) { Text("Inloggen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}

@Composable
fun NewPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit,
    title: String = "Stel een begeleider-PIN in",
    message: String = "Deze PIN beschermt het begeleidersdeel. Deel hem alleen met collega's. Minimaal 4 cijfers."
) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit); error = null },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                OutlinedTextField(
                    value = repeat,
                    onValueChange = { repeat = it.filter(Char::isDigit); error = null },
                    label = { Text("Herhaal PIN") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    pin.length < 4 -> "Gebruik minimaal 4 cijfers"
                    pin != repeat -> "De PIN-codes zijn niet gelijk"
                    else -> null
                }
                if (error == null) onPinSet(pin)
            }) { Text("Opslaan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } }
    )
}
