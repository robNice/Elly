package de.robnice.elly

import android.app.Application

class EllyApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
