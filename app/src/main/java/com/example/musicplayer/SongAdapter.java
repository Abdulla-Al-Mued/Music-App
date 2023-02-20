package com.example.musicplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    Context context;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;

    public SongAdapter(Context context, List<Song> songs, ExoPlayer player, ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item, parent, false);

        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        Song song = songs.get(position);
        SongViewHolder songViewHolder = (SongViewHolder) holder;

        songViewHolder.title.setText(song.getTitle());
        songViewHolder.duration.setText(getDuration(song.getDuration()));
        songViewHolder.size.setText(getSize(song.getSize()));


        Uri artWorkUri = song.getArtWorkUri();

        if (artWorkUri != null){
            songViewHolder.imageView.setImageURI(null);
            //songViewHolder.imageView.setImageURI(artWorkUri);
            //Toast.makeText(context, artWorkUri.toString(), Toast.LENGTH_LONG).show();
            Picasso.with(context).load(songs.get(position).artWorkUri).into(songViewHolder.imageView);

            if (songViewHolder.imageView.getDrawable() == null){
                songViewHolder.imageView.setImageResource(R.drawable.ic_music);
            }

        }

        songViewHolder.itemView.setOnClickListener(v -> {

            //start the player service
            context.startService(new Intent(context.getApplicationContext(), PlayerService.class));

            playerView.setVisibility(View.VISIBLE);

            if (!player.isPlaying()){
                player.setMediaItems(getMediaItems(), position, 0);
            }
            else {
                player.pause();
                player.seekTo(position, 0);
            }

            player.prepare();
            player.play();
            Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();


            //check if the record audio is granted
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){

                //request the record audio
                ((MainActivity)context).recordAudioPermissionLuncher.launch(Manifest.permission.RECORD_AUDIO);

            }

        });

    }

    private List<MediaItem> getMediaItems() {
        //list of media item

        List<MediaItem> mediaItems = new ArrayList<>();

        for (Song song: songs){
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();

            //add media items
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtWorkUri())
                .build();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder{

        ImageView imageView;
        TextView title, duration, size;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.artWorkHolder);
            title = itemView.findViewById(androidx.core.R.id.text);
            duration = itemView.findViewById(R.id.runtime);
            size = itemView.findViewById(R.id.size);

        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }


    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song> filterList){
        songs = filterList;
        notifyDataSetChanged();
    }


    private String getDuration(int totalDuration){

        String totalDurationText;
        int hours = totalDuration/(1000*60*60);
        int min = (totalDuration%(1000*60*60))/(1000*60);
        int sec = (((totalDuration%(1000*60*60))%(1000*60*60))%(1000*60*60))/(1000*60);

        if (hours<1){
            totalDurationText = String.format("%02d:%02d", min, sec);
        }
        else {
            totalDurationText = String.format("%1:%02d:%02d",hours, min, sec);
        }

        return totalDurationText;

    }

    private String getSize(long bytes){

        String size;

        double k = bytes/1024.0;
        double m = (bytes/1024.0)/1024;
        double g = ((bytes/1024.0)/1024)/1024;

        DecimalFormat dec = new DecimalFormat("0.00");

        if (g>1){
            size = dec.format(g).concat(" GB");
        }else if (m>1){
            size = dec.format(m).concat(" MB");
        }
        else if (k>1)
            size = dec.format(k).concat(" KB");
        else
            size = dec.format(g).concat(" Byte");

        return size;
    }





}
