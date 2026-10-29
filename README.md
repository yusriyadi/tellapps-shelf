# TellApps Shelf — Android

Aplikasi katalog Android sederhana untuk app Yusriyadi. Satu modul dengan **Kotlin, Jetpack Compose Material 3, MVVM, dan repository layer**; dependency injection manual lewat `AppContainer`.

- Application ID: `com.tellolabs.appshelf`
- Minimum Android 8.0 / API 26; compile/target API 36
- Versi awal: `0.1.0` (version code 1)
- Sumber: https://yusriyadi.github.io/json/store-list.json

## Fitur

- Katalog, pencarian lokal, detail, repository, dan riwayat rilis sesuai urutan JSON.
- Cek update TellApps Shelf dari Pengaturan berdasarkan package name dan version code; buka detail rilis untuk download/install melalui alur yang sudah ada. Versi terpasang memakai konfigurasi build, bukan teks versi manual.
- Tombol Buka aplikasi pada detail untuk aplikasi terpasang yang memiliki launcher activity yang dapat diakses.
- Status belum terpasang, terpasang, update tersedia, versi perangkat lebih baru, atau tidak diketahui pada katalog dan detail. Perbandingan memakai version code, bukan nama versi.
- Fallback metadata/icon kosong, refresh manual, cache JSON atomik, waktu sinkronisasi, dan katalog offline terakhir.
- DownloadManager: antrean, progres, pembatalan, coba lagi, status tersimpan di DataStore, dan pemulihan status saat aplikasi dibuka kembali.
- File APK disimpan di direktori external app-specific dengan nama UUID. Tidak membutuhkan izin storage luas.
- Install melalui FileProvider dan installer Android, setelah pengguna memberikan izin sumber instalasi. Setelah kembali dari pengaturan, ketuk Install APK lagi. Membatalkan installer tidak menghapus APK atau mengubahnya menjadi status terpasang.
- Unduhan siap install dapat dihapus. Checksum SHA-256 diperiksa bila tersedia dalam katalog; checksum berbeda memblokir install.
- Tema sistem/terang/gelap, pembersihan cache katalog, grid katalog pada layar lebar, dan konten detail dengan lebar terbatas.

## Menjalankan

Buka folder ini di Android Studio. Gunakan JDK 17 dan SDK platform Android 36. Atur lokasi SDK di `local.properties` (tidak dikomit).

```sh
./gradlew :app:assembleDebug
android run --apks=app/build/outputs/apk/debug/app-debug.apk
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Struktur

```text
app/src/main/java/com/tellolabs/appshelf/
  AppShelfApplication.kt   # application container
  MainActivity.kt          # lifecycle, permission, installer, external intents
  model/                   # katalog, rilis, snapshot metadata unduhan
  data/catalog/            # OkHttp, serialization, validasi, cache atomik
  data/download/           # DownloadManager, persistensi, checksum
  data/settings/           # DataStore dan tema
  platform/install/        # URI FileProvider dan installer
  ui/                      # ViewModel, Compose screens, navigation, theme
