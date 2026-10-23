package de.robnice.elly.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.robnice.elly.R

sealed interface OnboardingVisual {
    data class Icon(@DrawableRes val res: Int) : OnboardingVisual
    data object StatusLights : OnboardingVisual
    data object Elly : OnboardingVisual
}

data class OnboardingItem(val visual: OnboardingVisual, @StringRes val text: Int)

data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val items: List<OnboardingItem> = emptyList()
)

object OnboardingPages {

    private fun item(@DrawableRes icon: Int, @StringRes text: Int) = OnboardingItem(OnboardingVisual.Icon(icon), text)

    val all: List<OnboardingPage> = listOf(
        OnboardingPage(
            title = R.string.onboarding_welcome_title,
            text = R.string.onboarding_welcome_text,
            items = listOf(
                item(R.drawable.ic_live_tv, R.string.onboarding_welcome_connect),
                item(R.drawable.ic_apps, R.string.onboarding_welcome_screens),
                item(R.drawable.ic_settings, R.string.onboarding_welcome_settings),
                item(R.drawable.ic_pointer, R.string.onboarding_welcome_pointer)
            )
        ),
        OnboardingPage(
            title = R.string.onboarding_connect_title,
            text = R.string.onboarding_connect_text,
            items = listOf(
                item(R.drawable.ic_wifi, R.string.onboarding_connect_network),
                item(R.drawable.ic_live_tv, R.string.onboarding_connect_search),
                item(R.drawable.ic_touch, R.string.onboarding_connect_pair),
                item(R.drawable.ic_power, R.string.onboarding_connect_power),
                item(R.drawable.ic_settings, R.string.onboarding_connect_demo)
            )
        ),
        OnboardingPage(
            title = R.string.onboarding_screens_title,
            text = R.string.onboarding_screens_text,
            items = listOf(
                item(R.drawable.ic_chevron_right, R.string.onboarding_screens_swipe),
                OnboardingItem(OnboardingVisual.StatusLights, R.string.onboarding_screens_status),
                item(R.drawable.ic_live_tv, R.string.onboarding_screens_tv),
                item(R.drawable.ic_pointer, R.string.onboarding_screens_pointer),
                item(R.drawable.ic_settings, R.string.onboarding_screens_settings),
                OnboardingItem(OnboardingVisual.Elly, R.string.onboarding_screens_help),
                item(R.drawable.ic_keyboard, R.string.onboarding_screens_keyboard)
            )
        ),
        OnboardingPage(
            title = R.string.onboarding_settings_title,
            text = R.string.onboarding_settings_text,
            items = listOf(
                item(R.drawable.ic_live_tv, R.string.onboarding_settings_tv),
                item(R.drawable.ic_palette, R.string.onboarding_settings_look),
                item(R.drawable.ic_pointer, R.string.onboarding_settings_control),
                item(R.drawable.ic_screen_on, R.string.onboarding_settings_screens),
                item(R.drawable.ic_edit, R.string.onboarding_settings_edit),
                item(R.drawable.ic_apps, R.string.onboarding_settings_apps),
                item(R.drawable.ic_restore, R.string.onboarding_settings_reset)
            )
        ),
        OnboardingPage(
            title = R.string.onboarding_pointer_title,
            text = R.string.onboarding_pointer_text,
            items = listOf(
                item(R.drawable.ic_motion, R.string.onboarding_pointer_motion),
                item(R.drawable.ic_touch, R.string.onboarding_pointer_touch),
                item(R.drawable.ic_close, R.string.onboarding_pointer_close),
                item(R.drawable.ic_pointer, R.string.onboarding_pointer_quick)
            )
        ),
        OnboardingPage(
            title = R.string.onboarding_done_title,
            text = R.string.onboarding_done_text
        )
    )
}
