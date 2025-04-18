package watermark

import java.awt.Color
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

    val weight = getWeight()
    if (!isWeightValid(weight)) return

    val outputPath = getOutputPath()
    if (!isOutputExtensionValid(outputPath)) return
    val extension = outputPath.substringAfterLast('.')
    val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)

    applyWatermark(image, watermark, output, weight)
    saveOutput(output, extension, outputPath)
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

    if (!areEqualDimensions(image, watermark)) {
        println("The image and watermark dimensions are different.")
        return false
    }
    return true
}

private fun areEqualDimensions(image: BufferedImage, watermark: BufferedImage): Boolean {
    val imHeight = image.height
    val imWidth = image.width
    val wmHeight = watermark.height
    val wmWidth = watermark.width
    return imHeight == wmHeight && imWidth == wmWidth
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

private fun applyWatermark(
    image: BufferedImage,
    watermark: BufferedImage,
    output: BufferedImage,
    weight: Int
) {
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            try {
                val i = Color(image.getRGB(x, y))
                val w = Color(watermark.getRGB(x, y))
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

private fun saveOutput(output: BufferedImage, extension: String, outputPath: String) {
    try {
        ImageIO.write(output, extension, File(outputPath))
        println("The watermarked image $outputPath has been created.")
    } catch (e: Exception) {
        println("Error saving image: ${e.message}")
    }
}