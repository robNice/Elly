package de.robnice.elly.wear

import android.app.Application

class WearApplication : Application() {

    val container: WearContainer by lazy { WearContainer(this) }
}
