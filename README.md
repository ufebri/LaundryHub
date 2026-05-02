# LaundryHub
<img src="./assets/ic_branding.png" height="500px" />

LaundryHub adalah aplikasi manajemen laundry modern yang menggunakan arsitektur **Kotlin Multiplatform (KMP)**.

## Arsitektur
- **Frontend (Android):** Kotlin, Jetpack Compose, Paging 3.
- **Backend:** Ktor Server (Netty), PostgreSQL (Supabase), Exposed ORM.
- **Shared Module:** KMP untuk berbagi model data dan logika networking (Ktor Client).

## Fitur Utama
- Manajemen Transaksi (Order & Outcome) dengan Backend terpusat.
- Sinkronisasi otomatis ke Google Sheets sebagai backup/arsip.
- Paging 3 untuk performa list yang cepat dan efisien.
- Zero-Configuration (Login langsung masuk ke dashboard).

## Dokumentasi
Lihat rencana migrasi lengkap di [docs/migration/MIGRATION_PLAN.md](./docs/migration/MIGRATION_PLAN.md).

## Konfigurasi
Aplikasi menggunakan `config.properties` untuk konfigurasi lingkungan. Pastikan `BASE_URL_DEBUG` dan `BASE_URL_RELEASE` terkonfigurasi dengan benar untuk target lingkungan yang sesuai.

## Branding
- Logo & Branding dapat disesuaikan melalui file di direktori `app/src/main/res/`.
