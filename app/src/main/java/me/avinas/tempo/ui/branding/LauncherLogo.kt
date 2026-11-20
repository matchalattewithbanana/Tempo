package me.avinas.tempo.ui.branding

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.MipmapRes
import androidx.annotation.StringRes
import me.avinas.tempo.R

/**
 * A stable alias for each selectable launcher icon. The installed system icon
 * is owned by PackageManager, not by a theme preference that launchers cannot read.
 *
 * Never remove or rename a shipped alias: Android remembers the enabled state
 * across process death and package updates.
 */
enum class LauncherLogo(
    val aliasClassName: String,
    @MipmapRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
) {
    ORIGINAL("me.avinas.tempo.launcher.ClassicIcon", R.mipmap.ic_launcher,
        R.string.logo_original, R.string.logo_original_description),
    PULSE("me.avinas.tempo.launcher.PulseIcon", R.mipmap.ic_launcher_pulse,
        R.string.logo_pulse, R.string.logo_pulse_description),
    CLOCK("me.avinas.tempo.launcher.ClockIcon", R.mipmap.ic_launcher_clock,
        R.string.logo_clock, R.string.logo_clock_description),
    TRAIL("me.avinas.tempo.launcher.TrailIcon", R.mipmap.ic_launcher_trail,
        R.string.logo_trail, R.string.logo_trail_description),
    VINYL("me.avinas.tempo.launcher.VinylIcon", R.mipmap.ic_launcher_vinyl,
        R.string.logo_vinyl, R.string.logo_vinyl_description),
}

object LauncherLogoManager {
    private fun component(context: Context, logo: LauncherLogo): ComponentName =
        ComponentName(context.packageName, logo.aliasClassName)

    /**
     * Read the launcher icon directly from PackageManager, avoiding a second
     * preference that could disagree with Android or fail to persist.
     */
    fun selected(context: Context): LauncherLogo {
        val manager = context.packageManager
        return LauncherLogo.entries.firstOrNull { logo ->
            val state = manager.getComponentEnabledSetting(component(context, logo))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                (logo == LauncherLogo.ORIGINAL &&
                    state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        } ?: LauncherLogo.ORIGINAL
    }

    /**
     * Android 13+ applies the entire alias swap as one package-manager transaction.
     * Older versions enable the target first, then disable other aliases.
     *
     * DONT_KILL_APP prevents needless app restarts or lost tracking sessions.
     */
    fun select(context: Context, chosen: LauncherLogo) {
        if (selected(context) == chosen) return
        val manager = context.packageManager
        val flags = PackageManager.DONT_KILL_APP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.setComponentEnabledSettings(
                LauncherLogo.entries.map { logo ->
                    PackageManager.ComponentEnabledSetting(
                        component(context, logo),
                        if (logo == chosen) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        flags,
                    )
                },
            )
        } else {
            manager.setComponentEnabledSetting(
                component(context, chosen),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                flags,
            )
            LauncherLogo.entries.filterNot { it == chosen }.forEach { logo ->
                manager.setComponentEnabledSetting(
                    component(context, logo),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    flags,
                )
            }
        }
        check(selected(context) == chosen) {
            "The launcher did not accept the new icon. Please try again."
        }
    }
}
