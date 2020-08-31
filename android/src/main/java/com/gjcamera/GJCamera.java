package com.gjcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.IntStream;

public class GJCamera extends AppCompatActivity {

    private ImageButton btnCapture;
    private TextureView textureView;
    private static Promise promise;

    // Parameters
    private static int iso;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private ImageReader imageReader;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    // Exposurre
    Range<Integer> exposureRanges;
    private int exposureAdjustment;

    // Focus
    private boolean isManualFocusSupported;
    private float focus = 0f;

    // FPS
    private Range<Integer> fps;

    // ISO
    private int seekIso = 200;//, seekSs = 2000;
    private long seekSs = 117162276;
    private float seekFocus = 0.0f;

    // Resolution
    //Capture image with custom size
    private int width = 1600;
    private int height = 1200;

    SurfaceTexture texture;

    private static GJCamera sSoleInstance;

    private GJCamera(){}  //private constructor.

    public static GJCamera getInstance(){
        if (sSoleInstance == null){ //if there is no instance available... create new one
            sSoleInstance = new GJCamera();
        }

        return sSoleInstance;
    }

    CameraDevice.StateCallback SCB = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            setPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            returnHome();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            cameraDevice = camera;
            returnHome();
        }
    };

    public void setPromise(Promise promise) {
        this.promise = promise;
    }

    public void setFps( Range<Integer> fps) { this.fps = fps; }

    public void setISO(int iso) { this.seekIso = iso; }

    public void setExposure(int exposureAdjustment) { this.exposureAdjustment = exposureAdjustment; }

    public void setResolution(int width, int height) { this.width = width; this.height = height; }

    public void setFocus(float focus) { this.focus = focus; }

    public  List<Size> getAvailableResolutions() {
        List<Size> outputSizes = new ArrayList<Size>();

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            outputSizes = Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return outputSizes;
    }

    public boolean checkIsManualFocusSupported() {
        boolean mIsManualFocusSupported = false;

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            int[] capabilities = characteristics
                .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

            mIsManualFocusSupported = IntStream.of(capabilities)
                .anyMatch(x -> x == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return mIsManualFocusSupported;
    }


    public Range<Integer>[] getFpsRanges() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Get supported fps for this camera
            Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Log.d("FPS", "SYNC_MAX_LATENCY_PER_FRAME_CONTROL: " + Arrays.toString(fpsRanges));

            return fpsRanges;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Range<Integer> getExposureRanges() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // Get supported exposures for this camera
            // Get supported exposure for this camera
            Range<Integer> exposureRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            Log.d("EXPOSURES", ": " + Arrays.toString(exposureRanges));

            return exposureRanges;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gjcamera);

        textureView = findViewById(R.id.textureView);
        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        btnCapture = findViewById(R.id.clickButton);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
            }
        });
    }

    private String Double(int number) {
        return (number < 10 ? "0"+number : "" + number);
    }

    private void saveImage() {
        if (cameraDevice == null)
            return;
        CameraManager manager = null;
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);


            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size largestRaw = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.RAW_SENSOR)),
                    new CompareSizesByArea());

            // Save as RAW
            imageReader = ImageReader.newInstance(largestRaw.getWidth(),
                    largestRaw.getHeight(), ImageFormat.RAW_SENSOR, /*maxImages*/ 5);

           // imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(imageReader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            file = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera/IMG_" + calendar.get(Calendar.YEAR) + Double(calendar.get(Calendar.MONTH) +1) + Double(calendar.get(Calendar.DATE)) + "_" + Double(calendar.get(Calendar.HOUR_OF_DAY)) + Double(calendar.get(Calendar.MINUTE)) + Double(calendar.get(Calendar.SECOND)) + ".jpg");
            ImageReader.OnImageAvailableListener readerListener = null;
            readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReadr) {
                    Image image = null;
                    try {
                        image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        {
                            if (image != null)
                                image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    } finally {
                        if (outputStream != null)
                            outputStream.close();
                    }

                }
            };

            imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(GJCamera.this, "Saved " + file, Toast.LENGTH_SHORT).show();
                    returnHome();
                }
            };


            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
//                        cameraDevice.close();
                        //convertFileToWritableMap(file));
//                        promise = null;

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void returnHome() {
        texture.release();
//        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (cameraCaptureSessions != null) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        if (promise != null) {
            promise.resolve(convertFileToWritableMap(file));
            promise = null;
            finish();
        }

    }

    private void setPreview() {
        try {
            texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(1600, 1200);
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(GJCamera.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();

        // How to set focus manually if available capability
        /*
            https://stackoverflow.com/questions/42901334/manual-focus-using-android-camera2-api
        */
        if (isManualFocusSupported) {
             captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focus);
        }

        ///////////////////////////
        // How to set FPS
        // Set the frame rate of the preview screen. Select a frame rate range depending on the actual situation.
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fps);

        ///////////////////////////
        // How to set exposure
        // https://stackoverflow.com/questions/53514188/change-control-ae-exposure-compensation-propostional-to-seekbar-progress-in-came
        int minExposure = exposureRanges.getLower();
        int maxExposure = exposureRanges.getUpper();

        float newCalculatedValue = 0;
        if (exposureAdjustment >= 0) {
            newCalculatedValue = (float) (minExposure * exposureAdjustment);
        } else {
            newCalculatedValue = (float) (maxExposure * -1 * exposureAdjustment);
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, (int) newCalculatedValue);
        
        // https://github.com/mohankumar-s/android_camera2_manual/blob/master/Camera2ManualFragment.java
        // ISO
        captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, seekIso);

        /////
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            exposureRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

            isManualFocusSupported = this.checkIsManualFocusSupported();

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            //Check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, SCB, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            startCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable())
            startCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public static WritableMap convertFileToWritableMap(File fileLocation) {
        WritableMap newFile = Arguments.createMap();

        if (fileLocation == null) return newFile;

        newFile.putString("imgPath", fileLocation.getPath());
        return newFile;
    }

    /**
     * Comparator based on area of the given {@link Size} objects.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}

