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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Rebuilds each incoming web link as a bare ACTION_VIEW intent, dropping the
 * in-app browser extras the calling app attached, and hands it to the browser
 * chosen in [SettingsActivity].
 */
class RedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Always an explicit package. Leaving it unset would resolve straight
        // back to this activity, since we are the default browser.
        val target = Browsers.resolve(this)
        if (target == null) {
            Toast.makeText(this, R.string.no_browsers, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri: Uri? = intent?.data

        // Launched with no URL (e.g. from an "open browser" system action).
        // Just hand over to the chosen browser and get out of the way.
        if (uri == null) {
            openBrowserDirectly(target)
            finish()
            return
        }

        // A fresh intent built from the URI alone. This is the whole point of
        // the app: the session token, toolbar colours and every other extra the
        // caller attached are dropped rather than passed on.
        val clean = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(target)
        }

        try {
            startActivity(clean)
        } catch (e: ActivityNotFoundException) {
            // resolve() already checked the package is installed, so this means
            // it disappeared in the last instant or refuses the intent.
            Toast.makeText(this, R.string.no_browsers, Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun openBrowserDirectly(target: String) {
        val launch = packageManager.getLaunchIntentForPackage(target) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(launch)
        } catch (e: ActivityNotFoundException) {
            // Nothing useful to do here.
        }
    }
}
