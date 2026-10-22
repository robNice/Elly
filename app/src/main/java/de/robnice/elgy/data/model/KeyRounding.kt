package de.robnice.elgy.data.model

// Corner radius of the keys as a share of half their shorter side: 0 is square, 1 is fully round.
object KeyRounding {
    const val DEFAULT = 1f
    val RANGE = 0f..1f
}
