package net.lyrex.image

import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString

import kotlin.Throws
import java.io.IOException
import java.util.ArrayList

class ImageProcessor {
    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun imageToText(inputImage: ImageContainer): String {
            val requests: MutableList<AnnotateImageRequest> = ArrayList()
            val imgBytes = ByteString.copyFrom(inputImage.imageData)
            val img = Image.newBuilder().setContent(imgBytes).build()
            val feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
            val request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build()
            requests.add(request)

            var fullText = ""

            ImageAnnotatorClient.create().use { client ->
                val response = client.batchAnnotateImages(requests)
                val responses = response.responsesList
                client.close()

                for (res in responses) {
                    if (res.hasError()) {
                        // TODO(tobias): improve error handling
                        System.err.format("Error: %s\n", res.error.message)
                        return ""
                    }

                    // get text using full text annotation
                    val fullTextAnnotation = res.fullTextAnnotation
                    fullText += fullTextAnnotation.text
                }
            }

            return fullText
        }
    }
}