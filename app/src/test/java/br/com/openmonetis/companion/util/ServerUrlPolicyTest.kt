package br.com.openmonetis.companion.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlPolicyTest {
    @Test
    fun acceptsNormalizedHttpsOrigins() {
        assertEquals(
            "https://money.example.com",
            ServerUrlPolicy.normalize("  https://money.example.com/  ", allowLocalCleartext = false)
        )
    }

    @Test
    fun rejectsPublicCleartextUrls() {
        assertNull(ServerUrlPolicy.normalize("http://money.example.com", allowLocalCleartext = true))
    }

    @Test
    fun allowsCleartextOnlyForExplicitDevelopmentHosts() {
        assertEquals(
            "http://10.0.2.2:7002",
            ServerUrlPolicy.normalize("http://10.0.2.2:7002", allowLocalCleartext = true)
        )
        assertNull(
            ServerUrlPolicy.normalize("http://10.0.2.2:7002", allowLocalCleartext = false)
        )
    }

    @Test
    fun rejectsCredentialsPathsQueriesAndFragments() {
        assertNull(ServerUrlPolicy.normalize("https://user:secret@money.example.com"))
        assertNull(ServerUrlPolicy.normalize("https://money.example.com/openmonetis"))
        assertNull(ServerUrlPolicy.normalize("https://money.example.com?token=secret"))
        assertNull(ServerUrlPolicy.normalize("https://money.example.com#fragment"))
    }
}
