/*
 * Copyright (c) 2011-2013 The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

/**
 * Second level "MSim Call settings" UI; see res/xml/msim_call_feature_sub_setting.xml
 *
 * This preference screen is the root of the "MSim Call settings" hierarchy
 * available from the Phone app; the settings here let you control various
 * features related to phone calls (including voicemail settings
 * and others.)  It's used only on voice-capable phone devices.
 *
 * Note that this activity is part of the package com.android.phone, even
 * though you reach it from the "Phone" app (i.e. DialtactsActivity) which
 * is from the package com.android.contacts.
 *
 * For the "MSim Mobile network settings" screen under the main Settings app,
 * see apps/Phone/src/com/android/phone/Settings.java.
 */
public class MSimCallFeaturesSubSetting extends CallFeaturesSetting {
    private static final String LOG_TAG = "MSimCallFeaturesSubSetting";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    // String keys for preference lookup
    // TODO: Naming these "BUTTON_*" is confusing since they're not actually buttons(!)
    private static final String BUTTON_RINGTONE_CATEGORY_KEY = "button_ringtone_category_key";
    private static final String BUTTON_CF_EXPAND_KEY = "button_cf_expand_key";
    private static final String BUTTON_MORE_EXPAND_KEY = "button_more_expand_key";
    private static final String BUTTON_CB_EXPAND_KEY = "button_callbarring_expand_key";

    private PreferenceScreen mSubscriptionPrefFDN;
    private PreferenceScreen mSubscriptionPrefGSM;
    private PreferenceScreen mSubscriptionPrefCDMA;
    private PreferenceScreen mSubscriptionPrefEXPAND;
    private PreferenceScreen mSubscriptionPrefMOREEXPAND;


    /**
     * Receiver for Receiver for ACTION_AIRPLANE_MODE_CHANGED and ACTION_SIM_STATE_CHANGED.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver (){
        @Override
        public void onReceive(Context context, Intent intent) {
            setScreenState();
        }
    };

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    private void setScreenState() {
        int simState = MSimTelephonyManager.getDefault().getSimState(mSubscription);
        getPreferenceScreen().setEnabled(simState == TelephonyManager.SIM_STATE_READY);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        // getting selected subscription
        mSubscription = getIntent().getIntExtra(SUBSCRIPTION_KEY, 0);
        super.onCreate(icicle);
        log("onCreate(). Intent: " + getIntent());
        log("settings onCreate subscription =" + mSubscription);

        //Register for intent broadcasts
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);

        registerReceiver(mReceiver, intentFilter);

        mSubscriptionPrefFDN  = (PreferenceScreen) findPreference(BUTTON_FDN_KEY);
        mSubscriptionPrefGSM  = (PreferenceScreen) findPreference(BUTTON_GSM_UMTS_OPTIONS);
        mSubscriptionPrefCDMA = (PreferenceScreen) findPreference(BUTTON_CDMA_OPTIONS);
        if (mSubscriptionPrefFDN != null) {
            mSubscriptionPrefFDN.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
        }
        if (mSubscriptionPrefGSM != null) {
            mSubscriptionPrefGSM.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
        }
        if (mSubscriptionPrefCDMA != null) {
            mSubscriptionPrefCDMA.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setScreenState();
    }

    private static void log(String msg) {
        if (DBG) Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onCreateLookupPrefs() {
        //Do Nothing
    }

    @Override
    protected void onResumeLookupPrefs() {
       //Do Nothing
    }

    @Override
    protected void addOptionalPrefs(PreferenceScreen preferenceScreen) {
        super.addOptionalPrefs(preferenceScreen);
        if (!getResources().getBoolean(R.bool.world_phone)) {
            int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                mSubscriptionPrefEXPAND = (PreferenceScreen) findPreference(BUTTON_CF_EXPAND_KEY);
                mSubscriptionPrefMOREEXPAND =
                        (PreferenceScreen) findPreference(BUTTON_MORE_EXPAND_KEY);
                mSubscriptionPrefEXPAND.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
                mSubscriptionPrefMOREEXPAND.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
                findPreference(BUTTON_CB_EXPAND_KEY).getIntent().putExtra(SUBSCRIPTION_KEY,
                        mSubscription);
            }
        }
    }

    @Override
    protected void removeOptionalPrefs(PreferenceScreen preferenceScreen) {
        super.removeOptionalPrefs(preferenceScreen);
        // "Vibrate When Ringing" item is no long needed on DSDS mode
        if (mVibrateWhenRinging != null) {
            PreferenceGroup ringtoneCategory = (PreferenceGroup)
                    findPreference(BUTTON_RINGTONE_CATEGORY_KEY);
            ringtoneCategory.removePreference(mVibrateWhenRinging);
        }
    }

    /**
     * Finish current Activity and go up to the top level Settings ({@link CallFeaturesSetting}).
     * This is useful for implementing "HomeAsUp" capability for second-level Settings.
     */
    public static void goUpToTopLevelSetting(Activity activity) {
        Intent intent = new Intent(activity, SelectSubscription.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.putExtra(SelectSubscription.PACKAGE, "com.android.phone");
        intent.putExtra(SelectSubscription.TARGET_CLASS,
                "com.android.phone.MSimCallFeaturesSubSetting");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    protected int getPreferencesResource() {
        return R.xml.msim_call_feature_sub_setting;
    }

    @Override
    protected Phone getPhone() {
        return MSimPhoneGlobals.getInstance().getPhone(mSubscription);
    }
}
