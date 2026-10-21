package de.robnice.elgy

import android.app.Application

class ElgyApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
