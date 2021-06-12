package com.intvill.autovideoplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_VIDEO = 1;

    private FloatingActionButton addVideoButoon;
    private RelativeLayout welcomeScreen;

    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        addVideoButoon = findViewById(R.id.button_add_video);
        GridView videoListView = findViewById(R.id.video_list_view);
        pref = getApplicationContext().getSharedPreferences("video", 0);
        welcomeScreen = findViewById(R.id.welcome_screen);
        welcomeScreen.setVisibility(View.VISIBLE);
        addVideoButoon.setVisibility(View.GONE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                welcomeScreen.setVisibility(View.GONE);
                addVideoButoon.setVisibility(View.VISIBLE);
                Objects.requireNonNull(getSupportActionBar()).show();
            }
        }, 2000);

        String pathList = pref.getString("videoList", null);

        final ArrayList<String> list = new ArrayList<>();

        int rc = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int rc1 = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);

        if(pathList!=null){
            String[] items = pathList.split("~`~");
            list.addAll(Arrays.asList(items));
        }
        VideoListAdapter adapter = new VideoListAdapter(this, list);
        videoListView.setAdapter(adapter);

        videoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String path = list.get(position);
                Intent intent = new Intent(view.getContext(), VideoPlayerActivity.class);
                intent.putExtra("video_path", path);
                startActivity(intent);
            }
        });

        videoListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String path = list.get(position);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Auto Videoplayer")
                        .setMessage("Are you sure you want to remove this video?")

                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String savedVideoList = pref.getString("videoList", null);
                                SharedPreferences.Editor editor = pref.edit();

                                assert savedVideoList != null;
                                String updatedList = savedVideoList.replace(path+"~`~", "");

                                editor.putString("videoList", updatedList);
                                editor.apply();

                                finish();
                                startActivity(getIntent());
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });

        addVideoButoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, SELECT_VIDEO);
            }
        });

        if (rc == PackageManager.PERMISSION_DENIED && rc1 == PackageManager.PERMISSION_DENIED){
            requestPermissions();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_VIDEO) {
                String selectedVideoPath = getPath(data.getData());
                if(selectedVideoPath == null) {
                    Log.e("error","selected video path = null!");
                    finish();
                } else {
                    String savedVideoList = pref.getString("videoList", "");
                    if(!isDuplicate(selectedVideoPath, savedVideoList)){
                        StringBuilder videoList = new StringBuilder();

                        SharedPreferences.Editor editor = pref.edit();

                        assert savedVideoList != null;
                        if(!savedVideoList.equals("")) videoList.append(savedVideoList);
                        videoList.append(selectedVideoPath);
                        videoList.append("~`~");

                        editor.putString("videoList", videoList.toString());
                        editor.apply();

                        finish();
                        startActivity(getIntent());
                    } else{
                        Toast.makeText(getApplicationContext(), "Video already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        @SuppressLint("Recycle") Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if(cursor!=null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        else return null;
    }

    public boolean isDuplicate(String path, String list){
        if(list!=null){
            String[] listArray = list.split("~`~");
            for (String s : listArray) {
                if (s.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void requestPermissions() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, 2);
        }

    }
}
