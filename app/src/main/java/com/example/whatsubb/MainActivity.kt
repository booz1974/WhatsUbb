package com.example.whatsubb

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.whatsubb.ai.AiClient
import com.example.whatsubb.ai.GeminiClient
import com.example.whatsubb.audio.AudioRecorder
import com.example.whatsubb.data.AppSettings
import com.example.whatsubb.data.DocumentType
import com.example.whatsubb.data.KnowledgeBase
import com.example.whatsubb.data.KnowledgeDocuments
import com.example.whatsubb.data.DeviceStore
import com.example.whatsubb.data.WeekInfoStore
import com.example.whatsubb.data.Role
import com.example.whatsubb.ui.*
import com.example.whatsubb.ui.theme.*

/** Simpele "container" met alles wat de schermen nodig hebben. */
class AppServices(context: Context) {
    val settings = AppSettings(context)
    val knowledgeBase = KnowledgeBase(
        context, KnowledgeDocuments(context), WeekInfoStore(context), DeviceStore(context),
        callButtonLabel = { settings.currentCallTarget()?.label }
    )

    init {
        // Opnames die achtergebleven zijn (bijv. na een crash) direct wissen.
        AudioRecorder.deleteAll(context)
        // Verlopen weekmenu's en roosters opruimen.
        knowledgeBase.weekInfo.current()
    }

    /** Nieuwe client per aanvraag, zodat gewijzigde instellingen direct gelden. */
    fun aiClient(): AiClient = GeminiClient(apiKey = settings.apiKey, model = settings.model)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val services = AppServices(applicationContext)
        setContent {
            WhatsUbbTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SoftBackground
                ) {
                    MainScreen(services)
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Login : Screen
    data object BewonerChat : Screen
    data object BegeleiderHome : Screen
    data object BegeleiderChat : Screen
    data class Document(val type: DocumentType) : Screen
    data object Settings : Screen
    data object WeekInfo : Screen
}

@Composable
fun MainScreen(services: AppServices) {
    var screen by remember { mutableStateOf<Screen>(Screen.Login) }
    var askAdminPin by remember { mutableStateOf(false) }

    // Beheer (kennisbank, documenten, sleutel, codes) zit achter een aparte beheerders-PIN.
    if (askAdminPin) {
        val openSettings = { askAdminPin = false; screen = Screen.Settings }
        if (services.settings.hasAdminPin) {
            CodeDialog(
                title = "Beheerders-PIN",
                message = "Het beheer is alleen voor de beheerder van de app.",
                numeric = true,
                onDismiss = { askAdminPin = false },
                onSubmit = { pin -> services.settings.checkAdminPin(pin).also { if (it) openSettings() } }
            )
        } else {
            NewPinDialog(
                title = "Stel een beheerders-PIN in",
                message = "Met deze PIN kom je bij het beheer: kennisbank, documenten, AI-sleutel en codes. " +
                    "Gebruik een andere PIN dan de begeleider-PIN. Minimaal 4 cijfers.",
                onDismiss = { askAdminPin = false },
                onPinSet = { pin -> services.settings.setAdminPin(pin); openSettings() }
            )
        }
    }

    // Terug-knop: binnen het begeleidersdeel terug naar het menu, anders uitloggen.
    BackHandler(enabled = screen != Screen.Login) {
        screen = when (screen) {
            is Screen.Document, Screen.Settings, Screen.WeekInfo, Screen.BegeleiderChat -> Screen.BegeleiderHome
            else -> Screen.Login
        }
    }

    when (val current = screen) {
        Screen.Login -> LoginScreen(services.settings) { role ->
            screen = if (role == Role.BEWONER) Screen.BewonerChat else Screen.BegeleiderHome
        }
        Screen.BewonerChat -> ChatScreen(
            services = services,
            role = Role.BEWONER,
            actionLabel = "Uitloggen",
            onAction = { screen = Screen.Login }
        )
        Screen.BegeleiderHome -> BegeleiderHomeScreen(
            apiKeyMissing = services.settings.apiKey.isBlank(),
            onOpenChat = { screen = Screen.BegeleiderChat },
            onOpenDocument = { screen = Screen.Document(it) },
            onOpenWeekInfo = { screen = Screen.WeekInfo },
            onOpenSettings = { askAdminPin = true },
            onLogout = { screen = Screen.Login }
        )
        Screen.BegeleiderChat -> ChatScreen(
            services = services,
            role = Role.BEGELEIDER,
            actionLabel = "Terug",
            onAction = { screen = Screen.BegeleiderHome }
        )
        is Screen.Document -> DocumentScreen(services, current.type) { screen = Screen.BegeleiderHome }
        Screen.Settings -> SettingsScreen(services) { screen = Screen.BegeleiderHome }
        Screen.WeekInfo -> WeekInfoScreen(services) { screen = Screen.BegeleiderHome }
    }
}
