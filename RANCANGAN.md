# Rancangan TellApps Shelf Android

Status: implementasi MVP awal tersedia. Lihat README.md untuk fitur, cara menjalankan, serta validasi dan pekerjaan rilis yang masih tersisa.

## 1. Tujuan dan asumsi

Aplikasi Android untuk menjelajahi app buatan Yusriyadi, membaca catatan rilis, mengunduh APK, lalu membuka installer Android. Katalog menggunakan sumber yang sama dengan website:

`https://yusriyadi.github.io/json/store-list.json`

Data awal: LocalGuard, AppFin, dan TelLauncher. GitHub Pages menyimpan metadata; GitHub Releases menyimpan APK. Tidak memerlukan backend baru atau layanan berbayar.

Asumsi kerja: distribusi katalog dan APK bersifat publik, aplikasi dipasang melalui APK, dan platform awal Android. Nama aplikasi Android: **TellApps Shelf**. Akses publik masih perlu disepakati bila nantinya ada build khusus tester.

Usulan minimum Android 8.0 (API 26). compileSdk, targetSdk, AGP, Kotlin, dan versi library dipilih dari versi stabil yang kompatibel saat implementasi, bukan dikunci dalam rancangan ini. Application ID katalog ditetapkan sebelum rilis pertama; keystore katalog disimpan untuk pembaruan selanjutnya.

## 2. Cakupan fungsional

| ID | Fitur | Kriteria perilaku |
| --- | --- | --- |
| F01 | Katalog | Memuat nama, icon/inisial, deskripsi, dan label versi dari JSON |
| F02 | Pencarian | Memfilter nama dan deskripsi secara lokal; pencarian kosong punya tombol reset |
| F03 | Detail aplikasi | Menampilkan deskripsi, package name jika tersedia, informasi versi, dan repository |
| F04 | Riwayat rilis | Menampilkan rilis dan changelog, dengan download per rilis |
| F05 | Refresh | Refresh manual tanpa menghilangkan katalog yang masih valid |
| F06 | Cache offline | Menampilkan katalog terakhir yang berhasil dimuat beserta waktu sinkronisasi |
| F07 | Download APK | Menampilkan antrean, progres jika ukuran diketahui, pembatalan, kegagalan, dan coba lagi |
| F08 | Install | Pengguna mengetuk Install setelah download selesai; aplikasi membuka installer sistem |
| F09 | Pengelolaan unduhan | Melihat APK siap install dan menghapus file unduhan milik katalog |
| F10 | Berbagi | Membagikan URL detail website; base URL website menjadi konfigurasi setelah alamat deployment diketahui |
| F11 | Pengaturan | Tema sistem/terang/gelap, informasi sumber katalog, dan pembersihan cache |

F01–F09 adalah MVP utama. F10–F11 dapat menyusul bila ingin mempercepat rilis pertama.

Di luar MVP: login, undangan tester, upload dari aplikasi, silent install, auto-update, notifikasi rilis baru, rating, komentar, dan analitik. Manajemen konten tetap melalui JSON serta GitHub Releases.

## 3. Struktur layar dan arah visual

Gunakan Compose Material 3 dengan warna hijau sage, latar netral, dan identitas yang konsisten dengan website. Icon yang belum tersedia memakai inisial. Light/dark theme mengikuti sistem secara default. Dynamic color dapat ditambahkan kemudian.

Navigasi utama cukup dua tujuan: **Katalog** dan **Unduhan**. Pengaturan diakses dari toolbar; detail aplikasi menjadi layar tersendiri dengan tombol kembali. Navigasi tidak memerlukan akun atau onboarding wajib.

```text
Katalog                         Detail aplikasi
┌───────────────────────┐       ┌───────────────────────┐
│ TellApps Shelf         ⚙   │       │ ← Detail aplikasi     │
│ [ Cari aplikasi…    ] │       │ [icon] LocalGuard     │
│ 3 aplikasi            │       │ Deskripsi aplikasi    │
│                       │       │ Versi 0.2.0           │
│ [L] LocalGuard        │  →    │ [Download APK]        │
│     Versi 0.2.0       │       │                       │
│ [A] AppFin            │       │ Catatan rilis         │
│ [T] TelLauncher       │       │ Riwayat versi         │
│                       │       │ Repository · Bagikan  │
│ Katalog     Unduhan   │       └───────────────────────┘
└───────────────────────┘
```

