package dev.sweitz.veniceglasses

import dev.sweitz.veniceglasses.data.Credits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditsTest {
    @Test fun classifiesLicences() {
        assertEquals(Credits.LicenseKind.PERMISSIVE, Credits.kindOf("Apache-2.0"))
        assertEquals(Credits.LicenseKind.COPYLEFT, Credits.kindOf("GPL-3.0"))
        assertEquals(Credits.LicenseKind.WEAK_COPYLEFT, Credits.kindOf("EPL-1.0"))
        assertEquals(Credits.LicenseKind.CUSTOM, Credits.kindOf("NOASSERTION"))
        assertEquals(Credits.LicenseKind.NONE, Credits.kindOf("NONE"))
    }

    @Test fun flagsTheUnlicensedSdkAndCreditsCommunity() {
        val c = Credits
        assertTrue(c.unlicensedDependencies.any { it.name.contains("Mercury") })
        assertEquals(c.community.size, c.community.map { it.repo }.toSet().size)
        assertTrue(c.community.any { it.repo == "Dun-John/x3bluetooth" && it.kind == Credits.LicenseKind.PERMISSIVE })
    }
}