```

`AppViewModel` mengekspos StateFlow; Compose mengoleksinya mengikuti lifecycle. Polling DownloadManager hanya aktif saat Activity minimal STARTED. Transfer background ditangani sistem, tanpa service atau WorkManager sendiri.

## Pemeriksaan manual sebelum rilis

1. Buka online, cari aplikasi, buka detail; coba pencarian tanpa hasil dan Reset.
2. Buka ulang offline setelah sinkronisasi; refresh gagal harus mempertahankan katalog terakhir.
3. Download APK, batalkan/coba lagi, tutup aplikasi dan buka kembali; status harus dipulihkan.
4. Setelah selesai, coba izin sumber install ditolak/diizinkan, kembali dari installer, lalu hapus APK.
5. Coba URL gagal, file hilang, tema gelap, rotasi, font besar, dan layar tablet.

Build debug berhasil dan fitur serta instalasi sudah diperiksa manual oleh pemilik proyek pada perangkat. Pengujian perbandingan version code tersedia pada `AppInstallationTest`. Belum ada instrumentation test.

## Batasan dan rilis

- APK tunggal/universal. Belum ada split APK/AAB, update otomatis, login, atau notifikasi rilis.
- Berbagi URL detail website ditunda sampai base URL deployment ditentukan.
- Release signing memakai keystore khusus rilis, terpisah dari debug key. Simpan cadangan keystore dan kredensial dengan aman; gunakan key yang sama untuk pembaruan berikutnya.
- APK release tidak dapat menggantikan instalasi debug dengan package yang sama karena signing berbeda. Untuk beralih dari debug, uninstall build debug secara manual lebih dahulu; data lokal dan unduhan app-specific ikut dapat terhapus.
- File APK milik TellApps Shelf dapat ikut hilang saat TellApps Shelf di-uninstall. Cache dibersihkan terpisah dari file unduhan.
- Perbaikan arsitektur lebih lanjut dan pengujian menyeluruh dapat dilakukan pada iterasi berikutnya.

Lihat [RANCANGAN.md](RANCANGAN.md) untuk rancangan lengkap.

## Build release

Keystore dan `keystore.properties` disimpan lokal dan diabaikan Git. Untuk checkout baru, pulihkan keduanya dari cadangan pemilik proyek. Contoh struktur konfigurasi (ganti placeholder dengan kredensial lokal):

```properties
storeFile=.signing/tellapps-shelf-release.jks
storePassword=<password lokal>
keyAlias=tellapps-shelf
keyPassword=<password lokal>
```

```sh
./gradlew :app:assembleRelease
```

APK bertanda tangan: `app/build/outputs/apk/release/app-release.apk`. Jangan mengunggah keystore atau `keystore.properties` ke repository maupun GitHub Release.

## Status instalasi dan update

Status diperiksa melalui PackageManager pada repository, lalu disalurkan oleh ViewModel ke UI. Pemeriksaan diulang saat Activity RESUMED, katalog berubah, atau ada perubahan paket ketika UI aktif. Kembali dari installer yang dibatalkan tidak dianggap berhasil memasang aplikasi; status dibaca ulang dari perangkat.

Rilis pertama dalam array `releases` dianggap terbaru. Version code lebih tinggi di katalog berarti update tersedia; version code lebih rendah di katalog ditampilkan sebagai versi perangkat lebih baru. Version name hanya untuk tampilan. Kode versi yang tidak tersedia atau tidak valid menghasilkan status tidak diketahui.

Visibilitas paket Android 11+ dibatasi pada LocalGuard (`com.tellolabs.localguard`), AppFin (`com.tellolabs.appfin`), dan Tellauncher (`id.tellolabs.launcher`) melalui `<queries>`. Package aplikasi sendiri otomatis terlihat. Untuk app katalog baru, tambahkan package di manifest **dan** `InstallationRepository.queryablePackages`. Jika sebuah package tidak ditemukan dan visibilitasnya belum dijamin, status tetap tidak diketahui. Tidak menggunakan izin `QUERY_ALL_PACKAGES`.

Download tetap tersedia untuk mengambil ulang APK maupun rilis lama. Hanya rilis terbaru yang lebih tinggi dari versi terpasang diberi label tombol Download update; riwayat rilis tidak dipromosikan sebagai update. Status instalasi tidak mengubah status file di Unduhan. Update tetap memerlukan signing yang cocok dan konfirmasi installer Android.

Tombol **Buka aplikasi** muncul di detail bila PackageManager menyediakan launch intent. Saat tombol diketuk, Activity mencari intent terbaru dan membuka aplikasi tersebut. Jika app telah dihapus atau aksesnya ditolak, pengguna mendapat pesan dan Shelf tetap terbuka. Aplikasi tanpa launcher activity tidak mendapat tombol Buka; pemeriksaan ini tidak bergantung pada ketersediaan version code katalog.

## Pembaruan TellApps Shelf

Buka **Pengaturan → Update TellApps Shelf → Cek update**. Metadata aplikasi sendiri dicari berdasarkan `packageName` yang sama dengan application ID, bukan ID atau nama tampilan katalog. Jika version code rilis pertama lebih tinggi daripada build terpasang, tombol **Lihat update** membuka detail rilis untuk download dan install. Tidak ada pengunduhan atau instalasi otomatis.

Pemeriksaan memakai refresh katalog yang sama dengan halaman Katalog. Jika jaringan gagal, data lama tetap terlihat dengan pesan error dan penanda katalog tersimpan; hasil ini bukan konfirmasi bahwa server sudah diperiksa. Jika metadata aplikasi sendiri atau version code belum tersedia, status tidak disimpulkan sebagai versi terbaru. APK update harus memakai signing key yang sama dengan aplikasi terpasang; debug dan release memakai key berbeda.
