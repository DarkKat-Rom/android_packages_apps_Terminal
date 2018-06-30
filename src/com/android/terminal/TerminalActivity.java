/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.terminal.Terminal.TAG;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.android.internal.util.darkkat.ThemeColorHelper;
import com.android.internal.util.darkkat.ColorHelper;
import com.android.internal.util.darkkat.ThemeHelper;

/**
 * Activity that displays all {@link Terminal} instances running in a bound
 * {@link TerminalService}.
 */
public class TerminalActivity extends Activity {

    private TerminalService mService;

    private ViewPager mPager;
    private PagerTitleStrip mTitles;

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

    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TerminalService.ServiceBinder) service).getService();

            final int size = mService.getTerminals().size();
            Log.d(TAG, "Bound to service with " + size + " active terminals");

            // Give ourselves at least one terminal session
            if (size == 0) {
                mService.createTerminal();
            }

            // Bind UI to known terminals
            mTermAdapter.notifyDataSetChanged();
            invalidateOptionsMenu();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            throw new RuntimeException("Service in same process disconnected?");
        }
    };

    private final PagerAdapter mTermAdapter = new PagerAdapter() {
        private SparseArray<SparseArray<Parcelable>>
                mSavedState = new SparseArray<SparseArray<Parcelable>>();

        @Override
        public int getCount() {
            if (mService != null) {
                return mService.getTerminals().size();
            } else {
                return 0;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final TerminalView view = new TerminalView(container.getContext());
            view.setId(android.R.id.list);

            final Terminal term = mService.getTerminals().valueAt(position);
            view.setTerminal(term);

            final SparseArray<Parcelable> state = mSavedState.get(term.key);
            if (state != null) {
                view.restoreHierarchyState(state);
            }

            container.addView(view);
            view.requestFocus();
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            final TerminalView view = (TerminalView) object;

            final int key = view.getTerminal().key;
            SparseArray<Parcelable> state = mSavedState.get(key);
            if (state == null) {
                state = new SparseArray<Parcelable>();
                mSavedState.put(key, state);
            }
            view.saveHierarchyState(state);

            view.setTerminal(null);
            container.removeView(view);
        }

        @Override
        public int getItemPosition(Object object) {
            final TerminalView view = (TerminalView) object;
            final int key = view.getTerminal().key;
            final int index = mService.getTerminals().indexOfKey(key);
            if (index == -1) {
                return POSITION_NONE;
            } else {
                return index;
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mService.getTerminals().valueAt(position).getTitle();
        }
    };

    public void updatePreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final String orientation = sp.getString(TerminalSettingsActivity.KEY_SCREEN_ORIENTATION,
                "automatic");
        if (orientation.equals("automatic")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (orientation.equals("portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (orientation.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        for (int i = 0; i < mPager.getChildCount(); ++i) {
            View v = mPager.getChildAt(i);
            if (v instanceof TerminalView) {
                TerminalView view = (TerminalView) v;
                view.updatePreferences();
                view.invalidateViews();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        updateTheme();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);

        mPager = (ViewPager) findViewById(R.id.pager);
        mTitles = (PagerTitleStrip) findViewById(R.id.titles);

        mPager.setAdapter(mTermAdapter);

        ViewGroup root = (ViewGroup) findViewById(R.id.root);
        root.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        final int REQUEST_WRITE_STORAGE=51;

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_STORAGE);
        }

        if (mCustomizeColors && !mIsBlackoutTheme && !mIsWhiteoutTheme) {
            toolbar.setBackgroundColor(mPrimaryColor);
            mTitles.setBackgroundColor(mPrimaryColor);
        }
    }

    private void updateTheme() {
        mCustomizeColors = ThemeColorHelper.customizeColors(this);
        mDefaultPrimaryColor = getColor(R.color.theme_primary);
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
                    ? R.style.TermTheme_NoActionBar_LightStatusBar_LightNavigationBar
                    : R.style.TermTheme_NoActionBar_LightActionBar_LightNavigationBar;
        } else if (mLightActionBar) {
            mThemeResId = mLightStatusBar
                    ? R.style.TermTheme_NoActionBar_LightStatusBar
                    : R.style.TermTheme_NoActionBar_LightActionBar;
        } else if (mLightNavigationBar) {
            mThemeResId = R.style.TermTheme_NoActionBar_LightNavigationBar;
        } else {
            mThemeResId = R.style.TermTheme_NoActionBar;
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
        }
        if (mNavigationColor != 0) {
            getWindow().setNavigationBarColor(mNavigationColor);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TerminalService.class),
                mServiceConn, Context.BIND_AUTO_CREATE);
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
        } else {
            updatePreferences();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_close_tab).setEnabled(mTermAdapter.getCount() > 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_tab: {
                mService.createTerminal();
                mTermAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                final int index = mService.getTerminals().size() - 1;
                mPager.setCurrentItem(index, true);
                return true;
            }
            case R.id.menu_close_tab: {
                final int index = mPager.getCurrentItem();
                final int key = mService.getTerminals().keyAt(index);
                mService.destroyTerminal(key);
                mTermAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                return true;
            }
            case R.id.menu_item_settings: {
                startActivity(new Intent(TerminalActivity.this, TerminalSettingsActivity.class));
                return true;
            }
        }
        return false;
    }
}
