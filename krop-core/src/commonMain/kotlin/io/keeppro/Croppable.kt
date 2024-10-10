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
import io.keeppro.widget.CropHintWindow
import kotlinx.coroutines.flow.MutableStateFlow
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
    val windowStateFlow = remember { MutableStateFlow(false) }


    val boxModifier = if (cropHint != null) {
        modifier.border(cropHint.borderWidth, cropHint.borderColor).background(cropHint.backgroundColor)
    } else {
        modifier
    }
    BoxWithConstraints(modifier = boxModifier.clipToBounds()) {
        var childWidth by remember { mutableStateOf(0) }
        var childHeight by remember { mutableStateOf(0) }

        LaunchedEffect(
            childHeight,
            childWidth,
            contentScale,
            constraints.maxWidth,
            constraints.maxHeight
        ) {
            // Update scale based on contentScale and dimensions
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
            state.updateContainerAndChildSize(constraints.maxWidth, constraints.maxHeight, childWidth, childHeight)
            state.updateBounds()
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
                                state.animateDoubleTap(scale = afterScale, distance = dis)
                                state.updateBounds()
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
                        state.updateBounds()
                    }

                    scope.launch {
                        state.drag(pan - adjustedOffset)
                        windowStateFlow.value = true
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

        if (cropHint != null){
            CropHintWindow(
                cropHint.gridLineColor,
                state.cropWindow.value,
                windowStateFlow,
            )
        }
    }

}

