package com.quickprint.app.data

import android.content.ContentResolver
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject

const val MAX_UPLOAD_BYTES = 50L * 1024 * 1024
val DEFAULT_EXTENSIONS = listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "jpg", "jpeg", "png", "zip")

data class PickedFile(val uri: Uri, val name: String, val size: Long, val mimeType: String) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
}

object OrderRepository {

    suspend fun describe(resolver: ContentResolver, uri: Uri): PickedFile = withContext(Dispatchers.IO) {
        var name = "document"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getString(0)?.let { name = it }
                if (!c.isNull(1)) size = c.getLong(1)
            }
        }
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = resolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
        PickedFile(uri, name, size, mime)
    }

    suspend fun pageCount(resolver: ContentResolver, file: PickedFile): Int = withContext(Dispatchers.IO) {
        if (file.extension != "pdf") return@withContext 1
        runCatching {
            resolver.openFileDescriptor(file.uri, "r")?.use { fd -> PdfRenderer(fd).use { it.pageCount } }
        }.getOrNull()?.coerceAtLeast(1) ?: 1
    }

    suspend fun upload(resolver: ContentResolver, shopId: String, file: PickedFile, onProgress: (Float) -> Unit): String {
        val body = StreamingBody(resolver, file.uri, file.mimeType.toMediaTypeOrNull(), file.size, onProgress)
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, body)
            .build()
        val o = JSONObject(Api.post("/uploads/${Uri.encode(shopId)}", multipart))
        return o.getString("id")
    }

    suspend fun price(shopId: String, files: List<UploadedFile>): PriceBreakdown {
        val items = JSONArray()
        files.forEach { items.put(it.options.toJson()) }
        val o = JSONObject(Api.send("POST", "/pricing/calculate", JSONObject().put("shopId", shopId).put("items", items)))
        return PriceBreakdown.from(o)
    }

    suspend fun placeOrder(shopId: String, files: List<UploadedFile>, details: CustomerDetails, paymentMethod: String): PlacedOrder {
        val items = JSONArray()
        files.forEach { f -> items.put(f.options.toJson().put("uploadId", f.id).put("fileName", f.name).put("fileSize", f.size)) }
        val body = JSONObject()
            .put("shopId", shopId)
            .put("paymentMethod", paymentMethod)
            .put("items", items)
        details.name.trim().takeIf { it.isNotEmpty() }?.let { body.put("customerName", it) }
        details.email.trim().takeIf { it.isNotEmpty() }?.let { body.put("customerEmail", it) }
        details.phone.trim().takeIf { it.isNotEmpty() }?.let { body.put("customerPhone", it) }
        details.notes.trim().takeIf { it.isNotEmpty() }?.let { body.put("notes", it) }
        return PlacedOrder.from(JSONObject(Api.send("POST", "/orders", body)))
    }
}

private class StreamingBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val type: MediaType?,
    private val length: Long,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = type
    override fun contentLength() = length
    override fun writeTo(sink: BufferedSink) {
        val input = resolver.openInputStream(uri) ?: throw java.io.IOException("Could not read the file")
        input.source().use { source ->
            var sent = 0L
            while (true) {
                val read = source.read(sink.buffer, 64 * 1024L)
                if (read == -1L) break
                sent += read
                sink.emitCompleteSegments()
                if (length > 0) onProgress((sent.toFloat() / length).coerceIn(0f, 1f))
            }
        }
    }
}
