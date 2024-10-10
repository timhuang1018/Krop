package io.keeppro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val cropWindow: Rect by state.cropWindow
    var cropWindowVisible by remember { mutableStateOf(false) }
    val windowStateFlow = remember { MutableStateFlow(false) }
    var delayJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit){
        windowStateFlow.collectLatest {
            cropWindowVisible = it
            if(it){
                delayJob?.cancel()
                delayJob = scope.launch {
                    delay(1500)
                    withContext(Dispatchers.Main){
                        windowStateFlow.value = false
                    }
                }
            }else{
                delayJob?.cancel()
                delayJob = null
            }
        }
    }



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

        AnimatedVisibility(
            visible = cropWindowVisible,
            enter = EnterTransition.None,
            exit = fadeOut()
            ){
            SudokuGrid(
                gridLineColor = cropHint?.gridLineColor ?: Color.Black,
                modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints = constraints.copy(
                    maxWidth = cropWindow.width.toInt(),
                    maxHeight = cropWindow.height.toInt(),
                    minWidth = cropWindow.width.toInt(),
                    minHeight = cropWindow.height.toInt()
                ))
                layout(
                    width = cropWindow.width.toInt(),
                    height = cropWindow.height.toInt()
                ) {
                    placeable.place((- cropWindow.topLeft.x.toInt()), (- cropWindow.topLeft.y.toInt()))
                }
            })
        }
    }

}

