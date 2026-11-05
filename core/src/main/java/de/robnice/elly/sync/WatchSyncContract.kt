package de.robnice.elly.sync

// Names both apps must agree on. The capabilities are also declared in each app's res/values/wear.xml.
object WatchSyncContract {
    const val STATE_PATH = "/elly/state"
    const val REQUEST_STATE_PATH = "/elly/request_state"
    const val KEY_PAIRED_TVS = "paired_tvs"
    const val KEY_ACTIVE_TV_ID = "active_tv_id"
    const val KEY_DEMO_MODE = "demo_mode"
    const val KEY_WATCH_LAYOUT = "watch_layout"

    // One asset per app a watch key opens, named by this prefix and the app ID.
    const val KEY_APP_ICON_PREFIX = "app_icon:"
    const val PHONE_CAPABILITY = "elly_phone"
    const val WATCH_CAPABILITY = "elly_watch"

    // The watch opens the phone app through these links; the phone app declares the scheme in its manifest.
    const val PHONE_LINK_SCHEME = "de.robnice.elly"
    const val PHONE_LINK_OPEN = "$PHONE_LINK_SCHEME://open"
    const val PHONE_LINK_PRIVACY = "$PHONE_LINK_SCHEME://privacy"
    const val PHONE_LINK_PRIVACY_HOST = "privacy"
}
