package io.keeppro

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.Image
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Create a [CroppableState] that is remembered across compositions.
 *
 * Changes to the provided values for [minScale] and [maxScale] will **not** result
 * in the state being recreated or changed in any way if it has already been created.
 *
 * @param minScale the minimum scale value for [CroppableState.minScale]
 * @param maxScale the maximum scale value for [CroppableState.maxScale]
 */
@Composable
fun rememberCroppableState(
    @FloatRange(from = 0.0) minScale: Float = 1f,
    @FloatRange(from = 0.0) maxScale: Float = Float.MAX_VALUE,
    contentScale: ContentScale = ContentScale.Fit,
): CroppableState = rememberSaveable(
    saver = CroppableState.Saver
) {
    CroppableState(
        minScale = minScale,
        maxScale = maxScale,
        contentScale = contentScale
    )
}

/**
 * A state object that can be hoisted to observe scale and translate for [Croppable].
 *
 * In most cases, this will be created via [rememberCroppableState].
 *
 * @param minScale the minimum scale value for [CroppableState.minScale]
 * @param maxScale the maximum scale value for [CroppableState.maxScale]
 * @param initialTranslateX the initial translateX value for [CroppableState.translateX]
 * @param initialTranslateY the initial translateY value for [CroppableState.translateY]
 * @param initialScale the initial scale value for [CroppableState.scale]
 */
