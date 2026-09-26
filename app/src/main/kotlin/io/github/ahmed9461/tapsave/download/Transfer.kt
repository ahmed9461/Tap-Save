package io.github.ahmed9461.tapsave.download

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

class SaveFailure(val reason: Reason) : IOException(reason.name) {
    enum class Reason { UNAVAILABLE, RESTRICTED, UNSUPPORTED, NETWORK, TOO_LARGE, STORAGE, INTERRUPTED }
}

class TransferCancellation {
    private val cancelled = AtomicBoolean()
    @Volatile private var connection: HttpURLConnection? = null
    fun check() { if (cancelled.get()) throw CancellationException("Cancelled") }
    fun attach(value: HttpURLConnection) { connection = value; check() }
    fun detach(value: HttpURLConnection) { if (connection === value) connection = null }
    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            val active = connection
            // Closing an active socket must not stall the activity/notification main thread.
            if (active != null) Thread({ active.disconnect() }, "TapSave-cancel").start()
        }
    }
}

object NetworkPolicy {
    fun page(uri: URI): Boolean = base(uri) && uri.host.lowercase() in setOf("instagram.com", "www.instagram.com") &&
        Regex("/(?:reels?/[A-Za-z0-9_-]{1,64}(?:/embed)?|share/reel/[A-Za-z0-9_-]{1,64})/?").matches(uri.path)
    fun media(uri: URI): Boolean = base(uri) && listOf("cdninstagram.com", "fbcdn.net").any {
        uri.host.lowercase() == it || uri.host.lowercase().endsWith(".$it")
    }
    private fun base(uri: URI) = uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null && uri.port == -1 && uri.rawFragment == null
}

/** No cookies, credentials, hidden retries, private endpoints or redirects outside the selected policy. */
class HttpTransfer(private val connect: (URI) -> HttpURLConnection = { it.toURL().openConnection() as HttpURLConnection }) {
    class Response(val uri: URI, val connection: HttpURLConnection, private val cancellation: TransferCancellation) : Closeable {
        val length = connection.contentLengthLong.takeIf { it > 0 }
        val type = connection.contentType.orEmpty().substringBefore(';').lowercase()
        val input: InputStream get() = object : java.io.FilterInputStream(connection.inputStream) {
            override fun read(): Int = networkRead { super.read() }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = networkRead { super.read(buffer, offset, length) }
            private fun networkRead(read: () -> Int): Int = try { read() } catch (_: IOException) {
                cancellation.check()
                throw SaveFailure(SaveFailure.Reason.NETWORK)
            }
        }
        override fun close() { cancellation.detach(connection); connection.disconnect() }
    }

    fun get(url: String, allowed: (URI) -> Boolean, cancellation: TransferCancellation): Response {
        var uri = try { URI(url) } catch (_: Exception) { throw SaveFailure(SaveFailure.Reason.UNSUPPORTED) }
        repeat(5) {
            cancellation.check()
            if (!allowed(uri)) throw SaveFailure(SaveFailure.Reason.RESTRICTED)
            val connection = connect(uri)
            var handedOff = false
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", "TapSave/0.2 (Android; public media save)")
                connection.setRequestProperty("Accept-Encoding", "identity")
                cancellation.attach(connection)
                when (connection.responseCode) {
                    200 -> { handedOff = true; return Response(uri, connection, cancellation) }
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw SaveFailure(SaveFailure.Reason.NETWORK)
                        uri = uri.resolve(location)
                    }
                    401, 403 -> throw SaveFailure(SaveFailure.Reason.RESTRICTED)
                    404, 410 -> throw SaveFailure(SaveFailure.Reason.UNAVAILABLE)
                    else -> throw SaveFailure(SaveFailure.Reason.NETWORK)
                }
            } catch (failure: IOException) {
                cancellation.check()
                throw if (failure is SaveFailure) failure else SaveFailure(SaveFailure.Reason.NETWORK)
            } finally {
                if (!handedOff) { cancellation.detach(connection); connection.disconnect() }
            }
        }
        throw SaveFailure(SaveFailure.Reason.NETWORK)
    }

    fun page(url: String, cancellation: TransferCancellation): Pair<String, String> = get(url, NetworkPolicy::page, cancellation).use { response ->
        if (response.type !in setOf("text/html", "application/xhtml+xml")) throw SaveFailure(SaveFailure.Reason.UNSUPPORTED)
        val max = 3 * 1024 * 1024
        if ((response.length ?: 0) > max) throw SaveFailure(SaveFailure.Reason.TOO_LARGE)
        val bytes = response.input.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                cancellation.check()
                val read = input.read(buffer)
                if (read < 0) break
                if (output.size() + read > max) throw SaveFailure(SaveFailure.Reason.TOO_LARGE)
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        response.uri.toString() to bytes.toString(Charsets.UTF_8)
    }
}
