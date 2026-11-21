package me.avinas.tempo.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.components.LocalSettingsSectionTone
import me.avinas.tempo.ui.components.TempoTileTone

/** Shared geometry only; each Settings section keeps ownership of its own content. */
@Composable
internal fun SettingsSectionList(
    state: LazyListState,
    tone: TempoTileTone,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    CompositionLocalProvider(LocalSettingsSectionTone provides tone) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                content()
                item(key = "bottom-padding") {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
