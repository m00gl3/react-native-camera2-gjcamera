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

import com.google.gson.Gson;

public class GJCameraModule extends ReactContextBaseJavaModule {


    public GJCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void getFpsRanges(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        Range<Integer>[] result = gjc.getFpsRanges(this.getReactApplicationContext());

        String outputArray = "";

        for (int i = 0; i < result.length; i++) {
            String arrayElement = result[i].toString();
            outputArray += arrayElement;

            if (i < result.length - 1)
                outputArray += "|";
        }

        promise.resolve(outputArray);
    }

    @ReactMethod
    public void getExposureRanges(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        Range<Long> result = gjc.getExposureRanges(this.getReactApplicationContext());

        promise.resolve(result.toString());
    }

    @ReactMethod
    public void getIsoRanges(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        Range<Integer> result = gjc.getIsoRanges(this.getReactApplicationContext());

        promise.resolve(result.toString());
    }

    @ReactMethod
    public void getAvailableResolutions(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        List<Size> result = gjc.getAvailableResolutions(this.getReactApplicationContext());

        promise.resolve(result.toString());
    }

    @ReactMethod
    public void isManualFocusSupported(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        boolean result = gjc.checkIsManualFocusSupported(this.getReactApplicationContext());

        promise.resolve(result);
    }

    @ReactMethod
    public void getMinimumFocusDistance(final Promise promise) {
        GJCamera gjc = GJCamera.getInstance();
        float result = gjc.getMinimumFocusDistance(this.getReactApplicationContext());

        promise.resolve(result);
    }

    /*  final  Range<Integer> fps,
     final int exposureAdjustment,
     final int iso,
     final int width,
     final int height,
     final float focus
  */

    @ReactMethod
    public void openCamera(final Promise promise) {
        Activity currentActivity = getCurrentActivity();
        GJCamera gjc = GJCamera.getInstance();
        gjc.setPromise(promise);

        Intent intent = new Intent(getCurrentActivity(), GJCamera.class);
        currentActivity.startActivity(intent);
    }

    // promise must be used as last param
    @ReactMethod
    public void openCameraWithParams(String params, final Promise promise ) {
        Activity currentActivity = getCurrentActivity();
        GJCamera gjc = GJCamera.getInstance();
        gjc.setPromise(promise);

        Gson g = new Gson();

        Params paramsObj = g.fromJson(params, Params.class);

        // Set ISO
        gjc.setISO(paramsObj.getISO());

        // Set Exposure
        gjc.setExposure(paramsObj.getExposure());

        // "[10, 30]"
        // We need to extract Range value here
        String tempFpsRange = paramsObj.getFps();
        tempFpsRange = tempFpsRange.replace("\"", "");
        tempFpsRange = tempFpsRange.replace("[", "");
        tempFpsRange = tempFpsRange.replace("]", "");

        String[] fpsRange = tempFpsRange.split(",");
        int minFpsRange = Integer.valueOf(fpsRange[0]);
        int maxFpsRange = Integer.valueOf(fpsRange[1]);

        // Set FPS
        gjc.setFps(new Range<Integer>(minFpsRange, maxFpsRange));

        // Resolution
        // "[4224x3120]"
        String tempResolution = paramsObj.getResolution();
        tempResolution = tempResolution.replace("\"", "");
        tempResolution = tempResolution.replace("[", "");
        tempResolution = tempResolution.replace("]", "");

        String[] resolution = tempResolution.split("x");
        int width = Integer.valueOf(resolution[0]);
        int height = Integer.valueOf(resolution[1]);

        // Set resolution
        gjc.setResolution(width, height);

        if (gjc.checkIsManualFocusSupported(this.getReactApplicationContext())) {
            gjc.setFocus(paramsObj.getFocus());
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