package io.keeppro

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.launch

/**
 * A croppable and zoomable layout that can handle zoom in and out with drag support, and crop image.
 *
 * @param state the state object to be used to observe the [Croppable] state.
 * @param modifier the modifier to apply to this layout.
 * @param doubleTapScale a function called on double tap gesture, will scale to returned value.
 * @param content a block which describes the content.
 * @param onTap for interaction if need onClicked action
 */
@Composable
fun Croppable(
    state: CroppableState,
    contentScale: ContentScale = ContentScale.Fit,
    enable: Boolean = true,
    onTap: (() -> Unit)? = null,
    doubleTapScale: (() -> Float)? = null,
    cropHint: CropHint? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = updateTransition(targetState = state, label = "")
    val settingScale by transition.animateFloat(label = "") { it.scale }
    val settingTranslationX by transition.animateFloat(label = "") { it.translateX }
    val settingTranslationY by transition.animateFloat(label = "") { it.translateY }
    val scope = rememberCoroutineScope()

    val boxModifier = if (cropHint != null) {
        modifier.border(cropHint.borderWidth, cropHint.borderColor).background(cropHint.backgroundColor)
    } else {
        modifier
    }.clipToBounds()
    BoxWithConstraints(modifier = boxModifier) {
        var childWidth by remember { mutableStateOf(0) }
        var childHeight by remember { mutableStateOf(0) }

        fun getBounds(updateScale: Float): Pair<Float, Float> { //return boundary for translationX,Y
            return (childWidth * updateScale - constraints.maxWidth).coerceAtLeast(0F) / 2F to
                    (childHeight * updateScale - constraints.maxHeight).coerceAtLeast(0F) / 2F
        }

        LaunchedEffect(
            childHeight,
            childWidth,
        ) {
            val (maxX, maxY) = getBounds(state.scale)
            state.updateBounds(maxX, maxY)
            state.updateContainerAndChildSize(constraints.maxWidth, constraints.maxHeight, childWidth, childHeight)
        }

        LaunchedEffect(
            childHeight,
            childWidth,
            contentScale,
            constraints.maxWidth,
            constraints.maxHeight
        ) {
            // Update scale based on contentScale and dimensions
            println("contentScale: $contentScale, constraint.maxWidth: ${constraints.maxWidth}, constraint.maxHeight: ${constraints.maxHeight}")
            println("childWidth: $childWidth, childHeight: $childHeight")
            val updatedScale = when (contentScale) {
                ContentScale.Crop -> maxOf(
                    constraints.maxWidth / childWidth.toFloat(),
                    constraints.maxHeight / childHeight.toFloat()
                )
                ContentScale.Fit -> minOf(
                    constraints.maxWidth / childWidth.toFloat(),
                    constraints.maxHeight / childHeight.toFloat()
                )
                ContentScale.FillHeight -> constraints.maxHeight / childHeight.toFloat()
                ContentScale.FillWidth -> constraints.maxWidth / childWidth.toFloat()
                else -> 1f
            }
            state.snapScaleTo(updatedScale)
        }


        val tapModifier = if (enable) {
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (onTap!= null) onTap()
                    },
                    onDoubleTap = { offset ->
                        if (doubleTapScale != null) {
                            scope.launch {
                                val diffX = offset.x - (constraints.maxWidth / 2)
                                val diffY = offset.y - (constraints.maxHeight / 2)
                                val dis = Offset(-diffX, -diffY)

                                val afterScale = doubleTapScale()
                                val (maxX, maxY) = getBounds(afterScale)
                                state.updateBounds(maxX, maxY)
                                state.animateDoubleTap(scale = afterScale, distance = dis)
                            }
                        }
                    }
                )
            }
        } else {
            Modifier
        }
        val zoomableModifier: Modifier = if (enable){
            Modifier.pointerInput(Unit){
                detectTransformGestures (panZoomLock = true){ centroid, pan, zoom, _ ->
                    val newScale = settingScale * zoom

                    val scaleDifference = newScale - settingScale
                    val adjustedOffset = centroid * scaleDifference
                    scope.launch {
                        state.onZoomChange(zoom)
                        val (maxX, maxY) = getBounds(newScale)
                        state.updateBounds(maxX, maxY)
                    }

                    scope.launch {
                        state.drag(pan - adjustedOffset)
                    }
                }
            }
        }else{
            Modifier
        }

        Box(
            modifier = Modifier
                .then(zoomableModifier)
                .then(tapModifier)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints = constraints)
                    childHeight = placeable.height
                    childWidth = placeable.width
//                    println("constraints update : $constraints")
                    state.updateContainer(constraints.maxWidth, constraints.maxHeight)
                    layout(
                        width = constraints.maxWidth,
                        height = constraints.maxHeight
                    ) {
                        placeable.placeRelativeWithLayer(
                            (constraints.maxWidth - placeable.width) / 2,
                            (constraints.maxHeight - placeable.height) / 2,
                            layerBlock = {
                                scaleX = settingScale
                                scaleY = settingScale
                                translationX = settingTranslationX
                                translationY = settingTranslationY
                                state.calculateCropArea()
                            }
                        )
                    }
                }
        ) {
            content.invoke(this)
        }
    }

}

