package com.example.whatsubb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.whatsubb.AppServices
import com.example.whatsubb.data.WeekReminder
import com.example.whatsubb.ui.theme.*

/** Weekmenu en werkrooster bijwerken; voor alle begeleiders bereikbaar. */
@Composable
fun WeekInfoScreen(services: AppServices, reminder: WeekReminder?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        ScreenHeader(title = "🗓️ Weekinfo", actionLabel = "Terug", onAction = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Op zondag komt de begeleider hier automatisch als de volgende week nog niet klaarstaat.
            if (reminder?.isNextWeek == true) {
                Surface(color = Color(0xFFFFF1E6), shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "🗓️ Het is zondag. ${reminder.text} Maak nu een foto, dan staat alles klaar voor morgen.",
                        fontSize = 14.sp, color = TextDark, modifier = Modifier.padding(16.dp)
                    )
                }
            }
            WeekInfoSection(services, initialWeekOffset = reminder?.weekOffset ?: 0)
        }
    }
}
