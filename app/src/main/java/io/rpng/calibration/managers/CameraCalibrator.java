package io.rpng.calibration.managers;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class CameraCalibrator {
    private static final String TAG = "OCVSample::CameraCalibrator";

    private Activity activity;
    private SharedPreferences sharedPreferences;

    private Size mImageSize;
    private boolean mPatternWasFound = false;
    private MatOfPoint2f mCorners = new MatOfPoint2f();
    private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private boolean mIsCalibrated = false;

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private int captureTries;
    private double mRms;
    private double mSquareSize = 0.0181;

    public CameraCalibrator(Activity activity) {
        // Set our activity and pref manager
        this.activity = activity;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        // Setup non-size dependent configurations
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                Calib3d.CALIB_ZERO_TANGENT_DIST +
                Calib3d.CALIB_FIX_ASPECT_RATIO +
                Calib3d.CALIB_FIX_K4 +
                Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        // Update our image size
        mImageSize = new Size(grayFrame.width(), grayFrame.height());
        // Find the pattern then render it
        findPattern(grayFrame);
        renderFrame(rgbaFrame);
    }

    public void calibrate() {

        Size mPatternSize = this.getPatternSize();
        int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);

        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();
        ArrayList<Mat> objectPoints = new ArrayList<Mat>();
        objectPoints.add(Mat.zeros(mCornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));
        for (int i = 1; i < mCornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, mCornersBuffer, mImageSize, mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, mFlags);

        mIsCalibrated = Core.checkRange(mCameraMatrix) && Core.checkRange(mDistortionCoefficients);

        mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
        Log.i(TAG, String.format("Average re-projection error: %f", mRms));
        Log.i(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Log.i(TAG, "Distortion coefficients: " + mDistortionCoefficients.dump());
    }

    /**
     * Reset all the variables
     */
    public void clearCorners() {
        captureTries = 0;
        mCornersBuffer.clear();
        mIsCalibrated = false;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
    }

    private void calcBoardCornerPositions(Mat corners) {

        Size mPatternSize = this.getPatternSize();
        int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);

        final int cn = 3;
        float positions[] = new float[mCornersSize * cn];

        for (int i = 0; i < mPatternSize.height; i++) {
            for (int j = 0; j < mPatternSize.width * cn; j += cn) {
                positions[(int) (i * mPatternSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2) * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 1)] = i * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints, List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(mDistortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i), mCameraMatrix, distortionCoefficients, cornersProjected);
            error = Core.norm(mCornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError  += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private void findPattern(Mat grayFrame) {
        Size mPatternSize = this.getPatternSize();
        mPatternWasFound = Calib3d.findCirclesGrid(grayFrame, mPatternSize, mCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
        //mPatternWasFound = Calib3d.findChessboardCorners(grayFrame, mPatternSize, mCorners, Calib3d.CALIB_CB_FAST_CHECK);
    }

    public void addCorners() {
        if (mPatternWasFound) {
            mCornersBuffer.add(mCorners.clone());
        }
        captureTries++;
    }

    private void drawPoints(Mat rgbaFrame) {
        Size mPatternSize = this.getPatternSize();
        Calib3d.drawChessboardCorners(rgbaFrame, mPatternSize, mCorners, mPatternWasFound);
    }

    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);
        //Imgproc.putText(rgbaFrame, "Captured: " + mCornersBuffer.size(),
        // new Point(rgbaFrame.cols() / 3 * 2, rgbaFrame.rows() * 0.1), Core.FONT_HERSHEY_SIMPLEX, 0.3, new Scalar(255, 255, 0));
    }

    private Size getPatternSize() {
        // Get the size of the checkered board we are looking for
        String prefCalibSize = sharedPreferences.getString("prefCalibSize", "4x5");
        int widthCalib = Integer.parseInt(prefCalibSize.substring(0,prefCalibSize.lastIndexOf("x")));
        int heightCalib = Integer.parseInt(prefCalibSize.substring(prefCalibSize.lastIndexOf("x")+1));

        // Return our size
        return new Size(widthCalib,heightCalib);
    }

    public Mat getCameraMatrix() {
        return mCameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return mDistortionCoefficients;
    }

    public int getCornersBufferSize() {
        return mCornersBuffer.size();
    }

    public int getCaptureTries() {
        return captureTries;
    }

    public double getAvgReprojectionError() {
        return mRms;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibrated() {
        mIsCalibrated = true;
    }

}
