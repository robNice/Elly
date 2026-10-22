package de.robnice.elgy.data.model

// Background behind all screens; image is the user's own picture, kept while another background is chosen.
data class AppBackground(val type: ScreenBackground = ScreenBackground.NONE, val image: String? = null)
