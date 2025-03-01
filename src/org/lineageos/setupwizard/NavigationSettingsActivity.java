/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static com.evervolv.internal.util.DeviceKeysConstants.KEY_MASK_APP_SWITCH;
import static org.lineageos.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.lineageos.setupwizard.SetupWizardApp.NAVIGATION_OPTION_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.airbnb.lottie.LottieAnimationView;

import org.lineageos.setupwizard.util.SetupWizardUtils;

import evervolv.hardware.HardwareManager;
import evervolv.provider.EVSettings;

public class NavigationSettingsActivity extends BaseSetupWizardActivity {

    private SetupWizardApp mSetupWizardApp;

    private CheckBox mNavKeys;

    private String mSelection = NAV_BAR_MODE_GESTURAL_OVERLAY;

    private boolean mSupportsKeyDisabler = false;

    private final View.OnClickListener mNavKeysClickListener = view -> {
        boolean checked = !mNavKeys.isChecked();
        mNavKeys.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(DISABLE_NAV_KEYS, checked);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSetupWizardApp = (SetupWizardApp) getApplication();

        View navKeysRow = findViewById(R.id.nav_keys);
        navKeysRow.setOnClickListener(mNavKeysClickListener);
        mNavKeys = findViewById(R.id.nav_keys_checkbox);
        mSupportsKeyDisabler = isKeyDisablerSupported(this);
        if (mSupportsKeyDisabler) {
            mNavKeys.setChecked(EVSettings.Secure.getIntForUser(getContentResolver(),
                    EVSettings.Secure.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0);
        } else {
            navKeysRow.setVisibility(View.GONE);
        }

        boolean navBarEnabled = false;
        if (mSetupWizardApp.getSettingsBundle().containsKey(DISABLE_NAV_KEYS)) {
            navBarEnabled = mSetupWizardApp.getSettingsBundle().getBoolean(DISABLE_NAV_KEYS);
        }

        int deviceKeys = getResources().getInteger(
                com.evervolv.platform.internal.R.integer.config_deviceHardwareKeys);
        boolean hasHomeKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        getGlifLayout().setDescriptionText(getString(R.string.navigation_summary));
        setNextText(R.string.next);

        int available = 3;
        // Hide unavailable navigation modes
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            findViewById(R.id.radio_gesture).setVisibility(View.GONE);
            ((RadioButton) findViewById(R.id.radio_sw_keys)).setChecked(true);
            available--;
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_2BUTTON_OVERLAY)) {
            findViewById(R.id.radio_two_button).setVisibility(View.GONE);
            available--;
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            findViewById(R.id.radio_sw_keys).setVisibility(View.GONE);
            available--;
        }

        // Hide this page if the device has hardware keys but didn't enable navbar
        // or if there's <= 1 available navigation modes
        if (!navBarEnabled && hasHomeKey || available <= 1) {
            mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY,
                    NAV_BAR_MODE_3BUTTON_OVERLAY);
            finishAction(RESULT_OK);
        }

        final LottieAnimationView navigationIllustration =
                findViewById(R.id.navigation_illustration);
        final RadioGroup radioGroup = findViewById(R.id.navigation_radio_group);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radio_gesture:
                    mSelection = NAV_BAR_MODE_GESTURAL_OVERLAY;
                    navigationIllustration
                            .setAnimation(R.raw.lottie_system_nav_fully_gestural);
                    break;
                case R.id.radio_two_button:
                    mSelection = NAV_BAR_MODE_2BUTTON_OVERLAY;
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_2_button);
                    break;
                case R.id.radio_sw_keys:
                    mSelection = NAV_BAR_MODE_3BUTTON_OVERLAY;
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_3_button);
                    break;
            }

            navigationIllustration.playAnimation();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisableNavkeysOption();
    }

    @Override
    protected void onNextPressed() {
        mSetupWizardApp.getSettingsBundle().putString(NAVIGATION_OPTION_KEY, mSelection);
        super.onNextPressed();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_navigation;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_navigation;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_navigation;
    }

    private void updateDisableNavkeysOption() {
        if (mSupportsKeyDisabler) {
            final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
            boolean enabled = EVSettings.Secure.getIntForUser(getContentResolver(),
                    EVSettings.Secure.DEV_FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
            boolean checked = myPageBundle.containsKey(DISABLE_NAV_KEYS) ?
                    myPageBundle.getBoolean(DISABLE_NAV_KEYS) :
                    enabled;
            mNavKeys.setChecked(checked);
            myPageBundle.putBoolean(DISABLE_NAV_KEYS, checked);
        }
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final HardwareManager hardware = HardwareManager.getInstance(context);
        return hardware.isSupported(HardwareManager.FEATURE_KEY_DISABLE);
    }
}
