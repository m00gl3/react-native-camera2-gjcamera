package com.gjcamera;

/*
{
	"fps": "[10, 30]",
	"focus": 5,
	"focusSeekbar": 50,
	"isoSeekbar": 50,
	"ISO": 3250,
	"exposure": 15980529513,
	"exposureSeekbar": 50,
	"resolution": "[4224x3120]"
}
 */
public class Params {
    private String fps;
    private float focus;
    private int focusSeekbar;
    private int isoSeekbar;
    private int ISO;
    private long exposure;
    private int exposureSeekbar;

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public float getFocus() {
        return focus;
    }

    public void setFocus(float focus) {
        this.focus = focus;
    }

    public int getFocusSeekbar() {
        return focusSeekbar;
    }

    public void setFocusSeekbar(int focusSeekbar) {
        this.focusSeekbar = focusSeekbar;
    }

    public int getIsoSeekbar() {
        return isoSeekbar;
    }

    public void setIsoSeekbar(int isoSeekbar) {
        this.isoSeekbar = isoSeekbar;
    }

    public int getISO() {
        return ISO;
    }

    public void setISO(int ISO) {
        this.ISO = ISO;
    }

    public long getExposure() {
        return exposure;
    }

    public void setExposure(long exposure) {
        this.exposure = exposure;
    }

    public int getExposureSeekbar() {
        return exposureSeekbar;
    }

    public void setExposureSeekbar(int exposureSeekbar) {
        this.exposureSeekbar = exposureSeekbar;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    private String resolution;
}