@Stable
class CroppableState(
    @FloatRange(from = 0.0) val minScale: Float = 1f,
    @FloatRange(from = 0.0) val maxScale: Float = Float.MAX_VALUE,
    @FloatRange(from = 0.0) initialTranslateX: Float = 0f,
    @FloatRange(from = 0.0) initialTranslateY: Float = 0f,
    @FloatRange(from = 0.0) initialScale: Float = minScale,
    private val contentScale: ContentScale = ContentScale.Fit,
) {
    private val velocityTracker = VelocityTracker()
    private val _translateY = Animatable(initialTranslateY)
    private val _translateX = Animatable(initialTranslateX)
    private val _scale: Animatable<Float, AnimationVector1D> = Animatable(initialScale)

    private val krop = Krop()
    private var containerWidth = 0
    private var containerHeight = 0
    private var childWidth = 0
    private var childHeight = 0
    private var startPoint = Offset(0f, 0f)
    private var cropArea = Offset(0f, 0f) //width and height
    private var originalImageWidth: Int = 0
    /**
     * The window of the crop area from user's perspective
     */
    val cropWindow: MutableState<Rect> = mutableStateOf(Rect(0f, 0f, 0f, 0f))

    init {
        require(minScale < maxScale) { "minScale must be < maxScale" }
    }

    /**
     * The current scale value for [Croppable]
     */
    @get:FloatRange(from = 0.0)
    val scale: Float
        get() = _scale.value

    /**
     * The current translateY value for [Croppable]
     */
    @get:FloatRange(from = 0.0)
    val translateY: Float
        get() = _translateY.value

    /**
     * The current translateX value for [Croppable]
     */
    @get:FloatRange(from = 0.0)
    val translateX: Float
        get() = _translateX.value

    internal val zooming: Boolean
        get() = scale > minScale

    /**
     * Instantly sets scale of [Croppable] to given [scale]
     */
    suspend fun snapScaleTo(scale: Float) = coroutineScope {
        _scale.snapTo(scale.coerceIn(minimumValue = minScale, maximumValue = maxScale))
    }

    /**
     * Animates scale of [Croppable] to given [scale]
     */
    suspend fun animateScaleTo(
        scale: Float,
        animationSpec: AnimationSpec<Float> = spring(),
        initialVelocity: Float = 0f,
    ) = coroutineScope {
        _scale.animateTo(
            targetValue = scale.coerceIn(minimumValue = minScale, maximumValue = maxScale),
            animationSpec = animationSpec,
            initialVelocity = initialVelocity,
        )
    }

    suspend fun animateDoubleTap(
        scale: Float,
        distance: Offset,
        animationSpec: AnimationSpec<Float> = spring(),
        initialVelocity: Float = 0f,
    ) = coroutineScope {

        launch {
            animateScaleTo(scale, animationSpec, initialVelocity)
        }
        launch {
            _translateX.animateTo(_translateX.value + distance.x, animationSpec, initialVelocity)
        }
        launch {
            _translateY.animateTo(_translateY.value + distance.y, animationSpec, initialVelocity)
        }
    }

    internal suspend fun drag(dragDistance: Offset) = coroutineScope {
        launch {
            _translateY.snapTo((_translateY.value + dragDistance.y))
        }
        launch {
            _translateX.snapTo((_translateX.value + dragDistance.x))
        }
    }

    internal suspend fun updateBounds() = coroutineScope {
        val (maxX: Float, maxY: Float) = getBounds()
        if (maxX.isNaN() || maxY.isNaN()) return@coroutineScope
        _translateY.updateBounds(-maxY, maxY)
        _translateX.updateBounds(-maxX, maxX)
    }

    fun getBounds(): Pair<Float, Float> { //return boundary for translationX,Y
        val (maxX, maxY) = (childWidth * scale - containerWidth).coerceAtLeast(0F) / 2F to
                (childHeight * scale - containerHeight).coerceAtLeast(0F) / 2F
        return maxX to maxY
    }

    internal suspend fun onZoomChange(zoomChange: Float) = snapScaleTo(scale * zoomChange)

    override fun toString(): String = "CroppableState(" +
            "minScale=$minScale, " +
            "maxScale=$maxScale, " +
            "translateY=$translateY" +
            "translateX=$translateX" +
            "scale=$scale" +
            ")"

    fun updateContainerAndChildSize(
        maxWidth: Int,
        maxHeight: Int,
        childWidth: Int,
        childHeight: Int
    ) {
        containerWidth = maxWidth
        containerHeight = maxHeight
        this.childWidth = childWidth
        this.childHeight = childHeight

        calculateCropArea()
    }

    fun updateContainer(maxWidth: Int, maxHeight: Int) {
        containerWidth = maxWidth
        containerHeight = maxHeight
        calculateCropArea()
    }

    fun calculateCropArea() {
        val frameWidth: Float = if(originalImageWidth == childWidth) childWidth.toFloat() else originalImageWidth.toFloat()
        val frameHeight: Float = if(originalImageWidth == childWidth) childHeight.toFloat() else childHeight * (originalImageWidth / childWidth.toFloat() )
        startPoint = Offset(
            (max(0f, (childWidth * scale - containerWidth) / 2) - translateX).coerceAtLeast(0f),
            (max(0f, (childHeight * scale - containerHeight) / 2) - translateY).coerceAtLeast(0f)
        )
        cropArea = Offset(
            min(childWidth * scale, containerWidth.toFloat()),
            min(childHeight * scale, containerHeight.toFloat())
        )

        cropWindow.value = Rect(
            offset = Offset(
                if(childWidth * scale < containerWidth.toFloat()) (childWidth * scale - containerWidth) / 2 else 0f,
                if (childHeight * scale < containerHeight.toFloat()) (childHeight * scale - containerHeight) / 2 else 0f,
            ),
            size = Size(
                cropArea.x,
                cropArea.y
            )
        )
    }

    fun crop(): ByteArray {
        val originFrameRatio = originalImageWidth / childWidth.toFloat()
        val startX = (originFrameRatio * startPoint.x / scale).toInt()
        val startY = (originFrameRatio * startPoint.y / scale).toInt()
        val width = (originFrameRatio * cropArea.x / scale).toInt()
        val height = (originFrameRatio * cropArea.y / scale).toInt()
        return krop.crop(startX, startY, width, height)
    }

    fun prepareImage(image: Image) {
        krop.prepareImage(image)

        originalImageWidth = image.width
    }

    internal fun addPosition(timeMillis: Long, position: Offset) {
        velocityTracker.addPosition(timeMillis = timeMillis, position = position)
    }

    internal fun resetTracking() {
        velocityTracker.resetTracking()
    }

    internal fun isHorizontalDragFinis(dragDistance: Offset): Boolean {
        val lowerBounds = _translateX.lowerBound ?: return false
        val upperBounds = _translateX.upperBound ?: return false
        if (lowerBounds == 0f && upperBounds == 0f) return true

        val newPosition = _translateX.value + dragDistance.x
        if (newPosition <= lowerBounds) {
            return true
        }

        if (newPosition >= upperBounds) {
            return true
        }
        return false
    }

    internal fun isVerticalDragFinish(dragDistance: Offset): Boolean {
        val lowerBounds = _translateY.lowerBound ?: return false
        val upperBounds = _translateY.upperBound ?: return false
        if (lowerBounds == 0f && upperBounds == 0f) return true

        val newPosition = _translateY.value + dragDistance.y
        if (newPosition <= lowerBounds) {
            return true
        }

        if (newPosition >= upperBounds) {
            return true
        }
        return false
    }


    private suspend fun fling(velocity: Offset) = coroutineScope {
        launch {
            _translateY.animateDecay(
                velocity.y / 2f,
                exponentialDecay()
            )
        }
        launch {
            _translateX.animateDecay(
                velocity.x / 2f,
                exponentialDecay()
            )
        }
    }


    internal suspend fun dragEnd() {
        val velocity = velocityTracker.calculateVelocity()
        fling(Offset(velocity.x, velocity.y))
    }

    companion object {
        /**
         * The default [Saver] implementation for [CroppableState].
         */
        val Saver: Saver<CroppableState, *> = listSaver(
            save = {
                listOf(
                    it.translateX,
                    it.translateY,
                    it.scale,
                    it.minScale,
                    it.maxScale,
                )
            },
            restore = {
                CroppableState(
                    initialTranslateX = it[0],
                    initialTranslateY = it[1],
                    initialScale = it[2],
                    minScale = it[3],
                    maxScale = it[4],
                )
            }
        )
    }
}

data class CropHint(
    val backgroundColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val gridLineColor: Color?
){

    companion object{
        val Default = CropHint(
            backgroundColor = Color(0xFFBABABA),
            borderColor = Color(0xFFBABABA),
            borderWidth = 2.dp,
            gridLineColor = Color.Black,
        )
    }
}