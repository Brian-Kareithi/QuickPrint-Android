package com.quickprint.app.ui.shop

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickprint.app.data.ApiException
import com.quickprint.app.data.CustomerDetails
import com.quickprint.app.data.DEFAULT_EXTENSIONS
import com.quickprint.app.data.MAX_UPLOAD_BYTES
import com.quickprint.app.data.OrderRepository
import com.quickprint.app.data.Payments
import com.quickprint.app.data.PlacedOrder
import com.quickprint.app.data.Prefs
import com.quickprint.app.data.PriceBreakdown
import com.quickprint.app.data.PrintOptions
import com.quickprint.app.data.RecentShop
import com.quickprint.app.data.Shop
import com.quickprint.app.data.ShopRepository
import com.quickprint.app.data.UploadedFile
import kotlinx.coroutines.launch

enum class Step { UPLOAD, OPTIONS, REVIEW, DONE }

data class UploadProgress(val name: String, val progress: Float)

class OrderViewModel(app: Application, private val slug: String) : AndroidViewModel(app) {
    private val resolver = app.contentResolver
    private val prefs = Prefs(app)

    var shop by mutableStateOf<Shop?>(null); private set
    var loadError by mutableStateOf<String?>(null); private set
    var notFound by mutableStateOf(false); private set

    var step by mutableStateOf(Step.UPLOAD)
    var files by mutableStateOf<List<UploadedFile>>(emptyList()); private set
    var uploading by mutableStateOf<UploadProgress?>(null); private set
    var pricing by mutableStateOf<PriceBreakdown?>(null); private set
    var details by mutableStateOf(CustomerDetails())
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var order by mutableStateOf<PlacedOrder?>(null); private set

    init { load() }

    fun load() {
        loadError = null
        viewModelScope.launch {
            try {
                val s = ShopRepository.bySlug(slug)
                shop = s
                prefs.recentShop = RecentShop(s.name, s.slug, s.address)
                if (details.paymentMethod.isEmpty()) details = details.copy(paymentMethod = Payments.options(s).first().value)
            } catch (e: ApiException) {
                notFound = e.status == 404
                loadError = e.message
            }
        }
    }

    fun addFiles(uris: List<Uri>) {
        val s = shop ?: return
        if (uris.isEmpty()) return
        error = null
        viewModelScope.launch {
            val allowed = s.allowedExtensions.ifEmpty { DEFAULT_EXTENSIONS }
            for (uri in uris) {
                val picked = runCatching { OrderRepository.describe(resolver, uri) }.getOrNull()
                if (picked == null) { error = "Couldn't read one of the files. Try picking it again."; continue }
                if (picked.extension !in allowed) {
                    error = "${picked.name}: ${s.name} doesn't accept .${picked.extension.ifEmpty { "?" }} files. Allowed: ${allowed.joinToString(", ") { it.uppercase() }}."
                    continue
                }
                if (picked.size > MAX_UPLOAD_BYTES) { error = "${picked.name} is larger than 50 MB."; continue }
                uploading = UploadProgress(picked.name, 0f)
                try {
                    val pages = OrderRepository.pageCount(resolver, picked)
                    val id = OrderRepository.upload(resolver, s.id, picked) { p -> uploading = UploadProgress(picked.name, p) }
                    files = files + UploadedFile(id, picked.name, picked.size.coerceAtLeast(0), PrintOptions(pageCount = pages))
                } catch (e: Exception) {
                    error = "${picked.name}: ${e.message ?: "Upload failed"}"
                }
            }
            uploading = null
        }
    }

    fun removeFile(id: String) { files = files.filterNot { it.id == id } }

    fun updateOptions(id: String, change: (PrintOptions) -> PrintOptions) {
        files = files.map { if (it.id == id) it.copy(options = change(it.options)) else it }
    }

    fun review() {
        val s = shop ?: return
        error = null
        busy = true
        viewModelScope.launch {
            try {
                pricing = OrderRepository.price(s.id, files)
                step = Step.REVIEW
            } catch (e: Exception) {
                error = e.message ?: "Could not calculate the price"
            } finally {
                busy = false
            }
        }
    }

    fun placeOrder() {
        val s = shop ?: return
        val email = details.email.trim()
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error = "That email address doesn't look right."
            return
        }
        error = null
        busy = true
        viewModelScope.launch {
            try {
                order = OrderRepository.placeOrder(s.id, files, details, details.paymentMethod.ifEmpty { Payments.options(s).first().value })
                step = Step.DONE
            } catch (e: Exception) {
                error = e.message ?: "Could not place the order"
            } finally {
                busy = false
            }
        }
    }

    fun startOver() {
        files = emptyList()
        pricing = null
        order = null
        error = null
        step = Step.UPLOAD
    }

    fun back(): Boolean {
        error = null
        step = when (step) {
            Step.OPTIONS -> Step.UPLOAD
            Step.REVIEW -> Step.OPTIONS
            else -> return false
        }
        return true
    }
}
