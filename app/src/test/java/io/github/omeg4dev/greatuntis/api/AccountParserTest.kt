package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.model.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AccountParserTest {

    @Test
    fun testParseQrUrl() {
        val qr = "untis://setschool?url=demo.webuntis.com&school=demo-schule&user=Muster_Max_20080101&key=JBSWY3DPEHPK3PXP&schoolNumber=1234567"
        val account = AccountParser.parse(qr)
        assertNotNull(account)
        assertEquals(
            Account(
                host = "demo.webuntis.com",
                school = "demo-schule",
                user = "Muster_Max_20080101",
                key = "JBSWY3DPEHPK3PXP",
                schoolNumber = "1234567"
            ),
            account
        )
    }

    @Test
    fun testParseQrUrlWithHttpsAndArbitraryOrder() {
        val qr = "untis://setschool?schoolNumber=1234567&key=jbswy+3dpe+hpk3+pxp&user=Muster_Max_20080101&school=demo-schule&url=https%3A%2F%2Fdemo.webuntis.com%2FWebUntis%2Findex.do"
        val account = AccountParser.parse(qr)
        assertNotNull(account)
        assertEquals("demo.webuntis.com", account?.host)
        assertEquals("demo-schule", account?.school)
        assertEquals("Muster_Max_20080101", account?.user)
        assertEquals("JBSWY3DPEHPK3PXP", account?.key)
        assertEquals("1234567", account?.schoolNumber)
    }

    @Test
    fun testParsePlainTextGermanWithColons() {
        val text = """
            Schulnummer: 1234567
            Schule: demo-schule
            Url: demo.webuntis.com
            Benutzer: Muster_Max_20080101
            Schlüssel: JBSWY 3DPE HPK3 PXP
        """.trimIndent()

        val account = AccountParser.parse(text)
        assertNotNull(account)
        assertEquals("demo.webuntis.com", account?.host)
        assertEquals("demo-schule", account?.school)
        assertEquals("Muster_Max_20080101", account?.user)
        assertEquals("JBSWY3DPEHPK3PXP", account?.key)
        assertEquals("1234567", account?.schoolNumber)
    }

    @Test
    fun testParsePlainTextEnglishWithTabsAndHttps() {
        val text = "School number\t1234567\nSchool\tdemo-schule\nServer\thttps://demo.webuntis.com/WebUntis/\nUser\tMuster_Max_20080101\nKey\tJBSWY3DPEHPK3PXP"
        val account = AccountParser.parse(text)
        assertNotNull(account)
        assertEquals("demo.webuntis.com", account?.host)
        assertEquals("demo-schule", account?.school)
        assertEquals("Muster_Max_20080101", account?.user)
        assertEquals("JBSWY3DPEHPK3PXP", account?.key)
        assertEquals("1234567", account?.schoolNumber)
    }

    @Test
    fun testMissingKeyReturnsNull() {
        val qr = "untis://setschool?url=demo.webuntis.com&school=demo-schule&user=Muster_Max_20080101"
        assertNull(AccountParser.parse(qr))

        val text = """
            Schule: demo-schule
            Url: demo.webuntis.com
            Benutzer: Muster_Max_20080101
        """.trimIndent()
        assertNull(AccountParser.parse(text))
    }

    @Test
    fun testInvalidKeyReturnsNull() {
        val qr = "untis://setschool?url=demo.webuntis.com&school=demo-schule&user=Muster_Max_20080101&key=abc!"
        assertNull(AccountParser.parse(qr))

        val text = """
            Schule: demo-schule
            Url: demo.webuntis.com
            Benutzer: Muster_Max_20080101
            Schlüssel: 12345
        """.trimIndent()
        assertNull(AccountParser.parse(text))
    }

    @Test
    fun testGarbageReturnsNull() {
        assertNull(AccountParser.parse("Hello World"))
        assertNull(AccountParser.parse(""))
        assertNull(AccountParser.parse("   \n\t  "))
        assertNull(AccountParser.parse("foo: bar\nbaz: qux"))
    }

    @Test
    fun userinfoInUrlCannotRedirectTheLogin() {
        val qr = "untis://setschool?url=https%3A%2F%2Fdemo.webuntis.com%40evil.example%2FWebUntis&school=demo-schule&user=Muster_Max_20080101&key=JBSWY3DPEHPK3PXP"
        org.junit.Assert.assertEquals("evil.example", AccountParser.parse(qr)?.host)
        val plain = "untis://setschool?url=demo.webuntis.com%40evil.example&school=demo-schule&user=Muster_Max_20080101&key=JBSWY3DPEHPK3PXP"
        org.junit.Assert.assertEquals("evil.example", AccountParser.parse(plain)?.host)
    }

    @Test
    fun hostWithInvalidCharactersIsRejected() {
        val qr = "untis://setschool?url=demo.webuntis.com%5Cevil&school=demo-schule&user=Muster_Max_20080101&key=JBSWY3DPEHPK3PXP"
        org.junit.Assert.assertNull(AccountParser.parse(qr))
    }
}
