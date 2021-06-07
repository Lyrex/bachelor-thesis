package net.lyrex.image

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.vision.v1.*
import com.google.protobuf.ByteString

import java.io.File
import java.io.IOException

interface IImageProcessor {
    fun imageToText(inputImage: IImageContainer): String
}

class ImageProcessor {
    companion object: IImageProcessor {
        @Throws(IOException::class)
        @JvmStatic
        override fun imageToText(inputImage: IImageContainer): String {
            val requests: MutableList<AnnotateImageRequest> = ArrayList()
            val imgBytes = ByteString.copyFrom(inputImage.asByteArray())
            val img = Image.newBuilder().setContent(imgBytes).build()
            val feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build()
            val request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build()
            requests.add(request)

            var fullText = ""

            // use local google cloud platform credentials
            val credentials = GoogleCredentials.fromStream(File("gcp-credentials.dat").inputStream())
            val imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()

            ImageAnnotatorClient.create(imageAnnotatorSettings).use { client ->
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