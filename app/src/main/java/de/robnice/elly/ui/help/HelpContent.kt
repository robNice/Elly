package de.robnice.elly.ui.help

import de.robnice.elly.R
import de.robnice.elly.ui.elly.EllyPoses

object HelpContent {

    val chapters: List<HelpChapter> = listOf(
        HelpChapter(
            title = R.string.help_start_title,
            ellyIntro = R.string.help_start_elly,
            icon = R.drawable.ic_wifi,
            elly = EllyPoses.FirstSteps,
            topics = listOf(
                HelpTopic(R.string.help_start_requirements_title, R.string.help_start_requirements_body),
                HelpTopic(R.string.help_start_search_title, R.string.help_start_search_body),
                HelpTopic(R.string.help_start_ip_title, R.string.help_start_ip_body),
                HelpTopic(R.string.help_start_pair_title, R.string.help_start_pair_body),
                HelpTopic(R.string.help_start_multiple_title, R.string.help_start_multiple_body),
                HelpTopic(R.string.help_start_poweron_title, R.string.help_start_poweron_body),
                HelpTopic(R.string.help_start_problems_title, R.string.help_start_problems_body),
                HelpTopic(R.string.help_start_demo_title, R.string.help_start_demo_body)
            )
        ),
        HelpChapter(
            title = R.string.help_remote_title,
            ellyIntro = R.string.help_remote_elly,
            icon = R.drawable.ic_apps,
            elly = EllyPoses.Hero,
            topics = listOf(
                HelpTopic(R.string.help_remote_topbar_title, R.string.help_remote_topbar_body, R.drawable.help_img_remote_screen1),
                HelpTopic(R.string.help_remote_status_title, R.string.help_remote_status_body),
                HelpTopic(R.string.help_remote_screens_title, R.string.help_remote_screens_body),
                HelpTopic(R.string.help_remote_keys_title, R.string.help_remote_keys_body),
                HelpTopic(R.string.help_remote_defaults_title, R.string.help_remote_defaults_body),
                HelpTopic(R.string.help_remote_text_title, R.string.help_remote_text_body)
            )
        ),
        HelpChapter(
            title = R.string.help_keys_title,
            ellyIntro = R.string.help_keys_elly,
            icon = R.drawable.ic_home,
            elly = EllyPoses.KeysReading,
            topics = listOf(
                HelpTopic(R.string.help_keys_power_title, R.string.help_keys_power_body),
                HelpTopic(R.string.help_keys_screen_title, R.string.help_keys_screen_body),
                HelpTopic(R.string.help_keys_volume_title, R.string.help_keys_volume_body),
                HelpTopic(R.string.help_keys_nav_title, R.string.help_keys_nav_body),
                HelpTopic(R.string.help_keys_menus_title, R.string.help_keys_menus_body),
                HelpTopic(R.string.help_keys_digits_title, R.string.help_keys_digits_body, R.drawable.help_img_remote_screen2),
                HelpTopic(R.string.help_keys_input_title, R.string.help_keys_input_body, R.drawable.help_img_input_picker),
                HelpTopic(R.string.help_keys_media_title, R.string.help_keys_media_body, R.drawable.help_img_remote_screen3),
                HelpTopic(R.string.help_keys_colors_title, R.string.help_keys_colors_body),
                HelpTopic(R.string.help_keys_apps_title, R.string.help_keys_apps_body)
            )
        ),
        HelpChapter(
            title = R.string.help_pointer_title,
            ellyIntro = R.string.help_pointer_elly,
            icon = R.drawable.ic_pointer,
            elly = EllyPoses.Pointer,
            topics = listOf(
                HelpTopic(R.string.help_pointer_open_title, R.string.help_pointer_open_body),
                HelpTopic(R.string.help_pointer_motion_title, R.string.help_pointer_motion_body, R.drawable.help_img_pointer_motion),
                HelpTopic(R.string.help_pointer_tuning_title, R.string.help_pointer_tuning_body, R.drawable.help_img_settings_controls),
                HelpTopic(R.string.help_pointer_swipe_title, R.string.help_pointer_swipe_body),
                HelpTopic(R.string.help_pointer_joystick_title, R.string.help_pointer_joystick_body, R.drawable.help_img_pointer_joystick),
                HelpTopic(R.string.help_pointer_okpos_title, R.string.help_pointer_okpos_body),
                HelpTopic(R.string.help_pointer_quick_title, R.string.help_pointer_quick_body)
            )
        ),
        HelpChapter(
            title = R.string.help_edit_title,
            ellyIntro = R.string.help_edit_elly,
            icon = R.drawable.ic_edit,
            elly = EllyPoses.Painter,
            topics = listOf(
                HelpTopic(R.string.help_edit_manage_title, R.string.help_edit_manage_body, R.drawable.help_img_settings_appearance),
                HelpTopic(R.string.help_edit_editor_title, R.string.help_edit_editor_body, R.drawable.help_img_editor),
                HelpTopic(R.string.help_edit_add_title, R.string.help_edit_add_body, R.drawable.help_img_editor_groups),
                HelpTopic(R.string.help_edit_move_title, R.string.help_edit_move_body),
                HelpTopic(R.string.help_edit_multi_title, R.string.help_edit_multi_body),
                HelpTopic(R.string.help_edit_undo_title, R.string.help_edit_undo_body),
                HelpTopic(R.string.help_edit_apps_title, R.string.help_edit_apps_body, R.drawable.help_img_app_key)
            )
        ),
        HelpChapter(
            title = R.string.help_look_title,
            ellyIntro = R.string.help_look_elly,
            icon = R.drawable.ic_palette,
            elly = EllyPoses.Costumes,
            topics = listOf(
                HelpTopic(R.string.help_look_open_title, R.string.help_look_open_body),
                HelpTopic(R.string.help_look_palette_title, R.string.help_look_palette_body, R.drawable.help_img_settings_appearance),
                HelpTopic(R.string.help_look_background_title, R.string.help_look_background_body, R.drawable.help_img_settings_appearance_more),
                HelpTopic(R.string.help_look_rounding_title, R.string.help_look_rounding_body),
                HelpTopic(R.string.help_look_light_title, R.string.help_look_light_body)
            )
        ),
        HelpChapter(
            title = R.string.help_widgets_title,
            ellyIntro = R.string.help_widgets_elly,
            icon = R.drawable.ic_volume,
            elly = EllyPoses.Magician,
            topics = listOf(
                HelpTopic(R.string.help_widgets_overview_title, R.string.help_widgets_overview_body, R.drawable.help_img_widgets),
                HelpTopic(R.string.help_widgets_tv_title, R.string.help_widgets_tv_body),
                HelpTopic(R.string.help_widgets_hints_title, R.string.help_widgets_hints_body)
            )
        ),
        HelpChapter(
            title = R.string.help_settings_title,
            ellyIntro = R.string.help_settings_elly,
            icon = R.drawable.ic_settings,
            elly = EllyPoses.Mechanic,
            topics = listOf(
                HelpTopic(R.string.help_settings_tabs_title, R.string.help_settings_tabs_body),
                HelpTopic(R.string.help_settings_tv_title, R.string.help_settings_tv_body, R.drawable.help_img_settings_tv),
                HelpTopic(R.string.help_settings_haptics_title, R.string.help_settings_haptics_body)
            )
        ),
        HelpChapter(
            title = R.string.help_privacy_title,
            ellyIntro = R.string.help_privacy_elly,
            icon = R.drawable.ic_privacy,
            elly = EllyPoses.Judge,
            topics = listOf(
                HelpTopic(R.string.help_privacy_data_title, R.string.help_privacy_data_body),
                HelpTopic(R.string.help_privacy_policy_title, R.string.help_privacy_policy_body)
            )
        )
    )
}
