package dev.swrc.bee

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.swrc.bee.constants.Response
import dev.swrc.bee.constants.ScoreState
import dev.swrc.bee.ui.theme.BeeTheme
import dev.swrc.bee.viewmodel.ClusterViewModel
import dev.swrc.bee.viewmodel.GameViewModel
import dev.swrc.bee.viewmodel.GameViewModelFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Main(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(preview: Boolean, viewModel: ClusterViewModel = viewModel()) {
    println("Letters: " + viewModel.letters())
    val gameViewModel: GameViewModel = if(preview) viewModel() else viewModel(factory = GameViewModelFactory(LocalContext.current.applicationContext as Application, viewModel.letters()))
    val hexList by viewModel.hexagons.collectAsState()
    val text = viewModel.currentInput
    val errorMessage by viewModel.errorMessage
    val isPopupVisible by viewModel.isPopupVisible
    val isErrorVisible by viewModel.isErrorVisible
    val successMessage by viewModel.successMessage
    val scoreState by gameViewModel.scoreState
    val percentage by gameViewModel.percentToNextRank
    val foundWords by gameViewModel.foundWords.collectAsState(initial = emptyList())

    var shake by remember { mutableStateOf(false) }
    BeeTheme {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.bee_happy3),
                                contentDescription = "Bee",
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Spelling Bee")
                        }
                    },
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScoreBox(scoreState = scoreState)
                        Spacer(modifier = Modifier.height(16.dp))
                        ExpandableWordList(foundWords)
                        Spacer(modifier = Modifier.weight(1f))
                        ErrorMessagePopup(Modifier.zIndex(1f), errorMessage, isErrorVisible)
                        SuccessMessagePopup(Modifier.zIndex(1f), successMessage, isPopupVisible)
                        CurrentWordBox(
                            value = text,
                            highlightChar = viewModel.centerLetter[0],
                            shakeTrigger = shake,
                            onShakeFinished = {
                                shake = false
                                viewModel.clearWord()
                            }
                        )

                        HexagonCluster(
                            hexSize = 100.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) { index, hexSize, color, center, modifier ->
                            HexagonBox(
                                modifier = modifier,
                                hexSize = hexSize,
                                color = color,
                                center = center,
                                onClick = {
                                    viewModel.hidePopups()
                                    if(shake) {
                                        shake = false
                                        viewModel.clearWord()
                                    }
                                    val reachedLimit = viewModel.addLetter(index)
                                    if(reachedLimit) {
                                        viewModel.displayErrorMessage(Response.TOO_LONG)
                                        shake = true
                                    }
                                },
                                character = hexList[index].label,
                                letters = viewModel.letters()
                            )
                        }
                        Row(
                            modifier = Modifier.padding(bottom = 64.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                viewModel.removeLetter()
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ) { Text("Delete") }
                            Spacer(Modifier
                                .height(8.dp)
                                .width(8.dp))
                            Button(onClick = {
                                viewModel.shuffleLetters()
                                gameViewModel.print()
                            },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )) { Text(text = "⟳",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )}
                            Spacer(Modifier
                                .height(8.dp)
                                .width(8.dp))
                            Button(onClick = {
                                if(text.isEmpty()) return@Button
                                val validWord = gameViewModel.submitWord(text, viewModel.letters(), viewModel.centerLetter)
                                if(validWord.valid) {
                                    val points = gameViewModel.computePoints(viewModel.currentInput, viewModel.letters())
                                    viewModel.displaySuccessMessage(points = points)
                                    viewModel.clearWord()
                                } else {
                                    shake = true
                                    viewModel.displayErrorMessage(validWord.response)
                                }
                            },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )) { Text("Enter") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessagePopup(modifier: Modifier, errorMessage: String?, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250)),
        modifier = modifier
            .padding(16.dp)
    ) {
        Text(
            text = errorMessage.orEmpty(),
            color = MaterialTheme.colorScheme.onError,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessMessagePopup(modifier: Modifier, message: String?, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(250)),
        modifier = modifier
            .padding(16.dp)
    ) {
        Text(
            text = message.orEmpty(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun ScoreBox(scoreState: ScoreState) {
    Box(Modifier
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = scoreState.ranking.rankName,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 16.dp),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                DiscreteProgressBar(
                    steps = 8,
                    scoreState = scoreState,
                    barHeight = 2.dp,
                    circleRadius = 6.dp
                )
            }
        }
    }
}

@Composable
fun ExpandableWordList(
    words: List<String>,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 1,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    var expanded by remember { mutableStateOf(false) }
    val text = remember(words) { words.joinToString(", ") }

    Box(Modifier
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surfaceContainer)
        .clickable { expanded = !expanded }) {
        Text(
            text = text,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            style = textStyle,
            modifier = modifier
                .padding(8.dp)
        )
    }
}

