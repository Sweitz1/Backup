package dev.sweitz.veniceglasses.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads a character card from a store link or a direct file link (see [CardStores]) and
 * saves it with [CharacterStore.importBytes], which checks it's a valid, allowed card.
 * No API key or account is sent to the store.
 */
class CardDownloader(
    private val store: CharacterStore,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun import(link: String): Character = withContext(Dispatchers.IO) {
        val urls = CardStores.downloadUrls(link)
        var last: Exception? = null
        for (url in urls) {
            try {
                val bytes = fetch(url)
                return@withContext store.importBytes(bytes, CardStores.fileName(link, CharacterCard.isPng(bytes)))
            } catch (e: CharacterCard.InvalidCard) {
                throw e // a real card problem (or refused): don't try other URLs
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("Download failed")
    }

    private fun fetch(url: String): ByteArray {
        val req = Request.Builder().url(url).header("User-Agent", "VeniceX3/0.2 (card import)").get().build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} from ${r.request.url.host}")
            val body = r.body ?: throw IOException("Empty download")
            if (body.contentLength() > CharacterStore.MAX_CARD_BYTES) throw IOException("Card is too large")
            val source = body.source()
            source.request(CharacterStore.MAX_CARD_BYTES + 1)
            if (source.buffer.size > CharacterStore.MAX_CARD_BYTES) throw IOException("Card is too large")
            return source.buffer.readByteArray()
        }
    }
}
