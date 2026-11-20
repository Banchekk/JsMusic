package com.example.personalmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.personalmusicplayer.R
import com.example.personalmusicplayer.data.db.SongEntity

// Тёмная карточка с белым текстом читаема во всех трёх светлых темах.
private val CardBackground = Color(0xFF2A2830)
private val NoteColor = Color(0xFFA9A6B0)
private val UnavailableColor = Color(0xFFFF8A80)

/** Единый вид песни: название и исполнитель — слева, заметка — в центре, обложка — справа. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongCard(
    song: SongEntity,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (song.isAvailable) 1f else 0.6f)
            .clip(shape)
            .background(CardBackground)
            .then(
                if (isCurrent) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .combinedClickable(
                enabled = song.isAvailable && (onClick != null || onLongClick != null),
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist ?: stringResource(R.string.library_unknown_artist),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!song.isAvailable) {
                Text(
                    text = stringResource(R.string.song_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = UnavailableColor,
                )
            }
        }
        if (!song.note.isNullOrBlank()) {
            Text(
                text = song.note,
                modifier = Modifier.weight(0.9f),
                style = MaterialTheme.typography.bodySmall,
                color = NoteColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Cover(song.albumArtUri, Modifier.size(56.dp))
    }
}

@Composable
fun Cover(
    artUri: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    iconSize: Dp = 24.dp,
) {
    // Заглушка лежит под картинкой: пока обложка грузится (или её нет), видна заглушка,
    // а готовая картинка плавно проявляется поверх неё.
    Box(modifier.clip(RoundedCornerShape(cornerRadius))) {
        CoverPlaceholder(Modifier.fillMaxSize(), iconSize)
        if (artUri != null) {
            val context = LocalContext.current
            val request = remember(artUri) {
                ImageRequest.Builder(context).data(artUri).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun CoverPlaceholder(modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}
