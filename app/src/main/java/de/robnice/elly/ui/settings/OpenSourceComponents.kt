package de.robnice.elly.ui.settings

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import de.robnice.elly.R

data class OpenSourceComponent(
    @StringRes val name: Int,
    @StringRes val copyright: Int,
    @StringRes val license: Int,
    @RawRes val text: Int
)

object OpenSourceComponents {
    val all = listOf(
        OpenSourceComponent(R.string.app_name, R.string.license_copyright_elly, R.string.license_mit, R.raw.license_elly),
        OpenSourceComponent(R.string.license_name_fredoka, R.string.license_copyright_fredoka, R.string.license_ofl, R.raw.license_fredoka),
        OpenSourceComponent(R.string.license_name_androidx, R.string.license_copyright_androidx, R.string.license_apache, R.raw.license_apache_2_0),
        OpenSourceComponent(R.string.license_name_kotlin, R.string.license_copyright_kotlin, R.string.license_apache, R.raw.license_apache_2_0),
        OpenSourceComponent(R.string.license_name_okhttp, R.string.license_copyright_okhttp, R.string.license_apache, R.raw.license_apache_2_0)
    )
}