Kartu katalog membuka detail; CTA download utama berada di detail agar pengguna memahami versi yang dipilih. Progres unduhan juga muncul pada rilis terkait. Tombol berubah menjadi **Install APK** setelah file siap.

Pada tablet, gunakan grid katalog dengan lebar kartu yang dibatasi; detail tetap mudah dibaca dengan lebar konten maksimum. Dukung font besar, screen reader, target sentuh minimal 48 dp, edge-to-edge dan keyboard insets. Status tidak dibedakan hanya lewat warna.

## 4. Alur penggunaan

### Membuka katalog

1. Baca cache valid dan tampilkan segera jika tersedia.
2. Ambil JSON dari URL tetap melalui HTTPS.
3. Validasi versi schema dan struktur data, lalu simpan cache secara atomik.
4. Perbarui layar. Jika refresh gagal, pertahankan cache dan tampilkan pesan singkat.
5. Jika belum ada cache, tampilkan loading, lalu katalog atau halaman error dengan Coba lagi.

Katalog kosong yang valid adalah hasil sukses. JSON rusak tidak boleh menimpa cache terakhir. Metadata null tidak menjadi alasan menolak seluruh katalog; identitas app duplikat atau schema tak didukung harus menghasilkan error yang jelas.

### Download dan install

1. Pengguna memilih app dan rilis, lalu mengetuk Download APK.
2. Cegah unduhan duplikat untuk kombinasi app ID dan URL rilis yang sama.
3. Enqueue lewat Android DownloadManager; simpan download ID serta identitas rilis secara persisten. Download tetap ditangani sistem saat UI ditutup.
4. Saat layar aktif, query status untuk progres. Jika total byte belum diketahui, tampilkan indikator tanpa persentase. Pulihkan status melalui query saat aplikasi dibuka kembali, termasuk jika callback selesai terlewat.
5. Selesai mengunduh tidak berarti terpasang. File harus ada dan dapat dibaca sebelum CTA Install APK aktif.
6. Saat pengguna mengetuk Install, periksa izin sumber instalasi dengan `canRequestPackageInstalls()`. Bila belum diizinkan, buka pengaturan sumber instalasi untuk aplikasi katalog; setelah kembali, periksa lagi. Jika ditolak, APK tetap tersedia.
7. Berikan URI `content://` melalui FileProvider dengan izin baca sementara dan MIME APK kepada installer sistem. Pengguna mengonfirmasi instalasi di Android. Jangan membuka installer otomatis dari callback background.
8. Jika pengguna membatalkan installer, pertahankan status **Siap install**. Jangan menyimpulkan instalasi berhasil hanya karena installer sudah ditutup.

Gunakan direktori external app-specific untuk APK, tanpa izin akses storage luas. File dihapus hanya dari area milik aplikasi; penghapusan unduhan aktif juga membatalkan request. Setelah uninstall katalog, file app-specific tidak dijamin tetap tersedia. Uji URI installer dan akses file pada perangkat/API yang didukung.

Saat URL rilis berubah, unduhan lama tetap menunjuk snapshot rilis semula. Jangan menimpa file memakai nama APK dari URL; gunakan nama lokal yang unik dan terkontrol. MVP mendukung APK tunggal/universal, belum split APK atau AAB.

## 5. State yang harus terlihat

| Bagian | State |
| --- | --- |
| Katalog | Loading awal, konten, konten dari cache, refresh, kosong, error tanpa cache |
| Pencarian | Hasil ditemukan, tidak ditemukan |
| Detail | App ditemukan, ID sudah tidak ada dalam katalog, metadata belum tersedia |
| Download | Antre, berjalan, dijeda sistem, gagal, dibatalkan, selesai, file hilang |
| Install | Siap install, izin sumber belum aktif, installer tidak tersedia, pengguna kembali dari installer |

