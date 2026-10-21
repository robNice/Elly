package de.robnice.elgy.sensor

class RateSmoother(private val factor: Float = DEFAULT_FACTOR) {

    private val state = FloatArray(AXES)
    private var initialized = false

    fun smooth(values: FloatArray): FloatArray {
        if (!initialized) {
            for (i in 0 until AXES) state[i] = values[i]
            initialized = true
        } else {
            for (i in 0 until AXES) state[i] += factor * (values[i] - state[i])
        }
        return state.copyOf()
    }

    fun reset() {
        initialized = false
    }

    companion object {
        const val DEFAULT_FACTOR = 0.35f
        private const val AXES = 3
    }
}
