package watermark

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    println("Input the image filename:")
    val imagePath = readln()

    try {
        // Create a File object
        val imageFile = File(imagePath)
        // Read the image from the file
        val image: BufferedImage = ImageIO.read(imageFile)

        printImageInfo(imagePath, image)
    } catch (e: Exception) {
        // Handle exceptions such as IOException or IllegalArgumentException
        println("The file $imagePath doesn't exist.")
    }
}

private fun printImageInfo(imagePath: String, image: BufferedImage) {
    println("Image file: $imagePath")
    println("Width: ${image.width}")
    println("Height: ${image.height}")
    println("Number of components: ${image.colorModel.numComponents}")
    println("Number of color components: ${image.colorModel.numColorComponents}")
    println("Bits per pixel: ${image.colorModel.pixelSize}")
    printTransparency(image.transparency)
}

private fun printTransparency(transparencyType: Int) {
    val transparencyName = when(transparencyType) {
        BufferedImage.OPAQUE -> "OPAQUE"
        BufferedImage.BITMASK -> "BITMASK"
        BufferedImage.TRANSLUCENT -> "TRANSLUCENT"
        else -> "UNKNOWN"
    }
    println("Transparency: $transparencyName")
}