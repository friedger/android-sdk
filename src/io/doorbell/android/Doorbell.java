package io.doorbell.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.doorbell.android.callbacks.OnShowCallback;
import io.doorbell.android.manavo.rest.RestCallback;

public class Doorbell extends AlertDialog.Builder {

    public static final String PROPS_MODEL = "Model";
    public static final String PROPS_ANDROID_VERSION = "Android Version";
    public static final String PROPS_WI_FI_ENABLED = "WiFi enabled";
    public static final String PROPS_MOBILE_DATA_ENABLED = "Mobile Data enabled";
    public static final String PROPS_GPS_ENABLED = "GPS enabled";
    public static final String PROPS_SCREEN_RESOLUTION = "Screen Resolution";
    public static final String PROPS_ACTIVITY = "Activity";
    public static final String POWERED_BY_DOORBELL_TEXT = "Powered by <a href=\"https://doorbell.io\">Doorbell.io</a>";
    public static final String PROPS_APP_VERSION_NAME = "App Version Name";
    public static final String PROPS_APP_VERSION_CODE = "App Version Code";
    private Activity mActivity;
    private Context mContext;

    private String mMessageHint;
    private String mEmailHint;
    private int mEmailFieldVisibility = View.VISIBLE;
    private int mPoweredByVisibility = View.VISIBLE;
    private OnShowCallback mOnShowCallback = null;

    private String mEmail = "";
    private String mName = "";

    private EditText mMessageField;
    private EditText mEmailField;

    private JSONObject mProperties;

    private DoorbellApi mApi;

    public Doorbell(Activity activity, long id, String privateKey) {
        super(activity);

        this.mApi = new DoorbellApi(activity);

        this.mProperties = new JSONObject();

        this.mActivity = activity;
        this.mContext = activity;
        this.setAppId(id);
        this.setApiKey(privateKey);

        this.setTitle(getString(R.string.drbl__feedback);
        this.setMessageHint(getString(R.string.drbl__whats_on_your_mind));
        this.setEmail(getString(R.string.drbl__your_email));

        this.setCancelable(true);

        this.buildProperties();


        // Set app related properties
        PackageManager manager = activity.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);

            this.addProperty(PROPS_APP_VERSION_NAME, info.versionName);
            this.addProperty(PROPS_APP_VERSION_CODE, info.versionCode);
        } catch (NameNotFoundException e) {

        }
    }

    private void buildProperties() {
        // Set phone related properties
        // this.addProperty("Brand", Build.BRAND); // mobile phone carrier
        this.addProperty(PROPS_MODEL, Build.MODEL);
        this.addProperty(PROPS_ANDROID_VERSION, Build.VERSION.RELEASE);

        try {
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();

            this.addProperty(PROPS_WI_FI_ENABLED, supState);
        } catch (Exception e) {

        }


        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }
        this.addProperty(PROPS_MOBILE_DATA_ENABLED, mobileDataEnabled);

        try {
            final LocationManager manager = (LocationManager) this.mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            this.addProperty(PROPS_GPS_ENABLED, gpsEnabled);
        } catch (Exception e) {

        }


        try {
            DisplayMetrics metrics = new DisplayMetrics();

            this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

            String resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
            this.addProperty(PROPS_SCREEN_RESOLUTION, resolution);
        } catch (Exception e) {

        }

        try {
            String activityName = this.mActivity.getClass().getSimpleName();
            this.addProperty(PROPS_ACTIVITY, activityName);
        } catch (Exception e) {

        }
    }

    public Doorbell addProperty(String key, Object value) {
        try {
            this.mProperties.put(key, value);
        } catch (JSONException e) {
            // caught
        }

        return this;
    }

    public Doorbell setAppId(long id) {
        this.mApi.setAppId(id);
        return this;
    }

    public Doorbell setApiKey(String key) {
        this.mApi.setApiKey(key);
        return this;
    }

    public Doorbell setEmailFieldVisibility(int visibility) {
        this.mEmailFieldVisibility = visibility;
        return this;
    }

    public Doorbell setPoweredByVisibility(int visibility) {
        this.mPoweredByVisibility = visibility;
        return this;
    }

    public Doorbell setEmailHint(String emailHint) {
        this.mEmailHint = emailHint;
        return this;
    }

    public Doorbell setMessageHint(String messageHint) {
        this.mMessageHint = messageHint;
        return this;
    }

    public Doorbell setEmail(String email) {
        this.mEmail = email;
        return this;
    }

    public Doorbell setOnShowCallback(OnShowCallback onShowCallback) {
        this.mOnShowCallback = onShowCallback;
        return this;
    }

    public Doorbell setName(String name) {
        this.mName = name;
        return this;
    }

    public Doorbell impression() {
        this.mApi.impression();

        return this;
    }

    public AlertDialog show() {
        this.mApi.open();

        LinearLayout mainLayout = new LinearLayout(this.mContext);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        this.mMessageField = new EditText(this.mContext);
        this.mMessageField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mMessageField.setHint(this.mMessageHint);
        this.mMessageField.setMinLines(2);
        this.mMessageField.setGravity(Gravity.TOP);
        this.mMessageField.setInputType(this.mMessageField.getInputType() | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mainLayout.addView(this.mMessageField);

        this.mEmailField = new EditText(this.mContext);
        this.mEmailField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        this.mEmailField.setHint(this.mEmailHint);
        this.mEmailField.setText(this.mEmail);
        this.mEmailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        this.mEmailField.setVisibility(this.mEmailFieldVisibility);
        mainLayout.addView(this.mEmailField);

        TextView poweredBy = new TextView(this.mContext);
        poweredBy.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        poweredBy.setText(Html.fromHtml(POWERED_BY_DOORBELL_TEXT));
        poweredBy.setPadding(7, 7, 7, 7);
        poweredBy.setVisibility(this.mPoweredByVisibility);
        poweredBy.setMovementMethod(LinkMovementMethod.getInstance());
        mainLayout.addView(poweredBy);


        this.setView(mainLayout);

        this.setPositiveButton(getContext().getString(R.string.drbl__send), null);
        this.setNegativeButton(getContext().getString(R.string.drbl__cancle), null);

        final AlertDialog dialog = super.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Doorbell.this.mApi.setLoadingMessage(getContext().getString(R.string.drbl_sending));
                Doorbell.this.mApi.setCallback(new RestCallback() {
                    @Override
                    public void success(Object obj) {
                        Toast.makeText(Doorbell.this.mContext, obj.toString(), Toast.LENGTH_LONG).show();

                        Doorbell.this.mMessageField.setText("");
                        Doorbell.this.mProperties = new JSONObject();

                        dialog.hide();
                    }
                });
                Doorbell.this.mApi.sendFeedback(Doorbell.this.mMessageField.getText().toString(), Doorbell.this.mEmailField.getText().toString(), Doorbell.this.mProperties, Doorbell.this.mName);
            }
        });

        if (this.mOnShowCallback != null) {
            this.mOnShowCallback.handle();
        }

        return dialog;
    }

}
