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

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT_CHARGING;
import static android.provider.Settings.System.QS_SHOW_BATTERY_ESTIMATE;
import static android.provider.Settings.System.STATUS_BAR_BATTERY_STYLE;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT_INSIDE;

import static com.android.systemui.DejankUtils.whitelistIpcs;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.content.ContentResolver;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;

import com.android.app.animation.Interpolators;

import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.RLandscapeBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawable;
import com.android.settingslib.graph.LandscapeBatteryDrawableiOS15;
import com.android.settingslib.graph.LandscapeBatteryDrawableiOS16;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleA;
import com.android.settingslib.graph.RLandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableStyleB;
import com.android.settingslib.graph.LandscapeBatteryDrawableBuddy;
import com.android.settingslib.graph.LandscapeBatteryDrawableLine;
import com.android.settingslib.graph.LandscapeBatteryDrawableSignal;
import com.android.settingslib.graph.LandscapeBatteryDrawableMusku;
import com.android.settingslib.graph.LandscapeBatteryDrawablePill;
import com.android.settingslib.graph.LandscapeBatteryDrawableOrigami;
import com.android.settingslib.graph.LandscapeBatteryDrawableMiUIPill;
import com.android.settingslib.graph.LandscapeBatteryA;
import com.android.settingslib.graph.LandscapeBatteryB;
import com.android.settingslib.graph.LandscapeBatteryC;
import com.android.settingslib.graph.LandscapeBatteryD;
import com.android.settingslib.graph.LandscapeBatteryJ;
import com.android.settingslib.graph.LandscapeBatteryM;
import com.android.settingslib.graph.LandscapeBatteryN;
import com.android.settingslib.graph.LandscapeBatteryO;
import com.android.settingslib.graph.ThemedBatteryDrawable;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.policy.BatteryController;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatteryMeterView extends LinearLayout implements DarkReceiver {

    @Retention(SOURCE)
    @IntDef({MODE_DEFAULT, MODE_ON, MODE_OFF, MODE_ESTIMATE})
    public @interface BatteryPercentMode {}
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_ON = 1;
    public static final int MODE_OFF = 2;
    public static final int MODE_ESTIMATE = 3;

    static final String STATUS_BAR_BATTERY_STYLE =
            Settings.System.STATUS_BAR_BATTERY_STYLE;

    public static final int BATTERY_STYLE_PORTRAIT = 0;
    public static final int BATTERY_STYLE_CIRCLE = 1;
    public static final int BATTERY_STYLE_DOTTED_CIRCLE = 2;
    public static final int BATTERY_STYLE_FULL_CIRCLE = 3;
    public static final int BATTERY_STYLE_TEXT = 4;
    public static final int BATTERY_STYLE_RLANDSCAPE = 5;
    public static final int BATTERY_STYLE_LANDSCAPE = 6;
    public static final int BATTERY_STYLE_LANDSCAPE_IOS15 = 7;
    public static final int BATTERY_STYLE_LANDSCAPE_IOS16 = 8;
    public static final int BATTERY_STYLE_LANDSCAPE_BUDDY = 9;
    public static final int BATTERY_STYLE_LANDSCAPE_LINE = 10;
    public static final int BATTERY_STYLE_LANDSCAPE_MUSKU = 11;
    public static final int BATTERY_STYLE_LANDSCAPE_PILL = 12;
    public static final int BATTERY_STYLE_LANDSCAPE_SIGNAL = 13;
    public static final int BATTERY_STYLE_RLANDSCAPE_STYLE_A = 14;
    public static final int BATTERY_STYLE_LANDSCAPE_STYLE_A = 15;
    public static final int BATTERY_STYLE_RLANDSCAPE_STYLE_B = 16;
    public static final int BATTERY_STYLE_LANDSCAPE_STYLE_B = 17;
    public static final int BATTERY_STYLE_LANDSCAPE_ORIGAMI = 18;
    public static final int BATTERY_STYLE_LANDSCAPE_MIUI_PILL = 19;
    public static final int BATTERY_STYLE_LANDSCAPE_SIMPLY = 20;
    public static final int BATTERY_STYLE_LANDSCAPE_NENINE = 21;
    public static final int BATTERY_STYLE_LANDSCAPE_COLOROS = 22;
    public static final int BATTERY_STYLE_LANDSCAPE_LOVE = 23;
    public static final int BATTERY_STYLE_LANDSCAPE_STRIP = 24;
    public static final int BATTERY_STYLE_LANDSCAPE_IOS_OUTLINE = 25;
    public static final int BATTERY_STYLE_LANDSCAPE_RULER = 26;
    public static final int BATTERY_STYLE_LANDSCAPE_WINDOWS = 27;

    private final CircleBatteryDrawable mCircleDrawable;
    private final FullCircleBatteryDrawable mFullCircleDrawable;
    private final AccessorizedBatteryDrawable mThemedDrawable;
    private final RLandscapeBatteryDrawable mRLandscapeDrawable;
    private final LandscapeBatteryDrawable mLandscapeDrawable;
    private final LandscapeBatteryDrawableiOS15 mLandscapeDrawableiOS15;
    private final LandscapeBatteryDrawableiOS16 mLandscapeDrawableiOS16;
    private final RLandscapeBatteryDrawableStyleA mRLandscapeDrawableStyleA;
    private final LandscapeBatteryDrawableStyleA mLandscapeDrawableStyleA;
    private final RLandscapeBatteryDrawableStyleB mRLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableStyleB mLandscapeDrawableStyleB;
    private final LandscapeBatteryDrawableBuddy mLandscapeDrawableBuddy;
    private final LandscapeBatteryDrawableLine mLandscapeDrawableLine;
    private final LandscapeBatteryDrawableMusku mLandscapeDrawableMusku;
    private final LandscapeBatteryDrawablePill mLandscapeDrawablePill;
    private final LandscapeBatteryDrawableSignal mLandscapeDrawableSignal;
    private final LandscapeBatteryDrawableOrigami mLandscapeDrawableOrigami;
    private final LandscapeBatteryDrawableMiUIPill mLandscapeDrawableMiUIPill;
    private final LandscapeBatteryA mLandscapeBatteryA;
    private final LandscapeBatteryB mLandscapeBatteryB;
    private final LandscapeBatteryC mLandscapeBatteryC;
    private final LandscapeBatteryD mLandscapeBatteryD;
    private final LandscapeBatteryJ mLandscapeBatteryJ;
    private final LandscapeBatteryM mLandscapeBatteryM;
    private final LandscapeBatteryN mLandscapeBatteryN;
    private final LandscapeBatteryO mLandscapeBatteryO;
    
    private ImageView mBatteryIconView;
    private TextView mBatteryPercentView;

    private final @StyleRes int mPercentageStyleId;
    private int mTextColor;
    private int mLevel;
    private int mShowPercentMode = MODE_DEFAULT;
    private String mEstimateText = null;
    private boolean mPluggedIn;
    private boolean mIsBatteryDefender;
    private boolean mIsIncompatibleCharging;
    private boolean mDisplayShieldEnabled;
    private boolean mPCharging;
    // Error state where we know nothing about the current battery state
    private boolean mBatteryStateUnknown;
    // Lazily-loaded since this is expected to be a rare-if-ever state
    private Drawable mUnknownStateDrawable;

    private int mBatteryStyle;

    private DualToneHandler mDualToneHandler;

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
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
        mRLandscapeDrawable = new RLandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawable = new LandscapeBatteryDrawable(context, frameColor);
        mLandscapeDrawableiOS15 = new LandscapeBatteryDrawableiOS15(context, frameColor);
        mLandscapeDrawableiOS16 = new LandscapeBatteryDrawableiOS16(context, frameColor);
        mRLandscapeDrawableStyleA = new RLandscapeBatteryDrawableStyleA(context, frameColor);
        mLandscapeDrawableStyleA = new LandscapeBatteryDrawableStyleA(context, frameColor);
        mRLandscapeDrawableStyleB = new RLandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableStyleB = new LandscapeBatteryDrawableStyleB(context, frameColor);
        mLandscapeDrawableBuddy = new LandscapeBatteryDrawableBuddy(context, frameColor);
        mLandscapeDrawableLine = new LandscapeBatteryDrawableLine(context, frameColor);
        mLandscapeDrawableMusku = new LandscapeBatteryDrawableMusku(context, frameColor);
        mLandscapeDrawablePill = new LandscapeBatteryDrawablePill(context, frameColor);
        mLandscapeDrawableSignal = new LandscapeBatteryDrawableSignal(context, frameColor);
        mLandscapeDrawableOrigami = new LandscapeBatteryDrawableOrigami(context, frameColor);
        mLandscapeDrawableMiUIPill = new LandscapeBatteryDrawableMiUIPill(context, frameColor);
        mLandscapeBatteryA = new LandscapeBatteryA(context, frameColor);
        mLandscapeBatteryB = new LandscapeBatteryB(context, frameColor);
        mLandscapeBatteryC = new LandscapeBatteryC(context, frameColor);
        mLandscapeBatteryD = new LandscapeBatteryD(context, frameColor);
        mLandscapeBatteryJ = new LandscapeBatteryJ(context, frameColor);
        mLandscapeBatteryM = new LandscapeBatteryM(context, frameColor);
        mLandscapeBatteryN = new LandscapeBatteryN(context, frameColor);
        mLandscapeBatteryO = new LandscapeBatteryO(context, frameColor);
        mThemedDrawable = new AccessorizedBatteryDrawable(context, frameColor);
        atts.recycle();

        setupLayoutTransition();

        updateBatteryStyle();
        mDualToneHandler = new DualToneHandler(context);
        // Init to not dark at all.
        onDarkChanged(new ArrayList<Rect>(), 0, DarkIconDispatcher.DEFAULT_ICON_TINT);

        setClipChildren(false);
        setClipToPadding(false);
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
        mThemedDrawable.notifyDensityChanged();
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

    /**
     * Update battery level
     *
     * @param level     int between 0 and 100 (representing percentage value)
     * @param pluggedIn whether the device is plugged in or not
     */
    public void onBatteryLevelChanged(@IntRange(from = 0, to = 100) int level, boolean pluggedIn) {
        mThemedDrawable.setCharging(pluggedIn);
        mThemedDrawable.setBatteryLevel(level);
        mCircleDrawable.setCharging(pluggedIn);
        mCircleDrawable.setBatteryLevel(level);
        mFullCircleDrawable.setCharging(pluggedIn);
        mFullCircleDrawable.setBatteryLevel(level);
        mRLandscapeDrawable.setBatteryLevel(level);
        mRLandscapeDrawable.setCharging(pluggedIn);
        mLandscapeDrawable.setBatteryLevel(level);
        mLandscapeDrawable.setCharging(pluggedIn);
        mLandscapeDrawableiOS15.setBatteryLevel(level);
        mLandscapeDrawableiOS15.setCharging(pluggedIn);
        mLandscapeDrawableiOS16.setBatteryLevel(level);
        mLandscapeDrawableiOS16.setCharging(pluggedIn);
        mRLandscapeDrawableStyleA.setBatteryLevel(level);
        mLandscapeDrawableStyleA.setBatteryLevel(level);
        mRLandscapeDrawableStyleB.setBatteryLevel(level);
        mLandscapeDrawableStyleB.setBatteryLevel(level);
        mLandscapeDrawableBuddy.setBatteryLevel(level);
        mLandscapeDrawableLine.setBatteryLevel(level);
        mLandscapeDrawableMusku.setBatteryLevel(level);
        mLandscapeDrawablePill.setBatteryLevel(level);
        mLandscapeDrawableSignal.setBatteryLevel(level);
        mRLandscapeDrawableStyleA.setCharging(pluggedIn);
        mLandscapeDrawableStyleA.setCharging(pluggedIn);
        mRLandscapeDrawableStyleB.setCharging(pluggedIn);
        mLandscapeDrawableStyleB.setCharging(pluggedIn);
        mLandscapeDrawableBuddy.setCharging(pluggedIn);
        mLandscapeDrawableLine.setCharging(pluggedIn);
        mLandscapeDrawableMusku.setCharging(pluggedIn);
        mLandscapeDrawablePill.setCharging(pluggedIn);
        mLandscapeDrawableSignal.setCharging(pluggedIn);
        mLandscapeDrawableOrigami.setBatteryLevel(level);
        mLandscapeDrawableOrigami.setCharging(pluggedIn);
        mLandscapeDrawableMiUIPill.setBatteryLevel(level);
        mLandscapeDrawableMiUIPill.setCharging(pluggedIn);
        mLandscapeBatteryA.setBatteryLevel(level);
        mLandscapeBatteryB.setBatteryLevel(level);
        mLandscapeBatteryC.setBatteryLevel(level);
        mLandscapeBatteryD.setBatteryLevel(level);
        mLandscapeBatteryJ.setBatteryLevel(level);
        mLandscapeBatteryM.setBatteryLevel(level);
        mLandscapeBatteryN.setBatteryLevel(level);
        mLandscapeBatteryO.setBatteryLevel(level);
        mLandscapeBatteryA.setCharging(pluggedIn);
        mLandscapeBatteryB.setCharging(pluggedIn);
        mLandscapeBatteryC.setCharging(pluggedIn);
        mLandscapeBatteryD.setCharging(pluggedIn);
        mLandscapeBatteryJ.setCharging(pluggedIn);
        mLandscapeBatteryM.setCharging(pluggedIn);
        mLandscapeBatteryN.setCharging(pluggedIn);
        mLandscapeBatteryO.setCharging(pluggedIn);
        mPluggedIn = pluggedIn;
        mLevel = level;
        updatePercentText();
        updateShowPercent();
    }

    void onPowerSaveChanged(boolean isPowerSave) {
        mCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mThemedDrawable.setPowerSaveEnabled(isPowerSave);
        mFullCircleDrawable.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawable.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableiOS15.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableiOS16.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableStyleA.setPowerSaveEnabled(isPowerSave);
        mRLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableStyleB.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableBuddy.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableLine.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableMusku.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawablePill.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableSignal.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableOrigami.setPowerSaveEnabled(isPowerSave);
        mLandscapeDrawableMiUIPill.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryA.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryB.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryC.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryD.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryJ.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryM.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryN.setPowerSaveEnabled(isPowerSave);
        mLandscapeBatteryO.setPowerSaveEnabled(isPowerSave);
    }

    void onIsBatteryDefenderChanged(boolean isBatteryDefender) {
        boolean valueChanged = mIsBatteryDefender != isBatteryDefender;
        mIsBatteryDefender = isBatteryDefender;
        if (valueChanged) {
            updateContentDescription();
            // The battery drawable is a different size depending on whether it's currently
            // overheated or not, so we need to re-scale the view when overheated changes.
            scaleBatteryMeterViews();
        }
    }

    void onIsIncompatibleChargingChanged(boolean isIncompatibleCharging) {
        boolean valueChanged = mIsIncompatibleCharging != isIncompatibleCharging;
        mIsIncompatibleCharging = isIncompatibleCharging;
        if (valueChanged) {
            mThemedDrawable.setCharging(isCharging());
            mCircleDrawable.setCharging(isCharging());
            mFullCircleDrawable.setCharging(isCharging());
            mRLandscapeDrawable.setCharging(isCharging());
            mLandscapeDrawable.setCharging(isCharging());
            mLandscapeDrawableiOS15.setCharging(isCharging());
            mLandscapeDrawableiOS16.setCharging(isCharging());
            mRLandscapeDrawableStyleA.setCharging(isCharging());
            mLandscapeDrawableStyleA.setCharging(isCharging());
            mRLandscapeDrawableStyleB.setCharging(isCharging());
            mLandscapeDrawableStyleB.setCharging(isCharging());
            mLandscapeDrawableBuddy.setCharging(isCharging());
            mLandscapeDrawableLine.setCharging(isCharging());
            mLandscapeDrawableMusku.setCharging(isCharging());
            mLandscapeDrawablePill.setCharging(isCharging());
            mLandscapeDrawableSignal.setCharging(isCharging());
            mLandscapeDrawableOrigami.setCharging(isCharging());
            mLandscapeDrawableMiUIPill.setCharging(isCharging());
            mLandscapeBatteryA.setCharging(isCharging());
            mLandscapeBatteryB.setCharging(isCharging());
            mLandscapeBatteryC.setCharging(isCharging());
            mLandscapeBatteryD.setCharging(isCharging());
            mLandscapeBatteryJ.setCharging(isCharging());
            mLandscapeBatteryM.setCharging(isCharging());
            mLandscapeBatteryN.setCharging(isCharging());
            mLandscapeBatteryO.setCharging(isCharging());
            updateContentDescription();
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
        if (mBatteryPercentView != null) {
            removeView(mBatteryPercentView);
            mBatteryPercentView = null;
        }
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

        final boolean userShowEstimate = Settings.System.getIntForUser(
                getContext().getContentResolver(), QS_SHOW_BATTERY_ESTIMATE,
                1, UserHandle.USER_CURRENT) == 1;

        if (mBatteryPercentView != null) {
            if (mShowPercentMode == MODE_ESTIMATE && !isCharging() && userShowEstimate) {
                mBatteryEstimateFetcher.fetchBatteryTimeRemainingEstimate(
                        (String estimate) -> {
                    if (mBatteryPercentView == null) {
                        return;
                    }
                    if (estimate != null && mShowPercentMode == MODE_ESTIMATE) {
                        mEstimateText = estimate;
                        mBatteryPercentView.setText(estimate);
                        updateContentDescription();
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
        if (mBatteryPercentView != null) {
            mEstimateText = null;
            String percentText = NumberFormat.getPercentInstance().format(mLevel / 100f);
            // Setting text actually triggers a layout pass (because the text view is set to
            // wrap_content width and TextView always relayouts for this). Avoid needless
            // relayout if the text didn't actually change.
            if (!TextUtils.equals(mBatteryPercentView.getText(), percentText) || mPCharging != isCharging()) {
                mPCharging = isCharging();
                // Use the high voltage symbol ⚡ (u26A1 unicode) but prevent the system
                // to load its emoji colored variant with the uFE0E flag
                // only use it when there is no batt icon showing
                String indication = isCharging() && (mBatteryStyle == BATTERY_STYLE_TEXT)
                        ? "\u26A1\uFE0E " : "";
                mBatteryPercentView.setText(indication + percentText);
            }
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
                    mIsBatteryDefender
                            ? R.string.accessibility_battery_level_charging_paused_with_estimate
                            : R.string.accessibility_battery_level_with_estimate,
                    mLevel,
                    mEstimateText);
        } else if (mIsBatteryDefender) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging_paused, mLevel);
        } else if (isCharging()) {
            contentDescription =
                    context.getString(R.string.accessibility_battery_level_charging, mLevel);
        } else {
            contentDescription = context.getString(R.string.accessibility_battery_level, mLevel);
        }

        setContentDescription(contentDescription);
    }

    void updateShowPercent() {
        final ContentResolver resolver = getContext().getContentResolver();
        final boolean showing = mBatteryPercentView != null;

        // user settings
        final boolean showBatteryPercent = Settings.System.getIntForUser(
                resolver, SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT) == 1;
        final boolean userDrawPercentInside = Settings.System.getIntForUser(
                resolver, SHOW_BATTERY_PERCENT_INSIDE, 0, UserHandle.USER_CURRENT) == 1;
        final boolean showBatteryPercentCharging = Settings.System.getIntForUser(
                resolver, SHOW_BATTERY_PERCENT_CHARGING, 0, UserHandle.USER_CURRENT) == 1;

        // some boolean algebra, don't freak out
        final boolean chargeForcePercent = showBatteryPercentCharging && isCharging();
        final boolean drawPercent = mShowPercentMode == MODE_DEFAULT
                && (showBatteryPercent || chargeForcePercent);
        final boolean isEstimate = mShowPercentMode == MODE_ESTIMATE;
        final boolean isText = mBatteryStyle == BATTERY_STYLE_TEXT;
        final boolean drawInside = drawPercent && userDrawPercentInside;

        // always draw when we show estimate or in text mode
        // don't show if we're set to draw inside or we disabled % entirely
        if (isEstimate || isText || (drawPercent && (!drawInside || chargeForcePercent))) {
            // draw next to the icon
            mCircleDrawable.setShowPercent(false);
            mThemedDrawable.setShowPercent(false);
            mFullCircleDrawable.setShowPercent(false);
            mRLandscapeDrawable.setShowPercent(false);
            mLandscapeDrawable.setShowPercent(false);
            mLandscapeDrawableiOS15.setShowPercent(false);
            mLandscapeDrawableiOS16.setShowPercent(false);
            mRLandscapeDrawableStyleA.setShowPercent(false);
            mLandscapeDrawableStyleA.setShowPercent(false);
            mRLandscapeDrawableStyleB.setShowPercent(false);
            mLandscapeDrawableStyleB.setShowPercent(false);
            mLandscapeDrawableBuddy.setShowPercent(false);
            mLandscapeDrawableLine.setShowPercent(false);
            mLandscapeDrawableMusku.setShowPercent(false);
            mLandscapeDrawablePill.setShowPercent(false);
            mLandscapeDrawableSignal.setShowPercent(false);
            mLandscapeDrawableOrigami.setShowPercent(false);
            mLandscapeDrawableMiUIPill.setShowPercent(false);
            mLandscapeBatteryA.setShowPercent(false);
            mLandscapeBatteryB.setShowPercent(false);
            mLandscapeBatteryC.setShowPercent(false);
            mLandscapeBatteryD.setShowPercent(false);
            mLandscapeBatteryJ.setShowPercent(false);
            mLandscapeBatteryM.setShowPercent(false);
            mLandscapeBatteryN.setShowPercent(false);
            mLandscapeBatteryO.setShowPercent(false);
            if (!showing) {
                mBatteryPercentView = loadPercentView();
                if (mPercentageStyleId != 0) { // Only set if specified as attribute
                    mBatteryPercentView.setTextAppearance(mPercentageStyleId);
                }
                if (mTextColor != 0) mBatteryPercentView.setTextColor(mTextColor);
                updatePercentText();
                addView(mBatteryPercentView, new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.MATCH_PARENT));
            }
            int paddingStart = getResources().getDimensionPixelSize(
                    R.dimen.battery_level_padding_start);
            mBatteryPercentView.setPaddingRelative(isText ? 0 : paddingStart, 0, 0, 0);
        } else {
            // maybe draw inside
            mCircleDrawable.setShowPercent(drawInside);
            mThemedDrawable.setShowPercent(drawInside);
            mFullCircleDrawable.setShowPercent(drawInside);
            mRLandscapeDrawable.setShowPercent(drawInside);
            mLandscapeDrawable.setShowPercent(drawInside);
            mLandscapeDrawableiOS15.setShowPercent(drawInside);
            mLandscapeDrawableiOS16.setShowPercent(drawInside);
            mRLandscapeDrawableStyleA.setShowPercent(drawInside);
            mLandscapeDrawableStyleA.setShowPercent(drawInside);
            mRLandscapeDrawableStyleB.setShowPercent(drawInside);
            mLandscapeDrawableStyleB.setShowPercent(drawInside);
            mLandscapeDrawableBuddy.setShowPercent(drawInside);
            mLandscapeDrawableLine.setShowPercent(drawInside);
            mLandscapeDrawableMusku.setShowPercent(drawInside);
            mLandscapeDrawablePill.setShowPercent(drawInside);
            mLandscapeDrawableSignal.setShowPercent(drawInside);
            mLandscapeDrawableOrigami.setShowPercent(drawInside);
            mLandscapeDrawableMiUIPill.setShowPercent(drawInside);
            mLandscapeBatteryA.setShowPercent(drawInside);
            mLandscapeBatteryB.setShowPercent(drawInside);
            mLandscapeBatteryC.setShowPercent(drawInside);
            mLandscapeBatteryD.setShowPercent(drawInside);
            mLandscapeBatteryJ.setShowPercent(drawInside);
            mLandscapeBatteryM.setShowPercent(drawInside);
            mLandscapeBatteryN.setShowPercent(drawInside);
            mLandscapeBatteryO.setShowPercent(drawInside);
            if (showing) {
                // remove the percentage view
                removeView(mBatteryPercentView);
                mBatteryPercentView = null;
            }
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
        if (mBatteryIconView == null) return;
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
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_IOS15 ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_ios15) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_IOS16 ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_ios16) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_SIGNAL ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_signal) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_LINE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_line) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_PILL || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MUSKU ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_pill_musku) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_BUDDY ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_buddy) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_ORIGAMI ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_origami) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MIUI_PILL ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_miui_pill) : batteryHeight;
        batteryHeight = mBatteryStyle == BATTERY_STYLE_LANDSCAPEA ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEB ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEC ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPED ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEM ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEN ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEO ?
        res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_height_landscape_a_o) : batteryHeight;   

        int batteryWidth = mBatteryStyle == BATTERY_STYLE_CIRCLE || mBatteryStyle == BATTERY_STYLE_DOTTED_CIRCLE
                || mBatteryStyle == BATTERY_STYLE_FULL_CIRCLE ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_circle_width) :
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width);
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_A || mBatteryStyle == BATTERY_STYLE_RLANDSCAPE_STYLE_B || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_STYLE_B ?
                res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_IOS15 ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_ios15) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_IOS16 ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_ios16) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_SIGNAL ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_signal): batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_LINE ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_line) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_PILL || mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MUSKU ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_pill_musku) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_BUDDY ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_buddy) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_ORIGAMI ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_origami) : batteryWidth;
        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPE_MIUI_PILL ?
               res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_miui_pill) : batteryWidth;

        batteryWidth = mBatteryStyle == BATTERY_STYLE_LANDSCAPEA ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEB ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEC ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPED ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEJ ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEM ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEN ||
                	mBatteryStyle == BATTERY_STYLE_LANDSCAPEO ?
        res.getDimensionPixelSize(R.dimen.status_bar_battery_icon_width_landscape_a_o) : batteryWidth;

        float mainBatteryHeight = batteryHeight * iconScaleFactor;
        float mainBatteryWidth = batteryWidth * iconScaleFactor;

        boolean displayShield = mDisplayShieldEnabled && mIsBatteryDefender;
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

        mThemedDrawable.setDisplayShield(displayShield);
        mBatteryIconView.setLayoutParams(scaledLayoutParams);
        mBatteryIconView.invalidateDrawable(mThemedDrawable);
    }

    void updateBatteryStyle() {
        mBatteryStyle = Settings.System.getIntForUser(
                getContext().getContentResolver(), STATUS_BAR_BATTERY_STYLE,
                BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        switch (mBatteryStyle) {
            case BATTERY_STYLE_PORTRAIT:
                addOrRemoveIcon(mThemedDrawable);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_RLANDSCAPE:
                addOrRemoveIcon(mRLandscapeDrawable);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE:
                addOrRemoveIcon(mLandscapeDrawable);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS15:
                addOrRemoveIcon(mLandscapeDrawableiOS15);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_IOS16:
                addOrRemoveIcon(mLandscapeDrawableiOS16);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_A:
                addOrRemoveIcon(mRLandscapeDrawableStyleA);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_A:
                addOrRemoveIcon(mLandscapeDrawableStyleA);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_RLANDSCAPE_STYLE_B:
                addOrRemoveIcon(mRLandscapeDrawableStyleB);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_STYLE_B:
                addOrRemoveIcon(mLandscapeDrawableStyleB);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_BUDDY:
                addOrRemoveIcon(mLandscapeDrawableBuddy);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_LINE:
                addOrRemoveIcon(mLandscapeDrawableLine);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_MUSKU:
                addOrRemoveIcon(mLandscapeDrawableMusku);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_PILL:
                addOrRemoveIcon(mLandscapeDrawablePill);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_SIGNAL:
                addOrRemoveIcon(mLandscapeDrawableSignal);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_ORIGAMI:
                addOrRemoveIcon(mLandscapeDrawableOrigami);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPE_MIUI_PILL:
                addOrRemoveIcon(mLandscapeDrawableMiUIPill);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_LANDSCAPEA:
                addOrRemoveIcon(mLandscapeBatteryA);
                break;
            case BATTERY_STYLE_LANDSCAPEB:
                addOrRemoveIcon(mLandscapeBatteryB);
                break;
            case BATTERY_STYLE_LANDSCAPEC:
                addOrRemoveIcon(mLandscapeBatteryC);
                break;
            case BATTERY_STYLE_LANDSCAPED:
                addOrRemoveIcon(mLandscapeBatteryD);
                break;
            case BATTERY_STYLE_LANDSCAPEJ:
                addOrRemoveIcon(mLandscapeBatteryJ);
                break;
            case BATTERY_STYLE_LANDSCAPEM:
                addOrRemoveIcon(mLandscapeBatteryM);
                break;
            case BATTERY_STYLE_LANDSCAPEN:
                addOrRemoveIcon(mLandscapeBatteryN);
                break;
            case BATTERY_STYLE_LANDSCAPEO:
                addOrRemoveIcon(mLandscapeBatteryO);
                break;
            case BATTERY_STYLE_FULL_CIRCLE:
                addOrRemoveIcon(mFullCircleDrawable);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_CIRCLE:
            case BATTERY_STYLE_DOTTED_CIRCLE:
                mCircleDrawable.setMeterStyle(mBatteryStyle);
                addOrRemoveIcon(mCircleDrawable);
                scaleBatteryMeterViews();
                break;
            case BATTERY_STYLE_TEXT:
                addOrRemoveIcon(null);
                break;
        }
        updateShowPercent();
    }

    private void addOrRemoveIcon(Drawable style) {
        if (mBatteryIconView != null) {
            removeView(mBatteryIconView);
            mBatteryIconView = null;
        }

        if (style != null) {
            mBatteryIconView = new ImageView(getContext());
            mBatteryIconView.setImageDrawable(style);
            final MarginLayoutParams mlp = new MarginLayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_width),
                    getResources().getDimensionPixelSize(R.dimen.status_bar_battery_icon_height));
            mlp.setMargins(0, 0, 0,
                    getResources().getDimensionPixelOffset(R.dimen.battery_margin_bottom));
            addView(mBatteryIconView, 0, mlp);
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
        mCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mThemedDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mFullCircleDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawable.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableiOS15.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableiOS16.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mRLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableStyleB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableBuddy.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableLine.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableMusku.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawablePill.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableSignal.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeDrawableOrigami.setColors(foregroundColor,backgroundColor, singleToneColor);
        mLandscapeDrawableMiUIPill.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryA.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryB.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryC.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryD.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryJ.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryM.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryN.setColors(foregroundColor, backgroundColor, singleToneColor);
        mLandscapeBatteryO.setColors(foregroundColor, backgroundColor, singleToneColor);
        mTextColor = singleToneColor;
        if (mBatteryPercentView != null) {
            mBatteryPercentView.setTextColor(singleToneColor);
        }

        if (mUnknownStateDrawable != null) {
            mUnknownStateDrawable.setTint(singleToneColor);
        }
    }

    private boolean isCharging() {
        return mPluggedIn && !mIsIncompatibleCharging;
    }

    public void dump(PrintWriter pw, String[] args) {
        String powerSave = mThemedDrawable == null ? null : mThemedDrawable.getPowerSaveEnabled() + "";
        String displayShield = mThemedDrawable == null ? null : mThemedDrawable.getDisplayShield() + "";
        String charging = mThemedDrawable == null ? null : mThemedDrawable.getCharging() + "";
        CharSequence percent = mBatteryPercentView == null ? null : mBatteryPercentView.getText();
        pw.println("  BatteryMeterView:");
        pw.println("    mThemedDrawable.getPowerSave: " + powerSave);
        pw.println("    mThemedDrawable.getDisplayShield: " + displayShield);
        pw.println("    mThemedDrawable.getCharging: " + charging);
        pw.println("    mBatteryPercentView.getText(): " + percent);
        pw.println("    mTextColor: #" + Integer.toHexString(mTextColor));
        pw.println("    mBatteryStateUnknown: " + mBatteryStateUnknown);
        pw.println("    mIsIncompatibleCharging: " + mIsIncompatibleCharging);
        pw.println("    mPluggedIn: " + mPluggedIn);
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
}
