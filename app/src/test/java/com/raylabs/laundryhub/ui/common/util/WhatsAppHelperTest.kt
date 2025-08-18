package com.raylabs.laundryhub.ui.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppHelperTest {
    @Test
    fun `formatPhone converts 08 to 62`() {
        assertEquals("628123456789", WhatsAppHelper.formatPhone("08123456789"))
    }

    @Test
    fun `formatPhone keeps 62 as is`() {
        assertEquals("628123456789", WhatsAppHelper.formatPhone("628123456789"))
    }

    @Test
    fun `formatPhone removes plus from plus 62`() {
        assertEquals("628123456789", WhatsAppHelper.formatPhone("+628123456789"))
    }

    @Test
    fun `formatPhone adds 62 for numbers starting with 8`() {
        assertEquals("628123456789", WhatsAppHelper.formatPhone("8123456789"))
    }

    @Test
    fun `formatPhone leaves international unchanged`() {
        assertEquals("65123456789", WhatsAppHelper.formatPhone("65123456789"))
    }

    @Test
    fun `buildOrderMessage formats WhatsApp order message as expected`() {
        val result = WhatsAppHelper.buildOrderMessage(
            customerName = "Andi",
            packageName = "Reguler",
            total = "10.000",
            paymentStatus = "Cash",
        )
        // Cek keyword/konten utama
        assert(result.contains("Halo, Kak Andi!"))
        assert(result.contains("- Paket: Reguler"))
        assert(result.contains("- Total: Rp 10.000"))
        assert(result.contains("- Status Bayar: Cash"))
    }
}