Jika APK tidak cocok dengan perangkat atau update ditolak oleh installer, arahkan pengguna memeriksa kompatibilitas dan signing. Jangan otomatis menghapus app lama, karena itu dapat menghapus datanya.

## 6. Kontrak katalog

Gunakan `schemaVersion: 1` yang sama dengan website; tidak mengganti struktur JSON untuk kebutuhan Android saja.

| Field | Penggunaan dan fallback |
| --- | --- |
| `id` | Identitas stabil dan unik; tidak berubah ketika nama app berubah |
| `name` | Nama tampilan, wajib tidak kosong |
| `description` | Null → deskripsi belum tersedia |
| `iconUrl` | Null/gagal → inisial nama |
| `packageName` | Null → status instalasi tidak diketahui |
| `repositoryUrl` | Null/URL tidak valid → tombol repository disembunyikan |
| `releases[].apkUrl` | HTTPS valid → download; tidak valid → tombol dinonaktifkan |
| `versionName` | Null → nomor versi belum tersedia; bukan angka pembanding update |
| `versionCode` | Null → tidak dapat menentukan update |
| `releasedAt` | Tanggal ISO 8601; null → tanggal tidak ditampilkan |
| `minSdk` | Jika diketahui melebihi API perangkat, download ditandai tidak kompatibel |
| `changelog` | Array kosong → catatan rilis belum tersedia |

Konvensi utama: susun `releases` dari terbaru ke terlama. Kedua klien sebaiknya mengikuti konvensi yang sama; jangan menyortir sebagian pasangan berdasarkan metadata yang tidak lengkap karena urutan bisa tidak konsisten. Penyamaan aturan dengan website adalah pekerjaan implementasi terpisah, bukan perubahan dalam dokumen ini.

Tambahan opsional kompatibel ke depan: `sizeBytes` untuk perkiraan ukuran dan `sha256` untuk verifikasi file terhadap metadata. Checksum membantu mendeteksi file berbeda/rusak; tidak menggantikan kepercayaan pada sumber katalog atau signing Android. Jika checksum disediakan dan tidak cocok, blokir install file tersebut.

## 7. Arsitektur dan teknologi yang diusulkan

Satu modul `app` terlebih dahulu. Gunakan Kotlin, Jetpack Compose Material 3, ViewModel, Coroutines/StateFlow, OkHttp + Kotlin Serialization untuk katalog, Coil untuk icon, dan Navigation Compose untuk dua tujuan utama serta detail. Pilihan ini usulan; versi dependency ditentukan saat setup.

```text
Compose UI → ViewModel → CatalogRepository → HTTPS + cache JSON
                      → DownloadRepository → DownloadManager + DataStore
Activity / UI host    → InstallCoordinator → Settings + FileProvider + installer
```

- Repository mengelola data, cache, dan mapping DTO ke model UI/domain sederhana.
- ViewModel mengekspos state immutable; UI mengirim action dan mengoleksi state mengikuti lifecycle.
- Simpan cache katalog sebagai file JSON atomik. DataStore menyimpan preferensi, waktu sinkronisasi, dan metadata unduhan yang kecil; jika histori membesar, evaluasi Room.
- Activity/UI host menangani peluncuran pengaturan serta installer. ViewModel tidak menyimpan Activity, launcher, atau permission dialog.
- Polling progres hanya saat ada layar aktif yang membutuhkan progres. DownloadManager mengerjakan transfer background; MVP tidak memerlukan foreground service atau WorkManager sendiri.
- Manual dependency injection melalui application container cukup untuk skala ini.

Struktur paket yang diusulkan:

```text
app/src/main/java/<application-package>/
  AppShelfApplication.kt
  MainActivity.kt
  data/catalog/       # DTO, HTTP client, cache, repository
  data/download/      # mapping download ID, status, persistence
  model/              # CatalogApp, AppRelease, DownloadItem
  platform/install/   # settings intent, FileProvider, installer
  ui/catalog/
  ui/detail/
  ui/downloads/
  ui/settings/
  ui/navigation/
  ui/theme/
```

