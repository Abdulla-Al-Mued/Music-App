package com.example.musicplayer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer player;
    ActivityResultLauncher<String>  recordAudioPermissionLuncher;

    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;

    ConstraintLayout playerView, homeControllerWrapper, headWrapper, artworkWrapper, audioVisualizerWrapper, seekBarWrapper,
            controlWrapper, audioWrapper;

    TextView songNameView, skipPreviousBtn, skipNextButton, playBtn, repeatModeBtn, playListBtn, playerCloseBtn,
    homeSongNameView, homeSkipPreviousBtn, homePlayPauseBtn, homeSkipNextBtn, progressView, durationView;

    CircleImageView artWorkView;
    SeekBar seekBar;

    BarVisualizer audioVisualizer;
    BlurImageView blurImageView;
    int defaultStatusColor;
    int repeatMode = 1;
    boolean isBond = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        defaultStatusColor = getWindow().getStatusBarColor();
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));

        recyclerView = findViewById(R.id.rec_view);
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{
           if (granted){
               fetchSongs();
           }
           else{
               userResponse();
           }
        });

        //launch storage permission on create
        //storagePermissionLauncher.launch(permission);

        //record audio permission
        recordAudioPermissionLuncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted->{

            if (granted && player.isPlaying()){
                activateAudioVisualizer();
            }
            else {
                userResponseOnRecordAudioPerm();
            }

        });

        //player =new ExoPlayer.Builder(this).build();

        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipNextButton = findViewById(R.id.next);
        skipPreviousBtn = findViewById(R.id.previous);
        playBtn = findViewById(R.id.playPause);
        repeatModeBtn = findViewById(R.id.repeat);
        playListBtn = findViewById(R.id.songList);
        audioVisualizer = findViewById(R.id.visualizer);

        homeSongNameView = findViewById(R.id.title);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayBtn);

        homeControllerWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artWorkWrapper);
        seekBarWrapper = findViewById(R.id.seekBarWrapper);
        controlWrapper = findViewById(R.id.controllWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);

        artWorkView = findViewById(R.id.artWorkView);
        seekBar = findViewById(R.id.seekBar);
        progressView = findViewById(R.id.start);
        durationView = findViewById(R.id.end);
        audioVisualizer = findViewById(R.id.visualizer);
        blurImageView = findViewById(R.id.blurImgView);

        //playerControls();

        //bind to the player service
        doBindingService();


    }

    private void doBindingService() {

        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        isBond = true;

    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) service;
            player = binder.getPlayerService().player;
            isBond = true;
            storagePermissionLauncher.launch(permission);
            //call player control
            playerControls();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @SuppressLint("SuspiciousIndentation")
    @Override
    public void onBackPressed() {

        if (playerView.getVisibility() == View.VISIBLE)
            playerView.setVisibility(View.GONE);
        else
        super.onBackPressed();
    }

    private void playerControls() {

        SharedPreferences sp = getSharedPreferences("TitleFile", MODE_PRIVATE);
        homeSongNameView.setText(sp.getString("title", ""));

        //song name marquee
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        //exit player view
        playerCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                exitPlayerView();

            }
        });

        playListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitPlayerView();
            }
        });

        homeControllerWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlayerView();
            }
        });

        //player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                //show playing song
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                SharedPreferences sp = getSharedPreferences("TitleFile", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                assert mediaItem.mediaMetadata.title != null;
                editor.putString("title", mediaItem.mediaMetadata.title.toString());
                editor.commit();

                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekBar.setProgress((int) player.getCurrentPosition());
                seekBar.setMax((int) player.getDuration());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle, 0 ,0, 0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle, 0 , 0, 0);

                //show the current artWork
                showCurrentArtWork();
                //update progress position of a current playing song
                updatePlayerPositionProgress();
                //load artwork animation
                artWorkView.setAnimation(loadRotate());
                activateAudioVisualizer();
                updatePlayerColor();

                if (!player.isPlaying()){
                    player.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_READY){
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekBar.setMax((int) player.getDuration());
                    seekBar.setProgress((int) player.getCurrentPosition());

                    playBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle, 0 ,0, 0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle, 0 , 0, 0);

                    //show the current artWork
                    showCurrentArtWork();
                    //update progress position of a current playing song
                    updatePlayerPositionProgress();
                    //load artwork animation
                    artWorkView.setAnimation(loadRotate());
                    activateAudioVisualizer();
                    updatePlayerColor();

                }
                else {
                    playBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle, 0, 0, 0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle, 0,0,0);
                }
            }
        });


        skipNextButton.setOnClickListener(v -> skipNextSong());
        homeSkipNextBtn.setOnClickListener(v -> skipNextSong());

        skipPreviousBtn.setOnClickListener(v -> skipPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(v -> skipPreviousSong());

        playBtn.setOnClickListener(v -> playOrPausePlayer());
        homePlayPauseBtn.setOnClickListener(v -> playOrPausePlayer());

        //seekbar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                 progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (player.getPlaybackState() == ExoPlayer.STATE_READY){
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }

            }
        });

        repeatModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatMode == 1){
                    player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                    repeatMode = 2;
                    repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one, 0,0,0);

                }
                else if (repeatMode == 2){
                    player.setShuffleModeEnabled(true);
                    player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                    repeatMode = 3;
                    repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle, 0,0,0);
                }
                else if (repeatMode ==3){
                    player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                    player.setShuffleModeEnabled(false);
                    repeatMode = 1;
                    repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat, 0,0,0);
                }
            }
        });


    }

    private void playOrPausePlayer() {
        if (player.isPlaying()){
            player.pause();
            playBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle, 0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle,0,0,0);
            artWorkView.clearAnimation();
        }
        else {
            player.play();
            playBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle, 0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle,0,0,0);
            artWorkView.startAnimation(loadRotate());
        }

        //update player colors
        updatePlayerColor();

    }

    private void skipNextSong() {
        if (player.hasNextMediaItem()){
            player.seekToNext();
        }
        else {

        }
    }
    private void skipPreviousSong() {
        if (player.hasPreviousMediaItem()){
            player.seekToPrevious();
        }else {

        }
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()){
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekBar.setProgress((int) player.getCurrentPosition());
                }

                //repeat calling method
                updatePlayerPositionProgress();

            }
        }, 1000);
    }

    private Animation loadRotate() {

        RotateAnimation rotateAnimation = new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);

        return rotateAnimation;
    }

    String getReadableTime(int duration) {

        String time;

        int hrs = duration/(1000*60*60);
        int min = (duration%(1000*60*60))/(1000*60);
        int secs = (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;

        if (hrs<1){
            time = min +":"+secs;
        }
        else
            time = hrs +":"+ min +":"+secs;

        return time;
    }

    private void showCurrentArtWork() {
        //artWorkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);

        if (artWorkView.getDrawable() == null){
            artWorkView.setImageResource(R.drawable.sparkle);
        }
    }


    private void showPlayerView() {

        playerView.setVisibility(View.VISIBLE);
        updatePlayerColor();

    }

    private void updatePlayerColor() {

        if (playerView.getVisibility() == View.GONE)
            return;

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artWorkView.getDrawable();
        if (bitmapDrawable == null){
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.sparkle);
        }

        assert bitmapDrawable != null;
        Bitmap bitmap = bitmapDrawable.getBitmap();

        //set bitmap to blur image
        blurImageView.setImageBitmap(bitmap);
        blurImageView.setBlur(4);

        Palette.from(bitmap).generate(palette -> {
            if (palette != null){
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();

                if (swatch == null){
                    swatch = palette.getMutedSwatch();

                    if (swatch == null){
                        swatch = palette.getDominantSwatch();
                    }
                }

                //extract text colors
                int titleColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rgbColor = swatch.getRgb();

                //set colors to the views

                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);

                songNameView.setTextColor(titleColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);

                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipNextButton.getCompoundDrawables()[0].setTint(bodyTextColor);
                playBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                playListBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
            }
        });

    }

    private void exitPlayerView() {

        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

    }

    private void userResponseOnRecordAudioPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (shouldShowRequestPermissionRationale(recordAudioPermission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show Audio Visualizer")
                        .setMessage("Allow this app to display audio visualizer when music is playing")
                        .setPositiveButton("allow", (dialog, which) -> recordAudioPermissionLuncher.launch(recordAudioPermission))
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            }
            else {
                Toast.makeText(getApplicationContext(), "You denied to show visualizer", Toast.LENGTH_LONG);
            }
        }
    }

    private void activateAudioVisualizer() {

        //check if we have audio permission

        if (ContextCompat.checkSelfPermission(this, recordAudioPermission) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        //set audio visualizer
        audioVisualizer.setColor(ContextCompat.getColor(this, R.color.secondary_color));
        audioVisualizer.setDensity(20);;
        audioVisualizer.setPlayer(player.getAudioSessionId());

    }

    private void userResponse() {

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
            fetchSongs();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (shouldShowRequestPermissionRationale(permission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting Permission")
                        .setMessage("Allow Us to fetch songs from your device")
                        .setPositiveButton("allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                storagePermissionLauncher.launch(permission);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(), "you denied us to show songs", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        }
        else {
            Toast.makeText(this, "You canceled to show songs", Toast.LENGTH_LONG).show();
        }

    }

    private void fetchSongs() {

        List<Song> songs = new ArrayList<>();
        Uri uri;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        else {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection =  new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";


        //get the song
        try(Cursor cursor = getContentResolver().query(uri, projection, null, null , sortOrder)){

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            //clear the previous loaded before added

            while (cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                int albumId = cursor.getInt(albumIdColumn);

                //song uri

                Uri uri1 = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri albumArtWorkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                name = name.substring(0, name.lastIndexOf("."));

                Song song = new Song(name, uri1, albumArtWorkUri, size, duration);

                songs.add(song);

            }

            showSongs(songs);

        }



    }

    private void showSongs(List<Song> songs) {

        if (songs.size() == 0){
            Toast.makeText(this, "no songs", Toast.LENGTH_SHORT).show();
            return;
        }

        allSongs.clear();
        allSongs.addAll(songs);

        String title = getResources().getString(R.string.app_name) + " - "+songs.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        songAdapter = new SongAdapter(this, songs, player, playerView);
        recyclerView.setAdapter(songAdapter);


        //animation recview

        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(400);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);

    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {

        getMenuInflater().inflate(R.menu.search_btn, menu);

        MenuItem menuItem = menu.findItem(R.id.search_btn);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) menuItem.getActionView();

        searchSong(searchView);

        return super.onCreateOptionsMenu(menu);
    }

    private void searchSong(androidx.appcompat.widget.SearchView searchView) {

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //release the player
//        if (player.isPlaying())
//            player.stop();
//        player.release();

        doUnbindService();
    }

    private void doUnbindService() {
        if (isBond){
            unbindService(playerServiceConnection);
            isBond = false;
        }
    }

    private void filterSongs(String newText) {
        List<Song> filterSong = new ArrayList<>();

        if (allSongs.size()>0){
            for (Song song: allSongs){
                if (song.getTitle().toLowerCase().contains(newText)){
                    filterSong.add(song);
                }
            }
            if (songAdapter != null){
                songAdapter.filterSongs(filterSong);
            }
        }
    }
}