@Composable
fun DiscreteProgressBar(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(24.dp),
    steps: Int = 8,
    scoreState: ScoreState,
    barHeight: Dp = 2.dp,
    circleRadius: Dp = 6.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
) {
    val textMeasurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = MaterialTheme.typography.bodySmall.fontSize
    )
    val textToDraw = scoreState.score.toString()
    val textLayoutResult = remember(textToDraw) {
        textMeasurer.measure(textToDraw, style)
    }
    val currentStep = scoreState.ranking.ordinal
    Canvas(modifier = modifier) {
        val weight = size.width
        val height = size.height
        val halfHeight = height / 2
        val progressBarHeight = barHeight.toPx()
        val radius = circleRadius.toPx()

        val positions = List(steps) { i ->
            Offset(x = i * weight / (steps - 1), y = halfHeight)
        }

        drawLine(
            color = inactiveColor,
            start = positions.first(),
            end   = positions.last(),
            strokeWidth = progressBarHeight
        )

        val endPos = positions.getOrNull(currentStep.coerceIn(0, steps - 1))
            ?: positions.first()
        drawLine(
            color = activeColor,
            start = positions.first(),
            end   = endPos,
            strokeWidth = progressBarHeight
        )

        positions.forEachIndexed { index, pos ->
            drawCircle(
                color = if (index <= currentStep) activeColor else inactiveColor,
                radius = if(index == currentStep) radius * 2 else radius,
                center = pos
            )
            if(index == currentStep) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = textToDraw,
                    style = style,
                    topLeft = Offset(
                        x = pos.x - textLayoutResult.size.width / 2,
                        y = pos.y - textLayoutResult.size.height / 2,
                    )
                )
            }
        }
    }
}

@Composable
fun CurrentWordBox(
    value: String,
    highlightChar: Char,
    modifier: Modifier = Modifier,
    shakeTrigger: Boolean = false,
    onShakeFinished: () -> Unit = {},
) {
    val shakeAnim = remember { Animatable(0f) }
    val maxOffsetPx = with(LocalDensity.current) { 8.dp.toPx() }

    var showCursor by remember { mutableStateOf(true) }
    val textStyle = MaterialTheme.typography.headlineLarge.copy(
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    val cursorHeight = with(LocalDensity.current) { textStyle.fontSize.toDp() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger) {
            val shakeJob = launch {
                shakeAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 75,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }

            delay(750)
            shakeJob.cancel()
            shakeAnim.snapTo(0f)
            onShakeFinished()
        }
    }

    var coloredCursor by remember { mutableStateOf(false) }
    val cursorColor = if(!coloredCursor) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier
            .padding(16.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                coloredCursor = !coloredCursor
            }
            .offset {
                val dx = (shakeAnim.value * 2f - 1f) * maxOffsetPx
                IntOffset(dx.roundToInt(), 0)
            }
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val coloredText = remember(value, highlightChar) {
            buildAnnotatedString {
                value.forEach { c ->
                    if (c == highlightChar) {
                        pushStyle(SpanStyle(color = cursorColor))
                        append(c)
                        pop()
                    } else {
                        append(c)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = coloredText,
                style = textStyle
            )
            Box(
                Modifier
                    .width(2.dp)
                    .height(cursorHeight)
                    .alpha(if (showCursor) 1f else 0f)
                    .background(cursorColor)
            )
        }
    }
}

@Composable
fun HexagonCluster(
    modifier: Modifier = Modifier,
    hexSize: Dp,
    spacing: Dp = 4.dp,
    content: @Composable (index: Int, hexSize: Dp, color: Color, center: Boolean, modifier: Modifier) -> Unit
) {
    val hexWidthPx = with(LocalDensity.current) { hexSize.toPx() }
    val hexHeightPx = hexWidthPx * sqrt(3f) / 2f
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }

    val directions = listOf(
        Offset(-(-hexWidthPx * 0.75f - spacingPx), +hexHeightPx * 0.50f + spacingPx),
        Offset(0f, +hexHeightPx + 2 * spacingPx),
        Offset(-hexWidthPx * 0.75f - spacingPx, +hexHeightPx * 0.50f + spacingPx),
        Offset(-(-hexWidthPx * 0.75f - spacingPx), -hexHeightPx * 0.50f - spacingPx),
        Offset(0f, -hexHeightPx - 2 * spacingPx),
        Offset(-hexWidthPx * 0.75f - spacingPx, -hexHeightPx * 0.50f - spacingPx),
    )

    Box(
        modifier = modifier
            .wrapContentSize()
            .padding(hexSize)
    ) {
        Box(modifier = Modifier.offset { IntOffset(0, 0) }) {
            content(0, hexSize, MaterialTheme.colorScheme.inversePrimary, true, Modifier)
        }

        directions.forEachIndexed { i, offset ->
            Box(modifier = Modifier.offset {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            }) {
                content(
                    i + 1,
                    hexSize,
                    MaterialTheme.colorScheme.secondaryContainer,
                    false,
                    Modifier
                )
            }
        }
    }
}


