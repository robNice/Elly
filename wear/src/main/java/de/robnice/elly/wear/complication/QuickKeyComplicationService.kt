package de.robnice.elly.wear.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import de.robnice.elly.wear.remote.QuickKey

// One data source per key, because a watch face slot is filled from exactly one source.
abstract class QuickKeyComplicationService(private val key: QuickKey) : SuspendingComplicationDataSourceService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? = data(request.complicationType)

    override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type)

    private fun data(type: ComplicationType): ComplicationData? {
        val icon = Icon.createWithResource(this, key.icon)
        val description = PlainComplicationText.Builder(getString(key.label)).build()
        val tap = tapAction()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(getString(key.shortLabel)).build(),
                description
            )
                .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
                .setTapAction(tap)
                .build()
            // Wide slots show the full name next to the icon.
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(getString(key.label)).build(),
                description
            )
                .setMonochromaticImage(MonochromaticImage.Builder(icon).build())
                .setTapAction(tap)
                .build()
            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                MonochromaticImage.Builder(icon).build(),
                description
            )
                .setTapAction(tap)
                .build()
            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                SmallImage.Builder(icon, SmallImageType.ICON).build(),
                description
            )
                .setTapAction(tap)
                .build()
            else -> null
        }
    }

    private fun tapAction(): PendingIntent = PendingIntent.getBroadcast(
        this,
        key.ordinal,
        Intent(this, QuickKeyReceiver::class.java).putExtra(QuickKeyReceiver.EXTRA_KEY, key.id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

class PowerComplicationService : QuickKeyComplicationService(QuickKey.POWER)
class MuteComplicationService : QuickKeyComplicationService(QuickKey.MUTE)
class PlayPauseComplicationService : QuickKeyComplicationService(QuickKey.PLAY_PAUSE)
class VolumeDownComplicationService : QuickKeyComplicationService(QuickKey.VOLUME_DOWN)
class VolumeUpComplicationService : QuickKeyComplicationService(QuickKey.VOLUME_UP)
class HomeComplicationService : QuickKeyComplicationService(QuickKey.HOME)
