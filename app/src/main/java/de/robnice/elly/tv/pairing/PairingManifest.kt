package de.robnice.elly.tv.pairing

import android.content.Context
import de.robnice.elly.R
import org.json.JSONObject

class PairingManifest(private val context: Context) {

    val json: JSONObject by lazy {
        context.resources.openRawResource(R.raw.webos_pairing_manifest)
            .bufferedReader()
            .use { JSONObject(it.readText()) }
    }
}
