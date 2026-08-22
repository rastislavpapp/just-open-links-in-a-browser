/*
 * just open links in a browser - sends links to a real browser
 * instead of an in-app one.
 * Copyright (C) 2026 Rastislav Papp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.rastislavpapp.justopenlinksinabrowser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri

private const val PREFS = "browser_prefs"
private const val KEY_TARGET = "target_package"

/**
 * Finds the browsers installed on the device and remembers which one the user
 * picked.
 */
object Browsers {

    /**
     * A web intent with a scheme but deliberately no host. A concrete URL would
     * be claimed by any app holding a verified app link for that domain, which
     * hides the real browsers from the results.
     */
    private fun webIntent() = Intent(Intent.ACTION_VIEW, "http://".toUri())

    /**
     * Every browser on the device except us, sorted by the name the user sees.
     *
     * MATCH_ALL turns off the resolver's own filtering. Without it the query
     * collapses to the preferred activity for web links, which is this app
     * once it holds the browser role - so the list would come back containing
     * nothing but us.
     */
    fun installed(pm: PackageManager, self: String): List<ResolveInfo> =
        pm.queryIntentActivities(webIntent(), PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != self }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

    fun selected(ctx: Context): String? = prefs(ctx).getString(KEY_TARGET, null)

    fun select(ctx: Context, pkg: String) {
        prefs(ctx).edit { putString(KEY_TARGET, pkg) }
    }

    /**
     * Where links should go right now: the saved choice while it is still
     * installed, otherwise the first browser found, otherwise null when the
     * device has no browser at all.
     *
     * The fallback is written back to preferences so the answer stays stable
     * once made, and so a first run never dead-ends before the settings screen
     * has been opened.
     */
    fun resolve(ctx: Context): String? {
        val available = installed(ctx.packageManager, ctx.packageName)
            .map { it.activityInfo.packageName }
        if (available.isEmpty()) return null

        val saved = selected(ctx)
        if (saved != null && available.contains(saved)) return saved

        val fallback = available.first()
        select(ctx, fallback)
        return fallback
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
