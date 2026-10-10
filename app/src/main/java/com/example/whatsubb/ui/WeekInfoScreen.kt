package com.example.whatsubb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whatsubb.AppServices

/** Weekmenu en werkrooster bijwerken; voor alle begeleiders bereikbaar. */
@Composable
fun WeekInfoScreen(services: AppServices, onBack: () -> Unit) {
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
            WeekInfoSection(services)
        }
    }
}
