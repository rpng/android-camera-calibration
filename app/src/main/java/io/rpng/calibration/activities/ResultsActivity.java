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
                    Mat mat = MainActivity.mCameraCalibrator.getCameraMatrix();
                    Mat dist = MainActivity.mCameraCalibrator.getDistortionCoefficients();
                    // Display the values
                    mTextResults.setText("Calibration was successful.\n\n" +
                            "Average Reprojection Error:\n" + rms + "\n\n" +
                            "Calibration Matrix:\n" + mat.dump() + "\n\n" +
                            "Distortion Coefficients:\n" + dist.dump());
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
