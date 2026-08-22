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

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import io.github.rastislavpapp.justopenlinksinabrowser.databinding.ActivitySettingsBinding
import io.github.rastislavpapp.justopenlinksinabrowser.databinding.RowBrowserBinding
import androidx.core.net.toUri

/**
 * Shows which browser links are being sent to, lets the user pick a different one,
 * and offers a shortcut to the system default browser setting.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val browserAdapter = BrowserAdapter()

    private var browsers: List<ResolveInfo> = emptyList()

    /** The system role picker reports back here; the role may or may not have changed. */
    private val roleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Night mode is set in App.onCreate, before any window is themed.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.roleButton.setOnClickListener { requestDefaultBrowser() }
        binding.list.adapter = browserAdapter
        binding.list.setOnItemClickListener { _, _, position, _ ->
            Browsers.select(this, browsers[position].activityInfo.packageName)
            browserAdapter.notifyDataSetChanged()
        }

        keepContentClearOfSystemBars(binding.root)
    }

    /**
     * Pads the content clear of the status and navigation bars.
     *
     * enableEdgeToEdge() puts the window behind the system bars on every
     * release, so this runs the same way everywhere rather than only on
     * Android 15 and later. AppCompat lays the action bar out inside the view
     * hierarchy and takes the top inset for it, so what arrives here is what is
     * left over for the content.
     */
    private fun keepContentClearOfSystemBars(root: View) {
        val margin = resources.getDimensionPixelSize(R.dimen.screen_margin)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = bars.left + margin,
                top = bars.top + margin,
                right = bars.right + margin,
                bottom = bars.bottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /**
     * Re-reads device state. Browsers can be installed or removed, and the
     * browser role can change, while this screen sits in the background.
     */
    private fun refresh() {
        browsers = Browsers.installed(packageManager, packageName)

        val hasBrowsers = browsers.isNotEmpty()
        binding.empty.visibility = if (hasBrowsers) View.GONE else View.VISIBLE
        binding.list.visibility = if (hasBrowsers) View.VISIBLE else View.GONE

        // Settle on a selection first so the list always shows one checked row.
        if (hasBrowsers) Browsers.resolve(this)
        browserAdapter.notifyDataSetChanged()

        val isDefault = isDefaultBrowser()
        binding.roleStatus.setText(if (isDefault) R.string.is_default else R.string.not_default)
        binding.roleButton.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    private fun isDefaultBrowser(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                return rm.isRoleHeld(RoleManager.ROLE_BROWSER)
            }
        }
        @Suppress("DEPRECATION")
        val info = packageManager.resolveActivity(
            Intent(Intent.ACTION_VIEW, "http://".toUri()), 0
        )
        return info?.activityInfo?.packageName == packageName
    }

    private fun requestDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                return
            }
        }

        // Android 7.0 added the default apps screen. On Android 6 the only way
        // to become the default browser is the Open with dialog and its Always
        // button, so all we can do is point the user at Settings.
        val fallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            Toast.makeText(this, R.string.set_default_legacy, Toast.LENGTH_LONG).show()
            Intent(Settings.ACTION_SETTINGS)
        }

        try {
            startActivity(fallback)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.set_default_legacy, Toast.LENGTH_LONG).show()
        }
    }

    private inner class BrowserAdapter : BaseAdapter() {

        override fun getCount(): Int = browsers.size
        override fun getItem(position: Int): Any = browsers[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView?.tag as? RowBrowserBinding
                ?: RowBrowserBinding.inflate(layoutInflater, parent, false)
                    .also { it.root.tag = it }

            val info = browsers[position]
            row.icon.setImageDrawable(info.loadIcon(packageManager))
            row.label.text = info.loadLabel(packageManager)
            row.radio.isChecked =
                info.activityInfo.packageName == Browsers.selected(this@SettingsActivity)

            return row.root
        }
    }
}
