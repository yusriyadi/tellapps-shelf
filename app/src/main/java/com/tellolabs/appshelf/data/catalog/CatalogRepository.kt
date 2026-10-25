package com.tellolabs.appshelf.data.catalog

import android.content.Context
import android.util.AtomicFile
import com.tellolabs.appshelf.model.Catalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

const val CATALOG_URL = "https://yusriyadi.github.io/json/store-list.json"
@Serializable
private data class CachedCatalog(val json: String, val syncedAt: Long)
data class CatalogSnapshot(val catalog: Catalog, val syncedAt: Long)

class CatalogRepository(context: Context, private val client: OkHttpClient, private val json: Json) {
    private val cache = AtomicFile(File(context.filesDir, "catalog.json"))
    private val mutex = Mutex()

    fun parse(raw: String): Catalog {
        val catalog = json.decodeFromString<Catalog>(raw)
        require(catalog.schemaVersion == 1) { "Versi schema katalog tidak didukung: ${catalog.schemaVersion}" }
        require(catalog.apps.all { it.id.isNotBlank() && it.name.isNotBlank() }) { "Identitas atau nama aplikasi kosong" }
        require(catalog.apps.map { it.id }.distinct().size == catalog.apps.size) { "ID aplikasi duplikat dalam katalog" }
        return catalog
    }

    suspend fun cached(): CatalogSnapshot? = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val stored = json.decodeFromString<CachedCatalog>(cache.openRead().use { it.readBytes().toString(Charsets.UTF_8) })
                CatalogSnapshot(parse(stored.json), stored.syncedAt)
            }.getOrNull()
        }
    }

    suspend fun refresh(): CatalogSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            val raw = client.newCall(Request.Builder().url(CATALOG_URL).build()).execute().use { response ->
                check(response.isSuccessful) { "Server katalog mengembalikan HTTP ${response.code}" }
                val body = checkNotNull(response.body) { "Isi katalog kosong" }
                // Keep a malformed/oversized response from replacing the last valid cache.
                val buffer = okio.Buffer()
                val source = body.source()
                while (buffer.size <= 2_000_000) {
                    if (source.read(buffer, minOf(8_192L, 2_000_001L - buffer.size)) == -1L) break
                }
                val bytes = buffer.readByteArray()
                require(bytes.size <= 2_000_000) { "Katalog terlalu besar" }
                bytes.toString(Charsets.UTF_8)
            }
            val result = CatalogSnapshot(parse(raw), System.currentTimeMillis())
            val output = cache.startWrite()
            try {
                output.write(json.encodeToString(CachedCatalog(raw, result.syncedAt)).toByteArray())
                cache.finishWrite(output)
            } catch (e: Exception) {
                cache.failWrite(output)
                throw e
            }
            result
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) { mutex.withLock { cache.delete() } }
}
