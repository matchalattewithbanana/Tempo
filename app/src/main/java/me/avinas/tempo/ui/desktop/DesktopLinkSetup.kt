package me.avinas.tempo.ui.desktop

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopScanningScreen(
    scannerKey: Int,
    onQrDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    var cameraFailed by remember(scannerKey) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TempoDarkBackground)
    ) {
        if (!cameraFailed) {
            key(scannerKey) {
                QrScannerView(
                    modifier = Modifier.fillMaxSize(),
                    onCameraError = { cameraFailed = true },
                    onQrDetected = onQrDetected,
                )
            }
        }

        val scannerSurface = Color(0xFF0A0E0E)
        val scannerForeground = Color.White
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val panelMaxHeight = maxHeight
            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!cameraFailed) {
                        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(minOf(maxWidth, maxHeight, 240.dp))
                                    .border(2.dp, scannerForeground, RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = scannerSurface,
                    contentColor = scannerForeground,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.heightIn(max = panelMaxHeight).verticalScroll(rememberScrollState()).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (cameraFailed) R.string.desktop_scan_camera_unavailable else R.string.desktop_scan_instruction
                            ),
                            color = scannerForeground,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        if (cameraFailed) {
                            Button(
                                onClick = { cameraFailed = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = scannerForeground,
                                    contentColor = scannerSurface,
                                ),
                            ) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = scannerForeground),
                            border = BorderStroke(1.dp, scannerForeground),
                        ) {
                            Text(stringResource(R.string.desktop_cancel_scan))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UNPAIRED state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun UnpairedContent(
    uiState: DesktopLinkUiState,
    onScanClick: () -> Unit,
    onManualConnect: (ip: String, port: String, token: String) -> Unit
) {
    // Scan button card
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(18.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.desktop_scan_qr_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.desktop_scan_qr_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.desktop_scan_button))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Manual entry (collapsible)
    ManualEntrySection(onConnect = onManualConnect)
}

@Composable
private fun ManualEntrySection(
    onConnect: (ip: String, port: String, token: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8765") }
    var token by remember { mutableStateOf("") }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.desktop_manual_entry),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                DesktopTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = stringResource(R.string.desktop_ip_hint),
                    keyboardType = KeyboardType.Uri
                )
                Spacer(modifier = Modifier.height(8.dp))
                DesktopTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = stringResource(R.string.desktop_port_hint),
                    keyboardType = KeyboardType.Number
                )
                Spacer(modifier = Modifier.height(8.dp))
                DesktopTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = stringResource(R.string.desktop_token_hint),
                    monospace = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onConnect(ip, port, token) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.desktop_connect))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAIRED state
// ─────────────────────────────────────────────────────────────────────────────

