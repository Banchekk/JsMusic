package com.example.personalmusicplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalmusicplayer.R

/**
 * Обложка плейлиста: выбранное изображение либо автоколлаж 2×2 из обложек первых четырёх песен.
 * [arts] — обложки первых песен по порядку (null — у песни нет обложки).
 */
@Composable
fun PlaylistCover(
    coverUri: String?,
    arts: List<String?>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    iconSize: Dp = 32.dp,
) {
    Box(modifier.clip(RoundedCornerShape(cornerRadius))) {
        when {
            coverUri != null ->
                Cover(coverUri, Modifier.fillMaxSize(), cornerRadius = 0.dp, iconSize = iconSize)
            arts.isEmpty() ->
                CoverPlaceholder(Modifier.fillMaxSize(), iconSize)
            else -> Column(Modifier.fillMaxSize()) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f)) {
                        for (col in 0..1) {
                            Cover(
                                artUri = arts.getOrNull(row * 2 + col),
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                cornerRadius = 0.dp,
                                iconSize = iconSize / 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** «5 песен» с правильным склонением. */
@Composable
fun songsCountText(count: Int): String =
    LocalContext.current.resources.getQuantityString(R.plurals.playlist_songs_count, count, count)

/** Стиль подписи «N песен»: увеличенная высота строки, чтобы цифры не обрезались сверху. */
@Composable
fun countTextStyle(): TextStyle = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
