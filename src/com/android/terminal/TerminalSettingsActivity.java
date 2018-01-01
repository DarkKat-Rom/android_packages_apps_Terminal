/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.terminal;

import android.app.UiModeManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import static com.android.terminal.Terminal.TAG;
import com.android.internal.util.darkkat.ThemeOverlayHelper;
/**
 * Settings for Terminal.
 */
public class TerminalSettingsActivity extends PreferenceActivity {

    public static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
    public static final String KEY_FONT_SIZE          = "font_size";
    public static final String KEY_TEXT_COLORS        = "text_colors";
    public static final String KEY_VOLUME_MODE        = "volumekey_mode";
    public static final String KEY_TEXT_COLOR         = "text_color";
    public static final String KEY_BACKGROUND_COLOR   = "background_color";

    private boolean mUseOptionalLightStatusBar;
    private boolean mUseOptionalLightNavigationBar;

    private ListPreference mScreenOrientationPref;
    private ListPreference mFontSizePref;
    private ListPreference mTextColorsPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mUseOptionalLightStatusBar = ThemeOverlayHelper.themeSupportsOptionalĹightSB(this)
                && ThemeOverlayHelper.useLightStatusBar(this);
        mUseOptionalLightNavigationBar = ThemeOverlayHelper.themeSupportsOptionalĹightNB(this)
                && ThemeOverlayHelper.useLightNavigationBar(this);
        int themeResId = 0;

        if (mUseOptionalLightStatusBar && mUseOptionalLightNavigationBar) {
            themeResId = R.style.ThemeOverlay_LightStatusBar_LightNavigationBar;
        } else if (mUseOptionalLightStatusBar) {
            themeResId = R.style.ThemeOverlay_LightStatusBar;
        } else if (mUseOptionalLightNavigationBar) {
            themeResId = R.style.ThemeOverlay_LightNavigationBar;
        } else {
            themeResId = R.style.TermTheme;
        }
        setTheme(themeResId);

        int oldFlags = getWindow().getDecorView().getSystemUiVisibility();
        int newFlags = oldFlags;
        if (!mUseOptionalLightStatusBar) {
            // Possibly we are using the Whiteout theme
            boolean isWhiteoutTheme =
                    ThemeOverlayHelper.getThemeOverlay(this) == ThemeOverlayHelper.THEME_OVERLAY_WHITEOUT;
            boolean isLightStatusBar = (newFlags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                    == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            // Check if light status bar flag was set,
            // and we are not using the Whiteout theme,
            // (Whiteout theme should always use a light status bar).
            if (isLightStatusBar && !isWhiteoutTheme) {
                // Remove flag
                newFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        if (mUseOptionalLightNavigationBar) {
            newFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            // Check if light navigation bar flag was set
            boolean isLightNavigationBar = (newFlags & View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                    == View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            if (isLightNavigationBar) {
                // Remove flag
                newFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        if (oldFlags != newFlags) {
            getWindow().getDecorView().setSystemUiVisibility(newFlags);
        }

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mScreenOrientationPref = (ListPreference) findPreference(KEY_SCREEN_ORIENTATION);
        mFontSizePref = (ListPreference) findPreference(KEY_FONT_SIZE);
        mTextColorsPref = (ListPreference) findPreference(KEY_TEXT_COLORS);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean useOptionalLightStatusBar = ThemeOverlayHelper.themeSupportsOptionalĹightSB(this)
                && ThemeOverlayHelper.useLightStatusBar(this);
        boolean useOptionalLightNavigationBar = ThemeOverlayHelper.themeSupportsOptionalĹightNB(this)
                && ThemeOverlayHelper.useLightNavigationBar(this);
        if (mUseOptionalLightStatusBar != useOptionalLightStatusBar
                || mUseOptionalLightNavigationBar != useOptionalLightNavigationBar) {
            recreate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
