/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.systemui.battery;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableBuddy;
import com.android.settingslib.graph.LandscapeBatteryDrawableLine;
import com.android.settingslib.graph.LandscapeBatteryDrawableSignal;
import com.android.settingslib.graph.LandscapeBatteryDrawableMusku;
import com.android.settingslib.graph.LandscapeBatteryDrawablePill;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R;
import com.android.systemui.animation.Interpolators;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver {

    protected static final String STATUS_BAR_BATTERY_STYLE = Settings.System.STATUS_BAR_BATTERY_STYLE;

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3;

    private final AccessorizedBatteryDrawable mDrawable;

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_CIRCLE = 1;
    private static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    private static final int BATTERY_STYLE_FULL_CIRCLE = 3;
    private static final int BATTERY_STYLE_TEXT = 4; /*hidden icon*/
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_STYLE_RLANDSCAPE = 6;
    private static final int BATTERY_STYLE_LANDSCAPE = 7;
    private static final int BATTERY_STYLE_LANDSCAPE_BUDDY = 8;
    private static final int BATTERY_STYLE_LANDSCAPE_LINE = 9;
    private static final int BATTERY_STYLE_LANDSCAPE_MUSKU = 10;
    private static final int BATTERY_STYLE_LANDSCAPE_PILL = 11;
    private static final int BATTERY_STYLE_LANDSCAPE_SIGNAL = 12;
    private static final int BATTERY_STYLE_RLANDSCAPE_STYLE_A = 13;
    private static final int BATTERY_STYLE_LANDSCAPE_STYLE_A = 14;
    private static final int BATTERY_STYLE_RLANDSCAPE_STYLE_B = 15;
    private static final int BATTERY_STYLE_LANDSCAPE_STYLE_B = 16;

    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_SHOW_INSIDE = 1;
    private static final int BATTERY_PERCENT_SHOW_OUTSIDE = 2;

    private final CircleBatteryDrawable mCircleDrawable;
    private final FullCircleBatteryDrawable mFullCircleDrawable;
    private final ThemedBatteryDrawable mThemedDrawable;
    private final RLandscapeBatteryDrawable mRLandscapeDrawable;
    private final LandscapeBatteryDrawable mLandscapeDrawable;
    private final RLandscapeBatteryDrawableStyleA mRLandscapeDrawableStyleA;
    private final LandscapeBatteryDrawableStyleA mLandscapeDrawableStyleA;
    private final RLandscapeBatteryDrawableStyleB mRLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableStyleB mLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableBuddy mLandscapeDrawableBuddy;
    private final LandscapeBatteryDrawableLine mLandscapeDrawableLine;
    private final LandscapeBatteryDrawableMusku mLandscapeDrawableMusku;
    private final LandscapeBatteryDrawablePill mLandscapeDrawablePill;
    private final LandscapeBatteryDrawableSignal mLandscapeDrawableSignal;
    private final ImageView mBatteryIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    private String mEstimateText = null;
    private boolean mCharging;
    private boolean mPCharging;
    private boolean mIsOverheated;
    private boolean mDisplayShieldEnabled;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;

    private int mBatteryStyle = BATTERY_STYLE_PORTRAIT;
    public int mShowBatteryPercent;

    private DualToneHandler mDualToneHandler;

    private boolean mIsQsHeader;

    private final ArrayList<BatteryMeterViewCallbacks> mCallbacks = new ArrayList<>();

    private int mNonAdaptedSingleToneColor;
    private int mNonAdaptedForegroundColor;
    private int mNonAdaptedBackgroundColor;

    private BatteryEstimateFetcher mBatteryEstimateFetcher;

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.meter_background_color));
        mPercentageStyleId = atts.getResourceId(R.styleable.BatteryMeterView_textAppearance, 0);
        mDrawable = new AccessorizedBatteryDrawable(context, frameColor);
        mRLandscapeDrawable = new RLandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawable = new LandscapeBatteryDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mThemedDrawable = new ThemedBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
        mRLandscapeDrawableStyleA = new RLandscapeBatteryDrawableStyleA(context, frameColor);
        mLandscapeDrawableStyleA = new LandscapeBatteryDrawableStyleA(context, frameColor);
        mRLandscapeDrawableStyleB = new RLandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableStyleB = new LandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableBuddy = new LandscapeBatteryDrawableBuddy(context, frameColor);
        mLandscapeDrawableLine = new LandscapeBatteryDrawableLine(context, frameColor);
        mLandscapeDrawableMusku = new LandscapeBatteryDrawableMusku(context, frameColor);
        mLandscapeDrawablePill = new LandscapeBatteryDrawablePill(context, frameColor);
        mLandscapeDrawableSignal = new LandscapeBatteryDrawableSignal(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        mBatteryIconView = new ImageView(context);
        mBatteryIconView.setImageDrawable(mThemedDrawable);
        final MarginLayoutParams mlp = new MarginLayoutParams(
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width),
                getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
        mlp.setMargins(0, 0, 0,
                getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
        addView(mBatteryIconView, mlp);

        updateShowPercent();
        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        if (isNightMode()) {
            onDarkChanged(new ArrayList<Rect>(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);
        }        

        setClipChildren(false);
        setClipToPadding(false);
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200);

        // Animates appearing/disappearing of the battery percentage text using fade-in/fade-out
        // and disables all other animation types
        ObjectAnimator appearAnimator = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
        transition.setAnimator(LayoutTransition.APPEARING, appearAnimator);
        transition.setInterpolator(LayoutTransition.APPEARING, Interpolators.ALPHA_IN);

        ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
        transition.setInterpolator(LayoutTransition.DISAPPEARING, Interpolators.ALPHA_OUT);
        transition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);

        transition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
        transition.setAnimator(LayoutTransition.CHANGING, null);

        setLayoutTransition(transition);
    }

    protected void setBatteryStyle(int batteryStyle) {
        if (batteryStyle == mBatteryStyle) return;
        mBatteryStyle = batteryStyle;
        updateBatteryStyle();
        updateShowPercent();
    }

    public void setForceShowPercent(boolean show) {
        setPercentShowMode(show ? MODE_ON : MODE_DEFAULT);
    }

    /**
     * Force a particular mode of showing percent
     *
     * 0 - No preference
     * 1 - Force on
     * 2 - Force off
     * 3 - Estimate
     * @param mode desired mode (none, on, off)
     */
    public void setPercentShowMode(@BatteryPercentMode int mode) {
        if (mode == mShowPercentMode) return;
        mShowPercentMode = mode;
        updateShowPercent();
        updatePercentText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updatePercentView();
        mDrawable.notifyDensityChanged();
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    void updateSettings() {
        updateSbBatteryStyle();
        updateSbShowBatteryPercent();
    }

    private void updateSbBatteryStyle() {
        mBatteryStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT);
        updateBatteryStyle();
        updateVisibility();
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onHiddenBattery(mBatteryStyle == BATTERY_STYLE_HIDDEN);
        }
    }

    private void updateSbShowBatteryPercent() {
        //updateSbBatteryStyle already called
        switch (mBatteryStyle) {
            case BATTERY_STYLE_TEXT:
                mShowBatteryPercent = BATTERY_PERCENT_SHOW_OUTSIDE;
                updatePercentView();
                return;
            case BATTERY_STYLE_HIDDEN:
                mShowBatteryPercent = BATTERY_PERCENT_HIDDEN;
                updatePercentView();
                return;
            default:
                mShowBatteryPercent = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, BATTERY_PERCENT_HIDDEN);
                updatePercentView();
        }
    }

    void onBatteryLevelChanged(int level, boolean pluggedIn) {
        if (mLevel != level) {
            mLevel = level;
            mThemedDrawable.setBatteryLevel(mLevel);
            mRLandscapeDrawable.setBatteryLevel(mLevel);
            mLandscapeDrawable.setBatteryLevel(mLevel);

            mCircleDrawable.setBatteryLevel(mLevel);
            mFullCircleDrawable.setBatteryLevel(mLevel);

            mRLandscapeDrawableStyleA.setBatteryLevel(mLevel);
            mLandscapeDrawableStyleA.setBatteryLevel(mLevel);
            mRLandscapeDrawableStyleB.setBatteryLevel(mLevel);
            mLandscapeDrawableStyleB.setBatteryLevel(mLevel);
            mLandscapeDrawableBuddy.setBatteryLevel(mLevel);
            mLandscapeDrawableLine.setBatteryLevel(mLevel);
            mLandscapeDrawableMusku.setBatteryLevel(mLevel);
            mLandscapeDrawablePill.setBatteryLevel(mLevel);
            mLandscapeDrawableSignal.setBatteryLevel(mLevel);

        }
        if (mCharging != pluggedIn) {
            mCharging = pluggedIn;
            mThemedDrawable.setCharging(mCharging);
            mRLandscapeDrawable.setCharging(mCharging);
            mLandscapeDrawable.setCharging(mCharging);
            mCircleDrawable.setCharging(mCharging);
            mFullCircleDrawable.setCharging(mCharging);
            mRLandscapeDrawableStyleA.setCharging(mCharging);
            mLandscapeDrawableStyleA.setCharging(mCharging);
            mRLandscapeDrawableStyleB.setCharging(mCharging);
            mLandscapeDrawableStyleB.setCharging(mCharging);
            mLandscapeDrawableBuddy.setCharging(mCharging);
            mLandscapeDrawableLine.setCharging(mCharging);
            mLandscapeDrawableMusku.setCharging(mCharging);
            mLandscapeDrawablePill.setCharging(mCharging);
            mLandscapeDrawableSignal.setCharging(mCharging);
            updateShowPercent();
        } else {
            updatePercentText();
        }
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        mThemedDrawable.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mFullCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableBuddy.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableLine.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableMusku.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawablePill.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableSignal.setPowerSaveEnabled(isPowerSave);
        updateShowPercent();
    }

    void onIsOverheatedChanged(boolean isOverheated) {
        boolean valueChanged = mIsOverheated != isOverheated;
        mIsOverheated = isOverheated;
        if (valueChanged) {
            updateContentDescription();
            // The battery drawable is a different size depending on whether it's currently
            // overheated or not, so we need to re-scale the view when overheated changes.
            scaleBatteryMeterViews();
        }
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext())
                .inflate(R.layout.battery_percentage_view, null);
    }

    /**
     * Updates percent view by removing old one and reinflating if necessary
     */
    public void updatePercentView() {
        updateShowPercent();
    }

    /**
     * Sets the fetcher that should be used to get the estimated time remaining for the user's
     * battery.
     */
    void setBatteryEstimateFetcher(BatteryEstimateFetcher fetcher) {
        mBatteryEstimateFetcher = fetcher;
    }

    void setDisplayShieldEnabled(boolean displayShieldEnabled) {
        mDisplayShieldEnabled = displayShieldEnabled;
    }

    void updatePercentText() {
        if (mBatteryStateUnknown) {
            return;
        }

        if (mBatteryEstimateFetcher == null) {
            setPercentTextAtCurrentLevel();
            return;
        }

        if (mBatteryPercentView != null) {
            if (mShowPercentMode == MODE_ESTIMATE && !mCharging) {
                mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                        (String estimate) -> {
                    if (mBatteryPercentView == null) {
                        return;
                    }


                    if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                        mEstimateText = estimate;
                        mBatteryPercentView.setText(estimate);
                        updateContentDescription();
                    }

                    if (estimate != null) {
                        if (mBatteryPercentView != null) {
                            batteryPercentViewSetText(estimate);
                        }
                        setContentDescription(getContext().getString(
                                R.string.accessibility_battery_level_with_estimate,
                                mLevel, estimate));
                    } else {
                        setPercentTextAtCurrentLevel();
                    }
                });
            } else {
                setPercentTextAtCurrentLevel();
            }
        } else {
            updateContentDescription();
        }
    }

    private void setPercentTextAtCurrentLevel() {
        if (mBatteryPercentView == null) return;

        String percentText = NumberFormat.getPercentInstance().format(mLevel / 100f);
        // Setting text actually triggers a layout pass (because the text view is set to
        // wrap_content width and TextView always relayouts for this). Avoid needless
        // relayout if the text didn't actually change.
        if (!TextUtils.equals(mBatteryPercentView.getText(), percentText) || mPCharging != mCharging) {
            mPCharging = mCharging;
            // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
            // to load its emoji colored variant with the uFE0E flag
            mEstimateText = null;

            String bolt = "\u26A1\uFE0E";
            CharSequence mChargeIndicator = mCharging && (mBatteryStyle == BATTERY_STYLE_TEXT)
                ? (bolt + " ") : "";
            batteryPercentViewSetText(mChargeIndicator +
                NumberFormat.getPercentInstance().format(mLevel / 100f));
            setContentDescription(
                    getContext().getString(mCharging ? R.string.accessibility_battery_level_charging
                            : R.string.accessibility_battery_level, mLevel));
        }

        updateContentDescription();
    }

    private void updateContentDescription() {
        Context context = getContext();

        String contentDescription;
        if (mBatteryStateUnknown) {
            contentDescription = context.getString(R.string.accessibility_battery_unknown);
        } else if (mShowPercentMode == MODE_ESTIMATE && !TextUtils.isEmpty(mEstimateText)) {
            contentDescription = context.getString(
                    mIsOverheated
                            ? R.string.accessibility_battery_level_charging_paused_with_estimate
                            : R.string.accessibility_battery_level_with_estimate,
                    mLevel,
                    mEstimateText);
        } else if (mIsOverheated) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging_paused, mLevel);
        } else if (mCharging) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging, mLevel);
        } else {
            contentDescription = context.getString(R.string.accessibility_battery_level, mLevel);
        }
    }

    void updateShowPercent() {
        final boolean showing = mBatteryPercentView != null;
        final boolean drawPercentInside = mShowPercentMode == MODE_DEFAULT &&
                mShowBatteryPercent == BATTERY_PERCENT_SHOW_INSIDE;
        final boolean drawPercentOnly = mShowPercentMode == MODE_ESTIMATE ||
                mShowPercentMode == MODE_ON || mShowBatteryPercent == BATTERY_PERCENT_SHOW_OUTSIDE;
        if (!(!mIsQsHeader && mBatteryStyle == BATTERY_STYLE_HIDDEN)
                && drawPercentOnly && (!drawPercentInside || mCharging)) {
            mThemedDrawable.setShowPercent(false);
            mRLandscapeDrawable.setShowPercent(false);
            mLandscapeDrawable.setShowPercent(false);
            mCircleDrawable.setShowPercent(false);
            mFullCircleDrawable.setShowPercent(false);
            mRLandscapeDrawableStyleA.setShowPercent(false);
            mLandscapeDrawableStyleA.setShowPercent(false);
            mRLandscapeDrawableStyleB.setShowPercent(false);
            mLandscapeDrawableStyleB.setShowPercent(false);
            mLandscapeDrawableBuddy.setShowPercent(false);
            mLandscapeDrawableLine.setShowPercent(false);
            mLandscapeDrawableMusku.setShowPercent(false);
            mLandscapeDrawablePill.setShowPercent(false);
            mLandscapeDrawableSignal.setShowPercent(false);
            if (!showing) {

                mBatteryPercentView = loadPercentView();
                if (mPercentageStyleId != 0) { // Only set if specified as attribute
                    mBatteryPercentView.setTextAppearance(mPercentageStyleId);
                }
                if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
                addView(mBatteryPercentView,
                        new ViewGroup.LayoutParams(
                                LayoutParams.WRAP_CONTENT,
                                LayoutParams.MATCH_PARENT));
            }
            if (mBatteryStyle == BATTERY_STYLE_TEXT) {
                mBatteryPercentView.setPaddingRelative(0, 0, 0, 0);
            } else {
                Resources res = getContext().getResources();
                mBatteryPercentView.setPaddingRelative(
                        res.getDimensionPixelSize(R.dimen.battery_level_padding_start), 0, 0, 0);
            }
        } else {
            mThemedDrawable.setShowPercent(drawPercentInside);
            mRLandscapeDrawable.setShowPercent(drawPercentInside);
            mLandscapeDrawable.setShowPercent(drawPercentInside);
            mCircleDrawable.setShowPercent(drawPercentInside);
            mFullCircleDrawable.setShowPercent(drawPercentInside);
            mRLandscapeDrawableStyleA.setShowPercent(drawPercentInside);
            mLandscapeDrawableStyleA.setShowPercent(drawPercentInside);
            mRLandscapeDrawableStyleB.setShowPercent(drawPercentInside);
            mLandscapeDrawableStyleB.setShowPercent(drawPercentInside);
            mLandscapeDrawableBuddy.setShowPercent(drawPercentInside);
            mLandscapeDrawableLine.setShowPercent(drawPercentInside);
            mLandscapeDrawableMusku.setShowPercent(drawPercentInside);
            mLandscapeDrawablePill.setShowPercent(drawPercentInside);
            mLandscapeDrawableSignal.setShowPercent(drawPercentInside);
        }
        updatePercentText();
    }

    public void setIsQsHeader(boolean isQs) {
        mIsQsHeader = isQs;
    }

    public void updateVisibility() {
        if (mBatteryStyle == BATTERY_STYLE_TEXT || mBatteryStyle == BATTERY_STYLE_HIDDEN) {
            mBatteryIconView.setVisibility(View.GONE);
            mBatteryIconView.setImageDrawable(null);
        } else {
            mBatteryIconView.setVisibility(View.VISIBLE);
            scaleBatteryMeterViews();
        }
    }

    private void batteryPercentViewSetText(CharSequence text) {
        CharSequence currentText = mBatteryPercentView.getText();
        if (!currentText.toString().equals(text.toString())) {
            mBatteryPercentView.setText(text);
        }
    }

    private Drawable getUnknownStateDrawable() {
        if (mUnknownStateDrawable == null) {
            mUnknownStateDrawable = mContext.getDrawable(R.drawable.ic_battery_unknown);
            mUnknownStateDrawable.setTint(mTextColor);
        }

        return mUnknownStateDrawable;
    }

    void onBatteryUnknownStateChanged(boolean isUnknown) {
        if (mBatteryStateUnknown == isUnknown) {
            return;
        }

        mBatteryStateUnknown = isUnknown;
        updateContentDescription();

        if (mBatteryStateUnknown) {
            mBatteryIconView.setImageDrawable(getUnknownStateDrawable());
        } else {
            updateBatteryStyle();
        }

        updateShowPercent();
    }

    /**
     * Looks up the scale factor for status bar icons and scales the battery view by that amount.
     */
    void scaleBatteryMeterViews() {
        Resources res = getContext().getResources();
        TypedValue typedValue = new TypedValue();

        res.getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float iconScaleFactor = typedValue.getFloat();

        int batteryHeight = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height);
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_B || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_B ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_SIGNAL ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_signal) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_LINE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_line) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_PILL || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MUSKU ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_pill_musku) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_BUDDY ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_buddy) : batteryHeight;

        int batteryWidth = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);

        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_B || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_B ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_SIGNAL ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_signal): batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_LINE ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_line) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_PILL || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MUSKU ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_pill_musku) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_BUDDY ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_buddy) : batteryWidth;

        float mainBatteryHeight = batteryHeight * iconScaleFactor;
        float mainBatteryWidth = batteryWidth * iconScaleFactor;

        // If the battery is marked as overheated, we should display a shield indicating that the
        // battery is being "defended".
        boolean displayShield = mDisplayShieldEnabled && mIsOverheated;
        float fullBatteryIconHeight =
                BatterySpecs.getFullBatteryHeight(mainBatteryHeight, displayShield);
        float fullBatteryIconWidth =
                BatterySpecs.getFullBatteryWidth(mainBatteryWidth, displayShield);

        int marginTop;
        if (displayShield) {
            // If the shield is displayed, we need some extra marginTop so that the bottom of the
            // main icon is still aligned with the bottom of all the other system icons.
            int shieldHeightAddition = Math.round(fullBatteryIconHeight - mainBatteryHeight);
            // However, the other system icons have some embedded bottom padding that the battery
            // doesn't have, so we shouldn't move the battery icon down by the full amount.
            // See b/258672854.
            marginTop = shieldHeightAddition
                    - res.getDimensionPixelSize(R.dimen.status_bar_battery_extra_vertical_spacing);
        } else {
            marginTop = 0;
        }

        int marginBottom = res.getDimensionPixelSize(R.dimen.battery_margin_bottom);

        LinearLayout.LayoutParams scaledLayoutParams = new LinearLayout.LayoutParams(
                Math.round(fullBatteryIconWidth),
                Math.round(fullBatteryIconHeight));
        scaledLayoutParams.setMargins(0, marginTop, 0, marginBottom);

        mDrawable.setDisplayShield(displayShield);
        mBatteryIconView.setLayoutParams(scaledLayoutParams);
        mBatteryIconView.invalidateDrawable(mDrawable);
    }

    public void updateBatteryStyle() {
        switch (mBatteryStyle) {
            case BATTERY_STYLE_TEXT:
            case BATTERY_STYLE_HIDDEN:
            break;
            case BATTERY_STYLE_PORTRAIT:
                mBatteryIconView.setImageDrawable(mThemedDrawable);
                break;
            case BATTERY_STYLE_RLANDSCAPE:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawable);
                break;
            case BATTERY_STYLE_LANDSCAPE:
                mBatteryIconView.setImageDrawable(mLandscapeDrawable);
                break;
            case BATTERY_STYLE_FULL_CIRCLE:
                mBatteryIconView.setImageDrawable(mFullCircleDrawable);
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_A:
                mBatteryIconView.setImageDrawable(mRLandscapeDrawableStyleA);
                break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_A:
                mBatteryIconView.setImageDrawable(mLandscapeDrawableStyleA);
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_B:
                 mBatteryIconView.setImageDrawable(mRLandscapeDrawableStyleB);
                 break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_B:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawableStyleB);
                 break;
            case BATTERY_STYLE_LANDSCAPE_BUDDY:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawableBuddy);
                 break;
            case BATTERY_STYLE_LANDSCAPE_LINE:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawableLine);
                 break;
            case BATTERY_STYLE_LANDSCAPE_MUSKU:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawableMusku);
                 break;
            case BATTERY_STYLE_LANDSCAPE_PILL:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawablePill);
                 break;
            case BATTERY_STYLE_LANDSCAPE_SIGNAL:
                 mBatteryIconView.setImageDrawable(mLandscapeDrawableSignal);
                 break;
            default:
                mCircleDrawable.setMeterStyle(mBatteryStyle);
                mBatteryIconView.setImageDrawable(mCircleDrawable);
                break;
        }
    }

    @Override
    public void onDarkChanged(ArrayList<Rect> areas, float darkIntensity, int tint) {
        float intensity = DarkIconDispatcher.isInAreas(areas, this) ? darkIntensity : 0;
        mNonAdaptedSingleToneColor = mDualToneHandler.getSingleColor(intensity);
        mNonAdaptedForegroundColor = mDualToneHandler.getFillColor(intensity);
        mNonAdaptedBackgroundColor = mDualToneHandler.getBackgroundColor(intensity);

        updateColors(mNonAdaptedForegroundColor, mNonAdaptedBackgroundColor,
                mNonAdaptedSingleToneColor);
    }

    /**
     * Sets icon and text colors. This will be overridden by {@code onDarkChanged} events,
     * if registered.
     *
     * @param foregroundColor
     * @param backgroundColor
     * @param singleToneColor
     */
    public void updateColors(int foregroundColor, int backgroundColor, int singleToneColor) {
        mThemedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);

        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mFullCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);

        mRLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableBuddy.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableLine.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableMusku.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawablePill.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableSignal.setColors(foregroundColor, backgroundColor, singleToneColor);

        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        String powerSave = mThemedDrawable == null ?
                null : mThemedDrawable.getPowerSaveEnabled() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    getPowerSave: " + powerSave);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mLevel: " + mLevel);
        pw.println("    mMode: " + mShowPercentMode);
    }

    @VisibleForTesting
    CharSequence getBatteryPercentViewText() {
        return mBatteryPercentView.getText();
    }

    /** An interface that will fetch the estimated time remaining for the user's battery. */
    public interface BatteryEstimateFetcher {
        void fetchBatteryTimeRemainingEstimate(
                BatteryController.EstimateFetchCompletion completion);
    }

    public interface BatteryMeterViewCallbacks {
        default void onHiddenBattery(boolean hidden) {}
    }

    public void addCallback(BatteryMeterViewCallbacks callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(BatteryMeterViewCallbacks callbacks) {
        mCallbacks.remove(callbacks);
    }
}
