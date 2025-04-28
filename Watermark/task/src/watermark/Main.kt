package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val imagePath = getImagePath()
    val imageFile = File(imagePath)
    if (!isFileValid(imageFile, imagePath)) return
    val image: BufferedImage = try {
        ImageIO.read(imageFile)
    } catch (e: Exception) {
        println("Can't read file")
        return
    }
    if (!isImageValid(image)) return

    val watermarkPath = getWatermarkPath()
    val watermarkFile = File(watermarkPath)
    if (!isFileValid(watermarkFile, watermarkPath)) return
    val watermark: BufferedImage = try {
        ImageIO.read(watermarkFile)
    } catch (e: Exception) {
        println("Can't read file")
        return
    }
    if (!isWatermarkValid(watermark, image)) return

    val input = when (watermark.transparency) {
        Transparency.TRANSLUCENT -> {
            println("Do you want to use the watermark's Alpha channel?")
            readln().lowercase()
        }
        else -> {
            println("Do you want to set a transparency color?")
            readln().lowercase()
        }
    }

    val inputColor = if (input == "yes" && watermark.transparency != Transparency.TRANSLUCENT) {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        readln().trim()
    } else ""
    if (inputColor.isNotEmpty() && !isTransparencyColorValid(inputColor)) return

    val weight = getWeight()
    if (!isWeightValid(weight)) return

    val positionMethod = getPositionMethod()
    if (!isPositionMethodValid(positionMethod)) return
    when (positionMethod) {
        "single" -> {
            val diffX = image.width - watermark.width
            val diffY = image.height - watermark.height
            println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")
            val wmPositionInput = readln()
            if (!isWatermarkPositionInputCorrect(wmPositionInput, diffX, diffY)) return
            val (posX, posY) = wmPositionInput.split(" ").map { it.toInt() }

            val outputPath = getOutputPath()
            if (!isOutputExtensionValid(outputPath)) return
            val extension = outputPath.substringAfterLast('.')
            val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

            when (input) {
                "yes" -> {
                    if (watermark.transparency == Transparency.TRANSLUCENT) applySingleTransparent(image, watermark, output, weight, posX, posY)
                    else applySingleTransparentColor(inputColor, image, watermark, output, weight, posX, posY)
                }
                else -> applySingleWatermark(image, watermark, output, weight, posX, posY)
            }
            saveOutput(output, extension, outputPath)
        }
        "grid" -> {
            val outputPath = getOutputPath()
            if (!isOutputExtensionValid(outputPath)) return
            val extension = outputPath.substringAfterLast('.')
            val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

            when (input) {
                "yes" -> {
                    if (watermark.transparency == Transparency.TRANSLUCENT) applyGridTransparent(image, watermark, output, weight)
                    else applyGridTransparentColor(inputColor, image, watermark, output, weight)
                }
                else -> applyGridWatermark(image, watermark, output, weight)
            }
            saveOutput(output, extension, outputPath)
        }
    }
}

private fun getImagePath(): String {
    println("Input the image filename:")
    return readln()
}

private fun isFileValid(file: File, path: String): Boolean {
    if (!file.exists()) {
        println("The file $path doesn't exist.")
        return false
    }
    return true
}

private fun isImageValid(image: BufferedImage): Boolean {
    if (image.colorModel.numColorComponents != 3) {
        println("The number of image color components isn't 3.")
        return false
    }
    if (!(image.colorModel.pixelSize == 24 || image.colorModel.pixelSize == 32)) {
        println("The image isn't 24 or 32-bit.")
        return false
    }
    return true
}

private fun getWatermarkPath(): String {
    println("Input the watermark image filename:")
    return readln()
}

private fun isWatermarkValid(watermark: BufferedImage, image: BufferedImage): Boolean {
    if (watermark.colorModel.numColorComponents != 3) {
        println("The number of watermark color components isn't 3.")
        return false
    }
    if (!(watermark.colorModel.pixelSize == 24 || watermark.colorModel.pixelSize == 32)) {
        println("The watermark isn't 24 or 32-bit.")
        return false
    }

    if (!areEqualOrLessDimensions(image, watermark)) {
        println("The watermark's dimensions are larger.")
        return false
    }
    return true
}

private fun areEqualOrLessDimensions(image: BufferedImage, watermark: BufferedImage): Boolean {
    val imHeight = image.height
    val imWidth = image.width
    val wmHeight = watermark.height
    val wmWidth = watermark.width
    return wmHeight <= imHeight && wmWidth <= imWidth
}

private fun getWeight(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        return readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        return -1
    }
}

private fun isWeightValid(weight: Int): Boolean {
    if (weight !in 0..100) {
        println("The transparency percentage is out of range.")
        return false
    }
    return true
}

private fun getPositionMethod(): String {
    println("Choose the position method (single, grid):")
    return readln()
}

private fun isPositionMethodValid(positionMethod: String): Boolean {
    when (positionMethod) {
        "single", "grid" -> return true
        else -> {
            println("The position method input is invalid.")
            return false
        }
    }
}

private fun isWatermarkPositionInputCorrect(wmPositionInput: String, diffX: Int, diffY: Int): Boolean {
    val regex = "^-?\\d+\\s-?\\d+\$".toRegex()
    if (regex.matches(wmPositionInput)) {
        val (x, y) = wmPositionInput.split(" ").map { it.toInt() }
        if (x in 0..diffX && y in 0..diffY) return true
        else {
            println("The position input is out of range.")
            return false
        }
    } else {
        println("The position input is invalid.")
        return false
    }
}

private fun getOutputPath(): String {
    println("Input the output image filename (jpg or png extension):")
    return readln()
}

