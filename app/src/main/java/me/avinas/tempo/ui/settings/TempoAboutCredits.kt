package me.avinas.tempo.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.avinas.tempo.BuildConfig
import me.avinas.tempo.ui.branding.LauncherLogoManager
import androidx.compose.runtime.remember
import me.avinas.tempo.R

/** The two people requested in the About UI. Upstream attribution remains in NOTICE.md. */
internal data class TempoCredit(
    val handle: String,
    val roleRes: Int,
    val profileUrl: String,
)

internal val TEMPO_ABOUT_CREDITS = listOf(
    TempoCredit(
        handle = "matchalattewithbanana",
        roleRes = R.string.about_fork_maintainer,
        profileUrl = "https://github.com/matchalattewithbanana",
    ),
    TempoCredit(
        handle = "avinaxhroy",
        roleRes = R.string.about_original_creator,
        profileUrl = "https://github.com/avinaxhroy",
    ),
)

@Composable
fun TempoAboutCredits(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val noBrowserLabel = stringResource(R.string.settings_no_browser)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Launcher assets can be adaptive icons on Android 8+; ImageView
                // renders both adaptive XML and raster variants correctly.
                AndroidView(
                    factory = { androidContext ->
                        ImageView(androidContext).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }
                    },
                    update = { view ->
                        view.setImageResource(LauncherLogoManager.selected(context).imageRes)
                    },
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.about_tempo_description),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = stringResource(R.string.about_people_title),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                TEMPO_ABOUT_CREDITS.forEachIndexed { index, credit ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(credit.profileUrl)),
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(
                                        context, noBrowserLabel,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (index == 0) Icons.Outlined.Person else Icons.Outlined.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = credit.handle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(credit.roleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.about_view_github_profile),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (index != TEMPO_ABOUT_CREDITS.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 80.dp, end = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}
