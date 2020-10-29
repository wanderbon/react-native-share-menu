package com.alinz.parkerdan.shareextension;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ShareModule extends ReactContextBaseJavaModule {
    // Events
    final String NEW_SHARE_EVENT = "NewShareEvent";

    // Keys
    final String MIME_TYPE_KEY = "mimeType";
    final String DATA_KEY = "data";

    private ReactContext mReactContext;
    private Class mClass;

    public ShareModule(ReactApplicationContext reactContext, Class mClass) {
        super(reactContext);
        mReactContext = reactContext;
        this.mClass = mClass;
    }

    @NonNull
    @Override
    public String getName() {
        return "ShareMenu";
    }

    @Nullable
    private ReadableMap extractShared(Intent intent)  {
        String type = intent.getType();

        if (type == null) {
            return null;
        }

        String action = intent.getAction();

        WritableMap data = Arguments.createMap();
        data.putString(MIME_TYPE_KEY, type);

        if (Intent.ACTION_SEND.equals(action)) {
            if ("text/plain".equals(type)) {
                data.putString(DATA_KEY, intent.getStringExtra(Intent.EXTRA_TEXT));
                return data;
            }

            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                data.putString(DATA_KEY, fileUri.toString());
                return data;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (fileUris != null) {
                WritableArray uriArr = Arguments.createArray();
                for (Uri uri : fileUris) {
                    uriArr.pushString(uri.toString());
                }
                data.putArray(DATA_KEY, uriArr);
                return data;
            }
        }

        return null;
    }

    @ReactMethod
    public void getSharedText(Callback successCallback) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            successCallback.invoke(null);
            return;
        }

        Intent intent = currentActivity.getIntent();

        ReadableMap shared = extractShared(intent);
        successCallback.invoke(shared);
        clearSharedText();
    }

    private void dispatchEvent(ReadableMap shared) {
        if (mReactContext == null || !mReactContext.hasActiveCatalystInstance()) {
            return;
        }

        mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(NEW_SHARE_EVENT, shared);
    }

    public void clearSharedText() {
        Activity mActivity = getCurrentActivity();

        if(mActivity == null) { return; }

        Intent intent = mActivity.getIntent();
        String type = intent.getType();

        if (type == null) {
            return;
        }

        if ("text/plain".equals(type)) {
            intent.removeExtra(Intent.EXTRA_TEXT);
            return;
        }

        intent.removeExtra(Intent.EXTRA_STREAM);
    }


    @ReactMethod
    public void close() {
        Activity activity = getCurrentActivity();

        if(activity != null && activity.toString().contains("ShareActivity")) {
            activity.finish();
        }
    }

    @ReactMethod
    public void continueInApp(String data, String mimeType, String extraData) {
        Intent intent = new Intent(mReactContext, mClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        WritableMap extraMap = Arguments.createMap();
        extraMap.putString("text", extraData);

        WritableMap writableMap = Arguments.createMap();

        writableMap.putString("data", data);
        writableMap.putString("mimeType", mimeType);
        writableMap.putMap("extraData", extraMap);

        dispatchEvent(writableMap);

        mReactContext.startActivity(intent);

        close();
    }

    @ReactMethod
    public void data(Promise promise) {
        promise.resolve(processIntent());
    }

    public WritableMap processIntent() {
        WritableMap map = Arguments.createMap();

        String value = "";
        String type = "";
        String action = "";

        Activity currentActivity = getCurrentActivity();

        if (currentActivity != null) {
            Intent intent = currentActivity.getIntent();
            action = intent.getAction();
            type = intent.getType();
            if (type == null) {
                type = "";
            }
            if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
                value = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else if (Intent.ACTION_SEND.equals(action) && ("image/*".equals(type) || "image/jpeg".equals(type) || "image/png".equals(type) || "image/jpg".equals(type) ) ) {
                Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                value = "file://" + RealPathUtil.getRealPathFromURI(currentActivity, uri);

            } else {
                value = "";
            }
        } else {
            value = "";
            type = "";
        }

        map.putString("mimeType", type);
        map.putString("data", value);

        return map;
    }

    public static String getShareApp(Context context) {

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        String value = "";

        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.contains("share")) {
                value = packageInfo.packageName;

                break;
            }
        }

        return value;
    }
}