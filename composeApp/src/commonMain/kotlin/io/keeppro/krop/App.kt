package io.keeppro.krop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.util.DebugLogger
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
    MaterialTheme {

        val zoomableState = rememberZoomableState()

        var newImage by remember { mutableStateOf<ByteArray?>(null) }

        val imageRequest = getImageRequest(LocalPlatformContext.current, "https://picsum.photos/id/237/800/1200")
        val loader = ImageLoader.Builder(LocalPlatformContext.current)
            .logger(DebugLogger())
            .components {
                add { chain ->
                    val response = chain.proceed()
                    zoomableState.prepareImage(response.image)
                    response
                }
            }
            .build()

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
            ) {
            Zoomable(
                state = zoomableState,
                cropHint = CropHint.Default,
                modifier = Modifier.size(300.dp).aspectRatio(1f)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    imageLoader = loader,
                    contentScale = ContentScale.Inside
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.padding(16.dp),
                onClick = {
                newImage = zoomableState.crop()
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


