package xyz.aerii.athen.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import xyz.aerii.athen.Athen
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

object Beacon {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val gson = Gson()

    @JvmStatic
    @JvmOverloads
    fun get(url: String, log: Boolean = true, block: RequestBuilder.() -> Unit = {}) =
        RequestBuilder(url, "GET", log).apply(block).execute()

    @JvmStatic
    @JvmOverloads
    fun post(url: String, log: Boolean = true, block: RequestBuilder.() -> Unit = {}) =
        RequestBuilder(url, "POST", log).apply(block).execute()

    @JvmStatic
    @JvmOverloads
    fun put(url: String, log: Boolean = true, block: RequestBuilder.() -> Unit = {}) =
        RequestBuilder(url, "PUT", log).apply(block).execute()

    @JvmStatic
    @JvmOverloads
    fun delete(url: String, log: Boolean = true, block: RequestBuilder.() -> Unit = {}) =
        RequestBuilder(url, "DELETE", log).apply(block).execute()

    @JvmStatic
    @JvmOverloads
    fun download(url: String, output: File, log: Boolean = true, block: DownloadBuilder.() -> Unit = {}) =
        DownloadBuilder(url, output, log).apply(block).execute()

    @JvmStatic
    fun Int.isRetryableError(): Boolean =
        this in listOf(408, 429, 500, 502, 503, 504)

    @JvmStatic
    @JvmOverloads
    suspend fun <T> retry(
        maxRetries: Int = 3,
        initialDelay: Long = 2000,
        maxDelay: Long = 30000,
        backoffMultiplier: Double = 2.0,
        shouldRetry: (Exception) -> Boolean = { true },
        operation: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        var currentDelay = initialDelay

        repeat(maxRetries) { attempt ->
            try {
                return@withContext operation()
            } catch (e: Exception) {
                if (attempt == maxRetries - 1 || !shouldRetry(e)) throw e
                Athen.LOGGER.warn("Retrying operation (attempt ${attempt + 1}/$maxRetries) due to ${e::class.simpleName}: ${e.message}")
                delay(currentDelay.coerceAtMost(maxDelay))
                currentDelay = (currentDelay * backoffMultiplier).toLong()
            }
        }

        throw IllegalStateException("Retry exhausted")
    }

    class RequestBuilder(private val url: String, private val method: String, private val log: Boolean) {
        val headers = mutableMapOf("User-Agent" to "Mozilla/5.0 (Athen)")
        var body: String? = null
        var onSuccess: (String) -> Unit = {}
        var onError: (Exception) -> Unit = {}
        var connectTimeout = 15_000
        var readTimeout = 45_000
        var maxRetries = 3
        var retryDelay = 2000L

        fun header(key: String, value: String) = apply {
            headers[key] = value
        }

        fun headers(vararg pairs: Pair<String, String>) = apply {
            headers.putAll(pairs)
        }

        fun headers(map: Map<String, String>) = apply {
            headers.putAll(map)
        }

        fun body(data: Any) = apply {
            body = data as? String ?: gson.toJson(data)
            header("Content-Type", "application/json")
        }

        fun json(data: Any) = apply {
            body(data)
        }

        fun timeout(connect: Int = 15_000, read: Int = 45_000) = apply {
            connectTimeout = connect
            readTimeout = read
        }

        fun retries(max: Int, delay: Long = 2000L) = apply {
            maxRetries = max
            retryDelay = delay
        }

        @JvmName($$"method$onSuccess")
        fun onSuccess(block: (String) -> Unit) = apply {
            onSuccess = block
        }

        @JvmName($$"method$onError")
        fun onError(block: (Exception) -> Unit) = apply {
            onError = block
        }

        inline fun <reified T> onSuccess(crossinline block: (T) -> Unit) = apply {
            onSuccess = { response ->
                val type = object : TypeToken<T>() {}.type
                val data: T = gson.fromJson(response, type)
                block(data)
            }
        }

        fun onJsonSuccess(block: (JsonObject) -> Unit) = apply {
            onSuccess = { response ->
                val json = JsonParser().parse(response).asJsonObject
                block(json)
            }
        }

