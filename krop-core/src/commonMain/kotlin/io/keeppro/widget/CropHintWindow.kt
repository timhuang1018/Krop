package io.keeppro.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BoxScope.CropHintWindow(
    gridLineColor: Color,
    cropWindow: Rect,
    windowStateFlow: MutableStateFlow<Boolean>,
) {
    var cropWindowVisible by remember { mutableStateOf(false) }
    var delayJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        windowStateFlow.collectLatest {
            cropWindowVisible = it
            if (it) {
                delayJob?.cancel()
                delayJob = scope.launch {
                    delay(1500)
                    withContext(Dispatchers.Main) {
                        windowStateFlow.value = false
                    }
                }
            } else {
                delayJob?.cancel()
                delayJob = null
            }
        }
    }


    AnimatedVisibility(
        visible = cropWindowVisible,
        enter = EnterTransition.None,
        exit = fadeOut()
    ) {

        SudokuGrid(
            gridLineColor = gridLineColor,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints = constraints.copy(
                        maxWidth = cropWindow.width.toInt(),
                        maxHeight = cropWindow.height.toInt(),
                        minWidth = cropWindow.width.toInt(),
                        minHeight = cropWindow.height.toInt()
                    )
                )
                layout(
                    width = cropWindow.width.toInt(),
                    height = cropWindow.height.toInt()
                ) {
                    placeable.place(
                        (-cropWindow.topLeft.x.toInt()),
                        (-cropWindow.topLeft.y.toInt())
                    )
                }
            })
    }

}