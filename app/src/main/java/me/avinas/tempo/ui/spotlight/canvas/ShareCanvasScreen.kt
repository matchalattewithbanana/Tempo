package me.avinas.tempo.ui.spotlight.canvas

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.CaptureWrapper
import me.avinas.tempo.ui.components.rememberCaptureController
import me.avinas.tempo.ui.components.rememberCardShareActions
import me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter
import me.avinas.tempo.ui.spotlight.SpotlightCardData
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.data.analytics.TempoFeature

/** Composes shareable card collages on an Instagram Story 9:16 canvas (1080x1920). */
@Composable
fun ShareCanvasScreen(
    snapshot: SpotlightCardData?,
    onClose: () -> Unit,
    viewModel: ShareCanvasViewModel = hiltViewModel()
) {
    val appearancePalette = LocalTempoPalette.current
    val coroutineScope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    val shareActions = rememberCardShareActions(captureController, "tempo-canvas.png", TempoFeature.SHARE_CANVAS)
    var showExportMenu by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val generatedCards by viewModel.allCards.collectAsState()
    val allCards = remember(generatedCards, snapshot) {
        if (snapshot == null) generatedCards
        else listOf(snapshot) + generatedCards.filterNot { it.id == snapshot.id }
    }
    val isLoading by viewModel.isLoading.collectAsState()
    
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val storyAspectRatio = 9f / 16f

    // Size by height first; fall back to width when that overflows the side margins.
    val maxCanvasHeight = screenHeight * 0.85f
    val maxCanvasWidth = screenWidth * 0.90f // 5% margin on each side

    val widthByHeight = maxCanvasHeight * storyAspectRatio

    val (canvasWidth, canvasHeight) = if (widthByHeight > maxCanvasWidth) {
        maxCanvasWidth to (maxCanvasWidth / storyAspectRatio)
    } else {
        widthByHeight to maxCanvasHeight
    }
    
    var canvasState by viewModel.editorState
    val editorReady by viewModel.editorReady
    val viewport = with(density) { CanvasViewport(canvasWidth.toPx(), canvasHeight.toPx(), this.density) }
    LaunchedEffect(allCards, isLoading, snapshot, viewport) {
        if (!isLoading && viewport.width > 0 && viewport.height > 0) {
            viewModel.prepareEditor(viewport, allCards, snapshot ?: viewModel.getInitialCard())
        }
    }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var savingDraft by remember { mutableStateOf(false) }
    var saveFailed by rememberSaveable { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        if (!savingDraft && !shareActions.busy) {
            if (viewModel.isDirty()) showExitDialog = true else onClose()
        }
    }
    BackHandler(enabled = !canvasState.showTextEditor && !showExitDialog, onBack = requestClose)

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { if (!savingDraft) showExitDialog = false },
            title = { Text(stringResource(R.string.canvas_draft_exit_title)) },
            text = {
                Text(stringResource(if (saveFailed) R.string.canvas_draft_save_failed else R.string.canvas_draft_exit_body))
            },
            confirmButton = {
                TextButton(enabled = !savingDraft, onClick = {
                    savingDraft = true
                    saveFailed = false
                    coroutineScope.launch {
                        try {
                            if (viewModel.saveDraft()) onClose() else saveFailed = true
                        } finally {
                            savingDraft = false
                        }
                    }
                }) { Text(stringResource(R.string.canvas_draft_save)) }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(enabled = !savingDraft, onClick = onClose) {
                        Text(stringResource(R.string.canvas_draft_discard))
                    }
                    TextButton(enabled = !savingDraft, onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.canvas_draft_keep_editing))
                    }
                }
            },
        )
    }

    me.avinas.tempo.ui.components.DeepOceanBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!editorReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = appearancePalette.accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading your cards...",
                        color = appearancePalette.mutedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp, bottom = 100.dp), // Add safe areas for top bar and bottom dock
                contentAlignment = Alignment.Center
            ) {
                CaptureWrapper(
                    controller = captureController,
                    modifier = Modifier
                        .width(canvasWidth)
                        .height(canvasHeight)
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color.Black.copy(alpha = 0.5f),
                            ambientColor = Color.Black.copy(alpha = 0.3f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(getBackgroundModifier(canvasState.selectedBackground))
                            // Tap on empty canvas area to deselect
                            .pointerInput(Unit) {
                                detectTapGestures { 
                                    canvasState = canvasState.copy(
                                        selectedItemId = null,
                                        selectedItemType = SelectedItemType.None
                                    )
                                }
                            }
                    ) {
                        // Grid pattern (only on Deep Ocean)
                        if (canvasState.selectedBackground.id == "deep_ocean") {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridSize = 30.dp.toPx()
                                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f))
                                var x = 0f
                                while (x < size.width) {
                                    drawLine(Color.White.copy(alpha = 0.03f), Offset(x, 0f), Offset(x, size.height), 1f, pathEffect = pathEffect)
                                    x += gridSize
                                }
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(Color.White.copy(alpha = 0.03f), Offset(0f, y), Offset(size.width, y), 1f, pathEffect = pathEffect)
                                    y += gridSize
                                }
                            }
                        }
                        
                        // Render all items sorted by zIndex
                        val allItems = (canvasState.canvasItems.map { it to "card" } + 
                                       canvasState.textItems.map { it to "text" })
                            .sortedBy { 
                                when (val item = it.first) {
                                    is CanvasCardItem -> item.zIndex
                                    is CanvasTextItem -> item.zIndex
                                    else -> 0
                                }
                            }
                        
                        allItems.forEach { (item, type) ->
                            when {
                                type == "card" && item is CanvasCardItem -> {
                                    CanvasCard(
                                        item = item,
                                        isSelected = canvasState.selectedItemId == item.id && 
                                                    canvasState.selectedItemType == SelectedItemType.Card,
                                        canvasSize = androidx.compose.ui.unit.IntSize(
                                            with(density) { canvasWidth.roundToPx() },
                                            with(density) { canvasHeight.roundToPx() }
                                        ),
                                        onSelect = {
                                            canvasState = canvasState.copy(
                                                selectedItemId = item.id,
                                                selectedItemType = SelectedItemType.Card,
                                                canvasItems = canvasState.canvasItems.map { 
                                                    if (it.id == item.id) it.copy(zIndex = canvasState.maxZIndex + 1)
                                                    else it
                                                }
                                            )
                                        },
                                        onTransform = { offsetX, offsetY, scale, rotation ->
                                            canvasState = canvasState.copy(
                                                canvasItems = canvasState.canvasItems.map {
                                                    if (it.id == item.id) it.copy(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = rotation)
                                                    else it
                                                }
                                            )
                                        },
                                        onDelete = {
                                            canvasState = canvasState.copy(
                                                canvasItems = canvasState.canvasItems.filter { it.id != item.id },
                                                selectedItemId = null,
                                                selectedItemType = SelectedItemType.None
                                            )
                                        },
                                        modifier = Modifier.zIndex(item.zIndex.toFloat())
                                    )
                                }
                                type == "text" && item is CanvasTextItem -> {
                                    CanvasText(
                                        item = item,
                                        isSelected = canvasState.selectedItemId == item.id && 
                                                    canvasState.selectedItemType == SelectedItemType.Text,
                                        onSelect = {
                                            canvasState = canvasState.copy(
                                                selectedItemId = item.id,
                                                selectedItemType = SelectedItemType.Text,
                                                textItems = canvasState.textItems.map { 
                                                    if (it.id == item.id) it.copy(zIndex = canvasState.maxZIndex + 1)
                                                    else it
                                                }
                                            )
                                        },
                                        onTransform = { offsetX, offsetY, scale, rotation ->
                                            canvasState = canvasState.copy(
                                                textItems = canvasState.textItems.map {
                                                    if (it.id == item.id) it.copy(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = rotation)
                                                    else it
                                                }
                                            )
                                        },
                                        onDelete = {
                                            canvasState = canvasState.copy(
                                                textItems = canvasState.textItems.filter { it.id != item.id },
                                                selectedItemId = null,
                                                selectedItemType = SelectedItemType.None
                                            )
                                        },
                                        onEdit = {
                                            canvasState = canvasState.copy(
                                                showTextEditor = true,
                                                editingTextId = item.id
                                            )
                                        },
                                        modifier = Modifier.zIndex(item.zIndex.toFloat())
                                    )
                                }
                            }
                        }
                        
                        // Empty state
                        if (canvasState.canvasItems.isEmpty() && canvasState.textItems.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "Tap + to add cards or text",
                                        color = Color.White.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        
                        // Watermark
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "Tempo Spotlight",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(appearancePalette.surfaceSunken.copy(alpha = 0.8f))
                    .clickable(onClick = requestClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = appearancePalette.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Share Story",
                    style = MaterialTheme.typography.titleMedium,
                    color = appearancePalette.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = SpotlightPeriodFormatter.periodLabel(viewModel.timeRange),
                    style = MaterialTheme.typography.labelMedium,
                    color = appearancePalette.mutedText,
                )
            }

            // Invisible Spacer to balance the Close button for perfect centering
            Spacer(modifier = Modifier.size(44.dp))
        }
        
        // Floating Dock Toolbar
        if (editorReady) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .height(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    variant = GlassCardVariant.HighProminence,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp), // reduced padding, handled by items
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        
                        // Tools Group - Takes available weight
                        Row(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockItem(
                                icon = Icons.Default.Add,
                                label = "Card",
                                enabled = canvasState.canAddMoreCards,
                                modifier = Modifier.weight(1f),
                                onClick = { canvasState = canvasState.copy(showCardPicker = true) }
                            )
                            
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(appearancePalette.onSurface.copy(alpha = 0.1f))
                            )
                            
                            DockItem(
                                icon = Icons.Default.TextFields,
                                label = "Text",
                                enabled = canvasState.canAddMoreText,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val centerX = with(density) { (canvasWidth / 2).toPx() }
                                    val centerY = with(density) { (canvasHeight / 2).toPx() }
                                    val newText = CanvasTextItem(
                                        offsetX = centerX,
                                        offsetY = centerY,
                                        zIndex = canvasState.maxZIndex + 1
                                    )
                                    canvasState = canvasState.copy(
                                        textItems = canvasState.textItems + newText,
                                        selectedItemId = newText.id,
                                        selectedItemType = SelectedItemType.Text,
                                        showTextEditor = true,
                                        editingTextId = newText.id
                                    )
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(appearancePalette.onSurface.copy(alpha = 0.1f))
                            )

                            DockItem(
                                icon = Icons.Default.Palette,
                                label = "Bg",
                                enabled = true,
                                modifier = Modifier.weight(1f),
                                onClick = { canvasState = canvasState.copy(showBackgroundPicker = true) }
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                        
                        Box {
                            Button(
                                enabled = !shareActions.busy && (canvasState.canvasItems.isNotEmpty() || canvasState.textItems.isNotEmpty()),
                                onClick = { showExportMenu = true },
                            ) {
                                if (shareActions.busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_export_options))
                            }
                            DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                                fun clearSelection() {
                                    canvasState = canvasState.copy(selectedItemId = null, selectedItemType = SelectedItemType.None)
                                    showExportMenu = false
                                }
                                DropdownMenuItem(text = { Text(stringResource(R.string.share_image)) }, onClick = {
                                    clearSelection()
                                    coroutineScope.launch {
                                        delay(150)
                                        shareActions.share()
                                    }
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.share_save_png)) }, onClick = {
                                    clearSelection()
                                    shareActions.save()
                                })
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Card picker sheet
    if (canvasState.showCardPicker) {
        CardPickerSheet(
            availableCards = canvasState.availableCards,
            cardsOnCanvas = canvasState.canvasItems.map { it.cardData },
            canAddMore = canvasState.canAddMoreCards,
            onCardSelected = { selectedCard ->
                val newX = 20f + (canvasState.canvasItems.size * 25f)
                val newY = 100f + (canvasState.canvasItems.size * 40f)
                // Calculate scale same as initial card (50% of width)
                val targetCardWidthPx = with(density) { canvasWidth.toPx() * 0.5f }
                val baseCardWidthPx = with(density) { 320.dp.toPx() }
                val dynamicScale = targetCardWidthPx / baseCardWidthPx

                val newItem = CanvasCardItem(
                    cardData = selectedCard,
                    offsetX = newX,
                    offsetY = newY,
                    scale = dynamicScale,
                    zIndex = canvasState.maxZIndex + 1
                )
                canvasState = canvasState.copy(
                    canvasItems = canvasState.canvasItems + newItem,
                    showCardPicker = false,
                    selectedItemId = newItem.id,
                    selectedItemType = SelectedItemType.Card
                )
            },
            onDismiss = { canvasState = canvasState.copy(showCardPicker = false) }
        )
    }
    
    // Background picker sheet
    if (canvasState.showBackgroundPicker) {
        BackgroundPickerSheet(
            selectedBackground = canvasState.selectedBackground,
            onBackgroundSelected = { bg ->
                canvasState = canvasState.copy(selectedBackground = bg, showBackgroundPicker = false)
            },
            onDismiss = { canvasState = canvasState.copy(showBackgroundPicker = false) }
        )
    }
    
    // Instagram-style inline text editor (full screen overlay)
    if (canvasState.showTextEditor && canvasState.editingTextId != null) {
        val editingText = canvasState.textItems.find { it.id == canvasState.editingTextId }
        if (editingText != null) {
            InlineTextEditor(
                textItem = editingText,
                onTextChanged = { updatedText ->
                    canvasState = canvasState.copy(
                        textItems = canvasState.textItems.map {
                            if (it.id == updatedText.id) updatedText else it
                        }
                    )
                },
                onDismiss = {
                    canvasState = canvasState.copy(
                        showTextEditor = false,
                        editingTextId = null
                    )
                }
            )
        }
    }
}

