package com.example.whatsubb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.data.DocumentType
import com.example.whatsubb.data.WeekInfoType
import com.example.whatsubb.data.documentTypes
import com.example.whatsubb.ui.theme.*
import com.example.whatsubb.util.Weeks

@Composable
fun BegeleiderHomeScreen(
    apiKeyMissing: Boolean,
    missingWeekInfo: List<WeekInfoType>,
    onOpenChat: () -> Unit,
    onOpenDocument: (DocumentType) -> Unit,
    onOpenWeekInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        ScreenHeader(title = "Begeleider", actionLabel = "Uitloggen", onAction = onLogout)
        Text(
            text = "Wat wil je maken? Alles blijft anoniem: gebruik geen namen.",
            fontSize = 14.sp,
            color = TextDark.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (apiKeyMissing) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = Color(0xFFFFF1E6),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onOpenSettings)
                    ) {
                        Text(
                            text = "⚙️ Er is nog geen AI-sleutel ingesteld. Tik hier om dat te doen.",
                            fontSize = 14.sp,
                            color = TextDark,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (missingWeekInfo.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = Color(0xFFFFF1E6),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onOpenWeekInfo)
                    ) {
                        Text(
                            text = "🗓️ ${missingWeekInfoText(missingWeekInfo)} Tik hier om de foto toe te voegen.",
                            fontSize = 14.sp,
                            color = TextDark,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Tile(
                    emoji = "💬",
                    title = "Vraag over afspraken",
                    description = "Stel een vraag over regels, afspraken of de doelgroep van UBB2",
                    highlighted = true,
                    onClick = onOpenChat
                )
            }
            // Weekinfo staat direct na Gespreksverslag.
            val weekInfoIndex = documentTypes.indexOfFirst { it.id == "verslag" } + 1
            items(documentTypes.take(weekInfoIndex)) { type ->
                Tile(type.emoji, type.title, type.description, onClick = { onOpenDocument(type) })
            }
            item {
                Tile(
                    emoji = "🗓️",
                    title = "Weekinfo",
                    description = "Foto van weekmenu en werkrooster, zodat bewoners ernaar kunnen vragen",
                    onClick = onOpenWeekInfo
                )
            }
            items(documentTypes.drop(weekInfoIndex)) { type ->
                Tile(type.emoji, type.title, type.description, onClick = { onOpenDocument(type) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Tile(
                    emoji = "⚙️",
                    title = "Beheer",
                    description = "Kennisbank, documenten met regels en afspraken, AI-sleutel, codes (beheerders-PIN)",
                    onClick = onOpenSettings
                )
            }
        }
    }
}

/** Bijv. "Het weekmenu en werkrooster voor deze week (12 t/m 18 oktober) zijn nog niet toegevoegd." */
private fun missingWeekInfoText(missing: List<WeekInfoType>): String {
    val names = missing.joinToString(" en ") { it.title.lowercase() }
    val verb = if (missing.size > 1) "zijn" else "is"
    return "Het $names voor deze week (${Weeks.label(Weeks.start())}) $verb nog niet toegevoegd."
}

/** Melding na inloggen als de foto's van deze week nog ontbreken. */
@Composable
fun WeekReminderDialog(missing: List<WeekInfoType>, onAdd: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🗓️ Weekinfo ontbreekt") },
        text = {
            Text(
                missingWeekInfoText(missing) + " Maak een foto, zodat bewoners kunnen vragen wat we eten en wie er werkt.",
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAdd) { Text("Nu toevoegen", color = SagePrimary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@Composable
private fun Tile(
    emoji: String,
    title: String,
    description: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) SagePrimary else Color.White),
        border = if (highlighted) null else BorderStroke(1.dp, SagePrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = emoji, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) Color.White else TextDark
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = if (highlighted) Color.White.copy(alpha = 0.9f) else TextDark.copy(alpha = 0.7f)
            )
        }
    }
}