Izin MVP: `INTERNET` dan `REQUEST_INSTALL_PACKAGES` untuk alur instalasi yang dimulai pengguna. Jangan meminta izin sumber install saat first launch. Jika nanti menambah notifikasi milik aplikasi, rancang izin notifikasinya tersendiri. Rancangan tidak mengasumsikan distribusi via Google Play; jalur itu perlu evaluasi kebijakan instalasi paket sebelum publikasi.

## 8. Tahap berikutnya: status terpasang dan update

Fitur ini bergantung pada `packageName` dan `versionCode` yang benar. Nilai metadata saat ini belum mencukupi, sehingga MVP tetap menampilkan Download/Install APK.

Setelah metadata lengkap:

- Deklarasikan package yang memang perlu diperiksa melalui `<queries>` di manifest. Android 11+ membatasi visibilitas paket; tambahan package baru di JSON saja tidak otomatis memperluas visibilitas.
- Jangan menyimpulkan tidak terpasang jika visibilitas tidak dapat dipastikan; tampilkan status tidak diketahui. Hindari `QUERY_ALL_PACKAGES` untuk katalog kecil ini.
- Bandingkan `longVersionCode`: remote lebih tinggi → Update, sama → Terpasang, remote lebih rendah → Versi perangkat lebih baru. Jangan menawarkan downgrade sebagai update.
- Tombol Buka hanya muncul bila ada launcher activity yang dapat diakses.
- Cek ulang saat resume dan setelah perubahan paket yang relevan. Signing yang kompatibel tetap diperlukan agar update diterima Android.

## 9. Tahapan implementasi dan penerimaan

| Tahap | Hasil yang bisa diperiksa |
| --- | --- |
| 1. Fondasi | Project dapat dijalankan, Compose Material 3, navigasi dan tema dasar |
| 2. Katalog | Tiga app dari JSON, pencarian, detail, riwayat, null fallback, refresh dan cache offline |
| 3. Distribusi | Download APK, progres, cancel/retry, pemulihan status setelah process death, daftar unduhan |
| 4. Instalasi | Izin sumber tepat saat dibutuhkan, installer sistem, penanganan batal dan file hilang |
| 5. Penyelesaian MVP | Aksesibilitas, tablet, dark mode, APK katalog bertanda tangan dan dokumentasi rilis |
| 6. Pengembangan lanjutan | Metadata lengkap, status terpasang/update, berbagi dan preferensi tambahan |

Validasi terarah saat implementasi:

- Parser: metadata null, schema tidak didukung, ID duplikat, URL invalid, dan JSON rusak yang tidak merusak cache.
- UI: pencarian tanpa hasil, first launch offline, cache offline, dan refresh gagal dengan konten lama tetap terlihat.
- Download: GitHub redirect, URL 404, koneksi terputus, ruang habis, ukuran tak diketahui, dua klik cepat, cancel, serta aplikasi dibuka kembali setelah proses dihentikan.
- Install: sumber belum diizinkan/ditolak, kembali dari pengaturan, installer dibatalkan, APK rusak, file dihapus, dan update dengan signing berbeda.
- Layout: perangkat minimum yang didukung, Android versi target, Android 11+ untuk visibility saat fitur itu ditambahkan, tablet, rotasi dan font besar.

Implementasi awal menggunakan nama TellApps Shelf, application ID `com.tellolabs.appshelf`, dan cakupan download-install MVP. Project Gradle sudah tersedia. Signing rilis dikonfigurasi melalui keystore lokal di luar Git; fitur dan instalasi sudah diperiksa manual pada perangkat. Pengujian skenario lanjutan tetap dapat dilakukan pada iterasi berikutnya.

## 10. Referensi teknis

- [Rekomendasi arsitektur Android](https://developer.android.com/topic/architecture/recommendations): Compose, ViewModel, repository, dan aliran state.
- [DownloadManager](https://developer.android.com/reference/android/app/DownloadManager): pengelolaan unduhan oleh sistem.
- [PackageManager.canRequestPackageInstalls](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()): pemeriksaan izin sumber instalasi.
- [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider): berbagi file melalui content URI.
- [Deklarasi package visibility](https://developer.android.com/training/package-visibility/declaring): query package yang diperlukan untuk fitur status terpasang.
