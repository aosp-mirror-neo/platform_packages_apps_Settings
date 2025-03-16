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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSitesPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val allowAllSitesPreference = SupervisionAllowAllSitesPreference()

    private val blockExplicitSitesPreference = SupervisionBlockExplicitSitesPreference()

    @Test
    fun getTitle_allowAllSites() {
        assertThat(allowAllSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_allow_all_sites_title)
    }

    @Test
    fun getTitle_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_block_explicit_sites_title)
    }

    @Test
    fun getSummary_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.summary)
            .isEqualTo(
                R.string.supervision_web_content_filters_browser_block_explicit_sites_summary
            )
    }
}
