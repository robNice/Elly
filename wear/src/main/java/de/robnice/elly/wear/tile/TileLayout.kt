package de.robnice.elly.wear.tile

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.ColorRes
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint
import androidx.wear.protolayout.DimensionBuilders.DegreesProp
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders.FloatProp
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationParameters
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.backgroundImage
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconButton
import androidx.wear.protolayout.material3.imageButton
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.loadAction
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.TileBuilders
import de.robnice.elly.wear.R
import de.robnice.elly.data.model.AppDisplay
import de.robnice.elly.data.model.WatchAction
import de.robnice.elly.data.model.WatchLayout
import de.robnice.elly.data.model.WatchSlot
import de.robnice.elly.wear.data.AppIconStore
import de.robnice.elly.wear.remote.ActionCatalog
import de.robnice.elly.wear.ui.WearActivity

// Built from the Material 3 tile components, which size buttons, margins and text for each screen size and
// font scale as the Wear OS quality guidelines ask (48 dp touch targets, nothing cut off on a 192 dp circle).
class TileLayout(
    private val context: Context,
    private val device: DeviceParameters,
    private val keys: WatchLayout,
    private val resourcesVersion: String,
    private val appIcons: AppIconStore
) {

    private val actions = WatchSlot.TILE.map(keys::action)

    // What the last tap did: a ring runs once along the edge in the colour of the result. A message (errors, the
    // switch-on signal) takes the place of the keys for holdMillis, as the one-line title is too narrow for it on
    // small watches. Without animation the ring stays, while the TV has not answered yet.
    data class Feedback(val message: String?, @ColorRes val color: Int, val holdMillis: Long, val animated: Boolean = true)

    // tvName is null while no TV is known and the tile only points to the phone. The feedback is shown until
    // feedbackUntil; the second timeline entry brings the plain layout back on renderers without animations.
    fun tile(tvName: String?, feedback: Feedback? = null, feedbackUntil: Long = 0L): TileBuilders.Tile {
        val timeline = TimelineBuilders.Timeline.Builder()
        if (feedback != null && tvName != null) {
            val until = if (feedback.holdMillis > 0) minOf(feedbackUntil, System.currentTimeMillis() + feedback.holdMillis) else feedbackUntil
            timeline.addTimelineEntry(entry(root(tvName, feedback), end = until))
            timeline.addTimelineEntry(entry(root(tvName, null), start = until))
        } else {
            timeline.addTimelineEntry(entry(root(tvName, null)))
        }
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(resourcesVersion)
            .setTileTimeline(timeline.build())
            .build()
    }

    // Icons of the keys come from the app's drawables, those of apps are the PNGs the phone sent.
    fun resources(): ResourceBuilders.Resources = ResourceBuilders.Resources.Builder()
        .setVersion(resourcesVersion)
        .apply {
            actions.distinct().forEach { action ->
                val image = when (action) {
                    is WatchAction.App -> appIcons.bytes(action.appId)?.let(::inlineImage)
                    else -> ActionCatalog.visual(action, context.resources).icon?.let { icon ->
                        ResourceBuilders.ImageResource.Builder()
                            .setAndroidResourceByResId(ResourceBuilders.AndroidImageResourceByResId.Builder().setResourceId(icon).build())
                            .build()
                    }
                }
                image?.let { addIdToImageMapping(imageId(action), it) }
            }
        }
        .build()

    private fun inlineImage(bytes: ByteArray): ResourceBuilders.ImageResource? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return ResourceBuilders.ImageResource.Builder()
            .setInlineResource(
                ResourceBuilders.InlineImageResource.Builder()
                    .setData(bytes)
                    .setWidthPx(bounds.outWidth)
                    .setHeightPx(bounds.outHeight)
                    .setFormat(ResourceBuilders.IMAGE_FORMAT_UNDEFINED)
                    .build()
            )
            .build()
    }

    private fun imageId(action: WatchAction): String = when (action) {
        is WatchAction.App -> APP_IMAGE_PREFIX + action.appId
        else -> context.resources.getResourceEntryName(ActionCatalog.visual(action, context.resources).icon ?: 0)
    }

    private fun entry(root: LayoutElement, start: Long? = null, end: Long? = null): TimelineBuilders.TimelineEntry {
        val builder = TimelineBuilders.TimelineEntry.Builder()
            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build())
        if (start != null || end != null) {
            builder.setValidity(
                TimelineBuilders.TimeInterval.Builder()
                    .apply { start?.let(::setStartMillis) }
                    .apply { end?.let(::setEndMillis) }
                    .build()
            )
        }
        return builder.build()
    }

    private fun root(tvName: String?, feedback: Feedback?): LayoutElement = materialScope(context, device) {
        val layout = primaryLayout(
            titleSlot = { text((tvName ?: context.getString(R.string.app_name)).layoutString, maxLines = 1) },
            mainSlot = {
                val message = feedback?.message
                if (tvName == null) {
                    text(context.getString(R.string.no_tv).layoutString, maxLines = 3)
                } else if (message != null) {
                    text(message.layoutString, typography = Typography.BODY_SMALL, maxLines = 5)
                } else {
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .addContent(keyRow(WatchSlot.TILE.take(KEYS_PER_ROW)))
                        .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(ROW_GAP).build())
                        .addContent(keyRow(WatchSlot.TILE.drop(KEYS_PER_ROW)))
                        .build()
                }
            },
            bottomSlot = {
                textEdgeButton(
                    onClick = openApp(),
                    labelContent = { text(context.getString(R.string.tile_open_app).layoutString) }
                )
            }
        )
        if (feedback == null) {
            layout
        } else {
            LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(layout)
                .addContent(if (feedback.animated) flashRing(feedback.color) else steadyRing(feedback.color))
                .build()
        }
    }

    private fun steadyRing(@ColorRes ringColor: Int): LayoutElement = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(DegreesProp.Builder(0f).build())
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .addContent(
            LayoutElementBuilders.ArcLine.Builder()
                .setLength(DegreesProp.Builder(FULL_CIRCLE).build())
                .setThickness(RING_WIDTH)
                .setColor(argb(context.getColor(ringColor)))
                .build()
        )
        .build()

    // Runs once around the edge from twelve and fades, like the light ring in the app.
    private fun flashRing(@ColorRes ringColor: Int): LayoutElement = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(DegreesProp.Builder(0f).build())
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setOpacity(FloatProp.Builder(0f).setDynamicValue(fade(1f, 0f, SWEEP_MILLIS)).build())
                .build()
        )
        .addContent(
            LayoutElementBuilders.ArcLine.Builder()
                .setLength(
                    DegreesProp.Builder(FULL_CIRCLE)
                        .setDynamicValue(DynamicFloat.animate(0f, FULL_CIRCLE, spec(SWEEP_MILLIS, 0L)))
                        .build()
                )
                .setLayoutConstraintsForDynamicLength(AngularLayoutConstraint.Builder(FULL_CIRCLE).build())
                .setThickness(RING_WIDTH)
                .setColor(argb(context.getColor(ringColor)))
                .setStrokeCap(LayoutElementBuilders.STROKE_CAP_ROUND)
                .build()
        )
        .build()

    private fun fade(from: Float, to: Float, delayMillis: Long): DynamicFloat =
        DynamicFloat.animate(from, to, spec(FADE_MILLIS, delayMillis))

    private fun spec(durationMillis: Long, delayMillis: Long) = AnimationSpec.Builder()
        .setAnimationParameters(
            AnimationParameters.Builder()
                .setDurationMillis(durationMillis)
                .setDelayMillis(delayMillis)
                .build()
        )
        .build()

    private fun MaterialScope.keyRow(slots: List<WatchSlot>): LayoutElement =
        buttonGroup(height = expand()) {
            slots.forEach { slot -> buttonGroupItem { key(slot) } }
        }

    // The clickable ID names the slot, so the tap is matched to the key's function when the tile reloads.
    private fun MaterialScope.key(slot: WatchSlot): LayoutElement {
        val action = keys.action(slot)
        val visual = ActionCatalog.visual(action, context.resources)
        val onClick = clickable(loadAction(), slot.name)
        val description = LayoutModifier.contentDescription(visual.description)
        val colors = visual.container?.let { keyColors(it, visual.content ?: R.color.on_power_key) } ?: filledTonalButtonColors()
        return when {
            action is WatchAction.App && hasAppIcon(action) -> imageButton(
                onClick = onClick,
                backgroundContent = { backgroundImage(imageId(action), contentScaleMode = LayoutElementBuilders.CONTENT_SCALE_MODE_CROP) },
                modifier = description,
                width = expand(),
                height = expand()
            )
            visual.icon != null -> iconButton(
                onClick = onClick,
                iconContent = { icon(imageId(action)) },
                modifier = description,
                width = expand(),
                height = expand(),
                colors = colors
            )
            else -> textButton(
                onClick = onClick,
                // App names are longer than key labels and get the small type, so they wrap between words.
                labelContent = {
                    text(
                        (visual.text ?: "").layoutString,
                        typography = if (action is WatchAction.App) Typography.LABEL_SMALL else Typography.LABEL_LARGE,
                        maxLines = 2
                    )
                },
                modifier = description,
                width = expand(),
                height = expand(),
                colors = colors
            )
        }
    }

    // A tile key is too small for icon and name together, so "both" shows the icon there.
    private fun hasAppIcon(action: WatchAction.App): Boolean =
        action.display != AppDisplay.TEXT && appIcons.bytes(action.appId) != null

    private fun keyColors(@ColorRes container: Int, @ColorRes content: Int) = ButtonColors(
        containerColor = color(container),
        iconColor = color(content),
        labelColor = color(content),
        secondaryLabelColor = color(content)
    )

    private fun openApp(): ModifiersBuilders.Clickable = ModifiersBuilders.Clickable.Builder()
        .setId(OPEN_APP_ID)
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(WearActivity::class.java.name)
                        .build()
                )
                .build()
        )
        .build()

    private fun color(@ColorRes id: Int) = LayoutColor(context.getColor(id))

    companion object {
        private const val APP_IMAGE_PREFIX = "app:"
        private const val KEYS_PER_ROW = 3
        private const val OPEN_APP_ID = "open_app"
        private val ROW_GAP = dp(4f)
        private val RING_WIDTH = dp(4f)
        private const val FULL_CIRCLE = 360f
        private const val SWEEP_MILLIS = 450L
        private const val FADE_MILLIS = 500L
    }
}
