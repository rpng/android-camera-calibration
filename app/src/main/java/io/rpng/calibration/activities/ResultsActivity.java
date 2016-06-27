package io.rpng.calibration.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import io.rpng.calibration.R;
import io.rpng.calibration.managers.CameraCalibrator;
import io.rpng.calibration.managers.CameraManager;
import io.rpng.calibration.views.AutoFitTextureView;


public class ResultsActivity extends AppCompatActivity {

    private TextView mTextResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Pass to super
        super.onCreate(savedInstanceState);

        // Create our layout
        setContentView(R.layout.activity_results);

        // Add our button listeners
        addButtonListeners();

        // Get the text view we will display our results on
        mTextResults = (TextView) findViewById(R.id.text_results);

        // Run the async calibration
        run_calibration();

    }

    private void addButtonListeners() {

        // When the done button is pressed we should end the result activity
        Button button_done = (Button) findViewById(R.id.button_done);
        button_done.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultsActivity.this.finish();
            }
        });

        // When this is clicked we should save the settings file
        Button button_save = (Button) findViewById(R.id.button_save);
        button_save.setEnabled(false);
        button_save.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    // Taken from the calibration example, will run in async tast
    // https://github.com/Itseez/opencv/blob/master/samples/android/camera-calibration/src/org/opencv/samples/cameracalibration/CameraCalibrationActivity.java#L154-L188
    private void run_calibration() {

        final Resources res = getResources();

        new AsyncTask<Void, Void, Void>() {
            private ProgressDialog calibrationProgress;

            @Override
            protected void onPreExecute() {
                calibrationProgress = new ProgressDialog(ResultsActivity.this);
                calibrationProgress.setTitle("Calibrating");
                calibrationProgress.setMessage("Please Wait");
                calibrationProgress.setCancelable(false);
                calibrationProgress.setIndeterminate(true);
                calibrationProgress.show();
            }

            @Override
            protected Void doInBackground(Void... arg0) {
                try {
                    MainActivity.mCameraCalibrator.calibrate();
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {

                // Dismiss the processing popup
                calibrationProgress.dismiss();

                // Display our text with info
                if (MainActivity.mCameraCalibrator.isCalibrated()) {

                    // Get results from calibrator
                    double rms = MainActivity.mCameraCalibrator.getAvgReprojectionError();
                    Mat cal_mat = MainActivity.mCameraCalibrator.getCameraMatrix();
                    Mat cal_dist = MainActivity.mCameraCalibrator.getDistortionCoefficients();

                    // Get actual camera intrinsic values (api 23 and greater)
                    // https://developer.android.com/reference/android/hardware/camera2/CaptureResult.html#LENS_INTRINSIC_CALIBRATION
                    float[] intrinsic = MainActivity.mCameraManager.getIntrinsic();
                    Mat dev_mat = new Mat();
                    Mat.eye(3, 3, CvType.CV_64FC1).copyTo(dev_mat);
                    dev_mat.put(0,0, intrinsic[0]); // f_x
                    dev_mat.put(1,1, intrinsic[1]); // f_y
                    dev_mat.put(0,2, intrinsic[2]); // c_x
                    dev_mat.put(1,2, intrinsic[3]); // c_y
                    dev_mat.put(0,1, intrinsic[4]); // skew

                    // Get actual camera distortion values (radtan)
                    // https://developer.android.com/reference/android/hardware/camera2/CaptureResult.html#LENS_INTRINSIC_CALIBRATION
                    float[] distortion = MainActivity.mCameraManager.getDistortion();
                    Mat dev_dist = new Mat();
                    Mat.zeros(4, 1, CvType.CV_64FC1).copyTo(dev_dist);
                    dev_dist.put(0,0, distortion[0]); // kappa_0
                    dev_dist.put(1,0, distortion[1]); // kappa_1
                    dev_dist.put(2,0, distortion[2]); // kappa_2
                    dev_dist.put(3,0, distortion[3]); // kappa_3

                    // Display the values
                    mTextResults.setText("Calibration was successful.\n\n" +
                            "Average Reprojection Error:\n" + rms + "\n\n" +
                            "Calibration Matrix:\n" + cal_mat.dump() + "\n\n" +
                            "Calibration Distortion:\n" + cal_dist.dump() + "\n\n" +
                            "Device Matrix:\n" + dev_mat.dump() + "\n\n" +
                            "Device Distortion:\n" + dev_dist.dump() + "\n\n"
                    );
                } else {
                    // Get results from calibrator
                    double rms = MainActivity.mCameraCalibrator.getAvgReprojectionError();
                    Mat mat = MainActivity.mCameraCalibrator.getCameraMatrix();
                    Mat dist = MainActivity.mCameraCalibrator.getDistortionCoefficients();
                    // Display the values
                    mTextResults.setText("Unable to preform calibration.\n\n\n" +
                            "Average Reprojection Error:\n" + rms + "\n\n" +
                            "Calibration Matrix:\n" + mat.dump() + "\n\n" +
                            "Distortion Coefficients:\n" + dist.dump());
                }


                // Reset everything
                MainActivity.mCameraCalibrator.clearCorners();

            }
        }.execute();
    }

}
