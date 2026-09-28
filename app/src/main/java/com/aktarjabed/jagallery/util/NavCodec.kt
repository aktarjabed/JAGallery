package com.aktarjabed.jagallery.util

import android.util.Base64

object NavCodec {
    private const val PREFIX = "B64_"

    fun encode(payload: String): String {
        return PREFIX + Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun decode(encodedPayload: String): String {
        if (!encodedPayload.startsWith(PREFIX)) {
            return encodedPayload // Not encoded by us, return as is
        }
        val dataStr = encodedPayload.removePrefix(PREFIX)
        return try {
            val bytes = Base64.decode(dataStr, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encodedPayload // Failed decode
        }
    }
}