@Composable
fun HexagonBox(
    modifier: Modifier,
    hexSize: Dp,
    onClick: () -> Unit = {},
    center: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    character: String,
    letters: String
) {
    val textColor = if (center) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val roundedSquare = RoundedPolygon(numVertices = 4)
    val roundedHexagon = RoundedPolygon(numVertices = 6)
    val morph = remember { Morph(roundedHexagon, roundedSquare) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        label = "progress",
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)
    )

    val density = LocalDensity.current
    val hexSizePx = with(density) { hexSize.toPx() }

    Box(
        modifier = modifier
            .size(hexSize)
            .pointerInput(Unit) {
                coroutineScope {
                    detectTapGestures(
                        onPress = { offset ->
                            val radius = hexSizePx / 2
                            val centerX = hexSizePx / 2
                            val centerY = hexSizePx / 2

                            val hexPath = RoundedPolygon(
                                numVertices = 6,
                                radius = radius,
                                centerX = centerX,
                                centerY = centerY
                            ).toPath().asComposePath()

                            if (isPointInsideComposePath(hexPath, offset)) {
                                val press = PressInteraction.Press(offset)
                                launch { interactionSource.emit(press) }

                                try {
                                    awaitRelease()
                                    launch { interactionSource.emit(PressInteraction.Release(press)) }
                                    onClick()
                                } catch (e: CancellationException) {
                                    launch { interactionSource.emit(PressInteraction.Cancel(press)) }
                                }
                            }
                        }
                    )
                }
            }
            .clip(MorphPolygonShape(morph, animatedProgress))
            .drawWithCache {
                val radius = size.minDimension / 2
                val path = RoundedPolygon(
                    numVertices = 6,
                    radius = radius,
                    centerX = size.width / 2,
                    centerY = size.height / 2
                ).toPath().asComposePath()
                onDrawBehind {
                    drawPath(path, color)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = character,
            animationSpec = tween(durationMillis = 300)) { txt ->
            CharWidthText(Modifier, txt, letters.toList(), MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun CharWidthText(
    modifier: Modifier = Modifier,
    text: String,
    candidates: List<Char>,
    textStyle: TextStyle = LocalTextStyle.current
) {
    val textMeasurer = rememberTextMeasurer()
    val maxWidthPx = remember(candidates, textStyle) {
        candidates.maxOfOrNull { char ->
            textMeasurer
                .measure(AnnotatedString(char.toString()), style = textStyle)
                .size
                .width
                .toFloat()
        } ?: 0f
    }

    val maxWidthDp = with(LocalDensity.current) { maxWidthPx.toDp() }

    Text(
        text = text,
        style = textStyle,
        modifier = modifier
            .width(maxWidthDp)
    )
}

fun isPointInsideComposePath(path: Path, offset: Offset): Boolean {
    val androidPath = android.graphics.Path()
    path.asAndroidPath().let { androidPath.set(it) }
    val bounds = android.graphics.RectF()
    androidPath.computeBounds(bounds, true)

    val region = android.graphics.Region().apply {
        setPath(
            androidPath,
            android.graphics.Region(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt()
            )
        )
    }

    return region.contains(offset.x.toInt(), offset.y.toInt())
}

class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {

    private val matrix = Matrix()
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)

        val path = morph.toPath(progress = percentage).asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun BeePreview2() {
    Main(true)
}