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
import android.graphics.drawable.ColorDrawable;
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
import com.android.internal.util.darkkat.ThemeColorHelper;
import com.android.internal.util.darkkat.ColorHelper;
import com.android.internal.util.darkkat.ThemeHelper;

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

    private int mThemeResId = 0;
    private boolean mCustomizeColors = false;
    private int mDefaultPrimaryColor = 0;
    private int mStatusBarColor = 0;
    private int mPrimaryColor = 0;
    private int mNavigationColor = 0;
    private boolean mColorizeNavigationBar = false;
    private boolean mLightStatusBar = false;
    private boolean mLightActionBar = false;
    private boolean mLightNavigationBar = false;
    private boolean mIsBlackoutTheme = false;
    private boolean mIsWhiteoutTheme = false;
    private int mThemeOverlayAccentResId = 0;

    private ListPreference mScreenOrientationPref;
    private ListPreference mFontSizePref;
    private ListPreference mTextColorsPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateTheme();
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mScreenOrientationPref = (ListPreference) findPreference(KEY_SCREEN_ORIENTATION);
        mFontSizePref = (ListPreference) findPreference(KEY_FONT_SIZE);
        mTextColorsPref = (ListPreference) findPreference(KEY_TEXT_COLORS);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void updateTheme() {
        mCustomizeColors = ThemeColorHelper.customizeColors(this);
        mDefaultPrimaryColor = getColor(com.android.internal.R.color.material_indigo_500);
        mStatusBarColor = ThemeColorHelper.getStatusBarBackgroundColor(this, mDefaultPrimaryColor);
        mPrimaryColor = ThemeColorHelper.getPrimaryColor(this, mDefaultPrimaryColor);
        mNavigationColor = ThemeColorHelper.getNavigationBarBackgroundColor(this, mDefaultPrimaryColor);
        mColorizeNavigationBar = ThemeColorHelper.colorizeNavigationBar(this);
        mLightStatusBar = ThemeColorHelper.lightStatusBar(this, mDefaultPrimaryColor);
        mLightActionBar = ThemeColorHelper.lightActionBar(this, mDefaultPrimaryColor);
        mLightNavigationBar = ThemeColorHelper.lightNavigationBar(this, mDefaultPrimaryColor);
        mIsBlackoutTheme = ThemeHelper.isBlackoutTheme(this);
        mIsWhiteoutTheme = ThemeHelper.isWhiteoutTheme(this);

        if (mLightActionBar && mLightNavigationBar) {
            mThemeResId = mLightStatusBar
                    ? R.style.TermTheme_LightStatusBar_LightNavigationBar
                    : R.style.TermTheme_LightActionBar_LightNavigationBar;
        } else if (mLightActionBar) {
            mThemeResId = mLightStatusBar
                    ? R.style.TermTheme_LightStatusBar
                    : R.style.TermTheme_LightActionBar;
        } else if (mLightNavigationBar) {
            mThemeResId = R.style.TermTheme_LightNavigationBar;
        } else {
            mThemeResId = R.style.TermTheme;
        }
        setTheme(mThemeResId);

        mThemeOverlayAccentResId = ThemeColorHelper.getThemeOverlayAccentResId(this);
        if (mThemeOverlayAccentResId > 0) {
            getTheme().applyStyle(mThemeOverlayAccentResId, true);
        }

        int oldFlags = getWindow().getDecorView().getSystemUiVisibility();
        int newFlags = oldFlags;
        if (!mLightStatusBar) {
            boolean isLightStatusBar = (newFlags & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                    == View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            // Check if light status bar flag was set.
            if (isLightStatusBar) {
                // Remove flag
                newFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }
        if (!mLightNavigationBar) {
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

        if (mCustomizeColors && !mIsBlackoutTheme && !mIsWhiteoutTheme) {
            getWindow().setStatusBarColor(mStatusBarColor);
            getActionBar().setBackgroundDrawable(new ColorDrawable(mPrimaryColor));
        }
        if (mNavigationColor != 0) {
            getWindow().setNavigationBarColor(mNavigationColor);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean customizeColors = ThemeColorHelper.customizeColors(this);
        int primaryColor = ThemeColorHelper.getPrimaryColor(this, mDefaultPrimaryColor);
        boolean colorizeNavigationBar = ThemeColorHelper.colorizeNavigationBar(this);
        boolean lightStatusBar = ThemeColorHelper.lightStatusBar(this, mDefaultPrimaryColor);
        boolean lightActionBar = ThemeColorHelper.lightActionBar(this, mDefaultPrimaryColor);
        boolean lightNavigationBar = ThemeColorHelper.lightNavigationBar(this, mDefaultPrimaryColor);
        int themeOverlayAccentResId = ThemeColorHelper.getThemeOverlayAccentResId(this);

        if (mThemeOverlayAccentResId != themeOverlayAccentResId
                || mCustomizeColors != customizeColors
                || mPrimaryColor != primaryColor
                || mColorizeNavigationBar != colorizeNavigationBar
                || mLightStatusBar != lightStatusBar
                || mLightActionBar != lightActionBar
                || mLightNavigationBar != lightNavigationBar) {
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
