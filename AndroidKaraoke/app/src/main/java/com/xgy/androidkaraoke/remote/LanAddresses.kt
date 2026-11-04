package com.xgy.androidkaraoke.remote

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 挑选手机可达的电视局域网 IPv4。
 * 优先 Wi‑Fi / 192.168 / 10.x；降低 172.16–31（常见 VPN/虚拟网卡）、链路本地与回环。
 */
object LanAddresses {
    data class Candidate(
        val host: String,
        val interfaceName: String,
        val displayName: String,
        val score: Int,
    ) {
        val looksVirtual: Boolean
            get() = isDeprecatedPrivate172(host) || isVirtualInterface(interfaceName)
    }

    fun listIpv4(): List<Candidate> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                .orEmpty()
                .flatMap { nif ->
                    if (!nif.isUp || nif.isLoopback) return@flatMap emptyList()
                    nif.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isAnyLocalAddress }
                        .mapNotNull { addr ->
                            val host = addr.hostAddress ?: return@mapNotNull null
                            if (host.startsWith("169.254.")) return@mapNotNull null
                            Candidate(
                                host = host,
                                interfaceName = nif.name.orEmpty(),
                                displayName = nif.displayName.orEmpty().ifBlank { nif.name.orEmpty() },
                                score = score(host, nif.name.orEmpty()),
                            )
                        }
                }
                .distinctBy { it.host }
                .sortedByDescending { it.score }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun bestIpv4(): String? = listIpv4().firstOrNull()?.host

    fun tipIfSuspicious(selected: String?, all: List<Candidate> = listIpv4()): String? {
        val host = selected ?: return null
        val hasHomeLan = all.any { isHomeFriendly(it.host) }
        return if (isDeprecatedPrivate172(host) && hasHomeLan) {
            "当前地址像虚拟网卡；家里 Wi‑Fi 请点「换一个地址」选 192.168.x"
        } else if (isDeprecatedPrivate172(host)) {
            "当前是 172.16–31 段，手机若连不上请换家用 Wi‑Fi 地址或点刷新"
        } else {
            null
        }
    }

    private fun score(host: String, ifName: String): Int {
        var s = 0
        val n = ifName.lowercase()
        when {
            n.startsWith("wlan") || n.contains("wifi") || n.contains("wl") -> s += 100
            n.startsWith("eth") || n.startsWith("en") -> s += 70
            n.startsWith("ap") || n.contains("softap") -> s += 40
            isVirtualInterface(n) -> s -= 80
        }
        when {
            host.startsWith("192.168.") -> s += 90
            host.startsWith("10.") -> s += 70
            isDeprecatedPrivate172(host) -> s -= 40
        }
        return s
    }

    private fun isHomeFriendly(host: String): Boolean =
        host.startsWith("192.168.") || host.startsWith("10.")

    /** RFC1918 的 172.16/12，在电视上常来自 VPN/虚拟接口，家用 Wi‑Fi 少见。 */
    private fun isDeprecatedPrivate172(host: String): Boolean {
        if (!host.startsWith("172.")) return false
        val second = host.substringAfter('.').substringBefore('.').toIntOrNull() ?: return false
        return second in 16..31
    }

    private fun isVirtualInterface(name: String): Boolean {
        val n = name.lowercase()
        return listOf(
            "tun", "tap", "vpn", "ppp", "rmnet", "dummy", "docker",
            "veth", "br-", "virbr", "vmnet", "vbox", "wg", "ccmni",
        ).any { n.startsWith(it) || n.contains(it) }
    }
}
