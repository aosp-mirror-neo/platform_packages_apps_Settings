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

import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;
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
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;

import com.google.testing.junit.testparameterinjector.TestParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.List;
import java.util.Set;

/** Tests for {@link ToggleDaltonizerPreferenceFragment} */
@RunWith(RobolectricTestParameterInjector.class)
public class ToggleDaltonizerPreferenceFragmentTest {
    private static final String MAIN_SWITCH_PREF_KEY = "daltonizer_switch_preference_key";
    private static final String SHORTCUT_PREF_KEY = "daltonizer_shortcut_key";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentScenario<ToggleDaltonizerPreferenceFragment> mFragScenario = null;
    private ToggleDaltonizerPreferenceFragment mFragment;
    private ShadowAccessibilityManager mA11yManager =
            Shadow.extract(mContext.getSystemService(AccessibilityManager.class));


    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
    }

    @After
    public void cleanUp() {
        if (mFragScenario != null) {
            mFragScenario.close();
        }
    }

    @Test
    public void onResume_colorCorrectEnabled_mainSwitchIsOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);

        launchFragment();

        assertThat(getMainSwitch().isChecked()).isTrue();
    }

    @Test
    public void onResume_colorCorrectDisabled_mainSwitchIsOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF);

        launchFragment();

        assertThat(getMainSwitch().isChecked()).isFalse();
    }

    @Test
    public void clickShortcutToggle_shortcutWasOff_turnOnShortcutAndShowShortcutTutorial() {
        mA11yManager.enableShortcutsForTargets(
                /* enable= */ false, ALL, Set.of(DALTONIZER_COMPONENT_NAME.flattenToString()),
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
                Set.of(DALTONIZER_COMPONENT_NAME.flattenToString()), mContext.getUserId());
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
    public void turnOffMainSwitch_colorCorrectionTurnedOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, ON);
        launchFragment();
        assertThat(getMainSwitch().isChecked()).isTrue();

        getMainSwitch().performClick();
        ShadowLooper.idleMainLooper();

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF) == ON;
        assertThat(isEnabled).isFalse();
        assertThat(getMainSwitch().isChecked()).isFalse();
    }

    @Test
    public void turnOnMainSwitch_colorCorrectionTurnedOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF);
        launchFragment();
        assertThat(getMainSwitch().isChecked()).isFalse();

        getMainSwitch().performClick();
        ShadowLooper.idleMainLooper();

        final boolean isEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, OFF) == ON;
        assertThat(isEnabled).isTrue();
        assertThat(getMainSwitch().isChecked()).isTrue();
    }

    @Test
    @TestParameters(
            customName = "deuteranomaly",
            value = "{modePrefKey: \"daltonizer_mode_deuteranomaly\", expectedValue: 12}")
    @TestParameters(
            customName = "protanomaly",
            value = "{modePrefKey: \"daltonizer_mode_protanomaly\", expectedValue: 11}")
    @TestParameters(
            customName = "tritanomaly",
            value = "{modePrefKey: \"daltonizer_mode_tritanomaly\", expectedValue: 13}")
    @TestParameters(
            customName = "grayscale",
            value = "{modePrefKey: \"daltonizer_mode_grayscale\", expectedValue: 0}")
    public void setDaltonizerMode_updateSettingData(String modePrefKey, int expectedValue) {
        launchFragment();
        CheckBoxPreference modePref = mFragment.findPreference(modePrefKey);
        assertThat(modePref).isNotNull();

        modePref.performClick();
        ShadowLooper.idleMainLooper();

        assertThat(modePref.isChecked()).isTrue();
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER)).isEqualTo(
                Integer.toString(expectedValue));
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        launchFragment();

        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        launchFragment();

        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_daltonizer_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        launchFragment();

        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_color_correction);
    }

    @Test
    public void getNonIndexableKeys_containsNonIndexableItems() {
        final List<String> niks = ToggleDaltonizerPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys = List.of(
                "top_intro",
                "daltonizer_preview",
                "general_categories",
                "html_description"
        );

        assertThat(niks).containsExactlyElementsIn(keys);
    }

    @Test
    public void getXmlResourceToIndex() {
        final List<SearchIndexableResource> indexableResources =
                ToggleDaltonizerPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, true);

        assertThat(indexableResources).isNotNull();
        assertThat(indexableResources.size()).isEqualTo(1);
        assertThat(indexableResources.getFirst().xmlResId).isEqualTo(
                R.xml.accessibility_daltonizer_settings);
    }

    private void launchFragment() {
        mFragScenario = FragmentScenario.launch(
                ToggleDaltonizerPreferenceFragment.class,
                /* bundle= */ null,
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
