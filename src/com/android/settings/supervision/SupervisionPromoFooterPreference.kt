/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.supervision

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.CardPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A bottom banner promoting other supervision features offered by the supervision app. */
class SupervisionPromoFooterPreference(
    private val preferenceDataProvider: PreferenceDataProvider,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {

    /** Whether [intent] holds an initialized value. */
    private var initialized = false

    // Operation to be performed when the preference is clicked
    private var intent: Intent? = null

    override val key: String
        get() = KEY

    // TODO(b/399497788): Remove this and get title from supervision app
    override fun getTitle(context: Context) = "Full parental controls"

    // TODO(b/399497788): Remove this and get summary from supervision app
    override fun getSummary(context: Context) =
        "Set up an account for your kid & help them manage it (required for kids under [AOC])"

    override fun createWidget(context: Context) = CardPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (initialized) {
            preference.intent = intent
            preference.isVisible = intent != null
        }
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.lifecycleScope.launch {
            // TODO(b/399497788) Get title & summary from supervision app.
            val preferenceData =
                withContext(coroutineDispatcher) {
                    preferenceDataProvider.getPreferenceData(listOf(KEY))[KEY]
                }
            initialized = true
            val targetIntent =
                Intent(preferenceData?.action).apply {
                    `package` = preferenceData?.targetPackage
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            if (targetIntent.isValid(context)) intent = targetIntent
            context.notifyPreferenceChange(KEY)
        }
    }

    private fun Intent.isValid(context: Context) =
        action != null &&
            `package` != null &&
            context.packageManager.queryIntentActivitiesAsUser(this, 0, context.userId).isNotEmpty()

    companion object {
        const val KEY = "promo_footer"
    }
}
