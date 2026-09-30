package io.github.ahmed9461.tapsave

import io.github.ahmed9461.tapsave.download.HttpTransfer
import java.io.Closeable
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HttpFixture(private val response: (String) -> Reply) : Closeable {
    data class Reply(val type: String, val bytes: ByteArray, val declaredLength: Int = bytes.size, val delayMs: Long = 0, val status: Int = 200, val location: String? = null)
    private val server = ServerSocket(0)
    private val clients = Collections.synchronizedList(mutableListOf<Socket>())
    val headers = Collections.synchronizedList(mutableListOf<Map<String, String>>())
    val paths = Collections.synchronizedList(mutableListOf<String>())
    private val worker = Executors.newCachedThreadPool()
    val http = HttpTransfer { uri -> URL("http://127.0.0.1:${server.localPort}${uri.rawPath}").openConnection() as HttpURLConnection }
    init {
        worker.execute {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: java.io.IOException) { break }
                clients += socket
                worker.execute {
                    try { socket.use {
                        val reader = it.getInputStream().bufferedReader()
                        val path = reader.readLine().split(' ')[1]
                        val fields = mutableMapOf<String, String>()
                        while (true) {
                            val line = reader.readLine()
                            if (line.isNullOrBlank()) break
                            fields[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
                        }
                        headers += fields
                        paths += path
                        val reply = response(path)
                        val output = it.getOutputStream()
                        output.write(("HTTP/1.1 ${reply.status} Fixture\r\nContent-Type: ${reply.type}\r\nContent-Length: ${reply.declaredLength}\r\nConnection: close\r\n" +
                            (reply.location?.let { location -> "Location: $location\r\n" } ?: "") + "\r\n").toByteArray())
                        reply.bytes.asList().chunked(512).forEach { chunk ->
                            output.write(chunk.toByteArray()); output.flush()
                            if (reply.delayMs > 0) Thread.sleep(reply.delayMs)
                        }
                    } } catch (_: Exception) { /* Expected disconnect during cancellation. */ }
                }
            }
        }
    }
    override fun close() {
        server.close()
        synchronized(clients) { clients.forEach { it.close() } }
        worker.shutdownNow()
        check(worker.awaitTermination(5, TimeUnit.SECONDS))
    }
}

fun publicEmbed(code: String, restricted: Boolean = false, wrapped: Boolean = true): String {
    val context = org.json.JSONObject().put("context", org.json.JSONObject().put("shortcode", code).put("copyright_blocked", restricted))
        .put("gql_data", org.json.JSONObject().put("shortcode_media", org.json.JSONObject()
            .put("shortcode", code).put("is_video", true).put("has_audio", true)
            .put("video_url", "https://video.cdninstagram.com/media.mp4")))
    val payload = org.json.JSONObject().put("require", org.json.JSONArray().put(org.json.JSONObject().put("contextJSON", context.toString())))
    // The observed public embed places this data inside a JavaScript requireLazy/ServerJS wrapper.
    return if (wrapped) "<script>requireLazy([\"ServerJS\"],function(ServerJS){new ServerJS().handle($payload);});</script>"
    else "<script type=\"application/json\">$payload</script>"
}
