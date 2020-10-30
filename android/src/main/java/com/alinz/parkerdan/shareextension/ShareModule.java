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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class ShareModule extends ReactContextBaseJavaModule {
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
        WritableMap data = Arguments.createMap();

        data.putString(MIME_TYPE_KEY, "");
        data.putString(DATA_KEY, "");

        if (type == null) {
            return null;
        }

        String action = intent.getAction();


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

        data.putString(MIME_TYPE_KEY, "");
        data.putString(DATA_KEY, "");

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
        Activity activity = getCurrentActivity();
        Intent intent = new Intent(mReactContext, mClass);

        intent.putExtra("data", data);
        intent.putExtra("mimeType", mimeType);
        intent.putExtra("extraData", extraData);

        activity.startActivity(intent);

        close();
    }

    @ReactMethod
    public void data(Promise promise) {
        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();

        promise.resolve(extractShared(intent));
    }
}