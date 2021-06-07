package net.lyrex.image

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Base64


interface IImageContainer {
    fun asBase64(): String
    fun asByteArray(): ByteArray
}

class ImageContainer : IImageContainer {
    constructor(filePath: String) : this(File(filePath)) {
    }

    constructor(imageFile: File) {
        require(imageFile.path.isNotBlank()) { "filePath can not be null or empty" }

        if (!imageFile.exists()) {
            throw FileNotFoundException("could not find file \"$imageFile\"")
        }

        FileInputStream(imageFile).use { fis ->
            this.imageData = fis.readAllBytes()
        }
    }

    override fun asBase64(): String {
        return Base64.getEncoder().encodeToString(this.imageData)
    }

    override fun asByteArray(): ByteArray {
        return this.imageData
    }

    var imageData: ByteArray
        private set
}