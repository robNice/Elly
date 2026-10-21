package de.robnice.elgy.ui.remote

import de.robnice.elgy.R
import de.robnice.elgy.remote.SendResult

fun SendResult.messageRes(): Int? = when (this) {
    SendResult.NO_TV -> R.string.result_no_tv
    SendResult.UNREACHABLE -> R.string.result_unreachable
    SendResult.PAIRING_REQUIRED -> R.string.result_pairing_required
    SendResult.UNSUPPORTED -> R.string.result_unsupported
    SendResult.FAILED -> R.string.result_failed
    SendResult.WAKE_SENT -> R.string.result_wake_sent
    SendResult.SENT, SendResult.DEMO -> null
}