        fun execute() {
            scope.launch {
                if (log) Athen.LOGGER.info("Sent $method request to $url")

                runCatching {
                    retry(
                        maxRetries = maxRetries,
                        initialDelay = retryDelay,
                        shouldRetry = { it is SocketTimeoutException || (it is HttpException && it.statusCode.isRetryableError()) }
                    ) {
                        performRequest()
                    }
                }.onFailure {
                    Athen.LOGGER.error("Request $method $url failed", it)
                    onError(it as? Exception ?: Exception(it))
                }
            }
        }

        private suspend fun performRequest(): String = withContext(Dispatchers.IO) {
            val connection = createConnection()

            try {
                if (body != null && method in listOf("POST", "PUT", "PATCH")) {
                    connection.doOutput = true
                    connection.outputStream.use { it.write(body!!.toByteArray()) }
                }

                val statusCode = connection.responseCode

                if (statusCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    if (log) Athen.LOGGER.info("Success in $method for $url → $statusCode (${response.length} bytes)")
                    onSuccess(response)
                    response
                } else {
                    Athen.LOGGER.warn("Error in $method for $url → $statusCode")
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $statusCode"
                    throw HttpException(error, statusCode)
                }
            } finally {
                connection.disconnect()
            }
        }

        private fun createConnection(): HttpURLConnection {
            return URI(url).toURL().openConnection().apply {
                setRequestProperty("Accept", "*/*")
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
                this.connectTimeout = this@RequestBuilder.connectTimeout
                this.readTimeout = this@RequestBuilder.readTimeout
                (this as HttpURLConnection).requestMethod = method
            } as HttpURLConnection
        }
    }

    class DownloadBuilder(private val url: String, private val output: File, private val log: Boolean) {
        private val headers = mutableMapOf<String, String>()
        private var onProgress: (Long, Long) -> Unit = { _, _ -> }
        private var onComplete: (File) -> Unit = {}
        private var onError: (Exception) -> Unit = {}
        private var connectTimeout = 15_000
        private var readTimeout = 60_000
        private var maxRetries = 3

        fun header(key: String, value: String) = apply {
            headers[key] = value
        }

        fun headers(vararg pairs: Pair<String, String>) = apply {
            headers.putAll(pairs)
        }

        fun timeout(connect: Int = 15_000, read: Int = 60_000) = apply {
            connectTimeout = connect
            readTimeout = read
        }

        fun retries(max: Int) = apply {
            maxRetries = max
        }

        fun onProgress(block: (downloaded: Long, total: Long) -> Unit) = apply {
            onProgress = block
        }

        fun onComplete(block: (File) -> Unit) = apply {
            onComplete = block
        }

        fun onError(block: (Exception) -> Unit) = apply {
            onError = block
        }

        fun execute() {
            scope.launch {
                if (log) Athen.LOGGER.info("Starting download from $url → ${output.name}")

                runCatching {
                    retry(
                        maxRetries = maxRetries,
                        shouldRetry = { it is SocketTimeoutException || (it is HttpException && it.statusCode.isRetryableError()) }
                    ) {
                        performDownload()
                    }
                }.onFailure {
                    Athen.LOGGER.error("Download failed: $url", it)
                    onError(it as? Exception ?: Exception(it))
                }
            }
        }

        private suspend fun performDownload() = withContext(Dispatchers.IO) {
            val connection = URI(url).toURL().openConnection().apply {
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
                this.connectTimeout = this@DownloadBuilder.connectTimeout
                this.readTimeout = this@DownloadBuilder.readTimeout
            } as HttpURLConnection

            connection.requestMethod = "GET"

            try {
                if (connection.responseCode in 200..299) {
                    val totalBytes = connection.contentLengthLong
                    var bytesDownloaded = 0L

                    connection.inputStream.use { input ->
                        output.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                onProgress(bytesDownloaded, totalBytes)
                            }
                        }
                    }

                    if (log) Athen.LOGGER.info("Download complete: ${output.name} (${output.length()} bytes)")
                    onComplete(output)
                } else {
                    throw HttpException("HTTP ${connection.responseCode}", connection.responseCode)
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    data class HttpException(override val message: String, val statusCode: Int) : Exception(message)
}