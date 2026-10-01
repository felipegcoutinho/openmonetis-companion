package br.com.openmonetis.companion.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanionQrCodeTest {
    private val token = "opm_${"a".repeat(43)}"

    @Test
    fun readsVersionedCompanionPayload() {
        assertEquals(
            token,
            CompanionQrCode.extractToken("openmonetis://companion/token?v=1&token=$token")
        )
    }

    @Test
    fun acceptsRawTokenAsManualFallback() {
        assertEquals(token, CompanionQrCode.extractToken(token))
    }

    @Test
    fun rejectsUnknownVersionsAndDestinations() {
        assertNull(CompanionQrCode.extractToken("openmonetis://companion/token?v=2&token=$token"))
        assertNull(CompanionQrCode.extractToken("https://example.com/?v=1&token=$token"))
    }

    @Test
    fun rejectsMalformedTokensAndAdditionalParameters() {
        assertNull(CompanionQrCode.extractToken("openmonetis://companion/token?v=1&token=secret"))
        assertNull(
            CompanionQrCode.extractToken(
                "openmonetis://companion/token?v=1&token=$token&redirect=https://example.com"
            )
        )
    }

    @Test
    fun rejectsAuthorityOrFragmentVariations() {
        assertNull(
            CompanionQrCode.extractToken("openmonetis://user@companion/token?v=1&token=$token")
        )
        assertNull(
            CompanionQrCode.extractToken("openmonetis://companion:443/token?v=1&token=$token")
        )
        assertNull(
            CompanionQrCode.extractToken("openmonetis://companion/token?v=1&token=$token#ignored")
        )
    }
}
