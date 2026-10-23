package de.robnice.elly.ui.help

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.robnice.elly.ui.elly.EllyPose
import de.robnice.elly.ui.elly.EllyPoses

data class HelpTopic(@StringRes val title: Int, @StringRes val body: Int, @DrawableRes val image: Int? = null)

data class HelpChapter(
    @StringRes val title: Int,
    @StringRes val ellyIntro: Int,
    @DrawableRes val icon: Int,
    val topics: List<HelpTopic>,
    val elly: EllyPose = EllyPoses.Reading
)
