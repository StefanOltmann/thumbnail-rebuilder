/*
 * Thumbnail Rebuilder
 * Copyright (C) 2024 Stefan Oltmann
 * https://stefan-oltmann.de/thumbnail-rebuilder
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.ashampoo.kim.Kim
import com.ashampoo.kim.common.startsWith
import com.ashampoo.kim.format.ImageFormatMagicNumbers
import com.ashampoo.kim.format.jpeg.JpegOrientationOffsetFinder
import com.ashampoo.kim.input.ByteArrayByteReader
import com.ashampoo.kim.model.TiffOrientation
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.math.max

private val paint = Paint().apply {
    isAntiAlias = true
}

fun Uint8Array.toByteArray(): ByteArray =
    ByteArray(length) { this[it] }

fun ByteArray.toUint8Array(): Uint8Array {
    val result = Uint8Array(size)
    forEachIndexed { index, byte ->
        result[index] = byte
    }
    return result
}

fun ByteArray.toBlob(mimeType: String): Blob {

    val uint8Array: Uint8Array = toUint8Array()

    return Blob(
        jsArrayOf(uint8Array),
        BlobPropertyBag(mimeType)
    )
}

fun <T : JsAny?> jsArrayOf(vararg elements: T): JsArray<T> {

    val array = JsArray<T>()

    for (i in elements.indices)
        array[i] = elements[i]

    return array
}

fun rebuildEmbeddedThumbnail(
    bytes: ByteArray,
    size: Int,
    quality: Int
): ByteArray {

    /*
     * Use a copy of the bytes with reset orientation flag to prevent SKIA
     * from doing a rotation. Embedded thumbnails must not be rotated.
     */
    val bytesForThumbnail = bytes
        .copyOf()
        .apply {
            resetOrientationFlag(this)
        }

    val originalImage = Image.makeFromEncoded(bytesForThumbnail)

    val scaledImage = originalImage.scale(size)

    val thumbnailBytes = scaledImage.encodeToJpg(quality)

    val updatedBytes = Kim.updateThumbnail(
        bytes = bytes,
        thumbnailBytes = thumbnailBytes
    )

    return updatedBytes
}

private fun Image.scale(longSidePx: Int): Image {

    val isLandscape = width > height

    val resizeFactor =
        if (isLandscape)
            longSidePx / width.toFloat()
        else
            longSidePx / height.toFloat()

    val scaledWidth: Float = max(1f, (resizeFactor * width))
    val scaledHeight: Float = max(1f, (resizeFactor * height))

    val surface = Surface.makeRasterN32Premul(
        scaledWidth.toInt(), scaledHeight.toInt()
    )

    surface.canvas.drawImageRect(
        image = this,
        src = Rect.makeWH(width.toFloat(), height.toFloat()),
        dst = Rect.makeWH(scaledWidth, scaledHeight),
        samplingMode = SamplingMode.MITCHELL,
        paint = paint,
        strict = true
    )

    return surface.makeImageSnapshot()
}

private fun Image.encodeToJpg(quality: Int): ByteArray {

    val data = encodeToData(EncodedImageFormat.JPEG, quality)

    requireNotNull(data) { "JPG Encoding failed." }

    return data.bytes
}

/** Reset orientation flag in JPEG bytes. */
private fun resetOrientationFlag(bytes: ByteArray) {

    /* Only do this for JPG. */
    if (!bytes.startsWith(ImageFormatMagicNumbers.jpeg))
        return

    JpegOrientationOffsetFinder.findOrientationOffset(
        byteReader = ByteArrayByteReader(bytes)
    )?.let { offset ->

        bytes[offset.toInt()] = TiffOrientation.STANDARD.value.toByte()
    }
}
