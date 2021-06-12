package com.intvill.autovideoplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoListAdapter extends ArrayAdapter<String> {
    public VideoListAdapter(Context context, ArrayList<String> paths) {
        super(context, 0, paths);
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String path = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.video_list_item, parent, false);
        }

        ImageView thumbnailImage = convertView.findViewById(R.id.item_thumbnail);
        TextView nameText = convertView.findViewById(R.id.item_name);
        TextView pathText = convertView.findViewById(R.id.item_path);

        String name = path.substring(path.lastIndexOf('/') + 1);

        pathText.setText(path);
        nameText.setText(name);
        thumbnailImage.setImageBitmap(retriveVideoFrameFromVideo(path));

        return convertView;
    }

    public Bitmap retriveVideoFrameFromVideo(String videoPath){
        Bitmap bitmap = null;

        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());

            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mediaMetadataRetriever != null)
                mediaMetadataRetriever.release();
        }
        return bitmap;
    }
}
