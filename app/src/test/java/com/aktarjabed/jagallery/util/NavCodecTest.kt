package com.aktarjabed.jagallery.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavCodecTest {

    @Test
    fun encodeDecode_basicStrings_roundTripsCorrectly() {
        val testCases = listOf(
            "",
            "abc",
            "hello world",
            "foo/bar",
            "foo?bar",
            "foo#bar",
            "foo+bar",
            "foo=bar",
            "foo&bar",
            "foo%bar",
            "%25",
            "%2525",
            "100% complete"
        )
        for (original in testCases) {
            val encoded = NavCodec.encode(original)
            val decoded = NavCodec.decode(encoded)
            assertEquals("Failed for input: '$original'", original, decoded)
        }
    }

    @Test
    fun encodeDecode_unicodeAndEmojis_roundTripsCorrectly() {
        val testCases = listOf(
            "অসমীয়া",
            "বাংলা",
            "हिन्दी",
            "中文",
            "العربية",
            "🚀🌍🔥",
            "👨‍👩‍👧‍👦"
        )
        for (original in testCases) {
            val encoded = NavCodec.encode(original)
            val decoded = NavCodec.decode(encoded)
            assertEquals("Failed for unicode: '$original'", original, decoded)
        }
    }

    @Test
    fun encodeDecode_longPayloads_roundTripsCorrectly() {
        val original = "content://media/external/file/something/very/long/and/complex?with=parameters&and#fragments=true+spaces here"
        val encoded = NavCodec.encode(original)
        val decoded = NavCodec.decode(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun decode_invalidBase64_returnsFallbackPlainString() {
        val invalidBase64 = "not-base64-!!!"
        val decoded = NavCodec.decode(invalidBase64)
        assertEquals(invalidBase64, decoded)
    }
}
