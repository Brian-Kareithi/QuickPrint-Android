package com.quickprint.app.data

import android.net.Uri

sealed interface ShopLink {
    data class Slug(val slug: String) : ShopLink
    data class Code(val code: String) : ShopLink
}

object ShopLinks {
    private val CODE = Regex("^[A-Za-z0-9]{5}$")
    private val SLUG = Regex("^[a-z0-9][a-z0-9-]*$")

    fun cleanCode(input: String): String = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(5)

    fun parse(raw: String?): ShopLink? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (CODE.matches(text)) return ShopLink.Code(text.uppercase())
        val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
        if (uri.scheme != "https" && uri.scheme != "http") return null
        val segments = uri.pathSegments
        val i = segments.indexOf("shop")
        val slug = segments.getOrNull(i + 1)?.lowercase()
        if (i < 0 || slug == null || !SLUG.matches(slug)) return null
        return ShopLink.Slug(slug)
    }
}
