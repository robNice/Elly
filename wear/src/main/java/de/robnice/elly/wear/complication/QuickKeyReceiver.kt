package de.robnice.elly.wear.complication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.robnice.elly.wear.WearApplication
import de.robnice.elly.wear.remote.QuickKey

// Receives taps on Elly's complications. goAsync keeps the process alive until the command is sent.
class QuickKeyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = QuickKey.fromId(intent.getStringExtra(EXTRA_KEY)) ?: return
        val container = (context.applicationContext as WearApplication).container
        val pending = goAsync()
        container.quickCommands.press(key).invokeOnCompletion { pending.finish() }
    }

    companion object {
        const val EXTRA_KEY = "key"
    }
}
