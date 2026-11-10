package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Turns a camera/gallery [Uri] into a downscaled JPEG that can be sent to a
 * vision model as a data URI. Keeps the payload comfortably below provider
 * request limits while preserving enough text detail for nutrition labels.
 */
object ImageUtils {

    private const val DEFAULT_MAX_DIMENSION = 1600
    private const val JPEG_QUALITY = 85

    fun uriToBase64Jpeg(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): String? {
        val bitmap = decodeBitmap(context, uri, maxDimension) ?: return null
        return try {
            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri, maxDimension)
        } else {
            decodeLegacy(context, uri, maxDimension)
        }
    }

    /**
     * Decodes through [ImageDecoder], which only exists from API 28. The call
     * site in [decodeBitmap] is guarded by a runtime version check, and this
     * annotation tells lint (and the compiler) that the API 28 APIs below are
     * safe to use inside this function.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val width = info.size.width
                val height = info.size.height
                val longest = max(width, height)
                if (longest > maxDimension && longest > 0) {
                    val scale = maxDimension.toFloat() / longest.toFloat()
                    decoder.setTargetSize(
                        max(1, (width * scale).toInt()),
                        max(1, (height * scale).toInt())
                    )
                }
            }
        }.getOrNull()
    }

    /**
     * Fallback for API 24-27, where [ImageDecoder] does not exist.
     *
     * It mirrors the behaviour of the ImageDecoder path: the bitmap is
     * decoded in software (ARGB_8888), kept immutable, subsampled while
     * decoding to avoid allocating a huge bitmap, then scaled so the longest
     * edge matches [maxDimension]. ImageDecoder applies the EXIF orientation
     * on its own, so the rotation is applied explicitly here.
     */
    private fun decodeLegacy(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        var longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sampleSize > maxDimension * 2) {
            sampleSize *= 2
            longest /= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val rotated = applyExifRotation(bytes, decoded)

        val currentLongest = max(rotated.width, rotated.height)
        if (currentLongest <= maxDimension) return rotated

        val scale = maxDimension.toFloat() / currentLongest.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            rotated,
            max(1, (rotated.width * scale).toInt()),
            max(1, (rotated.height * scale).toInt()),
            true
        )
        if (scaled !== rotated) rotated.recycle()
        return scaled
    }

    private fun applyExifRotation(bytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { rotated -> if (rotated !== bitmap) bitmap.recycle() }
        }.getOrDefault(bitmap)
    }
}