@Composable
private fun getBackgroundModifier(background: CanvasBackground): Modifier {
    return when (background.id) {
        "holographic" -> {
             Modifier.background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFF9A8D4), // Pink
                        Color(0xFFC4B5FD), // Violet
                        Color(0xFF67E8F9), // Cyan
                        Color(0xFFF0ABFC), // Fuschia
                        Color(0xFFF9A8D4)  // Pink (loop)
                    )
                )
            ).background(
                Brush.verticalGradient(
                   listOf(Color.White.copy(alpha = 0.1f), Color.Transparent)
                )
            )
        }
        "midnight_grain" -> {
             Modifier.background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF020617)),
                    center = Offset.Unspecified,
                    radius = 2000f
                )
            )
            // Ideally we'd add noise here, but skipping for simple Modifier composition
        }
        "sunset_blur" -> {
             Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C1D95), Color(0xFFBE185D), Color(0xFFFB923C)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
        }
        "electric_void" -> {
            Modifier.background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF7C3AED), Color(0xFF1D4ED8), Color(0xFF000000)),
                    center = Offset(500f, 500f),
                    radius = 1500f
                )
            )
        }
        "neo_mint" -> {
            Modifier.background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F766E), Color(0xFF2DD4BF), Color(0xFF064E3B))
                )
            )
        }
        "deep_ocean" -> {
             Modifier.background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF020617), Color.Black),
                    center = Offset.Unspecified,
                    radius = 2500f
                )
            )
        }
        "aurora" -> {
             Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6)),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f) // Diagonal
                )
            )
        }
        "golden_hour" -> {
             Modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFBE123C)),
                    start = Offset(0f, 0f),
                    end = Offset(0f, 2000f) // Vertical but smoother
                )
            )
        }
        "pure_black" -> Modifier.background(Color.Black)
        "charcoal" -> Modifier.background(Color(0xFF1F2937))
        "off_white" -> Modifier.background(Color(0xFFF8FAFC))
        "deep_purple" -> Modifier.background(Color(0xFF2E1065))
        else -> {
             if (background.isGradient && background.colors != null) {
                Modifier.background(Brush.verticalGradient(background.colors))
            } else {
                Modifier.background(background.solidColor ?: Color.Black)
            }
        }
    }
}

@Composable
private fun DockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) appearancePalette.onSurface else appearancePalette.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) appearancePalette.onSurface.copy(alpha = 0.9f) else appearancePalette.onSurface.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
