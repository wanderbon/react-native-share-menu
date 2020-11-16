package com.alinz.parkerdan.shareextension;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ShareModule extends ReactContextBaseJavaModule {
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
        WritableMap map = Arguments.createMap();

        String value = "";
        String type = "";
        String action = "";

        Activity currentActivity = getCurrentActivity();

        if (currentActivity != null) {
            action = intent.getAction();
            type = intent.getType();

            if (type == null) {
                type = "";
            }
            if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
                value = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else if (Intent.ACTION_SEND.equals(action) && ("image/*".equals(type) || "image/jpeg".equals(type) || "image/png".equals(type) || "image/jpg".equals(type) ) ) {
                Uri uri = intent.getExtras().getParcelable(Intent.EXTRA_STREAM);

                if(uri == null) {
                  String extraString = (String) intent.getExtras().get(Intent.EXTRA_STREAM);

                  if(extraString != "" && extraString != null) {
                      uri = Uri.parse(extraString);
                  }
                }

                if(uri != null) {
                    String path = uri.toString();

                    if(path.contains("https://")) {
                        value = path;
                    } else if(path.contains("http://")) {
                        value = path.replace("http://", "https://");
                    } else {
                        value = "file://" + RealPathUtil.getFilePathFromURI(mReactContext, uri);
                    }
                } else {
                    value = "";
                    type = "";
                }
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
    public void data(Promise promise) {
        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();

        promise.resolve(extractShared(intent));
    }
}