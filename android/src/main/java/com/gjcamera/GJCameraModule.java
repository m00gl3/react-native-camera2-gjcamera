package com.gjcamera;
import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.util.Range;
import android.util.Size;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GJCameraModule extends ReactContextBaseJavaModule {


    public GJCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public Range<Integer>[] getFpsRanges() {
        GJCamera gjc = GJCamera.getInstance();
        return gjc.getFpsRanges();
    }

    @ReactMethod
    public Range<Integer> getExposureRanges() {
        GJCamera gjc = GJCamera.getInstance();
        return gjc.getExposureRanges();
    }

    @ReactMethod
    public List<Size> getAvailableResolutions() {
        GJCamera gjc = GJCamera.getInstance();
        return gjc.getAvailableResolutions();
    }
    
    @ReactMethod
    public boolean isManualFocusSupported() {
        GJCamera gjc = GJCamera.getInstance();
        return gjc.checkIsManualFocusSupported();
    }

    @ReactMethod
    public void openCamera(final Promise promise, final  Range<Integer> fps, final int exposureAdjustment, final int iso, final int width, final int height, final float focus) {
        Activity currentActivity = getCurrentActivity();
        GJCamera gjc = GJCamera.getInstance();
        gjc.setPromise(promise);

        // Set ISO
        gjc.setISO(iso);

        // Set FPS
        gjc.setFps(fps);

        // Set Exposure
        gjc.setExposure(exposureAdjustment);

        // Set resolution
        gjc.setResolution(width, height);

        if (gjc.checkIsManualFocusSupported()) {
            gjc.setFocus(focus);
        }

        Intent intent = new Intent(getCurrentActivity(), GJCamera.class);
        currentActivity.startActivity(intent);
    }

    @Override
    public String getName() {
        return "GJCamera";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        String android_id = Settings.System.getString(getReactApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        constants.put("uniqueId", android_id);
        return constants;
    }
}


