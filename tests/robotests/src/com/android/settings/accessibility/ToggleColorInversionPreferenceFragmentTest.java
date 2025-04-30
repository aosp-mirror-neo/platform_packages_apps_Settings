/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.ALL;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.testutils.AccessibilityTestUtils.assertEditShortcutsScreenShown;
import static com.android.settings.testutils.AccessibilityTestUtils.assertShortcutsTutorialDialogShown;
import static com.android.settings.testutils.AccessibilityTestUtils.inflateShortcutPreferenceView;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;
import java.util.Set;

/** Tests for {@link ToggleColorInversionPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleColorInversionPreferenceFragmentTest {
    private static final String MAIN_SWITCH_PREF_KEY = "color_inversion_switch_preference_key";
    private static final String SHORTCUT_PREF_KEY = "color_inversion_shortcut_key";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentScenario<ToggleColorInversionPreferenceFragment> mFragScenario = null;
    private ToggleColorInversionPreferenceFragment mFragment;
    private ShadowAccessibilityManager mA11yManager =
            Shadow.extract(mContext.getSystemService(AccessibilityManager.class));

    @After
    public void cleanUp() {
        if (mFragScenario != null) {
            mFragScenario.close();
        }
    }

    @Test
    public void onResume_colorInversionEnabled_shouldReturnTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, ON);

        launchFragment();

        assertThat(getMainSwitch().isChecked()).isTrue();
    }

    @Test
    public void onResume_colorInversionDisabled_shouldReturnFalse() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF);

        launchFragment();

        assertThat(getMainSwitch().isChecked()).isFalse();
    }

    @Test
    public void clickShortcutToggle_shortcutWasOff_turnOnShortcutAndShowShortcutTutorial() {
        mA11yManager.enableShortcutsForTargets(
                /* enable= */ false, ALL, Set.of(COLOR_INVERSION_COMPONENT_NAME.flattenToString()),
                mContext.getUserId());
        launchFragment();

        ShortcutPreference pref = getShortcutToggle();
        assertThat(pref).isNotNull();
        assertThat(pref.isChecked()).isFalse();
        PreferenceViewHolder viewHolder =
                inflateShortcutPreferenceView(mFragment.getContext(), pref);

        View widget = viewHolder.findViewById(pref.getSwitchResId());
        assertThat(widget).isNotNull();
        widget.performClick();
        ShadowLooper.idleMainLooper();

        assertThat(pref.isChecked()).isTrue();
        assertShortcutsTutorialDialogShown(mFragment);
    }

    @Test
    public void clickShortcutToggle_shortcutWasOn_turnOffShortcutAndNoTutorialShown() {
        mA11yManager.enableShortcutsForTargets(
                /* enable= */ true, HARDWARE,
                Set.of(COLOR_INVERSION_COMPONENT_NAME.flattenToString()), mContext.getUserId());
        launchFragment();

        ShortcutPreference pref = getShortcutToggle();
        assertThat(pref).isNotNull();
        assertThat(pref.isChecked()).isTrue();
        PreferenceViewHolder viewHolder = inflateShortcutPreferenceView(
                mFragment.getContext(), pref);

        View widget = viewHolder.findViewById(pref.getSwitchResId());
        assertThat(widget).isNotNull();
        widget.performClick();
        ShadowLooper.idleMainLooper();

        assertThat(pref.isChecked()).isFalse();
        assertThat(mA11yManager.getAccessibilityShortcutTargets(HARDWARE)).isEmpty();
        assertThat(ShadowDialog.getLatestDialog()).isNull();
    }

    @Test
    public void clickShortcutSettings_showEditShortcutsScreenWithoutChangingShortcutToggleState() {
        launchFragment();

        final ShortcutPreference pref = getShortcutToggle();
        assertThat(pref).isNotNull();
        final boolean shortcutToggleState = pref.isChecked();
        pref.performClick();
        ShadowLooper.idleMainLooper();

        assertEditShortcutsScreenShown(mFragment);
        assertThat(pref.isChecked()).isEqualTo(shortcutToggleState);
    }

    @Test
    public void turnOffMainSwitch_colorInversionTurnedOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, ON);
        launchFragment();
        assertThat(getMainSwitch().isChecked()).isTrue();

        getMainSwitch().performClick();
        ShadowLooper.idleMainLooper();

        assertThat(getMainSwitch().isChecked()).isFalse();
        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF) == ON;
        assertThat(isEnabled).isFalse();
    }

    @Test
    public void turnOnMainSwitch_colorInversionTurnedOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF);
        launchFragment();
        assertThat(getMainSwitch().isChecked()).isFalse();

        getMainSwitch().performClick();
        ShadowLooper.idleMainLooper();

        assertThat(getMainSwitch().isChecked()).isTrue();
        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, OFF) == ON;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        launchFragment();

        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        launchFragment();

        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_color_inversion_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        launchFragment();

        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_color_inversion);
    }

    @Test
    public void getNonIndexableKeys_containsNonIndexableItems() {
        final List<String> niks = ToggleColorInversionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys = List.of(
                "top_intro",
                "animated_image",
                "general_categories",
                "html_description"
        );

        assertThat(niks).containsExactlyElementsIn(keys);
    }

    @Test
    public void getXmlResourceToIndex() {
        final List<SearchIndexableResource> indexableResources =
                ToggleColorInversionPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, true);

        assertThat(indexableResources).isNotNull();
        assertThat(indexableResources.size()).isEqualTo(1);
        assertThat(indexableResources.getFirst().xmlResId).isEqualTo(
                R.xml.accessibility_color_inversion_settings);
    }

    private void launchFragment() {
        mFragScenario = FragmentScenario.launch(
                ToggleColorInversionPreferenceFragment.class,
                /* fragmentArgs= */ null,
                androidx.appcompat.R.style.Theme_AppCompat,
                (FragmentFactory) null).moveToState(Lifecycle.State.RESUMED);
        mFragScenario.onFragment(frag -> mFragment = frag);
    }

    private TwoStatePreference getMainSwitch() {
        return mFragment.findPreference(MAIN_SWITCH_PREF_KEY);
    }

    private ShortcutPreference getShortcutToggle() {
        return mFragment.findPreference(SHORTCUT_PREF_KEY);
    }
}
