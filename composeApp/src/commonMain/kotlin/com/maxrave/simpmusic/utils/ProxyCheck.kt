package com.maxrave.simpmusic.utils

import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** The proxy the user turned on, as "host:port", when nothing answers on that address. */
data class UnreachableProxy(
    val address: String,
)

/**
 * Whether the proxy from Settings is on and cannot be reached.
 *
 * Asked after a player error: a proxy left on with nothing listening (for example
 * `127.0.0.1:8000` with the proxy app closed) makes every song that is not cached fail with
 * ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, and the generic "check your internet" message never
 * mentions the proxy. Only opens a TCP connection to the proxy itself; nothing is sent through it.
 */
suspend fun findUnreachableProxy(dataStoreManager: DataStoreManager): UnreachableProxy? {
    if (dataStoreManager.usingProxy.first() != DataStoreManager.TRUE) return null
    val host =
        dataStoreManager.proxyHost
            .first()
            .substringAfter("://")
            .trim()
            .trimEnd('/')
    val port = dataStoreManager.proxyPort.first()
    val address = "$host:$port"
    if (host.isEmpty() || port !in 1..65535) return UnreachableProxy(address)
    val reachable =
        withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { it.connect(InetSocketAddress(host, port), PROXY_CHECK_TIMEOUT_MS) }
            }.isSuccess
        }
    return if (reachable) null else UnreachableProxy(address)
}

private const val PROXY_CHECK_TIMEOUT_MS = 3_000
