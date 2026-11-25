package com.quickprint.app.data

import android.net.Uri
import org.json.JSONObject

object ShopRepository {
    suspend fun bySlug(slug: String): Shop =
        Shop.from(JSONObject(Api.get("/shops/slug/${Uri.encode(slug)}")))

    /** Looks up a shop by its 5-character counter code and returns its slug. */
    suspend fun slugForCode(code: String): String {
        val clean = ShopLinks.cleanCode(code)
        if (clean.length != 5) throw ApiException("The shop code is 5 characters.")
        val o = JSONObject(Api.get("/shops/code/${Uri.encode(clean)}"))
        return o.getString("slug")
    }
}
