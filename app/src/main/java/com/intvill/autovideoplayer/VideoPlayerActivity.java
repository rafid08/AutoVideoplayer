package com.intvill.autovideoplayer;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.IOException;

public class VideoPlayerActivity extends AppCompatActivity {
    public int stopPosition;

    VideoView videoView;
    FrameLayout black;

    private static final String TAG = "FaceTracker";
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mPreview = findViewById(R.id.preview);
        videoView = findViewById(R.id.video_view);
        black = findViewById(R.id.black);

        if(!isPortraitMode()){
            mPreview.setRotation(90);
        }

        Bundle extras;
        String videoPath = null;

        if (savedInstanceState == null) {
            extras = getIntent().getExtras();
            if(extras != null) {
                videoPath= extras.getString("video_path");
            }
        }

        int rc = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int rc1 = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED && rc1 == PackageManager.PERMISSION_GRANTED) {
            assert videoPath != null;
            File videoFile = new File(videoPath);
            Uri path=Uri.fromFile(videoFile);
            videoView.setVideoURI(path);

            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });
            videoView.start();
            videoView.seekTo(1);
            videoView.pause();
            videoView.setAlpha(1.0f);

            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    return false;
                }
            });

            stopPosition = videoView.getCurrentPosition();
            createCameraSource();
        } else {
            requestPermissions();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void requestPermissions() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final AppCompatActivity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
    }

    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(5.0f)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.pause();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Auto Videoplayer")
                .setMessage("This App need camera permission")
                .setPositiveButton("ok", listener)
                .show();
    }

    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<com.google.android.gms.vision.face.Face> {
        @Override
        public Tracker<Face> create(com.google.android.gms.vision.face.Face face) {
            return new GraphicFaceTracker();
        }
    }

    private class GraphicFaceTracker extends Tracker<com.google.android.gms.vision.face.Face> {

        GraphicFaceTracker() {
        }

        @Override
        public void onNewItem(int faceId, com.google.android.gms.vision.face.Face item) {
            //TODO: start video
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.seekTo(stopPosition);
                    videoView.start();
//                    black.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onUpdate(FaceDetector.Detections<com.google.android.gms.vision.face.Face> detectionResults, com.google.android.gms.vision.face.Face face) {
            //TODO: resume video
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!videoView.isPlaying()){
                        videoView.seekTo(stopPosition);
                        videoView.start();
//                        black.setVisibility(View.GONE);
                    }
                }
            });
        }

        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            //TODO: pause video
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    videoView.pause();
                    stopPosition = videoView.getCurrentPosition();
                }
            });
        }

        @Override
        public void onDone(){
            //TODO: stop video
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    videoView.pause();
                    stopPosition = videoView.getCurrentPosition();
//                    black.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private boolean isPortraitMode() {
        int orientation = getApplicationContext().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }
}