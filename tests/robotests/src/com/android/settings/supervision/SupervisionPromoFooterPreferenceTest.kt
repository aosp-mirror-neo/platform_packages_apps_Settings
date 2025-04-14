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
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.SupervisionPromoFooterPreference.Companion.KEY
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.widget.CardPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SupervisionPromoFooterPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = CardPreference(context)

    private var preferenceData: PreferenceData? = null

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockPackageManager: PackageManager = mock()
    private val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
        on { lifecycleScope }.thenReturn(testScope)
        on { packageManager }.thenReturn(mockPackageManager)
        on { findPreference<Preference>(any()) }.thenReturn(preference)
    }
    private val preferenceDataProvider: PreferenceDataProvider = mock {
        onBlocking { getPreferenceData(any()) }
            .thenAnswer {
                when (preferenceData) {
                    null -> mapOf<String, PreferenceData>()
                    else -> mapOf(KEY to preferenceData)
                }
            }
    }

    @Test
    fun getTitle_returnsCorrectTitle() {
        val supervisionPromoFooterPreference =
            SupervisionPromoFooterPreference(preferenceDataProvider)
        assertThat(supervisionPromoFooterPreference.getPreferenceTitle(context))
            .isEqualTo("Full parental controls")
    }

    @Test
    fun getSummary_returnsCorrectSummary() {
        val supervisionPromoFooterPreference =
            SupervisionPromoFooterPreference(preferenceDataProvider)
        assertThat(supervisionPromoFooterPreference.getPreferenceSummary(context))
            .isEqualTo(
                "Set up an account for your kid & help them manage it (required for " +
                    "kids under [AOC])"
            )
    }

    @Test
    fun onResume_actionIsNull_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = PreferenceData(targetPackage = "test.package")

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_packageIsNull_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = PreferenceData(action = "Test Action")

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_emptyPreferenceData_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = null

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_noActivitiesCanHandleIntent_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = PreferenceData(action = "Test Action", targetPackage = "test.package")

            mockPackageManager.stub {
                on { queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>()) }
                    .thenReturn(emptyList())
            }

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
        }

    @Test
    fun onResume_validIntent_hasActivityToHandleIntent_preferenceIsVisible_validIntentCreated() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = PreferenceData(action = "Test Action", targetPackage = "test.package")

            mockPackageManager.stub {
                on { queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>()) }
                    .thenReturn(listOf(ResolveInfo()))
            }

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isTrue()
            val intent = preference.intent!!
            assertThat(intent.action).isEqualTo("Test Action")
            assertThat(intent.`package`).isEqualTo("test.package")
        }
}
