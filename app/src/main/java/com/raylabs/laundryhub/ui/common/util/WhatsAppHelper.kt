package com.raylabs.laundryhub.ui.common.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import java.net.URLEncoder

object WhatsAppHelper {
    fun buildOrderMessage(
        customerName: String,
        packageName: String,
        total: String,
        paymentStatus: String,
        alamatLaundry: String = "[alamat laundry kamu]"
    ): String {
        return """
            Halo, Kak $customerName!

            Terima kasih sudah laundry di Ray Labs Laundry.

            Detail order:
            - Paket: $packageName
            - Total: Rp $total
            - Status Bayar: $paymentStatus

            Laundry siap diambil/akan dikabari jika selesai.
            Alamat: $alamatLaundry

            (Ray Labs Laundry)
        """.trimIndent()
    }

    fun formatPhone(phone: String): String {
        // Convert 08xxx -> 628xxx
        return when {
            phone.startsWith("0") -> "62" + phone.drop(1)
            phone.startsWith("+62") -> phone.replace("+", "")
            phone.startsWith("8") -> "62$phone"
            else -> phone
        }
    }

    fun sendWhatsApp(context: Context, phone: String, message: String) {
        val formattedPhone = formatPhone(phone)
        val uri =
            "https://wa.me/$formattedPhone?text=${URLEncoder.encode(message, "UTF-8")}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp tidak terinstal!", Toast.LENGTH_SHORT).show()
        }
    }
}