private fun isOutputExtensionValid(outputPath: String): Boolean {
    if (!(outputPath.endsWith(".jpg") || outputPath.endsWith(".png"))) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        return false
    }
    return true
}

private fun applySingleWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int,
    posX: Int,
    posY: Int
) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))
                if ((x - posX) in 0 until watermark.width && (y - posY) in 0 until watermark.height) {
                    val w = Color(watermark.getRGB(x, y))
                    val color = Color(
                        (weight * w.red + (100 - weight) * i.red) / 100,
                        (weight * w.green + (100 - weight) * i.green) / 100,
                        (weight * w.blue + (100 - weight) * i.blue) / 100
                    )
                    output.setRGB(x, y, color.rgb)
                } else {
                    val color = Color(i.red, i.green, i.blue)
                    output.setRGB(x, y, color.rgb)
                }
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun applyGridWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int
) {
    val watermarkWidth = watermark.width
    val watermarkHeight = watermark.height
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))
                val gridX = x % watermarkWidth
                val gridY = y % watermarkHeight
                val w = Color(watermark.getRGB(gridX, gridY))
                val color = Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
                output.setRGB(x, y, color.rgb)
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun applySingleTransparent(
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int,
    posX: Int,
    posY: Int
) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))
                val alpha = if ((x - posX) in 0 until watermark.width && (y - posY) in 0 until watermark.height) {
                    Color(watermark.getRGB(x - posX, y - posY), true).alpha
                } else {
                    0
                }
                when (alpha) {
                    0 -> {
                        val color = Color(i.red, i.green, i.blue)
                        output.setRGB(x, y, color.rgb)
                    }
                    else -> {
                        val w = Color(watermark.getRGB(x - posX, y - posY), true)
                        val color = Color(
                            (weight * w.red + (100 - weight) * i.red) / 100,
                            (weight * w.green + (100 - weight) * i.green) / 100,
                            (weight * w.blue + (100 - weight) * i.blue) / 100
                        )
                        output.setRGB(x, y, color.rgb)
                    }
                }
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun applyGridTransparent(
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int
) {
    // Get the dimensions of the watermark for interval calculations
    val watermarkWidth = watermark.width
    val watermarkHeight = watermark.height

    // Loop through the image height and width
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y)) // Get the color of the original image pixel

                // Determine the position of the watermark based on grid positioning
                val gridX = x % watermarkWidth
                val gridY = y % watermarkHeight

                // Get the corresponding watermark pixel color, take into account the alpha channel
                val w = Color(watermark.getRGB(gridX, gridY), true)

                // Calculate the final color, taking transparency into account
                val alpha = w.alpha
                val finalColor: Color
                if (alpha == 0) {
                    // If the watermark pixel is fully transparent, retain the original pixel color
                    finalColor = i
                } else {
                    // Mix the colors based on the weight provided
                    finalColor = Color(
                        (weight * w.red + (100 - weight) * i.red) / 100,
                        (weight * w.green + (100 - weight) * i.green) / 100,
                        (weight * w.blue + (100 - weight) * i.blue) / 100
                    )
                }
                // Set the calculated color to the output image
                output.setRGB(x, y, finalColor.rgb)
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun isTransparencyColorValid(inputColor: String): Boolean {
    val regex = Regex("""\b(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d) (25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d) (25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)\b""")
    if (!regex.matches(inputColor)) {
        println("The transparency color input is invalid.")
        return false
    }
    return true
}

private fun applySingleTransparentColor(
    inputColor: String,
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int,
    posX: Int,
    posY: Int
) {
    val (red, green, blue) = inputColor.split(" ").map { it.toInt() }
    val transparentColor = Color(red, green, blue)

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))
                if ((x - posX) in 0 until watermark.width && (y - posY) in 0 until watermark.height) {
                    val w = Color(watermark.getRGB(x - posX, y - posY))
                    when (w) {
                        transparentColor -> {
                            val color = Color(i.red, i.green, i.blue)
                            output.setRGB(x, y, color.rgb)
                        }
                        else -> {
                            val color = Color(
                                (weight * w.red + (100 - weight) * i.red) / 100,
                                (weight * w.green + (100 - weight) * i.green) / 100,
                                (weight * w.blue + (100 - weight) * i.blue) / 100
                            )
                            output.setRGB(x, y, color.rgb)
                        }
                    }
                } else {
                    val color = Color(i.red, i.green, i.blue)
                    output.setRGB(x, y, color.rgb)
                }
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun applyGridTransparentColor(
    inputColor: String,
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int
) {
    val watermarkWidth = watermark.width
    val watermarkHeight = watermark.height
    val (red, green, blue) = inputColor.split(" ").map { it.toInt() }
    val transparentColor = Color(red, green, blue)

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))

                val gridX = x % watermarkWidth
                val gridY = y % watermarkHeight
                val w = Color(watermark.getRGB(gridX, gridY))
                val finalColor: Color = when (w) {
                    transparentColor -> Color(i.red, i.green, i.blue)
                    else ->
                        Color(
                            (weight * w.red + (100 - weight) * i.red) / 100,
                            (weight * w.green + (100 - weight) * i.green) / 100,
                            (weight * w.blue + (100 - weight) * i.blue) / 100
                        )
                }
                output.setRGB(x, y, finalColor.rgb)
            } catch (e: Exception) {
                println("An error occurred at pixel ($x, $y)")
            }
        }
    }
}

private fun saveOutput(output: BufferedImage, extension: String, outputPath: String) {
    try {
        ImageIO.write(output, extension, File(outputPath))
        println("The watermarked image $outputPath has been created.")
    } catch (e: Exception) {
        println("Error saving image: ${e.message}")
    }
}