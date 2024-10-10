package io.keeppro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.SudokuGrid(gridLineColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gridSize = 3 // For a 3x3 grid
        val lineThickness = 2.dp.toPx()
        val cellSizeX = size.width / gridSize
        val cellSizeY = size.height / gridSize

        // Draw the grid lines
        for (i in 1 until gridSize) {
            val x = i * cellSizeX
            val y = i * cellSizeY
            if (i < gridSize) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = lineThickness
                )
            }
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineThickness
            )
        }
    }
}