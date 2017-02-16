package com.example.armodule;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * controller for android camera object.
 *
 * @author qba
 */
public class CameraController {

    /**
     * android camera object.
     */
    private static Camera camera;
    /**
     * The m last preview data.
     */
    private static byte[] mLastPreviewData;
    /**
     * The m vertical fov.
     */
    private static float mVerticalFOV;
    /**
     * The m horizontal fov.
     */
    private static float mHorizontalFOV;
    /**
     * The camera controller.
     */
    private static CameraController cameraController;
    /**
     * context of activity that uses camera.
     */
    private Context context;
    /**
     * phone screen resolution.
     */
    private Point screenResolution;
    /**
     * best camera resolution.
     */
    private Point cameraResolution;
    /**
     * The camera preview callback.
     */
    private PreviewCallback cameraPreviewCallback = new PreviewCallback() {

        public void onPreviewFrame(byte[] arg0, Camera arg1) {
            mLastPreviewData = arg0;
        }
    };

    /**
     * Constructor for CameraController class.
     *
     * @param ctx context of calling activity
     */
    private CameraController(Context ctx) {
        this.context = ctx;

    }

    /**
     * initialize camera controller.
     *
     * @param ctx context of calling activity
     */
    public static void init(Context ctx) {
        if (cameraController == null) {
            cameraController = new CameraController(ctx);
            camera = Camera.open();
            mHorizontalFOV = camera.getParameters().getHorizontalViewAngle();
            mVerticalFOV = camera.getParameters().getVerticalViewAngle();
        }
    }

    /**
     * Gets the controller.
     *
     * @return the controller
     */
    public static CameraController getController() {
        return cameraController;
    }

    /**
     * Gets the last preview data.
     *
     * @return the last preview data
     */
    public static byte[] getLastPreviewData() {
        return mLastPreviewData;
    }

    /**
     * Gets the vertical fov.
     *
     * @return the vertical fov
     */
    public static float getVerticalFOV() {
        return mVerticalFOV;
    }

    /**
     * Gets the horizontal fov.
     *
     * @return the horizontal fov
     */
    public static float getHorizontalFOV() {
        return mHorizontalFOV;
    }

    /**
     * opens camera and set surface holder for it.
     *
     * @param holder surface holder for camera
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void cameraOpen(SurfaceHolder holder) throws IOException {
        if (camera == null) {
            camera = Camera.open();
        }
        if (camera == null) {
            Log.e("Camera error:", "camera.open() returned null");
            throw new IOException();
        }
        camera.setPreviewDisplay(holder);
        changeParams();
    }

    /**
     * Change params.
     */
    public void changeParams() {
        initResolutionsFromParams(camera);
        setBestResolutionParams(camera);
    }

    /**
     * release android camera object.
     */
    public void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * start camera preview.
     */
    public void startPreview() {
        if (camera != null) {
            camera.startPreview();
            camera.setPreviewCallback(cameraPreviewCallback);
        }
    }

    /**
     * stop camera preview.
     */
    public void stopPreview() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }
    }

    /**
     * sets camera resolution and best picture size.
     *
     * @param camera camera object
     */
    private void initResolutionsFromParams(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        screenResolution = new Point(dm.widthPixels, dm.heightPixels);
        cameraResolution = findBestPreviewOrPictureSize(
                params.getSupportedPreviewSizes(), screenResolution, false);
    }

    /**
     * Sets the best resolution params.
     *
     * @param camera android camera object
     */
    private void setBestResolutionParams(Camera camera) {
        Camera.Parameters params = camera.getParameters();
        params.set("jpeg-quality", "100");
        params.setPictureFormat(ImageFormat.JPEG);
        params.setPreviewSize(cameraResolution.x, cameraResolution.y);
        mVerticalFOV = params.getVerticalViewAngle();
        mHorizontalFOV = params.getHorizontalViewAngle();
        camera.setParameters(params);
    }

    /**
     * find best preview size or picture size. Best mean closest to screen
     * resolution in that case.
     *
     * @param availablePreviewSizes list containing all available preview sizes
     * @param screenResolution      resolution of phone screen
     * @param isPicture             the is picture
     * @return point (x,y) that contains best resolution
     */
    private Point findBestPreviewOrPictureSize(
            List<Size> availablePreviewSizes, Point screenResolution,
            boolean isPicture) {
        int bestX = 0;
        int bestY = 0;
        int diff = Integer.MAX_VALUE;
        for (Size size : availablePreviewSizes) {
            int newX = 0;
            int newY = 0;
            try {
                newX = size.width;
                newY = size.height;
            } catch (NumberFormatException nfe) {
                Log.e("error finding preview", "number format exception for:"
                        + newX + " " + newY);
                continue;
            }
            // if widest dimension is less than 480 we stop searching
            if (isPicture && Math.max(newX, newY) < 480) {
                break;
            }
            // we looking for size that is closest to screen resolution
            int newDiff = Math.abs(newX - screenResolution.x)
                    + Math.abs(newY - screenResolution.y);
            if (newDiff == 0) {
                bestX = newX;
                bestY = newY;
                break;
            } else if (newDiff < diff) {
                bestX = newX;
                bestY = newY;
                diff = newDiff;
            }
        }
        if (bestX > 0 && bestY > 0) {
            return new Point(bestX, bestY);
        }
        return null;
    }

    /**
     * Gets the camera.
     *
     * @return the camera
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Gets the context.
     *
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the context.
     *
     * @param context the new context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Gets the screen resolution.
     *
     * @return the screen resolution
     */
    public Point getScreenResolution() {
        return screenResolution;
    }

    /**
     * Gets the camera resolution.
     *
     * @return the camera resolution
     */
    public Point getCameraResolution() {
        return cameraResolution;
    }

    /**
     * Sets the callback.
     */
    public void setCallback() {
        if (camera != null) {
            camera.setPreviewCallback(cameraPreviewCallback);
        }
    }

}