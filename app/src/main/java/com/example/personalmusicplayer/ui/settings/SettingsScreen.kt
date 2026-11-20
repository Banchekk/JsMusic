package com.example.personalmusicplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.personalmusicplayer.ui.theme.AppTheme
import com.example.personalmusicplayer.ui.theme.ThemeGroup

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val selected by viewModel.theme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeGroup.entries.forEachIndexed { index, group ->
            if (index > 0) Spacer(Modifier.size(12.dp))
            Text(stringResource(group.titleRes), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(4.dp))
            AppTheme.entries.filter { it.group == group }.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(theme.card)
                        .clickable(role = Role.RadioButton) { viewModel.selectTheme(theme) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(Modifier.size(28.dp).clip(CircleShape).background(theme.primary))
                    // Цвета строки берутся из её собственной темы, чтобы текст читался на любом фоне.
                    Text(stringResource(theme.labelRes), Modifier.weight(1f), color = theme.text)
                    RadioButton(
                        selected = theme == selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = theme.primary,
                            unselectedColor = theme.textMuted,
                        ),
                    )
                }
            }
        }
    }
}
