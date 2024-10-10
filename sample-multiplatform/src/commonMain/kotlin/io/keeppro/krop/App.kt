package io.keeppro.krop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.util.DebugLogger
import io.keeppro.CropHint
import io.keeppro.Croppable
import io.keeppro.CroppableState
import io.keeppro.rememberCroppableState
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {
        val croppableState = rememberCroppableState(contentScale = ContentScale.Crop)

        var newImage by remember { mutableStateOf<ByteArray?>(null) }

        var aspectRatio by remember { mutableStateOf(4f/3f) }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
            ) {

            CroppableSample(croppableState, aspectRatio)

            Spacer(modifier = Modifier.height(16.dp))

            //buttons to change aspect ratio to 1:1, 4:3, 16:9, 3:4, 9:16
            Row (modifier = Modifier.horizontalScroll(rememberScrollState())) {

                listOfAspectRatios.forEach {
                    Button(
                        modifier = Modifier.padding(16.dp),
                        onClick = { aspectRatio = it.aspectRatio }
                    ) {
                        Text(it.text)
                    }
                }
            }

            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                newImage = croppableState.crop()
            }) {
                Text("Crop")
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (newImage != null) {
                AsyncImage(
                    model = newImage,
                    contentDescription = null,
                    imageLoader = ImageLoader(LocalPlatformContext.current)
                )
            }
        }


    }

}

@Composable
fun CroppableSample(croppableState: CroppableState, aspectRatio: Float) {
    val size = 300.dp

    val imageRequest = getImageRequest(LocalPlatformContext.current, "https://picsum.photos/id/237/1600/2400")
    val loader = ImageLoader.Builder(LocalPlatformContext.current)
        .logger(DebugLogger())
        .build()

    Croppable(
        state = croppableState,
        contentScale = ContentScale.Crop,
        cropHint = CropHint.Default.copy(gridLineColor = Color.White.copy(alpha = 0.3f)),
        modifier = Modifier
            .layout { measurable, constraints ->
                val newConstraints = if (aspectRatio >= 1f){
                    constraints.copy(maxWidth = size.roundToPx(), maxHeight = (size.roundToPx() / aspectRatio).toInt())
                }else{
                    constraints.copy(maxWidth = (size.roundToPx() * aspectRatio).toInt(), maxHeight = size.roundToPx())
                }
                val placeable = measurable.measure(newConstraints)

                layout(newConstraints.maxWidth, newConstraints.maxHeight){
                    placeable.placeRelative(0, 0)
                }
            }
        ,
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            imageLoader = loader,
            contentScale = ContentScale.Inside,
            onSuccess = {
                croppableState.prepareImage(it.result.image)
            },
        )
    }
}

private class AspectRatioButton(
    val text: String,
    val aspectRatio: Float,
)

private val listOfAspectRatios = listOf(
    AspectRatioButton("1:1", 1f),
    AspectRatioButton("4:3", 4f / 3f),
    AspectRatioButton("16:9", 16f / 9f),
    AspectRatioButton("3:4", 3f / 4f),
    AspectRatioButton("9:16", 9f / 16f),
)


