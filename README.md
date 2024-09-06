
# Krop: Kotlin Multiplatform Image Cropping Library

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-blue)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-green)](https://github.com/JetBrains/compose-multiplatform)

## Overview

Welcome to **Krop**, a Kotlin Multiplatform library for cropping images, built on top of [Coil](https://coil-kt.github.io/coil/). Whether you're targeting Android, iOS, web, or desktop, this library provides an easy-to-use API to customize and implement image cropping across platforms using [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform).

With **Krop**, you can seamlessly integrate image cropping functionality into your Kotlin applications, allowing users to interact with images in a native-like manner, regardless of the platform.

## Why Use Krop?

- **Multiplatform Compatibility:** `Krop` works across Android, iOS, web, and desktop with the power of Kotlin Multiplatform and Compose.
- **Easy Integration:** Simplify image cropping by leveraging Coil, a fast and lightweight image loading library.
- **Customizable:** Tailor the cropping experience to fit your application's specific requirements with flexible customization options.
- **Compose-based:** Benefit from a modern UI framework, enabling reactive and declarative UIs across all targets.

## Version Compatibility

The table below outlines the compatibility between different versions of the **Krop** library, Kotlin, and Coil. This ensures you can easily find the correct versions to use together in your project.

| **Krop Version** | **Kotlin Version** | **Coil Version**         |
|------------------|--------------------|--------------------------|
| 1.0.x            | 2.0.10+            | 3.0.0-alpha05+           |

## Features

- **Cross-platform Image Cropping:** Use a unified API to crop images across multiple platforms. Currently support image file `PNG`, `JPEG`
- **Built on Coil:** Take advantage of Coil’s efficiency and ease of use for image loading.
- **Compose Multiplatform Support:** Leverage the power of JetBrains Compose to create UIs that handle cropping interactions seamlessly.
- **Customizability:** Fine-tune the cropping area, aspect ratio, and more, to suit the needs of your app.

## Getting Started

### Installation

Add the following dependency to your `build.gradle.kts`, supporting Android, iOS, web, and desktop:

```kotlin
implementation("io.keeppro.krop:krop:1.0.1")
```

### Usage

To integrate **Krop** into your project, Wrapping an `AsyncImage` with `Croppable` and `CroppableState` is all you need to get started. Here's a simple example of how you can use **Krop** in your Compose code:

```kotlin
val croppableState = rememberCroppableState(contentScale = ContentScale.Crop)

    Croppable(
        state = croppableState,
        cropHint = CropHint.Default, 
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            imageLoader = loader,
            contentScale = ContentScale.Inside,
            onSuccess = { croppableState.prepareImage(it.result.image) },
        )
    }
```

Then you can call `croppableState.crop()` to get the cropped image.
`CroppableState` is also responsible for setting ContentScale, beware that you have to set AsyncImage `ContentScale.Inside` to make things work. And set the expected image size(width, height) at `Croppable`. 
You can also customize the cropping area by setting the aspect ratio to `Croppable`, or enabling tap and scale of doubleTap and more.

### Example Project

Check out the [example project](https://github.com/timhuang1018/Krop/tree/main/sample-multiplatform) to see how you can integrate **Krop** into your application across platforms.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Coil](https://coil-kt.github.io/coil/) for providing a fast, easy-to-use image loading library.
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) for offering a powerful UI framework that works across platforms.

## Showcase

This library is already powering production applications! Check out [KeepPro](https://keeppro.io), a queue management app built using `Krop` to gain simple cropping feature.

If you're using **Krop** in your project, we'd love to hear from you! Feel free to open a PR and add your app to this showcase.
