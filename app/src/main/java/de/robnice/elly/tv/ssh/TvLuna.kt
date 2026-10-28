package de.robnice.elly.tv.ssh

import org.json.JSONObject
import java.util.Base64
import java.util.zip.GZIPInputStream

// luna-send calls within one SSH session.
class TvLuna internal constructor(private val session: ShellSession) {

    suspend fun call(uri: String, payload: JSONObject = JSONObject()): JSONObject =
        LunaCommand.parse(session.run(LunaCommand.build(uri, payload)).output)

    // Services of the running broadcast need its ID, which stays valid only while the bind call is open, so
    // binding and the call run in one go on the TV; BROADCAST_ID in the payload is replaced by the ID.
    // Without a running broadcast the answer is a failure, like that of any other call.
    suspend fun callBound(uri: String, payload: JSONObject): JSONObject {
        val call = LunaCommand.build(uri, payload).replace(BROADCAST_ID, "'\"\$ID\"'")
        return LunaCommand.parse(session.run(BIND_BEFORE + call + BIND_AFTER).output)
    }

    // Guide answers run to megabytes of JSON; compressed on the TV they shrink about eightfold.
    suspend fun callCompressed(uri: String, payload: JSONObject): JSONObject {
        val result = session.run(LunaCommand.build(uri, payload) + COMPRESS, LONG_TIMEOUT_MILLIS)
        if (result.exitStatus != 0) throw TvShellException.CommandFailed(result.exitStatus, result.output.take(MAX_ERROR_LENGTH))
        val text = GZIPInputStream(decode(result.output).inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return LunaCommand.parse(text)
    }

    // Only files below the given folder are read, so paths from TV answers cannot reach anything else.
    suspend fun readFile(path: String, allowedFolder: String): ByteArray? {
        require(allowedFolder.endsWith("/")) { "Folder must end with a slash" }
        if (!path.startsWith(allowedFolder) || !path.matches(SAFE_PATH) || ".." in path) return null
        val result = session.run("base64 '$path'")
        if (result.exitStatus != 0) return null
        return runCatching { decode(result.output) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    // The TV downloads the file itself, so the phone only ever talks to the TV.
    suspend fun download(url: String): ByteArray? {
        if (!url.matches(SAFE_URL)) return null
        val result = session.run("curl -s -f -m $DOWNLOAD_TIMEOUT_SECONDS '$url' | base64")
        if (result.exitStatus != 0) return null
        return runCatching { decode(result.output) }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun decode(output: String): ByteArray =
        Base64.getDecoder().decode(output.filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' })

    companion object {
        const val BROADCAST_ID = "@broadcastId@"
        private const val BIND_BEFORE = "F=/tmp/elly-bind.\$\$; " +
            "luna-send -i luna://com.webos.service.utp.broadcast/bind '{\"subscribe\":true}' > \$F 2>&1 & B=\$!; " +
            "i=0; while [ \$i -lt 30 ] && ! grep -q broadcastId \$F; do usleep 100000; i=\$((i+1)); done; " +
            "ID=\$(sed -n 's/.*\"broadcastId\":\"\\([^\"]*\\)\".*/\\1/p' \$F | head -1); " +
            "case \"\$ID\" in \"\"|*[!0-9a-fA-F-]*) kill \$B; rm -f \$F; " +
            "echo '{\"returnValue\":false,\"errorText\":\"no broadcast\"}'; exit 0;; esac; "
        private const val BIND_AFTER = "; kill \$B; rm -f \$F"
        private const val COMPRESS = " | gzip -c | base64"
        private const val LONG_TIMEOUT_MILLIS = 60_000L
        private const val MAX_ERROR_LENGTH = 200
        private val SAFE_PATH = Regex("^/[A-Za-z0-9_./-]+$")
        private val SAFE_URL = Regex("^https?://[A-Za-z0-9.-]+/[A-Za-z0-9_./?&=-]+$")
        private const val DOWNLOAD_TIMEOUT_SECONDS = 10
    }
}
