package com.example.musicplayer;

import android.net.Uri;

public class Song {

    String title;
    Uri uri, artWorkUri;
    int size, duration;

    public Song(String title, Uri uri, Uri artWorkUri, int size, int duration) {
        this.title = title;
        this.uri = uri;
        this.artWorkUri = artWorkUri;
        this.size = size;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getArtWorkUri() {
        return artWorkUri;
    }

    public void setArtWorkUri(Uri artWorkUri) {
        this.artWorkUri = artWorkUri;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
