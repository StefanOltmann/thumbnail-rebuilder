/*
 * Thumbnail Rebuilder
 * Copyright (C) 2023 Stefan Oltmann
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
import kotlinx.browser.document
import kotlinx.dom.appendElement
import kotlinx.dom.appendText
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.DragEvent
import org.w3c.dom.Element
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.get
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get

private const val JPEG_MIME_TYPE = "image/jpeg"

private const val DEFAULT_SIZE = 320
private const val DEFAULT_QUALITY = 80

private var contentsElement: Element? = null
private var errorsElement: Element? = null
private var downloadAllLink: HTMLElement? = null

private var selectedSize: Int = DEFAULT_SIZE
private var selectedQuality: Int = DEFAULT_QUALITY

fun main() {

    contentsElement = document.getElementById("contents")
    errorsElement = document.getElementById("errors")
    downloadAllLink = document.getElementById("downloadAllLink") as? HTMLElement

    registerFileInputEvents()
}

private fun registerFileInputEvents() {

    val dropbox = document.getElementById("dropbox")
    val fileInput = document.getElementById("fileInput") as? HTMLElement

    dropbox?.addEventListener("dragover") { event ->

        event as DragEvent

        event.preventDefault()
        event.dataTransfer?.dropEffect = "copy"
        dropbox.classList.add("highlight")
    }

    dropbox?.addEventListener("dragleave") { event ->

        event as DragEvent

        event.preventDefault()
        dropbox.classList.remove("highlight")
    }

    dropbox?.addEventListener("drop") { event ->

        event as DragEvent

        event.preventDefault();
        dropbox.classList.remove("highlight");

        val items = event.dataTransfer?.items;

        if (items == null || items.length == 0)
            return@addEventListener

        for (i in 0..<items.length)
            handleFile(items[i]!!.getAsFile()!!)
    }

    dropbox?.addEventListener("click") { _ ->
        fileInput?.click()
    }

    fileInput?.addEventListener("change") { event ->

        val target = event.target as? HTMLInputElement ?: return@addEventListener

        val files = target.files

        if (files == null || files.length == 0)
            return@addEventListener

        for (i in 0..<files.length)
            handleFile(files[i]!!)
    }
}

private fun handleFile(file: File) {

    /* Only JPGs are supported. */
    if (file.type != JPEG_MIME_TYPE)
        return

    val fileReader = FileReader()

    fileReader.onload = { event ->

        val target = event.target as? FileReader

        if (target != null) {

            val arrayBuffer = target.result as? ArrayBuffer

            if (arrayBuffer != null) {

                val uInt8Bytes = Uint8Array(arrayBuffer)

                processFile(file.name, uInt8Bytes)
            }
        }
    }

    fileReader.readAsArrayBuffer(file)
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun setSize(size: Int) {

    selectedSize = size

    markSelectedSizeOption()
}

private fun markSelectedSizeOption() {

    val sizeOptions = document.querySelectorAll(".optionBox.size")

    for (element in sizeOptions.asList()) {

        element as HTMLButtonElement

        val selected = element.innerText == selectedSize.toString()

        if (selected)
            element.classList.add("selected")
        else
            element.classList.remove("selected")
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun setQuality(quality: Int) {

    selectedQuality = quality

    markSelectedQualityOption()
}

private fun markSelectedQualityOption() {

    val qualityOptions = document.querySelectorAll(".optionBox.quality")

    for (element in qualityOptions.asList()) {

        element as HTMLButtonElement

        val selected = element.innerText == selectedQuality.toString()

        if (selected)
            element.classList.add("selected")
        else
            element.classList.remove("selected")
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun processFile(
    fileName: String,
    uint8Array: Uint8Array
) {

    try {

        val bytes = uint8Array.toByteArray()

        val newBytes = rebuildEmbeddedThumbnail(
            bytes = bytes,
            size = selectedSize,
            quality = selectedQuality
        )

        /* Extract the thumbnail again for a roundtrip test. */
        val newThumbnailBytes = requireNotNull(
            Kim.readMetadata(newBytes)?.getExifThumbnailBytes()
        )

        val blob = newBytes.toBlob(JPEG_MIME_TYPE)

        val url = URL.Companion.createObjectURL(blob)

        val link = contentsElement?.appendElement("a") {

            this as HTMLAnchorElement

            className = "downloadLink"

            href = url
            download = fileName
        }

        link?.appendElement("img") {

            this as HTMLImageElement

            className = "thumb"

            val thumbBlob = newThumbnailBytes.toBlob(JPEG_MIME_TYPE)

            val thumbUrl = URL.Companion.createObjectURL(thumbBlob)

            src = thumbUrl
        }

        /* Link should become visible on first item. */
        downloadAllLink?.style?.display = "block"

    } catch (ex: Exception) {

        errorsElement?.let {

            it.appendElement("br") {}
            it.appendText(fileName)
            it.appendElement("br") {}
            it.appendText(ex.message ?: "Unknown error")
            it.appendElement("br") {}
        }

        ex.printStackTrace()
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
fun downloadAll() {

    val allLinks = document.querySelectorAll(".downloadLink")

    for (link in allLinks.asList())
        (link as HTMLAnchorElement).click()
}

