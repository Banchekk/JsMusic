package com.example.personalmusicplayer.ui.home

import androidx.compose.runtime.Composable
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.ui.EmptyState

@Composable
fun HomeScreen() {
    // Разделы «недавно прослушанные» и «плейлисты» добавятся вместе с данными (этапы 4–5).
    EmptyState(R.string.home_empty_title, R.string.home_empty_hint)
}
